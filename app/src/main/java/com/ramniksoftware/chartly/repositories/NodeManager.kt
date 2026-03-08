package com.ramniksoftware.chartly.repositories

import com.ramniksoftware.chartly.model.Node
import java.util.UUID

class NodeManager {
    // Stores everything for O(1) lookup
    private val allNodes = mutableMapOf<UUID, Node>()

    // Specifically tracks the order of top-level bullets
    private val rootIds = mutableListOf<UUID>()

    fun addNode(node: Node, parent: Node? = null) {
        if (parent == null) {
            allNodes[node.id] = node
            rootIds.add(node.id)
            return
        }

        // Since we aren't returning early, we handle the parent-child logic here
        val latestParent = allNodes[parent.id] ?: parent

        val linkedChild = node.copy(parentId = latestParent.id)
        allNodes[linkedChild.id] = linkedChild

        allNodes[latestParent.id] = latestParent.copy(
            childrenIds = latestParent.childrenIds + linkedChild.id
        )
    }

    fun getNode(id: UUID): Node? = allNodes[id]

    fun getRootNodes(): List<Node> {
        // Map the IDs back to the actual Node objects
        // Use filterNotNull to ensure safety, though IDs should always exist
        return rootIds.mapNotNull { allNodes[it] }
    }

    fun indentNode(nodeId: UUID) {
        val targetNode = allNodes[nodeId] ?: return
        val parentId = targetNode.parentId

        // 1. Get the list of siblings where this node currently lives
        val currentSiblings = if (parentId == null) {
            rootIds
        } else {
            allNodes[parentId]?.childrenIds ?: return
        }

        // 2. Find the index of our node
        val currentIndex = currentSiblings.indexOf(nodeId)

        // 3. Edge Case: If it's the first child, it can't be indented (nothing to move under)
        if (currentIndex <= 0) return

        // 4. The node above it becomes the NEW parent
        val newParentId = currentSiblings[currentIndex - 1]
        val newParent = allNodes[newParentId] ?: return

        // 5. Update the Node's parent reference
        allNodes[nodeId] = targetNode.copy(parentId = newParentId)

        // 6. Add the node to the new parent's children list
        allNodes[newParentId] = newParent.copy(
            childrenIds = newParent.childrenIds + nodeId
        )

        // 7. Remove the node from its old home
        if (parentId == null) {
            rootIds.remove(nodeId)
        } else {
            val oldParent = allNodes[parentId]!!
            allNodes[parentId] = oldParent.copy(
                childrenIds = oldParent.childrenIds - nodeId
            )
        }
    }
}