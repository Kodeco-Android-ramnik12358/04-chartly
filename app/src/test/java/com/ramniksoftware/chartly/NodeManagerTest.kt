package com.ramniksoftware.chartly

import com.ramniksoftware.chartly.model.Node
import com.ramniksoftware.chartly.repositories.NodeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        assertNull(root1?.parentId)
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

    @Test
    fun `outdentNode moves child to become sibling of its parent`() {
        // 1. Arrange: Root -> Child
        val root = Node(content = "Parent")
        val child = Node(content = "Child to Outdent")
        nodeManager.addNode(root)
        nodeManager.addNode(child, parent = root)

        // 2. Act
        nodeManager.outdentNode(child.id)

        // 3. Assert
        val updatedChild = nodeManager.getNode(child.id)
        val updatedRoot = nodeManager.getNode(root.id)

        // Child should now be a root (parentId is null)
        assertNull(updatedChild?.parentId)
        // Parent should no longer have this child
        assertFalse(updatedRoot!!.childrenIds.contains(child.id))
        // Child should be in the root list
        assertTrue(nodeManager.getRootNodes().map { it.id }.contains(child.id))
    }

    @Test
    fun `outdentNode moves node from level 2 to level 1`() {
        // 1. Arrange: G-Parent -> Parent -> Child
        val gp = Node(content = "Grandparent")
        val p = Node(content = "Parent")
        val c = Node(content = "Child")
        nodeManager.addNode(gp)
        nodeManager.addNode(p, parent = gp)
        nodeManager.addNode(c, parent = p)

        // 2. Act
        nodeManager.outdentNode(c.id)

        // 3. Assert
        val updatedChild = nodeManager.getNode(c.id)
        assertEquals("Child should now be a child of Grandparent", gp.id, updatedChild?.parentId)

        val updatedGP = nodeManager.getNode(gp.id)
        assertTrue(updatedGP!!.childrenIds.contains(c.id))
    }

    @Test
    fun `outdentNode does nothing if node is already a root`() {
        // 1. Arrange
        val root = Node(content = "I am already root")
        nodeManager.addNode(root)

        // 2. Act
        nodeManager.outdentNode(root.id)

        // 3. Assert: State is unchanged
        val retrieved = nodeManager.getNode(root.id)
        assertNull(retrieved?.parentId)
        assertTrue(nodeManager.getRootNodes().contains(retrieved))
    }

    @Test
    fun `outdentNode moves the entire subtree`() {
        // 1. Arrange: Root -> Parent -> Child
        val r = Node(content = "Root")
        val p = Node(content = "Parent")
        val c = Node(content = "Child")
        nodeManager.addNode(r)
        nodeManager.addNode(p, parent = r)
        nodeManager.addNode(c, parent = p)

        // 2. Act: Outdent 'Parent'
        nodeManager.outdentNode(p.id)

        // 3. Assert
        val updatedP = nodeManager.getNode(p.id)
        val updatedC = nodeManager.getNode(c.id)

        assertNull(updatedP?.parentId) // Parent is now root
        assertEquals(p.id, updatedC?.parentId) // Child is STILL under Parent
    }

    @Test
    fun `toggleExpansion flips the isExpanded state of a node`() {
        // 1. Arrange
        val node = Node(content = "Test Node", isExpanded = true)
        nodeManager.addNode(node)

        // 2. Act: Toggle to false
        nodeManager.toggleExpansion(node.id)

        // 3. Assert
        val collapsedNode = nodeManager.getNode(node.id)
        assertEquals(false, collapsedNode?.isExpanded)

        // 4. Act: Toggle back to true
        nodeManager.toggleExpansion(node.id)

        // 5. Assert
        val expandedNode = nodeManager.getNode(node.id)
        assertEquals(true, expandedNode?.isExpanded)
    }

    @Test
    fun `toggleExpansion preserves children and content`() {
        // 1. Arrange
        val parent = Node(content = "Parent")
        val child = Node(content = "Child")
        nodeManager.addNode(parent)
        nodeManager.addNode(child, parent = parent)

        // 2. Act
        nodeManager.toggleExpansion(parent.id)

        // 3. Assert
        val updatedParent = nodeManager.getNode(parent.id)
        assertEquals("Parent", updatedParent?.content)
        assertEquals(1, updatedParent?.childrenIds?.size)
        assertEquals(child.id, updatedParent?.childrenIds?.get(0))
    }

    @Test
    fun `updateNodeContent replaces the content string of an existing node`() {
        // 1. Arrange
        val originalNode = Node(content = "Original Content")
        nodeManager.addNode(originalNode)

        // 2. Act
        val updatedText = "Updated Content"
        nodeManager.updateNodeContent(originalNode.id, updatedText)

        // 3. Assert
        val retrieved = nodeManager.getNode(originalNode.id)
        assertEquals(updatedText, retrieved?.content)
    }

    @Test
    fun `updateNodeContent preserves node hierarchy and metadata`() {
        // 1. Arrange
        val parent = Node(content = "Parent")
        val child = Node(content = "Child")
        nodeManager.addNode(parent)
        nodeManager.addNode(child, parent = parent)

        // 2. Act
        nodeManager.updateNodeContent(parent.id, "New Parent Name")

        // 3. Assert
        val updatedParent = nodeManager.getNode(parent.id)
        assertEquals(1, updatedParent?.childrenIds?.size)
        assertEquals(child.id, updatedParent?.childrenIds?.get(0))
        // Expansion state should also be preserved
        assertTrue(updatedParent?.isExpanded == true)
    }
}
