package com.supernova.networkswitch.presentation.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supernova.networkswitch.R
import com.supernova.networkswitch.domain.model.NetworkMode
import com.supernova.networkswitch.presentation.theme.NetworkSwitchTheme
import com.supernova.networkswitch.presentation.viewmodel.NetworkModeConfigViewModel
import com.supernova.networkswitch.presentation.ui.composable.NetworkModeSelector
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NetworkModeConfigActivity : ComponentActivity() {
    
    private val viewModel: NetworkModeConfigViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            NetworkSwitchTheme {
                NetworkModeConfigScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkModeConfigScreen(
    viewModel: NetworkModeConfigViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentConfig by viewModel.currentConfig.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val configSaved by viewModel.configSaved.collectAsState()
    
    // Show toast when configuration is saved
    LaunchedEffect(configSaved) {
        if (configSaved) {
            Toast.makeText(context, context.getString(R.string.configuration_saved), Toast.LENGTH_SHORT).show()
            viewModel.resetSavedState()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.network_mode_config)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
            // Description Card
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        text = stringResource(R.string.network_mode_config),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.network_mode_config_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Mode A Selection
            NetworkModeSelector(
                label = stringResource(R.string.mode_a_label),
                selectedMode = currentConfig.modeA,
                onModeSelected = { mode ->
                    viewModel.updateModeA(mode)
                }
            )
            
            // Mode B Selection
            NetworkModeSelector(
                label = stringResource(R.string.mode_b_label),
                selectedMode = currentConfig.modeB,
                onModeSelected = { mode ->
                    viewModel.updateModeB(mode)
                }
            )
            
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
            
            // Save Button
            Button(
                onClick = { viewModel.saveConfiguration() },
                enabled = !isLoading && currentConfig.modeA != currentConfig.modeB,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.save_configuration))
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
}