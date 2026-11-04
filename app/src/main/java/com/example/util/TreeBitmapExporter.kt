package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.FileProvider
import com.example.data.model.BranchDirection
import com.example.data.model.TreeWithDetails
import com.example.domain.layout.TreeLayoutEngine
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object TreeBitmapExporter {

  fun generateTreeBitmap(treeDetails: TreeWithDetails): Bitmap {
    val nodes = treeDetails.nodes
    if (nodes.isEmpty()) {
      return Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
    }

    val bounds = TreeLayoutEngine.calculateBoundingBox(nodes) ?: TreeLayoutEngine.RectF(0f, 0f, 400f, 300f)
    val padding = 80f

    val contentW = (bounds.right - bounds.left) + padding * 2
    val contentH = (bounds.bottom - bounds.top) + padding * 2

    val width = max(contentW, 800f).toInt()
    val height = max(contentH, 600f).toInt()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Midnight Navy Background
    canvas.drawColor(android.graphics.Color.parseColor("#090E17"))

    val offsetX = padding - bounds.left
    val offsetY = padding - bounds.top

    val linePaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = 4f
      strokeCap = Paint.Cap.ROUND
    }

    val cardPaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.FILL
      color = android.graphics.Color.parseColor("#152033")
    }

    val cardBorderPaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = 3f
      color = android.graphics.Color.parseColor("#283A56")
    }

    val rootBorderPaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = 4f
      color = android.graphics.Color.parseColor("#F3C64D")
    }

    val textPaint = Paint().apply {
      isAntiAlias = true
      color = android.graphics.Color.parseColor("#F1F5F9")
      textSize = 22f
      typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val notePaint = Paint().apply {
      isAntiAlias = true
      color = android.graphics.Color.parseColor("#94A3B8")
      textSize = 16f
    }

    val tagTextPaint = Paint().apply {
      isAntiAlias = true
      color = android.graphics.Color.parseColor("#090E17")
      textSize = 14f
      typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val cardW = TreeLayoutEngine.CARD_WIDTH
    val cardH = TreeLayoutEngine.CARD_HEIGHT

    // 1. Draw connections
    for (conn in treeDetails.connections) {
      val parent = treeDetails.nodeMap[conn.fromNodeId] ?: continue
      val child = treeDetails.nodeMap[conn.toNodeId] ?: continue

      val pL = parent.posX + offsetX
      val pT = parent.posY + offsetY
      val pR = pL + cardW
      val pB = pT + cardH
      val pCX = (pL + pR) / 2f
      val pCY = (pT + pB) / 2f

      val cL = child.posX + offsetX
      val cT = child.posY + offsetY
      val cR = cL + cardW
      val cB = cT + cardH
      val cCX = (cL + cR) / 2f
      val cCY = (cT + cB) / 2f

      val path = android.graphics.Path()

      val colorHex = when (conn.relType.name) {
        "WHY" -> "#F59E0B"
        "HOW" -> "#06B6D4"
        "WHAT" -> "#38BDF8"
        "RESULT" -> "#10B981"
        "EVIDENCE" -> "#8B5CF6"
        else -> "#FB7185"
      }
      linePaint.color = android.graphics.Color.parseColor(colorHex)

      when (conn.branchDir) {
        BranchDirection.RIGHT -> {
          path.moveTo(pR, pCY)
          val midX = pR + (cL - pR) * 0.5f
          path.cubicTo(midX, pCY, midX, cCY, cL, cCY)
        }
        BranchDirection.LEFT -> {
          path.moveTo(pL, pCY)
          val midX = pL + (cR - pL) * 0.5f
          path.cubicTo(midX, pCY, midX, cCY, cR, cCY)
        }
        BranchDirection.DOWN -> {
          path.moveTo(pCX, pB)
          val midY = pB + (cT - pB) * 0.5f
          path.cubicTo(pCX, midY, cCX, midY, cCX, cT)
        }
        BranchDirection.UP -> {
          path.moveTo(pCX, pT)
          val midY = pT + (cB - pT) * 0.5f
          path.cubicTo(pCX, midY, cCX, midY, cCX, cB)
        }
      }
      canvas.drawPath(path, linePaint)
    }

    // 2. Draw nodes
    for (node in treeDetails.nodes) {
      val x = node.posX + offsetX
      val y = node.posY + offsetY
      val rect = RectF(x, y, x + cardW, y + cardH)

      canvas.drawRoundRect(rect, 16f, 16f, cardPaint)
      canvas.drawRoundRect(rect, 16f, 16f, if (node.isRoot) rootBorderPaint else cardBorderPaint)

      val title = if (node.text.length > 28) node.text.take(28) + "..." else node.text
      canvas.drawText(title, x + 16f, y + 45f, textPaint)

      if (node.notes.isNotBlank()) {
        val note = if (node.notes.length > 32) node.notes.take(32) + "..." else node.notes
        canvas.drawText(note, x + 16f, y + 80f, notePaint)
      }
    }

    return bitmap
  }

  fun shareBitmap(context: Context, bitmap: Bitmap, title: String) {
    try {
      val cacheDir = context.cacheDir
      val file = File(cacheDir, "whytree_${System.currentTimeMillis()}.png")
      FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
      }

      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )

      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      val chooser = Intent.createChooser(intent, "Share Tree Diagram")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    } catch (e: Exception) {
      // Fallback intent without file provider
      e.printStackTrace()
    }
  }

  fun shareText(context: Context, text: String, subject: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, text)
      putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    val chooser = Intent.createChooser(intent, "Share")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
  }
}
