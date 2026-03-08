package com.ramniksoftware.chartly

import com.ramniksoftware.chartly.model.Node
import com.ramniksoftware.chartly.repositories.NodeManager
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ChartlyViewModelTest {

    private lateinit var nodeManager: NodeManager

    @Before
    fun setup() {
        // Initialize a fresh manager for every test
        nodeManager = NodeManager()
    }

    /**
     * Helper to ensure the ViewModel is always initialized AFTER
     * nodes are added to the manager in the Arrange block.
     */
    private fun createViewModel(): ChartlyViewModel {
        return ChartlyViewModel(nodeManager)
    }

    @Test
    fun `uiState emits nodes in correct depth-first order with accurate depths`() {
        // 1. Arrange
        val root1 = Node(content = "Root 1")
        nodeManager.addNode(root1)

        // We add the child using the ID of the root we just added
        val child1_1 = Node(content = "Child 1.1")
        nodeManager.addNode(child1_1, parent = root1)

        val root2 = Node(content = "Root 2")
        nodeManager.addNode(root2)

        val viewModel = createViewModel()

        // 2. Act
        val items = viewModel.uiState.value.items

        // 3. Assert
        assertEquals(3, items.size) // This should now be 3
        assertEquals("Root 1", items[0].node.content)
        assertEquals(0, items[0].depth)
        assertEquals("Child 1.1", items[1].node.content)
        assertEquals(1, items[1].depth)
        assertEquals("Root 2", items[2].node.content)
        assertEquals(0, items[2].depth)
    }

    @Test
    fun `setFocusedNode updates the uiState with the correct ID`() = runTest {
        val viewModel = createViewModel()
        val testId = UUID.randomUUID()

        // Act
        viewModel.setFocusedNode(testId)

        // Assert
        assertEquals(testId, viewModel.uiState.value.focusedNodeId)
    }

    @Test
    fun `updating content does not clear the focusedNodeId`() = runTest {
        // 1. Arrange
        val node = Node(content = "Original")
        nodeManager.addNode(node)
        val viewModel = createViewModel()
        viewModel.setFocusedNode(node.id)

        // 2. Act: User types into the focused field
        viewModel.updateNodeContent(node.id, "Updated")

        // 3. Assert: The ID must remain the same so the Bottom Bar stays visible
        assertEquals(node.id, viewModel.uiState.value.focusedNodeId)
    }

    @Test
    fun `indentFocusedNode correctly indents the node currently holding focus`() = runTest {
        // 1. Arrange: Two root nodes
        val root1 = Node(content = "R1")
        val root2 = Node(content = "R2")
        nodeManager.addNode(root1)
        nodeManager.addNode(root2)

        val viewModel = createViewModel()

        // 2. Act: Set focus to R2 and indent
        viewModel.setFocusedNode(root2.id)
        viewModel.indentFocusedNode()

        // 3. Assert: R2 should now be at depth 1
        val items = viewModel.uiState.value.items
        assertEquals("R2", items[1].node.content)
        assertEquals(1, items[1].depth)
    }

    @Test
    fun `indentFocusedNode does nothing if focusedNodeId is null`() = runTest {
        nodeManager.addNode(Node(content = "Test"))
        val viewModel = createViewModel()

        // Ensure focus is null
        viewModel.setFocusedNode(null)

        // Act
        viewModel.indentFocusedNode()

        // Assert: No changes should have occurred to the depth
        assertEquals(0, viewModel.uiState.value.items[0].depth)
    }

    @Test
    fun `outdentFocusedNode calls manager and refreshes state`() = runTest {
        // 1. Arrange: Parent -> Child
        val root = Node(content = "Parent")
        val child = Node(content = "Child")
        nodeManager.addNode(root)
        nodeManager.addNode(child, parent = root)

        val viewModel = createViewModel()

        // Set focus to the child node
        viewModel.setFocusedNode(child.id)

        // 2. Act: Outdent the child while it has focus
        viewModel.outdentFocusedNode()

        // 3. Assert: Both should now be depth 0 (siblings)
        val items = viewModel.uiState.value.items
        assertTrue("All items should be at root depth", items.all { it.depth == 0 })
    }

    @Test
    fun `toggleExpansion in ViewModel updates the flattened list size`() = runTest {
        // 1. Arrange: Parent -> Child
        val parent = Node(content = "Parent")
        nodeManager.addNode(parent)
        nodeManager.addNode(Node(content = "Child"), parent = parent)

        val viewModel = createViewModel()

        // Initially, both should be visible
        assertEquals(2, viewModel.uiState.value.items.size)

        // 2. Act: Collapse the parent
        viewModel.toggleExpansion(parent.id)

        // 3. Assert: Only the parent should be in the list now
        val items = viewModel.uiState.value.items
        assertEquals(1, items.size)
        assertEquals("Parent", items[0].node.content)

        // 4. Act: Expand it again
        viewModel.toggleExpansion(parent.id)
        assertEquals(2, viewModel.uiState.value.items.size)
    }

    @Test
    fun `updateNodeContent in ViewModel updates the UI State immediately`() = runTest {
        // 1. Arrange
        val node = Node(content = "Before")
        nodeManager.addNode(node)

        // Initialize VM after adding node to avoid the "stale state" issue
        val viewModel = createViewModel()

        // Initial check
        assertEquals("Before", viewModel.uiState.value.items[0].node.content)

        // 2. Act
        val newText = "After"
        viewModel.updateNodeContent(node.id, newText)

        // 3. Assert
        val items = viewModel.uiState.value.items
        assertEquals(1, items.size)
        assertEquals(newText, items[0].node.content)
    }
}