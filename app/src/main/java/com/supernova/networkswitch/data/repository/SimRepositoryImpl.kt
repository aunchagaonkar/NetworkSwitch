package com.supernova.networkswitch.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.supernova.networkswitch.domain.model.SimInfo
import com.supernova.networkswitch.domain.model.SimQueryResult
import com.supernova.networkswitch.domain.repository.SimRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SimRepository that uses Android's SubscriptionManager
 * to detect and retrieve information about available SIM cards
 */
@Singleton
class SimRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SimRepository {

    private companion object {
        const val TAG = "NetworkSwitch"
    }

    private val subscriptionManager: SubscriptionManager? by lazy {
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
    }

    override suspend fun getAvailableSimCards(): SimQueryResult = querySims()

    /**
     * The platform invokes a newly registered listener once immediately, which supplies
     * the initial emission.
     */
    override fun observeAvailableSimCards(): Flow<SimQueryResult> = callbackFlow {
        val producer = this
        val manager = subscriptionManager
        if (manager == null || !hasReadPhoneStatePermission()) {
            trySend(querySims())
            awaitClose { }
            return@callbackFlow
        }

        val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
            override fun onSubscriptionsChanged() {
                // Reading the subscription list is a binder call, so keep it off the
                // main thread the listener is invoked on.
                producer.launch { trySend(querySims()) }
            }
        }

        val handler = Handler(Looper.getMainLooper())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.addOnSubscriptionsChangedListener(context.mainExecutor, listener)
        } else {
            // The executor-less overload registers against the calling thread's Looper.
            @Suppress("DEPRECATION")
            handler.post { manager.addOnSubscriptionsChangedListener(listener) }
        }

        // Posted to the same Looper as the pre-R registration so removal cannot overtake
        // a registration still queued on it, which would leave the listener attached.
        awaitClose { handler.post { manager.removeOnSubscriptionsChangedListener(listener) } }
    }

    private fun querySims(): SimQueryResult {
        if (!hasReadPhoneStatePermission()) {
            return SimQueryResult.PermissionDenied
        }

        val manager = subscriptionManager
            ?: return SimQueryResult.Failed(IllegalStateException("SubscriptionManager unavailable"))

        return try {
            val subscriptions = manager.activeSubscriptionInfoList ?: emptyList()

            SimQueryResult.Loaded(
                subscriptions
                    .mapNotNull { mapToSimInfo(it) }
                    .sortedBy { it.simSlotIndex }
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "SIM query denied", e)
            SimQueryResult.PermissionDenied
        } catch (e: Exception) {
            Log.w(TAG, "SIM query failed", e)
            SimQueryResult.Failed(e)
        }
    }

    private fun hasReadPhoneStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Map SubscriptionInfo to SimInfo domain model.
     *
     * Returns null for a subscription the platform cannot describe, so one unusable
     * entry does not discard the rest of the list.
     */
    private fun mapToSimInfo(subscriptionInfo: SubscriptionInfo): SimInfo? {
        return try {
            SimInfo(
                subscriptionId = subscriptionInfo.subscriptionId,
                simSlotIndex = subscriptionInfo.simSlotIndex,
                displayName = buildDisplayName(subscriptionInfo)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Skipping unreadable subscription", e)
            null
        }
    }

    /**
     * Build a user-friendly display name for the SIM card.
     *
     * A slot index is negative for a subscription that is not tied to a physical slot,
     * such as an inactive eSIM profile.
     */
    private fun buildDisplayName(subscriptionInfo: SubscriptionInfo): String {
        val carrierName = subscriptionInfo.displayName?.toString()
        val slotIndex = subscriptionInfo.simSlotIndex

        return when {
            !carrierName.isNullOrBlank() && slotIndex >= 0 -> "$carrierName (Slot ${slotIndex + 1})"
            !carrierName.isNullOrBlank() -> carrierName
            slotIndex >= 0 -> "SIM ${slotIndex + 1}"
            else -> "SIM ${subscriptionInfo.subscriptionId}"
        }
    }
}
