package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NodeEntity
import com.example.ui.theme.CardBorder
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SurfaceElevatedNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EditNodeDialog(
  node: NodeEntity,
  onDismiss: () -> Unit,
  onSave: (text: String, notes: String) -> Unit
) {
  var text by remember { mutableStateOf(node.text) }
  var notes by remember { mutableStateOf(node.notes) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceElevatedNavy,
    title = {
      Text(
        text = if (node.isRoot) "Edit Root Problem" else "Edit Card",
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text("Card Text / Explanation") },
          minLines = 2,
          maxLines = 5,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_node_text"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SoftGold,
            unfocusedBorderColor = CardBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes / Supporting Evidence") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_node_notes"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SoftGold,
            unfocusedBorderColor = CardBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (text.isNotBlank()) {
            onSave(text.trim(), notes.trim())
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = SoftGold,
          contentColor = MidnightNavy
        ),
        modifier = Modifier.testTag("button_save_edit")
      ) {
        Text("Save", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
