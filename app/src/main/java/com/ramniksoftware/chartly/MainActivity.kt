package com.ramniksoftware.chartly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ramniksoftware.chartly.model.Node
import com.ramniksoftware.chartly.repositories.NodeManager
import com.ramniksoftware.chartly.ui.components.ControlBar
import com.ramniksoftware.chartly.ui.theme.ChartlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val nodeManager = NodeManager().apply {
            val root1 = Node(content = "Buy groceries")
            val root2 = Node(content = "Prepare for interview")
            addNode(root1)
            addNode(root2)
            addNode(Node(content = "Buy milk"), parent = root1)
        }

        val viewModel = ChartlyViewModel(nodeManager)

        enableEdgeToEdge()
        setContent {
            ChartlyTheme {
                ChartlyScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ChartlyScreen(viewModel: ChartlyViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = state.selectedNodeId != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                ControlBar(
                    onIndent = { viewModel.indentSelectedNode() },
                    onOutdent = { viewModel.outdentSelectedNode() },
                    onClose = { viewModel.selectNode(null) }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Combine Scaffold padding with our 16.dp margin
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            )
        ) {
            items(state.items) { item ->
                ChartlyRow(
                    flattenedNode = item,
                    isSelected = state.selectedNodeId == item.node.id,
                    onClick = { viewModel.selectNode(item.node.id) }
                )
            }
        }
    }
}

@Composable
fun ChartlyRow(
    flattenedNode: FlattenedNode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            // Dynamic padding based on depth
            .padding(start = (flattenedNode.depth * 24).dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.padding(horizontal = 12.dp).size(8.dp)) {
            drawCircle(color = Color.Gray)
        }
        Text(
            text = flattenedNode.node.content,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChartlyPreview() {
    val mockManager = NodeManager().apply {
        addNode(Node(content = "Preview Task 1"))
        addNode(Node(content = "Preview Task 2"))
    }
    val mockViewModel = ChartlyViewModel(mockManager)

    ChartlyTheme {
        ChartlyScreen(mockViewModel)
    }
}