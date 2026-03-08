package com.ramniksoftware.chartly

import androidx.lifecycle.ViewModel
import com.ramniksoftware.chartly.repositories.NodeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ChartlyViewModel(private val nodeManager: NodeManager) : ViewModel() {

    // 1. The "Source of Truth" for the UI
    private val _uiState = MutableStateFlow(ChartlyUIState())
    val uiState: StateFlow<ChartlyUIState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // 2. Fetch the root nodes from our manager
        val roots = nodeManager.getRootNodes()
        // 3. Push them into the UI State
        _uiState.value = ChartlyUIState(items = roots)
    }

    // A helper for the UI to find children
    fun getNodeById(id: UUID) = nodeManager.getNode(id)
}