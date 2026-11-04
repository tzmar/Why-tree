package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BranchDirection
import com.example.data.model.NodeEntity
import com.example.domain.layout.TreeLayoutEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Why Tree", appName)
  }

  @Test
  fun `test tree layout collision avoidance`() {
    val parent = NodeEntity("p1", "t1", "Parent", posX = 0f, posY = 0f, isRoot = true)
    val existing = listOf(
      parent,
      NodeEntity("c1", "t1", "Child 1", posX = 330f, posY = 0f)
    )

    val (newX, newY) = TreeLayoutEngine.calculateNewChildPosition(
      parent = parent,
      direction = BranchDirection.RIGHT,
      existingNodes = existing,
      siblingIndex = 0
    )

    // With existing node at (330, 0), candidate shifts down vertically to avoid collision
    assertTrue(newX >= 330f)
    assertTrue(newY != 0f)
  }
}
