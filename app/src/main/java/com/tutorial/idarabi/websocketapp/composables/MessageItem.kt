package com.tutorial.idarabi.websocketapp.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tutorial.idarabi.websocketapp.ChatMessage
import com.tutorial.idarabi.websocketapp.MessageType

@Composable
fun MessageItem(message: ChatMessage) {
    val backgroundColor = when (message.type) {
        MessageType.SENT -> Color(0xFFE3F2FD)
        MessageType.RECEIVED -> Color(0xFFF3E5F5)
        MessageType.SYSTEM -> Color(0xFFE8F5E8)
        MessageType.ERROR -> Color(0xFFFFEBEE)
    }

    val textColor = when (message.type) {
        MessageType.SENT -> Color(0xFF1976D2)
        MessageType.RECEIVED -> Color(0xFF7B1FA2)
        MessageType.SYSTEM -> Color(0xFF388E3C)
        MessageType.ERROR -> Color(0xFFD32F2F)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = message.sender,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = textColor
            )
            Text(
                text = message.content,
                fontSize = 14.sp,
                color = textColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}