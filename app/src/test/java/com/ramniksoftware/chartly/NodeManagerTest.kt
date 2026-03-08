package com.ramniksoftware.chartly

import com.ramniksoftware.chartly.model.Node
import com.ramniksoftware.chartly.repositories.NodeManager
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

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

    @Test
    fun `addNode supports deep nesting with correct bidirectional links`() {
        // 1. Arrange: Create the hierarchy
        val grandparent = Node(content = "Grandparent")
        val parent = Node(content = "Parent")
        val child = Node(content = "Child")

        // 2. Act: Add them one by one
        nodeManager.addNode(grandparent) // Level 0
        nodeManager.addNode(parent, parent = grandparent) // Level 1
        nodeManager.addNode(child, parent = parent) // Level 2

        // 3. Assert: Verify Level 2 -> Level 1 link
        val retrievedChild = nodeManager.getNode(child.id)
        assertEquals(parent.id, retrievedChild?.parentId)

        // 4. Assert: Verify Level 1 -> Level 2 link
        val retrievedParent = nodeManager.getNode(parent.id)
        assertTrue(retrievedParent!!.childrenIds.contains(child.id))
        assertEquals(grandparent.id, retrievedParent.parentId)

        // 5. Assert: Verify Level 0 -> Level 1 link
        val retrievedGrandparent = nodeManager.getNode(grandparent.id)
        assertTrue(retrievedGrandparent!!.childrenIds.contains(parent.id))
        assertNull(retrievedGrandparent.parentId)
    }

    @Test
    fun `addNode handles 50 levels of nesting`() {
        var lastNode = Node(content = "Level 0")
        nodeManager.addNode(lastNode)

        for (i in 1..50) {
            val newNode = Node(content = "Level $i")
            nodeManager.addNode(newNode, parent = lastNode)
            lastNode = newNode // Move down the tree
        }

        val deepNode = nodeManager.getNode(lastNode.id)
        assertNotNull(deepNode)
        assertEquals(50, getDepth(deepNode!!.id)) // Helper to count parents
    }

    /**
     * Calculates depth by traversing up the parent chain.
     * Root nodes have a depth of 0.
     */
    private fun getDepth(nodeId: UUID): Int {
        val node = nodeManager.getNode(nodeId) ?: return 0
        val parentId = node.parentId ?: return 0

        // Recursive: 1 + depth of my parent
        return 1 + getDepth(parentId)
    }
}