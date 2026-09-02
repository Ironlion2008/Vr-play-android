package com.example.vrplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.vrplayer.ui.theme.*

enum class HomeTab(
    val title: String,
    val icon: ImageVector
) {
    STREAMS("Streams", Icons.Default.Wifi),
    LOCAL_MEDIA("Local", Icons.Default.VideoLibrary),
    ENVIRONMENTS("Environment", Icons.Default.Image),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun HomeScreen(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.background(VRBackgroundDark),
        containerColor = VRBackgroundDark,
        bottomBar = {
            NavigationBar(
                containerColor = VRSurfaceDark,
                contentColor = VRPrimaryDark
            ) {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VRBackgroundDark,
                            selectedTextColor = VRPrimaryDark,
                            unselectedIconColor = VRTextSecondary,
                            unselectedTextColor = VRTextSecondary,
                            indicatorColor = VRPrimaryDark
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(VRBackgroundDark)
        ) {
            when (selectedTab) {
                HomeTab.STREAMS -> StreamsContent()
                HomeTab.LOCAL_MEDIA -> LocalMediaContent()
                HomeTab.ENVIRONMENTS -> EnvironmentsContent()
                HomeTab.SETTINGS -> SettingsContent()
            }
        }
    }
}

@Composable
private fun StreamsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "VR Streams",
            style = MaterialTheme.typography.headlineMedium,
            color = VRTextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Server discovery and stream management coming soon.",
            style = MaterialTheme.typography.bodyLarge,
            color = VRTextSecondary
        )
    }
}

@Composable
private fun LocalMediaContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Local Media",
            style = MaterialTheme.typography.headlineMedium,
            color = VRTextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Browse your local videos.",
            style = MaterialTheme.typography.bodyLarge,
            color = VRTextSecondary
        )
    }
}

@Composable
private fun EnvironmentsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Environments",
            style = MaterialTheme.typography.headlineMedium,
            color = VRTextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Choose your player environment background.",
            style = MaterialTheme.typography.bodyLarge,
            color = VRTextSecondary
        )
    }
}

@Composable
private fun SettingsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = VRTextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Player and app settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = VRTextSecondary
        )
    }
}
