package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NodeEntity
import com.example.ui.theme.CardBorder
import com.example.ui.theme.SurfaceElevatedNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ConfirmDeleteDialog(
  node: NodeEntity,
  descendantCount: Int,
  onDismiss: () -> Unit,
  onDeleteSubtree: () -> Unit,
  onDeleteNodeOnly: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceElevatedNavy,
    icon = {
      Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = "Warning",
        tint = Color(0xFFEF4444)
      )
    },
    title = {
      Text(
        text = "Delete Node?",
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "\"${node.text}\"",
          color = TextPrimary,
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.sp
        )
        if (descendantCount > 0) {
          Text(
            text = "This node has $descendantCount dependent sub-branch${if (descendantCount > 1) "es" else ""}. You can either delete this entire branch or reparent its children to the parent.",
            color = TextSecondary,
            fontSize = 13.sp
          )
        } else {
          Text(
            text = "Are you sure you want to remove this node from the tree?",
            color = TextSecondary,
            fontSize = 13.sp
          )
        }
      }
    },
    confirmButton = {
      Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        if (descendantCount > 0) {
          Button(
            onClick = onDeleteSubtree,
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFDC2626),
              contentColor = Color.White
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("button_delete_subtree")
          ) {
            Text("Delete Branch ($descendantCount child nodes)")
          }

          OutlinedButton(
            onClick = onDeleteNodeOnly,
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = TextPrimary
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("button_delete_node_only")
          ) {
            Text("Delete Node Only (Keep children)")
          }
        } else {
          Button(
            onClick = onDeleteNodeOnly,
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFDC2626),
              contentColor = Color.White
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("button_delete_single_node")
          ) {
            Text("Delete Node")
          }
        }
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}
