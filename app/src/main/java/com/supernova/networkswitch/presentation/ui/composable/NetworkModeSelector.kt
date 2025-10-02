package com.supernova.networkswitch.presentation.ui.composable

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supernova.networkswitch.domain.model.NetworkMode

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NetworkModeSelector(
    label: String,
    selectedMode: NetworkMode,
    onModeSelected: (NetworkMode) -> Unit,
    modifier: Modifier = Modifier,
    availableModes: List<NetworkMode> = NetworkMode.values().toList()
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedMode.displayName,
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableModes.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            onClick = {
                                onModeSelected(mode)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}