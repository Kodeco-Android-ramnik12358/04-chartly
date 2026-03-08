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
}