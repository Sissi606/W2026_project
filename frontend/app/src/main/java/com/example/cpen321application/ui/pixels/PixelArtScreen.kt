package com.example.cpen321application.ui.pixels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.cpen321application.R

@Composable
fun PixelArtScreen(
    viewModel: PixelArtViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.pixel_title),
            style = MaterialTheme.typography.headlineSmall
        )

        StatusRow(
            state = viewModel.connectionState,
            pixelsReceived = viewModel.pixelsReceived
        )

        PixelGrid(
            cells = viewModel.cells,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .semantics { contentDescription = "pixel_grid" }
        )

        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}

@Composable
private fun StatusRow(state: ConnectionState, pixelsReceived: Int) {
    val (label, color) = when (state) {
        ConnectionState.CONNECTING ->
            stringResource(R.string.status_connecting) to MaterialTheme.colorScheme.onSurfaceVariant
        ConnectionState.LIVE ->
            stringResource(R.string.status_live) to MaterialTheme.colorScheme.primary
        ConnectionState.RECONNECTING ->
            stringResource(R.string.status_reconnecting) to MaterialTheme.colorScheme.error
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = color)
        Text(
            text = stringResource(R.string.pixels_received, pixelsReceived),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Drawn on a Canvas rather than as 256 nested composables: a grid of
 * composables would mean 256 layout nodes recomposing several times a second,
 * where this is a few hundred draw calls into one node.
 */
@Composable
private fun PixelGrid(cells: List<Int>, modifier: Modifier = Modifier) {
    val blankColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val side = size.minDimension
        val cell = side / PixelArtViewModel.GRID_SIZE

        for (y in 0 until PixelArtViewModel.GRID_SIZE) {
            for (x in 0 until PixelArtViewModel.GRID_SIZE) {
                val value = cells.getOrNull(y * PixelArtViewModel.GRID_SIZE + x)
                    ?: PixelArtViewModel.NO_COLOR

                // An unpainted cell keeps the blank-canvas look instead of
                // being drawn as transparent black.
                val color = if (value == PixelArtViewModel.NO_COLOR) {
                    blankColor
                } else {
                    Color(value)
                }

                drawRect(
                    color = color,
                    topLeft = Offset(x * cell, y * cell),
                    size = Size(cell, cell)
                )
            }
        }
    }
}
