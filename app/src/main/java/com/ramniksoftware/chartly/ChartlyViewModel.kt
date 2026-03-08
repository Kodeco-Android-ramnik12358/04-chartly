package com.ramniksoftware.chartly

import androidx.lifecycle.ViewModel
import com.ramniksoftware.chartly.model.Node
import com.ramniksoftware.chartly.repositories.NodeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class ChartlyViewModel(private val nodeManager: NodeManager) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartlyUiState())
    val uiState: StateFlow<ChartlyUiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun selectNode(id: UUID?) {
        _uiState.update { it.copy(selectedNodeId = id) }
    }

    fun indentSelectedNode() {
        val id = _uiState.value.selectedNodeId ?: return
        nodeManager.indentNode(id)
        refreshState()
    }

    fun outdentSelectedNode() {
        val id = _uiState.value.selectedNodeId ?: return
        nodeManager.outdentNode(id)
        refreshState()
    }

    fun toggleExpansion(nodeId: UUID) {
        // 1. Tell the manager to flip the bit
        nodeManager.toggleExpansion(nodeId)

        // 2. Immediately re-flatten the tree to reflect the change in the UI
        refreshState()
    }

    fun updateNodeContent(nodeId: UUID, newContent: String) {
        nodeManager.updateNodeContent(nodeId, newContent)
        // We refresh the state so the flattened list has the latest content
        refreshState()
    }

    private fun refreshState() {
        val rootNodes = nodeManager.getRootNodes()
        val flatList = mutableListOf<FlattenedNode>()

        // For every root, kick off the recursive flattening
        rootNodes.forEach { root ->
            flatten(root, 0, flatList)
        }

        _uiState.value = ChartlyUiState(items = flatList, selectedNodeId = _uiState.value.selectedNodeId)
    }

    private fun flatten(node: Node, depth: Int, result: MutableList<FlattenedNode>) {
        // 1. Add the current node
        result.add(FlattenedNode(node, depth))

        // 2. Recursively add all children, incrementing the depth
        // ONLY continue if the node is expanded
        if (node.isExpanded) {
            node.childrenIds.forEach { childId ->
                nodeManager.getNode(childId)?.let { flatten(it, depth + 1, result) }
            }
        }
    }}