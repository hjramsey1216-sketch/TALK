package com.example.imessageclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.imessageclone.data.Chat
import com.example.imessageclone.data.DeliveryChannel
import com.example.imessageclone.data.Message
import com.example.imessageclone.ui.theme.SmsBubbleGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chat: Chat,
    messages: List<Message>,
    currentUid: String,
    sentBubbleColor: Color,
    smsPermissionGranted: Boolean,
    onRequestSmsPermission: () -> Unit,
    onSend: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text(chat.contactName) }) },
        bottomBar = {
            Column {
                if (!chat.isAppChat && !smsPermissionGranted) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "SMS permission needed to text ${chat.contactName}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = onRequestSmsPermission) { Text("Allow") }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(if (chat.isAppChat) "Message" else "Text Message") }
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (draft.isNotBlank()) {
                                onSend(draft)
                                draft = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = sentBubbleColor)
                    ) { Text("Send") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                MessageBubble(msg, isMine = msg.senderUid == currentUid || msg.senderUid == "local-sms", sentBubbleColor = sentBubbleColor)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, isMine: Boolean, sentBubbleColor: Color) {
    // BUG FIX: this used to hardcode DefaultReceivedBubbleLight + black text for every
    // received message, even in dark mode, producing a light-gray bubble floating on a
    // dark background with poor contrast. Using theme-aware surfaceVariant/onSurfaceVariant
    // instead makes received bubbles adapt correctly in both modes.
    val bubbleColor = when {
        isMine && message.deliveryChannel == DeliveryChannel.SMS -> SmsBubbleGreen // green bubble, like real iMessage's SMS fallback cue
        isMine -> sentBubbleColor
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isMine -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(message.text, color = textColor)
        }
    }
}
