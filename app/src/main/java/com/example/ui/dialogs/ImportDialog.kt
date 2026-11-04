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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBorder
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SurfaceElevatedNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import org.json.JSONObject

@Composable
fun ImportDialog(
  onDismiss: () -> Unit,
  onImport: (jsonString: String) -> Unit
) {
  var jsonContent by remember { mutableStateOf("") }
  var validationError by remember { mutableStateOf<String?>(null) }
  var previewTitle by remember { mutableStateOf<String?>(null) }
  var previewNodeCount by remember { mutableStateOf<Int?>(null) }

  fun validateJson(text: String): Boolean {
    if (text.isBlank()) {
      validationError = "Please paste the .why JSON file content."
      return false
    }
    return try {
      val obj = JSONObject(text)
      val format = obj.optString("format")
      if (format != "whytree") {
        validationError = "Invalid file. Format must be 'whytree'."
        false
      } else {
        val tree = obj.getJSONObject("tree")
        val nodes = obj.getJSONArray("nodes")
        previewTitle = tree.optString("title", "Untitled")
        previewNodeCount = nodes.length()
        validationError = null
        true
      }
    } catch (e: Exception) {
      validationError = "Invalid JSON: ${e.localizedMessage ?: "Parsing error"}"
      false
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceElevatedNavy,
    title = {
      Text(
        text = "Import .why Backup",
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Paste the .why JSON contents below. It will be created as a new tree; your existing trees will never be overwritten.",
          color = TextSecondary,
          fontSize = 12.sp
        )

        OutlinedTextField(
          value = jsonContent,
          onValueChange = {
            jsonContent = it
            validateJson(it)
          },
          placeholder = { Text("{\n  \"format\": \"whytree\",\n  \"tree\": { ... }\n}") },
          minLines = 4,
          maxLines = 8,
          isError = validationError != null,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_import_json"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SoftGold,
            unfocusedBorderColor = CardBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )

        if (previewTitle != null && previewNodeCount != null && validationError == null) {
          Text(
            text = "✓ Valid Why Tree: \"$previewTitle\" ($previewNodeCount nodes)",
            color = Color(0xFF10B981),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        if (validationError != null) {
          Text(
            text = validationError ?: "",
            color = Color(0xFFEF4444),
            fontSize = 12.sp
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (validateJson(jsonContent)) {
            onImport(jsonContent.trim())
          }
        },
        enabled = validationError == null && jsonContent.isNotBlank(),
        colors = ButtonDefaults.buttonColors(
          containerColor = SoftGold,
          contentColor = MidnightNavy
        ),
        modifier = Modifier.testTag("button_confirm_import")
      ) {
        Text("Import Tree", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
