package com.ramniksoftware.chartly.model

import java.util.UUID

data class Node(
    val id: UUID = UUID.randomUUID(),
    val content: String,
    val parentId: UUID? = null,
    val childrenIds: List<UUID> = emptyList(),
    val isExpanded: Boolean = true
)