package com.supernova.networkswitch.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.supernova.networkswitch.domain.model.CompatibilityState
import com.supernova.networkswitch.domain.model.ControlMethod

/**
 * Reusable UI components to reduce code duplication
 */

@Composable
fun StatusIndicator(
    state: CompatibilityState,
    modifier: Modifier = Modifier
) {
    when (state) {
        is CompatibilityState.Pending -> CircularProgressIndicator(
            modifier = modifier.size(20.dp),
            strokeWidth = 2.dp
        )
        is CompatibilityState.Compatible -> Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Compatible",
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(20.dp)
        )
        is CompatibilityState.PermissionDenied -> Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Permission denied",
            tint = MaterialTheme.colorScheme.error,
            modifier = modifier.size(20.dp)
        )
        is CompatibilityState.Incompatible -> Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Not available",
            tint = MaterialTheme.colorScheme.error,
            modifier = modifier.size(20.dp)
        )
    }
}

@Composable
fun MethodSelectionRow(
    method: ControlMethod,
    title: String,
    description: String,
    selected: Boolean,
    compatibilityState: CompatibilityState,
    onMethodSelected: (ControlMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = { onMethodSelected(method) }
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = { onMethodSelected(method) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        StatusIndicator(state = compatibilityState)
    }
}

@Composable
fun CardSection(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row { actions() }
            }
            content()
        }
    }
}

@Composable
fun StatusText(
    compatibilityState: CompatibilityState,
    currentMethod: ControlMethod? = null
) {
    when (compatibilityState) {
        is CompatibilityState.Pending -> {
            Text(
                text = "Checking compatibility...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        is CompatibilityState.Compatible -> {
            Text(
                text = "✓ Compatible",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        is CompatibilityState.PermissionDenied -> {
            val methodName = when (compatibilityState.method) {
                ControlMethod.ROOT -> "Root"
                ControlMethod.SHIZUKU -> "Shizuku"
            }
            Text(
                text = "✗ $methodName Permission Denied",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = when (compatibilityState.method) {
                    ControlMethod.ROOT -> "Grant root access or try Shizuku method"
                    ControlMethod.SHIZUKU -> "Grant Shizuku permission or try Root method"
                },
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
        is CompatibilityState.Incompatible -> {
            Text(
                text = "✗ Not Compatible",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun NetworkModeToggleButton(
    networkState: Boolean,
    isLoading: Boolean,
    onToggleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onToggleClick,
        enabled = !isLoading,
        modifier = modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = if (networkState) "Switch to 4G" else "Switch to 5G")
    }
}
