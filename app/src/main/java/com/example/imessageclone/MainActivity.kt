package com.example.imessageclone

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.imessageclone.data.*
import com.example.imessageclone.ui.screens.ChatListScreen
import com.example.imessageclone.ui.screens.ChatScreen
import com.example.imessageclone.ui.screens.SettingsScreen
import com.example.imessageclone.ui.theme.TalkTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private val repo = FirebaseRepository()
    private val resolver = ContactResolver()
    private lateinit var smsSender: SmsFallbackSender

    private var smsPermissionGranted by mutableStateOf(false)
    private lateinit var requestSmsPermission: androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        smsSender = SmsFallbackSender(this)
        smsPermissionGranted = smsSender.hasPermission()

        requestSmsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            smsPermissionGranted = granted
        }

        setContent {
            var bubbleColorHex by remember { mutableStateOf("#0B84FE") }
            var themeMode by remember { mutableStateOf("system") }
            // BUG FIX: previously nothing ever signed the user into Firebase Auth.
            // With auth.currentUser always null, FirebaseRepository.currentUid was
            // always null too, so sendAppMessage/observeChats/upsertCurrentUser all
            // silently returned early doing nothing — the whole app-chat pipeline
            // was dead on arrival with no visible error. This is a placeholder
            // (anonymous auth) standing in for the real sign-up flow noted in the
            // README's TODOs.
            var signedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }
            LaunchedEffect(Unit) {
                if (!signedIn) {
                    try {
                        FirebaseAuth.getInstance().signInAnonymously().await()
                        signedIn = true
                    } catch (e: Exception) {
                        // Leaves `signedIn` false; UI below shows a spinner rather than
                        // silently proceeding into a broken, uid-less state.
                    }
                }
            }

            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            TalkTheme(darkTheme = isDark, userSentBubbleColor = Color(android.graphics.Color.parseColor(bubbleColorHex))) {
                if (!signedIn) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val nav = rememberNavController()
                    AppNav(
                        nav = nav,
                        repo = repo,
                        resolver = resolver,
                        smsSender = smsSender,
                        smsPermissionGranted = smsPermissionGranted,
                        onRequestSmsPermission = { requestSmsPermission.launch(Manifest.permission.SEND_SMS) },
                        bubbleColorHex = bubbleColorHex,
                        onBubbleColorChange = { bubbleColorHex = it },
                        themeMode = themeMode,
                        onThemeModeChange = { themeMode = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNav(
    nav: NavHostController,
    repo: FirebaseRepository,
    resolver: ContactResolver,
    smsSender: SmsFallbackSender,
    smsPermissionGranted: Boolean,
    onRequestSmsPermission: () -> Unit,
    bubbleColorHex: String,
    onBubbleColorChange: (String) -> Unit,
    themeMode: String,
    onThemeModeChange: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val chats by repo.observeChats().collectAsState(initial = emptyList())
    var activeChat by remember { mutableStateOf<Chat?>(null) }

    NavHost(navController = nav, startDestination = "list") {
        composable("list") {
            ChatListScreen(
                chats = chats,
                onOpenChat = { chat -> activeChat = chat; nav.navigate("chat") },
                onNewMessage = { /* TODO: contact picker -> resolver.resolve(...) -> nav.navigate("chat") */ },
                onOpenSettings = { nav.navigate("settings") }
            )
        }
        composable("chat") {
            val chat = activeChat ?: return@composable
            val context = androidx.compose.ui.platform.LocalContext.current
            val messages by repo.observeMessages(chat.chatId).collectAsState(initial = emptyList())
            ChatScreen(
                chat = chat,
                messages = messages,
                currentUid = repo.currentUid ?: "",
                sentBubbleColor = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(bubbleColorHex)),
                smsPermissionGranted = smsPermissionGranted,
                onRequestSmsPermission = onRequestSmsPermission,
                onSend = { text ->
                    scope.launch {
                        // This is the whole "iMessage vs SMS" decision: same chat UI, different pipe out.
                        // BUG FIX: previously any failure here (a denied SMS, a dropped
                        // Firestore write) was silently swallowed — the message just
                        // vanished with no feedback that it didn't go anywhere.
                        try {
                            if (chat.isAppChat) {
                                repo.sendAppMessage(chat, text)
                            } else {
                                val phone = chat.contactPhone
                                if (phone == null) {
                                    Toast.makeText(context, "No phone number for this contact", Toast.LENGTH_SHORT).show()
                                } else {
                                    val result = smsSender.send(phone, text)
                                    if (result.isSuccess) {
                                        repo.logSmsMessage(chat, text)
                                    } else {
                                        Toast.makeText(context, "Text failed to send: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Message failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                currentBubbleColorHex = bubbleColorHex,
                currentThemeMode = themeMode,
                onBubbleColorChange = onBubbleColorChange,
                onThemeModeChange = onThemeModeChange
            )
        }
    }
}
