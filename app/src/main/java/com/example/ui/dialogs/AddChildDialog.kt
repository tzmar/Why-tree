package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BranchDirection
import com.example.data.model.NodeEntity
import com.example.data.model.RelationshipType
import com.example.ui.theme.CardBorder
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevatedNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddChildDialog(
  parentNode: NodeEntity,
  onDismiss: () -> Unit,
  onConfirm: (
    rel: RelationshipType,
    customLabel: String,
    text: String,
    notes: String,
    direction: BranchDirection
  ) -> Unit
) {
  var selectedRel by remember { mutableStateOf(RelationshipType.WHY) }
  var customLabel by remember { mutableStateOf("") }
  var explanationText by remember { mutableStateOf("") }
  var notesText by remember { mutableStateOf("") }
  var selectedDirection by remember { mutableStateOf(BranchDirection.RIGHT) }
  var isError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceElevatedNavy,
    title = {
      Column {
        Text(
          text = "Expand Tree: Add Branch",
          color = TextPrimary,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Branching from: \"${parentNode.text.take(40)}${if (parentNode.text.length > 40) "..." else ""}\"",
          color = TextSecondary,
          fontSize = 12.sp
        )
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Step 1: Relationship Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "1. RELATIONSHIP",
            color = SoftGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            RelationshipType.entries.forEach { rel ->
              val isSelected = selectedRel == rel
              val color = rel.defaultColor
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) color.copy(alpha = 0.25f) else SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isSelected) color else CardBorder
                ),
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { selectedRel = rel }
                  .testTag("rel_selector_${rel.name}")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .background(color, RoundedCornerShape(4.dp))
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = rel.label,
                    color = if (isSelected) color else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
          if (selectedRel == RelationshipType.CUSTOM) {
            OutlinedTextField(
              value = customLabel,
              onValueChange = { customLabel = it },
              placeholder = { Text("Custom relationship label (e.g. Depends on, Blocks)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftGold,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              )
            )
          }
        }

        // Step 2: Explanation Input
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "2. EXPLANATION / REASON",
            color = SoftGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          OutlinedTextField(
            value = explanationText,
            onValueChange = {
              explanationText = it
              if (it.isNotBlank()) isError = false
            },
            placeholder = { Text("What is the reason, mechanism, or evidence?") },
            isError = isError,
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_explanation"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SoftGold,
              unfocusedBorderColor = CardBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )
          if (isError) {
            Text(
              text = "Please enter an explanation for the branch.",
              color = Color(0xFFEF4444),
              fontSize = 11.sp
            )
          }

          OutlinedTextField(
            value = notesText,
            onValueChange = { notesText = it },
            placeholder = { Text("Optional details, data references, notes") },
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

        // Step 3: Direction Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "3. BRANCH DIRECTION",
            color = SoftGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            listOf(
              BranchDirection.RIGHT to Icons.AutoMirrored.Filled.ArrowForward,
              BranchDirection.DOWN to Icons.Default.ArrowDownward,
              BranchDirection.LEFT to Icons.AutoMirrored.Filled.ArrowBack,
              BranchDirection.UP to Icons.Default.ArrowUpward
            ).forEach { (dir, icon) ->
              val isSelected = selectedDirection == dir
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) SoftGold.copy(alpha = 0.2f) else SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isSelected) SoftGold else CardBorder
                ),
                modifier = Modifier
                  .weight(1f)
                  .clickable { selectedDirection = dir }
                  .testTag("direction_${dir.name}")
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.padding(vertical = 8.dp)
                ) {
                  Icon(
                    imageVector = icon,
                    contentDescription = dir.label,
                    tint = if (isSelected) SoftGold else TextSecondary,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = dir.label,
                    color = if (isSelected) SoftGold else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (explanationText.isBlank()) {
            isError = true
          } else {
            onConfirm(
              selectedRel,
              customLabel.trim(),
              explanationText.trim(),
              notesText.trim(),
              selectedDirection
            )
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = SoftGold,
          contentColor = MidnightNavy
        ),
        modifier = Modifier.testTag("button_save_branch")
      ) {
        Text("Add Branch", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
