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
import com.example.data.model.TreeEntity
import com.example.ui.theme.CardBorder
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SurfaceElevatedNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TreeSettingsDialog(
  tree: TreeEntity,
  onDismiss: () -> Unit,
  onSave: (title: String, description: String) -> Unit
) {
  var title by remember { mutableStateOf(tree.title) }
  var description by remember { mutableStateOf(tree.description) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceElevatedNavy,
    title = {
      Text(
        text = "Tree Settings",
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
          value = title,
          onValueChange = { title = it },
          label = { Text("Tree Title") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_tree_title"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SoftGold,
            unfocusedBorderColor = CardBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description / Context") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_tree_desc"),
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
          if (title.isNotBlank()) {
            onSave(title.trim(), description.trim())
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = SoftGold,
          contentColor = MidnightNavy
        ),
        modifier = Modifier.testTag("button_save_tree_settings")
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
