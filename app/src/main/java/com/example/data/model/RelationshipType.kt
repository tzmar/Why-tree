package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.RelCustom
import com.example.ui.theme.RelEvidence
import com.example.ui.theme.RelHow
import com.example.ui.theme.RelResult
import com.example.ui.theme.RelWhat
import com.example.ui.theme.RelWhy

enum class RelationshipType(
  val label: String,
  val description: String,
  val defaultColor: Color
) {
  WHY("WHY", "Root reason or causation", RelWhy),
  HOW("HOW", "Mechanism or procedure", RelHow),
  WHAT("WHAT", "Definition or breakdown", RelWhat),
  RESULT("RESULT", "Outcome or consequence", RelResult),
  EVIDENCE("EVIDENCE", "Proof, data or observation", RelEvidence),
  CUSTOM("CUSTOM", "Custom relationship", RelCustom);

  companion object {
    fun fromString(value: String): RelationshipType {
      return entries.find { it.name.equals(value, ignoreCase = true) } ?: WHY
    }
  }
}
