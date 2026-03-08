package com.ramniksoftware.chartly.model

import java.util.UUID

data class Node(
    val id: UUID = UUID.randomUUID(),
    val parentId: UUID? = null,
    val childrenIds: List<UUID> = emptyList(),
    val content: String = "",
    val isCompleted: Boolean = false,
    val isExpanded: Boolean = true
)