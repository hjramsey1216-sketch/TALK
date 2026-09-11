package com.example.imessageclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// The customization surface: swap out sent-bubble color and light/dark/system.
// Extend this with wallpaper, font size, bubble shape (rounded vs sharp), etc.
private val presetColors = listOf(
    0xFF0B84FE, // iMessage blue (default)
    0xFF34C759, // green
    0xFFFF3B30, // red
    0xFFAF52DE, // purple
    0xFFFF9500, // orange
    0xFF000000  // black/monochrome
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentBubbleColorHex: String,
    currentThemeMode: String, // "light" | "dark" | "system"
    onBubbleColorChange: (String) -> Unit,
    onThemeModeChange: (String) -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Customize") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Bubble color", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row {
                presetColors.forEach { colorLong ->
                    val hex = "#" + Integer.toHexString(colorLong.toInt()).takeLast(6).uppercase()
                    Box(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(colorLong.toInt()))
                            .clickable { onBubbleColorChange(hex) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            listOf("system", "light", "dark").forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onThemeModeChange(mode) }.padding(vertical = 8.dp)
                ) {
                    RadioButton(selected = currentThemeMode == mode, onClick = { onThemeModeChange(mode) })
                    Spacer(Modifier.width(8.dp))
                    Text(mode.replaceFirstChar { it.uppercase() })
                }
            }
        }
    }
}
