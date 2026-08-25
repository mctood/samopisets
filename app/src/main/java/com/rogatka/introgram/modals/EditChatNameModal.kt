package com.rogatka.introgram.modals

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.rogatka.introgram.Chat
import com.rogatka.introgram.singleLineChatName

@Composable
fun EditChatNameModal(
    show: Boolean,
    chat: Chat?,
    onDismiss: () -> Unit,
    onConfirm: (newChatName: String) -> Unit
) {
    if (chat == null) return
    var newChatName by remember { mutableStateOf(chat.name.singleLineChatName()) }

    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Как назовем чат?") },
            text = {
                OutlinedTextField(
                    value = newChatName,
                    onValueChange = { newChatName = it.singleLineChatName() },
                    placeholder = { Text("Введите название...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(newChatName.singleLineChatName()) }) {
                    Text("Переименовать")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Назад")
                }
            }
        )
    }
}
