package com.supernova.networkswitch.data.repository

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Looper
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import com.supernova.networkswitch.domain.model.SimInfo
import com.supernova.networkswitch.domain.model.SimQueryResult
import com.supernova.networkswitch.util.TestSubscriptionManagerShadow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowSubscriptionManager

/**
 * Unit tests for SimRepositoryImpl, driving the real SubscriptionManager call path
 * through Robolectric rather than a mocked repository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [TestSubscriptionManagerShadow::class])
class SimRepositoryImplTest {

    private lateinit var application: Application
    private lateinit var repository: SimRepositoryImpl

    @Before
    fun setUp() {
        TestSubscriptionManagerShadow.resetTestState()
        application = RuntimeEnvironment.getApplication()
        grantReadPhoneState()
        repository = SimRepositoryImpl(application)
    }

    // region getAvailableSimCards

    @Test
    fun `should report permission denied without READ_PHONE_STATE`() = runTest {
        denyReadPhoneState()

        assertEquals(SimQueryResult.PermissionDenied, repository.getAvailableSimCards())
    }

    @Test
    fun `should report a device with no SIMs as loaded and empty`() = runTest {
        setSubscriptions(emptyList())

        assertEquals(SimQueryResult.Loaded(emptyList()), repository.getAvailableSimCards())
    }

    @Test
    fun `should treat an absent subscription list as empty rather than as a failure`() = runTest {
        assertEquals(SimQueryResult.Loaded(emptyList()), repository.getAvailableSimCards())
    }

    @Test
    fun `should map a single SIM with its carrier name and one-based slot`() = runTest {
        setSubscriptions(listOf(subscription(id = 7, slot = 0, name = "Airtel")))

        assertEquals(
            SimQueryResult.Loaded(
                listOf(SimInfo(subscriptionId = 7, simSlotIndex = 0, displayName = "Airtel (Slot 1)"))
            ),
            repository.getAvailableSimCards()
        )
    }

    @Test
    fun `should order SIMs by slot regardless of the platform's ordering`() = runTest {
        setSubscriptions(
            listOf(
                subscription(id = 2, slot = 1, name = "Jio"),
                subscription(id = 1, slot = 0, name = "Airtel")
            )
        )

        val loaded = repository.getAvailableSimCards() as SimQueryResult.Loaded

        assertEquals(listOf(0, 1), loaded.sims.map { it.simSlotIndex })
        assertEquals(listOf("Airtel (Slot 1)", "Jio (Slot 2)"), loaded.sims.map { it.displayName })
    }

    @Test
    fun `should fall back to the slot number when the carrier name is missing`() = runTest {
        setSubscriptions(listOf(subscription(id = 3, slot = 1, name = null)))

        assertEquals(
            SimQueryResult.Loaded(
                listOf(SimInfo(subscriptionId = 3, simSlotIndex = 1, displayName = "SIM 2"))
            ),
            repository.getAvailableSimCards()
        )
    }

    @Test
    fun `should fall back to the slot number when the carrier name is blank`() = runTest {
        setSubscriptions(listOf(subscription(id = 3, slot = 0, name = "   ")))

        assertEquals(
            SimQueryResult.Loaded(
                listOf(SimInfo(subscriptionId = 3, simSlotIndex = 0, displayName = "SIM 1"))
            ),
            repository.getAvailableSimCards()
        )
    }

    @Test
    fun `should omit the slot for a subscription with no physical slot`() = runTest {
        setSubscriptions(listOf(subscription(id = 42, slot = -1, name = "eSIM Profile")))

        assertEquals(
            SimQueryResult.Loaded(
                listOf(SimInfo(subscriptionId = 42, simSlotIndex = -1, displayName = "eSIM Profile"))
            ),
            repository.getAvailableSimCards()
        )
    }

    @Test
    fun `should name a slotless unnamed subscription by its subscription id`() = runTest {
        setSubscriptions(listOf(subscription(id = 42, slot = -1, name = null)))

        assertEquals(
            SimQueryResult.Loaded(
                listOf(SimInfo(subscriptionId = 42, simSlotIndex = -1, displayName = "SIM 42"))
            ),
            repository.getAvailableSimCards()
        )
    }

    @Test
    fun `should report permission denied when the platform rejects the query`() = runTest {
        TestSubscriptionManagerShadow.queryFailure = SecurityException("no access")

        assertEquals(SimQueryResult.PermissionDenied, repository.getAvailableSimCards())
    }

    @Test
    fun `should report an unexpected query error as a failure`() = runTest {
        val cause = IllegalStateException("telephony died")
        TestSubscriptionManagerShadow.queryFailure = cause

        assertEquals(SimQueryResult.Failed(cause), repository.getAvailableSimCards())
    }

    @Test
    fun `should report a failure when SubscriptionManager is unavailable`() = runTest {
        removeSubscriptionService()

        val result = SimRepositoryImpl(application).getAvailableSimCards()

        assertTrue(result is SimQueryResult.Failed)
    }

    // endregion

    // region observeAvailableSimCards

    @Test
    fun `should emit the current SIMs as soon as the listener is registered`() = runTest {
        setSubscriptions(listOf(subscription(id = 1, slot = 0, name = "Airtel")))
        val emissions = mutableListOf<SimQueryResult>()

        val job = startObserving(emissions)
        idleMainLooper()

        assertEquals(
            listOf(
                SimQueryResult.Loaded(
                    listOf(SimInfo(subscriptionId = 1, simSlotIndex = 0, displayName = "Airtel (Slot 1)"))
                )
            ),
            emissions
        )
        job.cancelAndJoin()
    }

    @Test
    @Config(sdk = [29])
    fun `should emit on the pre-R registration path too`() = runTest {
        setSubscriptions(listOf(subscription(id = 1, slot = 0, name = "Airtel")))
        val emissions = mutableListOf<SimQueryResult>()

        val job = startObserving(emissions)
        idleMainLooper()

        assertEquals(1, emissions.size)
        assertEquals(1, TestSubscriptionManagerShadow.registeredListeners.size)
        job.cancelAndJoin()
    }

    @Test
    fun `should re-emit when the SIM set changes`() = runTest {
        setSubscriptions(listOf(subscription(id = 1, slot = 0, name = "Airtel")))
        val emissions = mutableListOf<SimQueryResult>()
        val job = startObserving(emissions)
        idleMainLooper()

        setSubscriptions(
            listOf(
                subscription(id = 1, slot = 0, name = "Airtel"),
                subscription(id = 2, slot = 1, name = "Jio")
            )
        )
        idleMainLooper()

        assertEquals(2, emissions.size)
        assertEquals(
            listOf("Airtel (Slot 1)", "Jio (Slot 2)"),
            (emissions.last() as SimQueryResult.Loaded).sims.map { it.displayName }
        )
        job.cancelAndJoin()
    }

    @Test
    fun `should detach the listener when collection stops`() = runTest {
        setSubscriptions(listOf(subscription(id = 1, slot = 0, name = "Airtel")))
        val job = startObserving(mutableListOf<SimQueryResult>())
        idleMainLooper()
        val listener = TestSubscriptionManagerShadow.registeredListeners.single()
        assertTrue(subscriptionShadow().hasOnSubscriptionsChangedListener(listener))

        job.cancelAndJoin()
        idleMainLooper()

        assertFalse(subscriptionShadow().hasOnSubscriptionsChangedListener(listener))
    }

    @Test
    fun `should emit permission denied without registering a listener`() = runTest {
        denyReadPhoneState()
        val emissions = mutableListOf<SimQueryResult>()

        val job = startObserving(emissions)
        idleMainLooper()

        assertEquals(listOf(SimQueryResult.PermissionDenied), emissions)
        assertTrue(TestSubscriptionManagerShadow.registeredListeners.isEmpty())
        job.cancelAndJoin()
    }

    @Test
    fun `should emit a failure without registering a listener when SubscriptionManager is unavailable`() =
        runTest {
            removeSubscriptionService()
            val emissions = mutableListOf<SimQueryResult>()

            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                SimRepositoryImpl(application).observeAvailableSimCards().collect { emissions += it }
            }
            idleMainLooper()

            assertEquals(1, emissions.size)
            assertTrue(emissions.single() is SimQueryResult.Failed)
            assertTrue(TestSubscriptionManagerShadow.registeredListeners.isEmpty())
            job.cancelAndJoin()
        }

    // endregion

    private fun TestScope.startObserving(into: MutableList<SimQueryResult>): Job =
        launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeAvailableSimCards().collect { into += it }
        }

    /** The looper is paused under Robolectric, so posted work runs only when idled. */
    private fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun subscriptionShadow(): TestSubscriptionManagerShadow =
        Shadow.extract(application.getSystemService(SubscriptionManager::class.java))

    private fun setSubscriptions(subscriptions: List<SubscriptionInfo>) =
        subscriptionShadow().setActiveSubscriptionInfoList(subscriptions)

    private fun subscription(id: Int, slot: Int, name: String?): SubscriptionInfo =
        ShadowSubscriptionManager.SubscriptionInfoBuilder.newBuilder()
            .setId(id)
            .setSimSlotIndex(slot)
            .setDisplayName(name)
            .buildSubscriptionInfo()

    private fun grantReadPhoneState() =
        shadowOf(application).grantPermissions(Manifest.permission.READ_PHONE_STATE)

    private fun denyReadPhoneState() =
        shadowOf(application).denyPermissions(Manifest.permission.READ_PHONE_STATE)

    private fun removeSubscriptionService() {
        val contextImpl: ShadowContextImpl = Shadow.extract(application.baseContext)
        contextImpl.removeSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
    }
}
