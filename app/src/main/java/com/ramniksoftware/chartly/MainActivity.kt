package com.ramniksoftware.chartly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.ramniksoftware.chartly.ui.theme.ChartlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize the Engine
        val nodeManager = NodeManager().apply {
            // Seed some data so the screen isn't empty
            val root1 = Node(content = "Buy groceries")
            val root2 = Node(content = "Prepare for interview")
            addNode(root1)
            addNode(root2)
            addNode(Node(content = "Buy milk"), parent = root1)
        }

        // 2. Initialize the ViewModel
        val viewModel = ChartlyViewModel(nodeManager)

        enableEdgeToEdge()
        setContent {
            ChartlyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 3. Call your screen and pass the VM
                    ChartlyScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ChartlyScreen(
    viewModel: ChartlyViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(state.items) { item ->
            ChartlyRow(flattenedNode = item)
        }
    }
}

@Composable
fun ChartlyRow(flattenedNode: FlattenedNode) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ((flattenedNode.depth * 24) + 8).dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .padding(end = 12.dp)
                .size(10.dp)
        ) {
            drawCircle(
                color = Color.Gray,
                radius = size.minDimension / 2 // Perfectly centered circle
            )
        }
        Text(text = flattenedNode.node.content)
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