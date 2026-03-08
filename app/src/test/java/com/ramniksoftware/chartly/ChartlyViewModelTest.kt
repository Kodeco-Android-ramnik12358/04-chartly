package com.ramniksoftware.chartly

import com.ramniksoftware.chartly.model.Node
import com.ramniksoftware.chartly.repositories.NodeManager
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test

class ChartlyViewModelTest {

    private lateinit var nodeManager: NodeManager
    private lateinit var viewModel: ChartlyViewModel

    @Before
    fun setup() {
        // Initialize a fresh manager for every test
        nodeManager = NodeManager()
        // Initialize the ViewModel with the fresh manager
        viewModel = ChartlyViewModel(nodeManager)
    }

    @Test
    fun `uiState emits nodes in correct depth-first order with accurate depths`() {
        // 1. Arrange
        val root1 = Node(content = "Root 1")
        val child1_1 = Node(content = "Child 1.1")
        val root2 = Node(content = "Root 2")

        // Use our addNode logic
        nodeManager.addNode(root1)
        nodeManager.addNode(child1_1, parent = root1)
        nodeManager.addNode(root2)

        // Since the VM was initialized empty, we need to refresh it
        // OR re-instantiate it after adding nodes to the manager.
        val testViewModel = ChartlyViewModel(nodeManager)

        // 2. Act
        val items = testViewModel.uiState.value.items

        // 3. Assert
        assertEquals(3, items.size)

        // Verify Root 1
        assertEquals("Root 1", items[0].node.content)
        assertEquals(0, items[0].depth)

        // Verify Child follows Parent
        assertEquals("Child 1.1", items[1].node.content)
        assertEquals(1, items[1].depth)
    }
}