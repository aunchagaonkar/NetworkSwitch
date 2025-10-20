package com.supernova.networkswitch.presentation.ui.composable

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

@Composable
fun AboutCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "About",
            style = MaterialTheme.typography.headlineSmall
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Source Code",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp)
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                LinkItem(
                    title = "NetworkSwitch",
                    subtitle = "https://github.com/aunchagaonkar/NetworkSwitch",
                    link = "https://github.com/aunchagaonkar/NetworkSwitch",
                    icon = Icons.Outlined.Code
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Open Source Licenses",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp)
            )

            val licenses = listOf(
                Triple("Shizuku", "Apache License 2.0\nhttps://github.com/RikkaApps/Shizuku", "https://github.com/RikkaApps/Shizuku"),
                Triple("libsu", "Apache License 2.0\nhttps://github.com/topjohnwu/libsu", "https://github.com/topjohnwu/libsu"),
                Triple("Android Jetpack", "Apache License 2.0\nhttps://android.googlesource.com/platform/frameworks/support", "https://android.googlesource.com/platform/frameworks/support"),
                Triple("Kotlin", "Apache License 2.0\nhttps://github.com/JetBrains/kotlin", "https://github.com/JetBrains/kotlin")
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    licenses.forEachIndexed { index, item ->
                        LinkItem(
                            title = item.first,
                            subtitle = item.second,
                            link = item.third,
                            icon = Icons.Outlined.Info
                        )
                        if (index < licenses.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = DividerDefaults.Thickness,
                                color = DividerDefaults.color
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkItem(
    title: String,
    subtitle: String,
    link: String,
    icon: ImageVector
) {
    val context = LocalContext.current
    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary
            )
        },
        supportingContent = { Text(text = subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable {
            context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
        }
    )
}
