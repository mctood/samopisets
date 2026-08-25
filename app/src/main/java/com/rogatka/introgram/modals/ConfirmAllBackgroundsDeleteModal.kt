package com.rogatka.introgram.modals

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ConfirmAllBackgroundsDeleteModal(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Вы уверены?") },
            text = { Text("Эта опция позволяет удалить все фоны чатов, включая фон главного экрана, с целью освобождения места. \n\nОперация необратима. Продолжить?") },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Делай")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        )
    }
}