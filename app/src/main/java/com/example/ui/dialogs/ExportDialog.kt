package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBorder
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevatedNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ExportDialog(
  onDismiss: () -> Unit,
  onExportPng: () -> Unit,
  onExportJson: () -> Unit,
  onExportMarkdown: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceElevatedNavy,
    title = {
      Text(
        text = "Export Tree",
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
        ExportOptionTile(
          icon = Icons.Default.Image,
          title = "Export as PNG Image",
          subtitle = "High-resolution diagram image of full tree",
          onClick = {
            onDismiss()
            onExportPng()
          },
          tag = "export_png"
        )

        ExportOptionTile(
          icon = Icons.Default.DataObject,
          title = "Export as .why Backup",
          subtitle = "Versioned JSON backup preserving all layout & connections",
          onClick = {
            onDismiss()
            onExportJson()
          },
          tag = "export_json"
        )

        ExportOptionTile(
          icon = Icons.Default.Code,
          title = "Export as Markdown Outline",
          subtitle = "Structured hierarchical text with relationship tags",
          onClick = {
            onDismiss()
            onExportMarkdown()
          },
          tag = "export_markdown"
        )
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = TextSecondary)
      }
    }
  )
}

@Composable
private fun ExportOptionTile(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  tag: String
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = SurfaceCard,
    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag(tag)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(12.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = SoftGold,
        modifier = Modifier.size(28.dp)
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = title,
          color = TextPrimary,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = subtitle,
          color = TextSecondary,
          fontSize = 11.sp
        )
      }
    }
  }
}
