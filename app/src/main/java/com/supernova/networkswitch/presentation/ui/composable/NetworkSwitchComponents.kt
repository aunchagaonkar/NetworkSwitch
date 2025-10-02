package com.supernova.networkswitch.presentation.ui.composable

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.presentation.ui.components.CardSection
import com.supernova.networkswitch.service.NetworkTileService

private fun ControlMethod.displayName() = if (this == ControlMethod.SHIZUKU) "Shizuku" else "Root"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CompatibilityCard(
    compatibilityState: CompatibilityState,
    currentControlMethod: ControlMethod,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (compatibilityState) {
                is CompatibilityState.Pending -> {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Checking compatibility...",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }

                is CompatibilityState.Compatible -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Device Compatible",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Using ${currentControlMethod.displayName()} method",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                is CompatibilityState.PermissionDenied -> {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${compatibilityState.method.displayName()} Access Denied",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (compatibilityState.method == ControlMethod.ROOT)
                            "Please grant root access to use this app"
                        else
                            "Please grant Shizuku permission or install Shizuku",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetryClick,shapes = ButtonDefaults.shapes()) { Text("Retry") }
                }

                is CompatibilityState.Incompatible -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Device Not Compatible",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = compatibilityState.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetryClick, shapes = ButtonDefaults.shapes()) { Text("Retry") }

                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NetworkToggleCard(
    currentMode: NetworkMode?,
    toggleButtonText: String,
    isLoading: Boolean,
    onToggleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CardSection(
        title = "Network Mode",
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (currentMode != null) "Current: ${currentMode.displayName}" else "Network mode unavailable",
            style = MaterialTheme.typography.titleMedium,
            color = if (currentMode != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (currentMode != null) {
                "Tap to switch to the configured alternate network mode"
            } else {
                "Unable to detect current network mode"
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onToggleClick,
            enabled = !isLoading && currentMode != null,
            modifier = Modifier.fillMaxWidth(),
            shapes = ButtonDefaults.shapes()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = toggleButtonText)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuickSettingsHintCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isTileAdded by NetworkTileService.isTileAdded.collectAsState()
    var hasTriedAutoAdd by remember { mutableStateOf(false) }
    var showAddButton by remember { mutableStateOf(false) }

    // Auto-add tile on first composition
    LaunchedEffect(Unit) {
        if (!hasTriedAutoAdd && !isTileAdded) {
            hasTriedAutoAdd = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val success = requestAddTileToQuickSettings(context)
                showAddButton = !success
            } else {
                showAddButton = true
            }
        }
    }

    // Update showAddButton when tile status changes
    LaunchedEffect(isTileAdded) {
        if (isTileAdded) {
            showAddButton = false
        }
    }

    if (isTileAdded || showAddButton) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (isTileAdded) "✅ Quick Settings" else "💡 Quick Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isTileAdded) {
                        "Great! You can now switch network modes directly from your Quick Settings panel. Just pull down your notification panel and tap the \"Network Switch\" tile."
                    } else {
                        "Add the \"Network Switch\" tile to your Quick Settings for instant network switching from anywhere on your device."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isTileAdded && showAddButton) {
                    Spacer(modifier = Modifier.height(16.dp))

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = {
                                val success = requestAddTileToQuickSettings(context)
                                if (success) showAddButton = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add to Quick Settings")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pull down your notification panel → Tap the pencil/edit icon → Find \"Network Switch\" → Drag it to your Quick Settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun requestAddTileToQuickSettings(context: Context): Boolean {
    return try {
        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
        val componentName = ComponentName(context, NetworkTileService::class.java)

        statusBarManager?.requestAddTileService(
            componentName,
            "Network Switch",
            Icon.createWithResource(context, com.supernova.networkswitch.R.drawable.ic_5g_big),
            { runnable -> runnable.run() },
            { result ->
              // ->  STATUS_BAR_MANAGER_TILE_ADDED or STATUS_BAR_MANAGER_TILE_NOT_ADDED
            }
        )
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}