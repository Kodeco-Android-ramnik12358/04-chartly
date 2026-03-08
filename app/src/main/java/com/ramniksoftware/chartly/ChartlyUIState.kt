package com.ramniksoftware.chartly

import com.ramniksoftware.chartly.model.Node
import java.util.UUID

data class FlattenedNode(
    val node: Node,
    val depth: Int
)

data class ChartlyUiState(
    val items: List<FlattenedNode> = emptyList(),
    val selectedNodeId: UUID? = null
)