package com.example.waller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.waller.ui.wallpaper.WallpaperGeneratorScreen
import com.example.waller.ui.theme.WallerTheme

@Composable
fun WallerApp() {
    val systemIsDark = isSystemInDarkTheme()
    var useDarkTheme by remember { mutableStateOf(systemIsDark) }

    WallerTheme(darkTheme = useDarkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    )
                )
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) { padding ->
                WallpaperGeneratorScreen(
                    modifier = Modifier.padding(padding),
                    isAppDarkMode = useDarkTheme,
                    onThemeChange = { useDarkTheme = !useDarkTheme }
                )
            }
        }
    }
}