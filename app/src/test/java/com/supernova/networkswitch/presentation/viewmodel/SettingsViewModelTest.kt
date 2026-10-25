package com.supernova.networkswitch.presentation.viewmodel

import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.repository.NetworkControlRepository
import com.supernova.networkswitch.domain.repository.PreferencesRepository
import com.supernova.networkswitch.util.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var networkControlRepository: NetworkControlRepository

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        preferencesRepository = mockk(relaxed = true)
        networkControlRepository = mockk(relaxed = true)

        coEvery { preferencesRepository.observeControlMethod() } returns flowOf(ControlMethod.SHIZUKU)
    }

    private fun createViewModel() {
        viewModel = SettingsViewModel(
            preferencesRepository,
            networkControlRepository
        )
    }

    @Test
    fun `init calls checkAllCompatibility`() = runTest {
        createViewModel()
        coVerify { networkControlRepository.checkCompatibility(ControlMethod.ROOT) }
        coVerify { networkControlRepository.checkCompatibility(ControlMethod.SHIZUKU) }
    }

    @Test
    fun `checkAllCompatibility updates compatibility states`() = runTest {
        val rootState = CompatibilityState.Compatible
        val shizukuState = CompatibilityState.Incompatible("Test Reason")
        coEvery { networkControlRepository.checkCompatibility(ControlMethod.ROOT) } returns rootState
        coEvery { networkControlRepository.checkCompatibility(ControlMethod.SHIZUKU) } returns shizukuState

        createViewModel()

        assertEquals(rootState, viewModel.rootCompatibility)
        assertEquals(shizukuState, viewModel.shizukuCompatibility)
    }

    @Test
    fun `updateControlMethod calls repository`() = runTest {
        createViewModel()
        val method = ControlMethod.ROOT
        viewModel.updateControlMethod(method)
        coVerify { preferencesRepository.setControlMethod(method) }
    }

    @Test
    fun `retryCompatibilityCheck calls checkAllCompatibility`() = runTest {
        createViewModel()
        viewModel.retryCompatibilityCheck()
        coVerify(exactly = 2) { networkControlRepository.checkCompatibility(ControlMethod.ROOT) }
        coVerify(exactly = 2) { networkControlRepository.checkCompatibility(ControlMethod.SHIZUKU) }
    }

    @Test
    fun `retryCompatibilityCheck requests Shizuku permission when it is denied`() = runTest {
        coEvery { preferencesRepository.getControlMethod() } returns ControlMethod.SHIZUKU
        coEvery { networkControlRepository.checkCompatibility(ControlMethod.SHIZUKU) } returns
            CompatibilityState.PermissionDenied(ControlMethod.SHIZUKU)
        createViewModel()

        viewModel.retryCompatibilityCheck()

        coVerify(exactly = 1) { networkControlRepository.requestPermission(ControlMethod.SHIZUKU) }
        coVerify(exactly = 2) { networkControlRepository.checkCompatibility(ControlMethod.SHIZUKU) }
    }

    @Test
    fun `retryCompatibilityCheck does not request permission when root is selected`() = runTest {
        coEvery { preferencesRepository.getControlMethod() } returns ControlMethod.ROOT
        coEvery { networkControlRepository.checkCompatibility(ControlMethod.SHIZUKU) } returns
            CompatibilityState.PermissionDenied(ControlMethod.SHIZUKU)
        createViewModel()

        viewModel.retryCompatibilityCheck()

        coVerify(exactly = 0) { networkControlRepository.requestPermission(any()) }
    }

    @Test
    fun `retryCompatibilityCheck does not request permission when Shizuku is compatible`() = runTest {
        coEvery { preferencesRepository.getControlMethod() } returns ControlMethod.SHIZUKU
        coEvery { networkControlRepository.checkCompatibility(ControlMethod.SHIZUKU) } returns
            CompatibilityState.Compatible
        createViewModel()

        viewModel.retryCompatibilityCheck()

        coVerify(exactly = 0) { networkControlRepository.requestPermission(any()) }
    }
}
