package com.supernova.networkswitch.presentation.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.supernova.networkswitch.BuildConfig
import com.supernova.networkswitch.R
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.presentation.theme.NetworkSwitchTheme
import com.supernova.networkswitch.presentation.viewmodel.MainViewModel
import com.supernova.networkswitch.presentation.viewmodel.NetworkModeConfigViewModel
import com.supernova.networkswitch.presentation.viewmodel.SettingsViewModel
import com.supernova.networkswitch.presentation.ui.composable.AboutCard
import com.supernova.networkswitch.presentation.ui.composable.SettingsBottomSheet
import com.supernova.networkswitch.presentation.ui.composable.CompatibilityCard
import com.supernova.networkswitch.presentation.ui.composable.NetworkToggleCard
import com.supernova.networkswitch.presentation.ui.composable.QuickSettingsHintCard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val networkModeConfigViewModel: NetworkModeConfigViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetworkSwitchTheme {
                MainScreen(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
                    networkModeConfigViewModel = networkModeConfigViewModel
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshAllData()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    networkModeConfigViewModel: NetworkModeConfigViewModel
) {
    val compatibilityState = viewModel.compatibilityState
    val aboutBottomSheetState = rememberModalBottomSheetState()
    val settingsBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAboutBottomSheet by remember { mutableStateOf(false) }
    var showSettingsBottomSheet by remember { mutableStateOf(false) }

    // Settings state
    val controlMethod by settingsViewModel.controlMethod.collectAsState()
    val currentConfig by networkModeConfigViewModel.currentConfig.collectAsState()

    if (showAboutBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAboutBottomSheet = false },
            sheetState = aboutBottomSheetState
        ) {
            AboutCard()
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showSettingsBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsBottomSheet = false },
            sheetState = settingsBottomSheetState
        ) {
            SettingsBottomSheet(
                selectedControlMethod = controlMethod,
                onControlMethodSelected = { settingsViewModel.updateControlMethod(it) },
                rootCompatibility = settingsViewModel.rootCompatibility,
                shizukuCompatibility = settingsViewModel.shizukuCompatibility,
                onRetryCompatibilityClick = { settingsViewModel.retryCompatibilityCheck() },
                currentConfig = currentConfig,
                onModeASelected = { mode ->
                    networkModeConfigViewModel.updateModeA(mode)
                    networkModeConfigViewModel.saveConfiguration()
                },
                onModeBSelected = { mode ->
                    networkModeConfigViewModel.updateModeB(mode)
                    networkModeConfigViewModel.saveConfiguration()
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.app_name))
                        if (BuildConfig.DEBUG) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DEBUG",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsBottomSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                    IconButton(onClick = { showAboutBottomSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Compatibility Status Card
            CompatibilityCard(
                compatibilityState = compatibilityState,
                currentControlMethod = viewModel.selectedMethod,
                onRetryClick = { viewModel.retryCompatibilityCheck() }
            )
            
            // Network Toggle Card (show if compatible)
            if (compatibilityState is CompatibilityState.Compatible) {
                NetworkToggleCard(
                    currentMode = viewModel.currentNetworkMode,
                    toggleButtonText = viewModel.getToggleButtonText(),
                    isLoading = viewModel.isLoading,
                    onToggleClick = { viewModel.toggleNetworkMode() }
                )
            }
            
            // Quick Settings Tip Card
            QuickSettingsHintCard()
        }
    }
}
