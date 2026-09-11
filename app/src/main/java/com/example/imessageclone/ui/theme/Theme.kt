package com.example.imessageclone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// iMessage-inspired defaults — these are what a fresh install looks like,
// but every one of them is user-overridable via SettingsScreen.
val DefaultSentBubble = Color(0xFF0B84FE)   // iOS "blue bubble" blue
val DefaultReceivedBubbleLight = Color(0xFFE9E9EB)
val DefaultReceivedBubbleDark = Color(0xFF262628)
val SmsBubbleGreen = Color(0xFF34C759)      // signals "this went out as SMS", like real iMessage green bubbles

private val LightColors = lightColorScheme(primary = DefaultSentBubble)
private val DarkColors = darkColorScheme(primary = DefaultSentBubble)

/**
 * @param userSentBubbleColor lets a user's chosen color (from AppUser.bubbleColorHex)
 * override the default blue bubble, independent of light/dark mode.
 */
@Composable
fun TalkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    userSentBubbleColor: Color? = null,
    content: @Composable () -> Unit
) {
    val base = if (darkTheme) DarkColors else LightColors
    val colors = if (userSentBubbleColor != null) base.copy(primary = userSentBubbleColor) else base
    MaterialTheme(colorScheme = colors, typography = TalkTypography, content = content)
}
