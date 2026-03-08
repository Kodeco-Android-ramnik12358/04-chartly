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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
                    onContentChange = { newText ->
                        viewModel.updateNodeContent(item.node.id, newText)
                    },
                    onToggleExpand = { viewModel.toggleExpansion(item.node.id)},
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
    onContentChange: (String) -> Unit,
    onToggleExpand: () -> Unit,
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
            .padding(
                start = (flattenedNode.depth * 24).dp,
                top = 0.dp,
                bottom = 0.dp,
                end = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. The Expansion Toggle
        if (flattenedNode.node.childrenIds.isNotEmpty()) {
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (flattenedNode.node.isExpanded)
                        Icons.Default.KeyboardArrowDown
                    else
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (flattenedNode.node.isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            // Keep the indentation consistent even if there's no arrow
            Spacer(modifier = Modifier.size(32.dp))
        }

        // 2. The Bullet (Canvas)
        Canvas(modifier = Modifier.padding(end = 12.dp).size(8.dp)) {
            drawCircle(color = Color.Gray)
        }

        // 3. The Content
        BasicTextField(
            value = flattenedNode.node.content,
            onValueChange = onContentChange,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp), // Slightly more vertical padding for easier tapping
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
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