package com.aterminal.app.readability

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ReadingModeScreen(
    blocks: List<AgentOutputBlock>,
    onCopyRaw: (String) -> Unit,
    onJumpToTerminal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandedOutputBlocks = remember(blocks) { mutableStateMapOf<Int, Boolean>() }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Reading mode",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedButton(onClick = onJumpToTerminal) {
                    Text("Jump to terminal")
                }
            }

            if (blocks.isEmpty()) {
                Text(
                    text = "No agent output to render yet.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = blocks.withIndex().toList(),
                    key = { indexed -> indexed.index },
                ) { indexed ->
                    ReadingBlockCard(
                        block = indexed.value,
                        expanded = expandedOutputBlocks[indexed.index] == true,
                        onToggleExpanded = {
                            expandedOutputBlocks[indexed.index] =
                                expandedOutputBlocks[indexed.index] != true
                        },
                        onCopyRaw = onCopyRaw,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingBlockCard(
    block: AgentOutputBlock,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onCopyRaw: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReadingBlockContent(
                block = block,
                expanded = expanded,
                onToggleExpanded = onToggleExpanded,
            )
            OutlinedButton(
                onClick = { onCopyRaw(block.rawSource) },
            ) {
                Text("Copy raw")
            }
        }
    }
}

@Composable
private fun ReadingBlockContent(
    block: AgentOutputBlock,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    when (block) {
        is AgentOutputBlock.Heading -> Text(
            text = block.text,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineSmall
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
        )
        is AgentOutputBlock.ListBlock -> Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            block.items.forEach { item ->
                Text(
                    text = item,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        is AgentOutputBlock.Code -> LabeledMonospaceBlock(
            label = block.language ?: "code",
            text = block.code,
        )
        is AgentOutputBlock.Command -> LabeledMonospaceBlock(
            label = "command",
            text = block.command,
        )
        is AgentOutputBlock.Output -> OutputBlock(
            block = block,
            expanded = expanded,
            onToggleExpanded = onToggleExpanded,
        )
        is AgentOutputBlock.Diff -> LabeledMonospaceBlock(
            label = "DIFF",
            text = block.text,
            accent = MaterialTheme.colorScheme.tertiaryContainer,
        )
        is AgentOutputBlock.Approval -> LabeledMonospaceBlock(
            label = "APPROVAL",
            text = block.text,
            accent = MaterialTheme.colorScheme.errorContainer,
        )
        is AgentOutputBlock.PlainText -> Text(
            text = block.text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
        is AgentOutputBlock.Unknown -> Text(
            text = block.text.ifBlank { "Unrenderable terminal control block" },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun OutputBlock(
    block: AgentOutputBlock.Output,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val lines = block.text.lines()
    val visibleText = if (block.collapsedByDefault && !expanded) {
        lines.take(COLLAPSED_OUTPUT_LINE_COUNT).joinToString(separator = "\n")
    } else {
        block.text
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LabeledMonospaceBlock(
            label = "output",
            text = visibleText,
        )
        if (block.collapsedByDefault && lines.size > COLLAPSED_OUTPUT_LINE_COUNT) {
            Button(onClick = onToggleExpanded) {
                Text(if (expanded) "Collapse output" else "Show full output")
            }
        }
    }
}

@Composable
private fun LabeledMonospaceBlock(
    label: String,
    text: String,
    accent: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            onClick = {},
            label = { Text(label) },
        )
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .background(accent)
                .padding(10.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private const val COLLAPSED_OUTPUT_LINE_COUNT = 6
