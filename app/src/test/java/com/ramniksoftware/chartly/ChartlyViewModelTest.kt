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
    fun `selectNode updates the uiState with the correct ID`() = runTest {
        val viewModel = createViewModel()
        val testId = UUID.randomUUID()

        // Act
        viewModel.selectNode(testId)

        // Assert
        assertEquals(testId, viewModel.uiState.value.selectedNodeId)
    }

    @Test
    fun `indentSelectedNode calls manager and refreshes state`() = runTest {
        // 1. Arrange
        val root1 = Node(content = "Root 1")
        val root2 = Node(content = "Root 2")
        nodeManager.addNode(root1)
        nodeManager.addNode(root2)

        val viewModel = createViewModel()
        viewModel.selectNode(root2.id)

        // 2. Act
        viewModel.indentSelectedNode()

        // 3. Assert
        val items = viewModel.uiState.value.items
        assertEquals(2, items.size)
        assertEquals("Root 2", items[1].node.content)
        assertEquals(1, items[1].depth)
    }

    @Test
    fun `outdentSelectedNode calls manager and refreshes state`() = runTest {
        // 1. Arrange
        val root = Node(content = "Parent")
        val child = Node(content = "Child")
        nodeManager.addNode(root)
        nodeManager.addNode(child, parent = root)

        val viewModel = createViewModel()
        viewModel.selectNode(child.id)

        // 2. Act
        viewModel.outdentSelectedNode()

        // 3. Assert
        val items = viewModel.uiState.value.items
        assertTrue(items.all { it.depth == 0 })
    }

    @Test
    fun `indentSelectedNode does nothing if no node is selected`() = runTest {
        // 1. Arrange
        nodeManager.addNode(Node(content = "Test"))
        val viewModel = createViewModel()

        // 2. Act
        viewModel.indentSelectedNode()

        // 3. Assert
        val items = viewModel.uiState.value.items
        assertEquals(1, items.size)
        assertEquals(0, items[0].depth)
    }
}