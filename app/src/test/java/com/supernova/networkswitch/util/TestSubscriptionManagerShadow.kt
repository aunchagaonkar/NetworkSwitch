package com.supernova.networkswitch.util

import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
import java.util.concurrent.Executor
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowSubscriptionManager

/**
 * Extends Robolectric's SubscriptionManager shadow with the two things the stock shadow
 * cannot express: a query that throws, and the identity of the listener the code under
 * test registered.
 *
 * Register it with `@Config(shadows = [TestSubscriptionManagerShadow::class])`.
 */
@Implements(SubscriptionManager::class)
class TestSubscriptionManagerShadow : ShadowSubscriptionManager() {

    @Implementation
    override fun getActiveSubscriptionInfoList(): MutableList<SubscriptionInfo> {
        queryFailure?.let { throw it }
        return super.getActiveSubscriptionInfoList()
    }

    @Implementation
    override fun addOnSubscriptionsChangedListener(listener: OnSubscriptionsChangedListener) {
        registeredListeners += listener
        super.addOnSubscriptionsChangedListener(listener)
    }

    @Implementation
    override fun addOnSubscriptionsChangedListener(
        executor: Executor,
        listener: OnSubscriptionsChangedListener
    ) {
        registeredListeners += listener
        super.addOnSubscriptionsChangedListener(executor, listener)
    }

    companion object {
        /** Thrown by every subscription query while set. */
        @JvmStatic
        var queryFailure: RuntimeException? = null

        /** Every listener passed to either registration overload, in registration order. */
        @JvmStatic
        val registeredListeners = mutableListOf<OnSubscriptionsChangedListener>()

        /**
         * Must be called from the test's own setup: Robolectric only invokes @Resetter
         * on its built-in shadows, not on one registered through `@Config(shadows = ...)`.
         *
         * Named apart from the superclass resetter, whose JVM signature it would
         * otherwise collide with, so it has to clear the inherited state itself.
         */
        @JvmStatic
        fun resetTestState() {
            queryFailure = null
            registeredListeners.clear()
            reset()
        }
    }
}
