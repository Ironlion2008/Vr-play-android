package com.example.vrplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vrplayer.data.LocalMedia
import com.example.vrplayer.ui.components.MediaCard
import com.example.vrplayer.ui.components.ServerCard
import com.example.vrplayer.ui.components.ServerStatus
import com.example.vrplayer.ui.theme.*

@Composable
fun StreamsScreen(
    discoveredServers: List<Pair<String, Pair<String, Int>>>,
    isDiscovering: Boolean,
    onDiscover: () -> Unit,
    onConnect: (String, Int) -> Unit,
    onManualConnect: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var manualIp by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("8080") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Discovery header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered Servers",
                    style = MaterialTheme.typography.headlineSmall,
                    color = VRTextPrimary
                )

                Button(
                    onClick = onDiscover,
                    enabled = !isDiscovering,
                    colors = ButtonDefaults.buttonColors(containerColor = VRPrimaryDark)
                ) {
                    if (isDiscovering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = VRBackgroundDark,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan")
                }
            }
        }

        // Discovered servers
        items(discoveredServers) { (name, ipPort) ->
            val (ip, port) = ipPort
            ServerCard(
                name = name,
                ip = ip,
                port = port,
                status = ServerStatus.ONLINE,
                onConnect = { onConnect(ip, port) }
            )
        }

        // Empty state
        if (discoveredServers.isEmpty() && !isDiscovering) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.WifiFind,
                            contentDescription = null,
                            tint = VRTextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No servers found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = VRTextSecondary
                        )
                        Text(
                            text = "Make sure your VR headset is on the same network",
                            style = MaterialTheme.typography.bodySmall,
                            color = VRTextSecondary
                        )
                    }
                }
            }
        }

        // Manual connection section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = VRCardBorder)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Manual Connection",
                style = MaterialTheme.typography.titleMedium,
                color = VRTextPrimary
            )
        }

        item {
            OutlinedTextField(
                value = manualIp,
                onValueChange = { manualIp = it },
                label = { Text("IP Address") },
                placeholder = { Text("192.168.1.100") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = VRTextPrimary,
                    unfocusedTextColor = VRTextPrimary,
                    focusedBorderColor = VRPrimaryDark,
                    unfocusedBorderColor = VRTextSecondary,
                    focusedLabelColor = VRPrimaryDark,
                    unfocusedLabelColor = VRTextSecondary
                )
            )
        }

        item {
            OutlinedTextField(
                value = manualPort,
                onValueChange = { manualPort = it },
                label = { Text("Port") },
                placeholder = { Text("8080") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val port = manualPort.toIntOrNull() ?: 8080
                        if (manualIp.isNotBlank()) {
                            onManualConnect(manualIp, port)
                        }
                    }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = VRTextPrimary,
                    unfocusedTextColor = VRTextPrimary,
                    focusedBorderColor = VRPrimaryDark,
                    unfocusedBorderColor = VRTextSecondary,
                    focusedLabelColor = VRPrimaryDark,
                    unfocusedLabelColor = VRTextSecondary
                )
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val port = manualPort.toIntOrNull() ?: 8080
                    if (manualIp.isNotBlank()) {
                        onManualConnect(manualIp, port)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = manualIp.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = VRSecondaryDark)
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect")
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun LocalMediaScreen(
    mediaList: List<LocalMedia>,
    isLoading: Boolean,
    onMediaClick: (LocalMedia) -> Unit,
    onRequestPermission: () -> Unit,
    hasPermission: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Local Media",
            style = MaterialTheme.typography.headlineSmall,
            color = VRTextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            !hasPermission -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = VRTextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Storage permission required",
                            style = MaterialTheme.typography.bodyLarge,
                            color = VRTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Grant permission to browse your local videos",
                            style = MaterialTheme.typography.bodySmall,
                            color = VRTextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = VRPrimaryDark)
                        ) {
                            Text("Grant Permission", color = VRBackgroundDark)
                        }
                    }
                }
            }

            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = VRPrimaryDark)
                }
            }

            mediaList.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = VRTextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No videos found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = VRTextSecondary
                        )
                        Text(
                            text = "Add videos to your device storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = VRTextSecondary
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mediaList) { media ->
                        MediaCard(
                            media = media,
                            onClick = { onMediaClick(media) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}
