package com.ramniksoftware.chartly

import androidx.lifecycle.ViewModel
import com.ramniksoftware.chartly.model.Node
import com.ramniksoftware.chartly.repositories.NodeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ChartlyViewModel(private val nodeManager: NodeManager) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartlyUiState())
    val uiState: StateFlow<ChartlyUiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    private fun refreshState() {
        val rootNodes = nodeManager.getRootNodes()
        val flatList = mutableListOf<FlattenedNode>()

        // For every root, kick off the recursive flattening
        rootNodes.forEach { root ->
            flatten(root, 0, flatList)
        }

        _uiState.value = ChartlyUiState(items = flatList)
    }

    private fun flatten(node: Node, depth: Int, result: MutableList<FlattenedNode>) {
        // 1. Add the current node
        result.add(FlattenedNode(node, depth))

        // 2. Recursively add all children, incrementing the depth
        node.childrenIds.forEach { childId ->
            val childNode = nodeManager.getNode(childId)
            if (childNode != null) {
                flatten(childNode, depth + 1, result)
            }
        }
    }
}