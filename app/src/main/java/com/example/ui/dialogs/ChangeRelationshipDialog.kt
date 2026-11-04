package com.example.ui.dialogs

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BranchDirection
import com.example.data.model.ConnectionEntity
import com.example.data.model.RelationshipType
import com.example.ui.theme.CardBorder
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevatedNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChangeRelationshipDialog(
  connection: ConnectionEntity,
  onDismiss: () -> Unit,
  onSave: (rel: RelationshipType, customLabel: String, dir: BranchDirection) -> Unit
) {
  var selectedRel by remember { mutableStateOf(connection.relType) }
  var customLabel by remember { mutableStateOf(connection.customLabel) }
  var selectedDir by remember { mutableStateOf(connection.branchDir) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceElevatedNavy,
    title = {
      Text(
        text = "Change Relationship",
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "TYPE",
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
                  .testTag("change_rel_${rel.name}")
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
        }

        OutlinedTextField(
          value = customLabel,
          onValueChange = { customLabel = it },
          label = { Text("Custom Label (optional)") },
          placeholder = { Text(selectedRel.label) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SoftGold,
            unfocusedBorderColor = CardBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          )
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "CONNECTION DIRECTION",
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
              val isSelected = selectedDir == dir
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) SoftGold.copy(alpha = 0.2f) else SurfaceCard,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isSelected) SoftGold else CardBorder
                ),
                modifier = Modifier
                  .weight(1f)
                  .clickable { selectedDir = dir }
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
          onSave(selectedRel, customLabel.trim(), selectedDir)
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = SoftGold,
          contentColor = MidnightNavy
        ),
        modifier = Modifier.testTag("button_save_rel")
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
