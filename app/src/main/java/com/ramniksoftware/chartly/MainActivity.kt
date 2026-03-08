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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.ramniksoftware.chartly.model.Node
import com.ramniksoftware.chartly.repositories.NodeManager
import com.ramniksoftware.chartly.ui.components.ControlBar
import com.ramniksoftware.chartly.ui.theme.ChartlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allows the layout to resize when the keyboard appears.
        WindowCompat.setDecorFitsSystemWindows(window, false)

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartlyScreen(viewModel: ChartlyViewModel) {
    val state by viewModel.uiState.collectAsState()
    val isKeyboardVisible = WindowInsets.isImeVisible

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PaddingValues(top = 32.dp))
    ) {
        // 1. The List: Uses weight(1f) to push everything else down
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(state.items, key = { it.node.id }) { item ->
                ChartlyRow(
                    flattenedNode = item,
                    onFocused = { viewModel.setFocusedNode(item.node.id) },
                    onContentChange = { viewModel.updateNodeContent(item.node.id, it) },
                    onToggleExpand = { viewModel.toggleExpansion(item.node.id) }
                )
            }
        }

        // 2. The Keyboard Toolbar
        // Logic: Only exist if the keyboard is up AND we know what node to edit
        if (isKeyboardVisible && state.focusedNodeId != null) {
            ControlBar(
                onIndent = { viewModel.indentFocusedNode() },
                onOutdent = { viewModel.outdentFocusedNode() },
                onClose = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime)
                    .padding(8.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartlyRow(
    flattenedNode: FlattenedNode,
    onFocused: () -> Unit,
    onContentChange: (String) -> Unit,
    onToggleExpand: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val isKeyboardVisible = WindowInsets.isImeVisible

    LaunchedEffect(isKeyboardVisible) {
        if (!isKeyboardVisible) {
            // If the keyboard was closed (by any means), remove the cursor
            focusManager.clearFocus()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
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
        Canvas(modifier = Modifier
            .padding(end = 12.dp)
            .size(8.dp)) {
            drawCircle(color = Color.Gray)
        }

        // 3. The Content
        BasicTextField(
            value = flattenedNode.node.content,
            onValueChange = onContentChange,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .onFocusChanged { if (it.isFocused) onFocused() },
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