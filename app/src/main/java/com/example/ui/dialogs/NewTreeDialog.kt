package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBorder
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevatedNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun NewTreeDialog(
  onDismiss: () -> Unit,
  onCreate: (title: String, rootQuestion: String, description: String) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var rootQuestion by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceElevatedNavy,
    title = {
      Text(
        text = "Start a New Why Tree",
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
        // Quick Starters
        Text(
          text = "STARTER TEMPLATES",
          color = SoftGold,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          TemplatePill(
            icon = Icons.Default.Psychology,
            label = "5 Whys",
            onClick = {
              title = "5 Whys: Incident Analysis"
              rootQuestion = "Why did the failure occur?"
              description = "Systematic 5 Whys iterative questioning"
            }
          )
          TemplatePill(
            icon = Icons.Default.AccountTree,
            label = "Decision",
            onClick = {
              title = "Decision: Architecture Choice"
              rootQuestion = "What framework or system should we adopt?"
              description = "Evaluating evidence and consequences"
            }
          )
          TemplatePill(
            icon = Icons.Default.Lightbulb,
            label = "Idea Why",
            onClick = {
              title = "Idea: Why Build Feature X?"
              rootQuestion = "Why is this problem valuable to solve?"
              description = "Validating user thesis and problem space"
            }
          )
        }

        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it
            if (it.isNotBlank()) isError = false
          },
          label = { Text("Tree Title") },
          placeholder = { Text("e.g. Why is customer churn up?") },
          singleLine = true,
          isError = isError,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("new_tree_title_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SoftGold,
            unfocusedBorderColor = CardBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )

        OutlinedTextField(
          value = rootQuestion,
          onValueChange = { rootQuestion = it },
          label = { Text("Central Root Card Question / Problem") },
          placeholder = { Text("The initial problem or question to expand from") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("new_tree_root_input"),
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
          label = { Text("Context or Scope (optional)") },
          placeholder = { Text("Background information, date, team context") },
          minLines = 1,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth(),
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
          if (title.isBlank()) {
            isError = true
          } else {
            onCreate(
              title.trim(),
              if (rootQuestion.isNotBlank()) rootQuestion.trim() else title.trim(),
              description.trim()
            )
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = SoftGold,
          contentColor = MidnightNavy
        ),
        modifier = Modifier.testTag("button_create_new_tree")
      ) {
        Text("Create Tree", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}

@Composable
private fun TemplatePill(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = SurfaceCard,
    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
    modifier = Modifier
      .clickable { onClick() }
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = SoftGold,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = label,
        color = TextPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}
