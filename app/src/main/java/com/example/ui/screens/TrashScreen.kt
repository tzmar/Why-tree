package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TreeEntity
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevatedNavy
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
  trashTrees: List<TreeEntity>,
  onBack: () -> Unit,
  onRestore: (treeId: String) -> Unit,
  onDeletePermanently: (treeId: String) -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onBack() }

  var treeToDeleteForever by remember { mutableStateOf<TreeEntity?>(null) }

  Scaffold(
    containerColor = MidnightNavy,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = DeepNavy,
          titleContentColor = TextPrimary
        ),
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("trash_back_btn")) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = TextPrimary
            )
          }
        },
        title = {
          Column {
            Text(
              text = "Trash",
              color = TextPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Items kept for 30 days before permanent deletion",
              color = TextSecondary,
              fontSize = 11.sp
            )
          }
        }
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    if (trashTrees.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Empty Trash",
            tint = TextMuted,
            modifier = Modifier.size(56.dp)
          )
          Text(
            text = "Trash is empty",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Deleted trees will appear here for 30 days.",
            color = TextSecondary,
            fontSize = 13.sp
          )
        }
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
      ) {
        items(trashTrees, key = { it.id }) { tree ->
          TrashItemCard(
            tree = tree,
            onRestore = { onRestore(tree.id) },
            onDeleteForever = { treeToDeleteForever = tree }
          )
        }
      }
    }
  }

  if (treeToDeleteForever != null) {
    val tree = treeToDeleteForever!!
    AlertDialog(
      onDismissRequest = { treeToDeleteForever = null },
      containerColor = SurfaceElevatedNavy,
      title = {
        Text(
          text = "Permanently Delete Tree?",
          color = TextPrimary,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Text(
          text = "Are you sure you want to permanently delete \"${tree.title}\"? This action cannot be undone.",
          color = TextSecondary,
          fontSize = 13.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            treeToDeleteForever = null
            onDeletePermanently(tree.id)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFDC2626),
            contentColor = Color.White
          ),
          modifier = Modifier.testTag("confirm_delete_forever_btn")
        ) {
          Text("Delete Forever")
        }
      },
      dismissButton = {
        TextButton(onClick = { treeToDeleteForever = null }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun TrashItemCard(
  tree: TreeEntity,
  onRestore: () -> Unit,
  onDeleteForever: () -> Unit
) {
  val daysRemaining = remember(tree.deletedAt) {
    tree.daysUntilPermanentDeletion()
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = SurfaceCard,
    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = tree.title,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "$daysRemaining days left until permanent removal",
            color = if (daysRemaining <= 3) Color(0xFFEF4444) else SoftGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Button(
          onClick = onRestore,
          colors = ButtonDefaults.buttonColors(
            containerColor = SoftGold,
            contentColor = MidnightNavy
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("trash_restore_${tree.id}")
        ) {
          Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = onDeleteForever,
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFFEF4444)
          ),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
          modifier = Modifier
            .weight(1f)
            .testTag("trash_delete_forever_${tree.id}")
        ) {
          Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Delete Forever", fontSize = 12.sp)
        }
      }
    }
  }
}
