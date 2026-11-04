package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

@Composable
fun HomeScreen(
  trees: List<TreeEntity>,
  trashCount: Int,
  onOpenTree: (String) -> Unit,
  onCreateNewTree: () -> Unit,
  onOpenTrash: () -> Unit,
  onImportBackup: () -> Unit,
  onDuplicateTree: (String) -> Unit,
  onEditTreeSettings: (TreeEntity) -> Unit,
  onMoveToTrash: (String) -> Unit,
  onExportTree: (TreeEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }

  val filteredTrees = remember(trees, searchQuery) {
    if (searchQuery.isBlank()) trees
    else trees.filter {
      it.title.contains(searchQuery, ignoreCase = true) ||
        it.description.contains(searchQuery, ignoreCase = true)
    }
  }

  Scaffold(
    containerColor = MidnightNavy,
    floatingActionButton = {
      FloatingActionButton(
        onClick = onCreateNewTree,
        containerColor = SoftGold,
        contentColor = MidnightNavy,
        shape = CircleShape,
        modifier = Modifier.testTag("fab_create_tree")
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Create Tree",
          modifier = Modifier.size(28.dp)
        )
      }
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Header Section
      Surface(
        color = DeepNavy,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .background(SoftGold.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                  .border(1.dp, SoftGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Psychology,
                  contentDescription = "Why Tree Logo",
                  tint = SoftGold,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "WHY TREE",
                  color = TextPrimary,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = 1.sp
                )
                Text(
                  text = "Visual Thinking & Root-Cause Explorer",
                  color = TextSecondary,
                  fontSize = 11.sp
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onImportBackup,
                modifier = Modifier.testTag("btn_import_backup")
              ) {
                Icon(
                  imageVector = Icons.Default.FileUpload,
                  contentDescription = "Import .why Backup",
                  tint = TextSecondary
                )
              }

              IconButton(
                onClick = onOpenTrash,
                modifier = Modifier.testTag("btn_open_trash")
              ) {
                BadgedBox(
                  badge = {
                    if (trashCount > 0) {
                      Badge(containerColor = SoftGold, contentColor = MidnightNavy) {
                        Text("$trashCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                ) {
                  Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Trash",
                    tint = TextSecondary
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Search Bar
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search your why trees...", color = TextMuted) },
            leadingIcon = {
              Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = SoftGold)
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("home_search_bar"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SurfaceCard,
              unfocusedContainerColor = SurfaceCard,
              focusedBorderColor = SoftGold,
              unfocusedBorderColor = CardBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )
        }
      }

      // Trees List or Empty State
      if (filteredTrees.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.AccountTree,
              contentDescription = "No Trees",
              tint = SoftGold.copy(alpha = 0.5f),
              modifier = Modifier.size(56.dp)
            )
            Text(
              text = if (searchQuery.isNotBlank()) "No trees match \"$searchQuery\"" else "No active trees",
              color = TextPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Create a new central root card to start uncovering causes and relationships.",
              color = TextSecondary,
              fontSize = 13.sp,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(filteredTrees, key = { it.id }) { tree ->
            TreeItemCard(
              tree = tree,
              onOpen = { onOpenTree(tree.id) },
              onEditSettings = { onEditTreeSettings(tree) },
              onDuplicate = { onDuplicateTree(tree.id) },
              onExport = { onExportTree(tree) },
              onMoveToTrash = { onMoveToTrash(tree.id) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TreeItemCard(
  tree: TreeEntity,
  onOpen: () -> Unit,
  onEditSettings: () -> Unit,
  onDuplicate: () -> Unit,
  onExport: () -> Unit,
  onMoveToTrash: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }

  val relativeTime = remember(tree.updatedAt) {
    DateUtils.getRelativeTimeSpanString(
      tree.updatedAt,
      System.currentTimeMillis(),
      DateUtils.MINUTE_IN_MILLIS,
      DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
  }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = SurfaceCard,
    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
    shadowElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable { onOpen() }
      .testTag("tree_card_${tree.id}")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(16.dp)
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .background(SoftGold.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
          .border(1.dp, SoftGold.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.AccountTree,
          contentDescription = "Tree",
          tint = SoftGold,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = tree.title,
          color = TextPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (tree.description.isNotBlank()) {
          Text(
            text = tree.description,
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Updated $relativeTime",
          color = TextMuted,
          fontSize = 11.sp
        )
      }

      Box {
        IconButton(
          onClick = { showMenu = true },
          modifier = Modifier.testTag("tree_menu_${tree.id}")
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Options",
            tint = TextSecondary
          )
        }

        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          modifier = Modifier.background(SurfaceElevatedNavy)
        ) {
          DropdownMenuItem(
            text = { Text("Open Canvas", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null, tint = SoftGold) },
            onClick = { showMenu = false; onOpen() }
          )
          DropdownMenuItem(
            text = { Text("Rename / Details", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary) },
            onClick = { showMenu = false; onEditSettings() }
          )
          DropdownMenuItem(
            text = { Text("Duplicate Tree", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary) },
            onClick = { showMenu = false; onDuplicate() }
          )
          DropdownMenuItem(
            text = { Text("Export (PNG, .why, MD)", color = TextPrimary) },
            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = TextSecondary) },
            onClick = { showMenu = false; onExport() }
          )
          DropdownMenuItem(
            text = { Text("Move to Trash", color = Color(0xFFEF4444)) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
            onClick = { showMenu = false; onMoveToTrash() }
          )
        }
      }
    }
  }
}
