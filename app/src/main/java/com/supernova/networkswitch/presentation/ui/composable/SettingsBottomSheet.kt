package com.supernova.networkswitch.presentation.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.domain.model.ToggleModeConfig

@Composable
fun SettingsBottomSheet(
    // Settings properties
    selectedControlMethod: ControlMethod,
    onControlMethodSelected: (ControlMethod) -> Unit,
    rootCompatibility: CompatibilityState,
    shizukuCompatibility: CompatibilityState,
    onRetryCompatibilityClick: () -> Unit,

    // Network Mode Config properties
    currentConfig: ToggleModeConfig,
    onModeASelected: (NetworkMode) -> Unit,
    onModeBSelected: (NetworkMode) -> Unit,

    // App settings
    hideLauncherIcon: Boolean,
    onHideLauncherIconChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ControlMethodCard(
            selectedMethod = selectedControlMethod,
            onMethodSelected = onControlMethodSelected,
            rootCompatibility = rootCompatibility,
            shizukuCompatibility = shizukuCompatibility,
            onRetryClick = onRetryCompatibilityClick
        )

        NetworkModeConfigurationCard(
            currentConfig = currentConfig,
            onModeASelected = onModeASelected,
            onModeBSelected = onModeBSelected
        )

        AppSettingsCard(
            hideLauncherIcon = hideLauncherIcon,
            onHideLauncherIconChanged = onHideLauncherIconChanged
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AppSettingsCard(
    hideLauncherIcon: Boolean,
    onHideLauncherIconChanged: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "App Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hide launcher icon",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Relaunch the app for changes to take effect",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = hideLauncherIcon,
                    onCheckedChange = onHideLauncherIconChanged
                )
            }
        }
    }
}

@Composable
private fun ControlMethodCard(
    selectedMethod: ControlMethod,
    onMethodSelected: (ControlMethod) -> Unit,
    rootCompatibility: CompatibilityState,
    shizukuCompatibility: CompatibilityState,
    onRetryClick: () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Control Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
            Box {
                IconButton(onClick = { showTooltip = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "More info",
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = showTooltip,
                    onDismissRequest = { showTooltip = false }
                ) {
                    Text(
                        text = "Choose how the app should control network settings. Root method requires a rooted device, while Shizuku method works with non-rooted devices that have Shizuku installed.",
                        modifier = Modifier
                            .padding(8.dp)
                            .width(300.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onRetryClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh compatibility"
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Root Method Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedMethod == ControlMethod.ROOT,
                            onClick = { onMethodSelected(ControlMethod.ROOT) }
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Root Method",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Requires rooted device with root access granted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Compatibility status indicator
                    when (rootCompatibility) {
                        is CompatibilityState.Pending -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }

                        is CompatibilityState.Compatible -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Compatible",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        is CompatibilityState.PermissionDenied -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Permission denied",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        is CompatibilityState.Incompatible -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    RadioButton(
                        selected = selectedMethod == ControlMethod.ROOT,
                        onClick = { onMethodSelected(ControlMethod.ROOT) }
                    )
                }

                // Shizuku Method Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedMethod == ControlMethod.SHIZUKU,
                            onClick = { onMethodSelected(ControlMethod.SHIZUKU) }
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Shizuku Method",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Works with non-rooted devices using Shizuku service",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Compatibility status indicator
                    when (shizukuCompatibility) {
                        is CompatibilityState.Pending -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }

                        is CompatibilityState.Compatible -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Compatible",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        is CompatibilityState.PermissionDenied -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Permission denied",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        is CompatibilityState.Incompatible -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Not available",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    RadioButton(
                        selected = selectedMethod == ControlMethod.SHIZUKU,
                        onClick = { onMethodSelected(ControlMethod.SHIZUKU) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkModeConfigurationCard(
    currentConfig: ToggleModeConfig,
    onModeASelected: (NetworkMode) -> Unit,
    onModeBSelected: (NetworkMode) -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Configure Network Modes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
            Box {
                IconButton(onClick = { showTooltip = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "More info",
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = showTooltip,
                    onDismissRequest = { showTooltip = false }
                ) {
                    Text(
                        text = "Set up the two network modes that the toggle will switch between. Changes are saved automatically.",
                        modifier = Modifier
                            .padding(8.dp)
                            .width(300.dp)
                    )
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                NetworkModeSelector(
                    selectedMode = currentConfig.modeA,
                    onModeSelected = onModeASelected
                )

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )

                Icon(
                    painter = rememberVectorPainter(image = Icons.Default.ArrowCircleDown),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = null,
                    modifier = Modifier.align(
                        Alignment.CenterHorizontally
                    )
                )

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )

                NetworkModeSelector(
                    selectedMode = currentConfig.modeB,
                    onModeSelected = onModeBSelected
                )
            }
        }

        // Current Configuration Preview
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Configuration Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Toggle will switch between:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "• ${currentConfig.modeA.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "• ${currentConfig.modeB.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Warning if both modes are the same
        if (currentConfig.modeA == currentConfig.modeB) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mode A and Mode B must be different",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
