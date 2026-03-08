package com.ramniksoftware.chartly

import com.ramniksoftware.chartly.model.Node
import com.ramniksoftware.chartly.repositories.NodeManager
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test

class NodeManagerTest {

    private lateinit var nodeManager: NodeManager

    @Before
    fun setup() {
        nodeManager = NodeManager()
    }

    @Test
    fun `addNode without parent creates a root node`() {
        val rootNode = Node(content = "Root Task")
        nodeManager.addNode(rootNode)

        val retrieved = nodeManager.getNode(rootNode.id)
        assertNotNull(retrieved)
        assertNull(retrieved?.parentId)
    }

    @Test
    fun `getRootNodes returns nodes in the order they were added`() {
        // Arrange
        val node1 = Node(content = "First Task")
        val node2 = Node(content = "Second Task")
        val node3 = Node(content = "Third Task")

        // Act
        nodeManager.addNode(node1)
        nodeManager.addNode(node2)
        nodeManager.addNode(node3)

        val roots = nodeManager.getRootNodes()

        // Assert
        assertEquals(3, roots.size)
        assertEquals("First Task", roots[0].content)
        assertEquals("Second Task", roots[1].content)
        assertEquals("Third Task", roots[2].content)
    }

    @Test
    fun `addNode with parent updates bidirectional links immediately`() {
        // Arrange
        val parent = Node(content = "Parent")
        nodeManager.addNode(parent)
        val child = Node(content = "Child")

        // Act - Using your suggested API
        nodeManager.addNode(child, parent = parent)

        // Assert
        val updatedParent = nodeManager.getNode(parent.id)
        val updatedChild = nodeManager.getNode(child.id)

        // Verify the parent knows about the child
        assertTrue(updatedParent!!.childrenIds.contains(child.id))
        // Verify the child knows about the parent
        assertEquals(parent.id, updatedChild!!.parentId)
    }
}