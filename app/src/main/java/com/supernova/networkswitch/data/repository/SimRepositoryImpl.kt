package com.supernova.networkswitch.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.supernova.networkswitch.domain.model.SimInfo
import com.supernova.networkswitch.domain.model.SimQueryResult
import com.supernova.networkswitch.domain.repository.SimRepository
import dagger.hilt.android.qualifiers.ApplicationContext
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

    override suspend fun getAvailableSimCards(): SimQueryResult {
        if (!hasReadPhoneStatePermission()) {
            return SimQueryResult.PermissionDenied
        }

        val manager = subscriptionManager
            ?: return SimQueryResult.Failed(IllegalStateException("SubscriptionManager unavailable"))

        return try {
            val subscriptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                manager.activeSubscriptionInfoList ?: emptyList()
            } else {
                emptyList()
            }

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

    /**
     * Check if READ_PHONE_STATE permission is granted
     */
    private fun hasReadPhoneStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Map SubscriptionInfo to SimInfo domain model
     */
    private fun mapToSimInfo(subscriptionInfo: SubscriptionInfo): SimInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val displayName = buildDisplayName(subscriptionInfo)
                
                SimInfo(
                    subscriptionId = subscriptionInfo.subscriptionId,
                    simSlotIndex = subscriptionInfo.simSlotIndex,
                    displayName = displayName
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Build a user-friendly display name for the SIM card
     */
    private fun buildDisplayName(subscriptionInfo: SubscriptionInfo): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            // Try to get the display name from the subscription
            val carrierName = subscriptionInfo.displayName?.toString()
            val slotIndex = subscriptionInfo.simSlotIndex

            return when {
                // If carrier name exists and slot index is valid
                !carrierName.isNullOrBlank() && slotIndex >= 0 -> {
                    "$carrierName (Slot ${slotIndex + 1})"
                }
                // If only carrier name exists
                !carrierName.isNullOrBlank() -> carrierName
                // If only slot index is valid
                slotIndex >= 0 -> "SIM ${slotIndex + 1}"
                // Fallback
                else -> "SIM ${subscriptionInfo.subscriptionId}"
            }
        }
        return "Unknown SIM"
    }
}
