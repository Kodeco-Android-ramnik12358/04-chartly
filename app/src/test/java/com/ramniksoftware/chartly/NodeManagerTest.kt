package com.ramniksoftware.chartly

import com.ramniksoftware.chartly.model.Node
import com.ramniksoftware.chartly.repositories.NodeManager
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
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

    @Test
    fun `indentNode moves a node to become a child of its previous sibling`() {
        // 1. Arrange
        val root1 = Node(content = "First Root")
        val root2 = Node(content = "Second Root") // This is the one we will indent
        nodeManager.addNode(root1)
        nodeManager.addNode(root2)

        // 2. Act
        nodeManager.indentNode(root2.id)

        // 3. Assert
        val updatedRoot1 = nodeManager.getNode(root1.id)
        val updatedRoot2 = nodeManager.getNode(root2.id)

        // root2 should now have root1 as its parent
        assertEquals(root1.id, updatedRoot2?.parentId)
        // root1 should now have root2 in its children list
        assertTrue(updatedRoot1!!.childrenIds.contains(root2.id))
        // root2 should no longer be a root node
        assertFalse(nodeManager.getRootNodes().contains(updatedRoot2))
    }

    @Test
    fun `indentNode handles non-existent nodeId gracefully`() {
        // 1. Arrange: Create a clean manager with one node
        val existingNode = Node(content = "Existing")
        nodeManager.addNode(existingNode)
        val nonExistentId = UUID.randomUUID()

        // 2. Act: Try to indent a ghost ID
        // This should NOT crash
        nodeManager.indentNode(nonExistentId)

        // 3. Assert: The existing node is untouched
        val rootNodes = nodeManager.getRootNodes()
        assertEquals(1, rootNodes.size)
        assertEquals("Existing", rootNodes[0].content)
    }

    @Test
    fun `indentNode does nothing if the node is the first in its list`() {
        // 1. Arrange: Two nodes, but we try to indent the first one
        val firstRoot = Node(content = "I am first")
        val secondRoot = Node(content = "I am second")
        nodeManager.addNode(firstRoot)
        nodeManager.addNode(secondRoot)

        // 2. Act: Try to indent the first node
        nodeManager.indentNode(firstRoot.id)

        // 3. Assert: Hierarchy remains unchanged
        val root1 = nodeManager.getNode(firstRoot.id)
        assertNull("First node should still have no parent", root1?.parentId)
        assertEquals(0, root1?.childrenIds?.size)

        // Ensure it's still at the top of the root list
        assertEquals(firstRoot.id, nodeManager.getRootNodes()[0].id)
    }

    @Test
    fun `indentNode moves the entire subtree when indenting a parent`() {
        // 1. Arrange:
        // Root 1
        // Root 2
        //   -> Child 2.1
        val r1 = Node(content = "Root 1")
        val r2 = Node(content = "Root 2")
        val c2_1 = Node(content = "Child 2.1")

        nodeManager.addNode(r1)
        nodeManager.addNode(r2)
        nodeManager.addNode(c2_1, parent = r2)

        // 2. Act: Indent Root 2 (which has a child)
        nodeManager.indentNode(r2.id)

        // 3. Assert
        val updatedR2 = nodeManager.getNode(r2.id)
        val updatedC2_1 = nodeManager.getNode(c2_1.id)

        // Root 2 moved under Root 1
        assertEquals(r1.id, updatedR2?.parentId)

        // Child 2.1 is STILL under Root 2
        assertEquals(r2.id, updatedC2_1?.parentId)
        assertTrue(updatedR2!!.childrenIds.contains(c2_1.id))
    }
}