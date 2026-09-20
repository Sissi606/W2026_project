package com.example.cpen321application.ui.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cpen321application.R

@Composable
fun TimerScreen(
    state: TimerUiState,
    onMinutesChange: (String) -> Unit,
    onSecondsChange: (String) -> Unit,
    onStart: () -> Unit,
    onReset: () -> Unit,
    onAnotherFact: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.timer_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth()
        )

        when (state.phase) {
            TimerPhase.IDLE -> TimerSetup(
                state = state,
                onMinutesChange = onMinutesChange,
                onSecondsChange = onSecondsChange,
                onStart = onStart
            )

            TimerPhase.RUNNING -> Countdown(state = state, onReset = onReset)

            TimerPhase.REVEALING, TimerPhase.REVEALED ->
                Surprise(state = state, onAnotherFact = onAnotherFact, onReset = onReset)
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.back)) }
    }
}

@Composable
private fun TimerSetup(
    state: TimerUiState,
    onMinutesChange: (String) -> Unit,
    onSecondsChange: (String) -> Unit,
    onStart: () -> Unit
) {
    Text(
        text = stringResource(R.string.timer_prompt),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.minutesInput,
            onValueChange = onMinutesChange,
            label = { Text(stringResource(R.string.label_minutes)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "minutes_input" }
        )
        OutlinedTextField(
            value = state.secondsInput,
            onValueChange = onSecondsChange,
            label = { Text(stringResource(R.string.label_seconds)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "seconds_input" }
        )
    }

    Button(
        onClick = onStart,
        enabled = state.canStart,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { contentDescription = "start_timer" }
    ) {
        Text(
            text = stringResource(R.string.start_timer),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun Countdown(state: TimerUiState, onReset: () -> Unit) {
    // Animated so the ring sweeps smoothly between ticks rather than stepping.
    val progress by animateFloatAsState(targetValue = state.progress, label = "progress")

    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 16.dp)) {
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        val progressColor = MaterialTheme.colorScheme.primary

        Canvas(modifier = Modifier.size(220.dp)) {
            val stroke = Stroke(width = 18f, cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = stroke
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = stroke
            )
        }

        Text(
            text = TimerViewModel.format(state.remainingSeconds),
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.semantics { contentDescription = "remaining_time" }
        )
    }

    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.cancel_timer))
    }
}

@Composable
private fun Surprise(state: TimerUiState, onAnotherFact: () -> Unit, onReset: () -> Unit) {
    Text(
        text = stringResource(R.string.times_up),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                state.phase == TimerPhase.REVEALING -> CircularProgressIndicator()

                state.errorMessage != null -> Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                state.fact != null -> {
                    Text(
                        text = stringResource(R.string.did_you_know),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = state.fact.text,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { contentDescription = "fact_text" }
                    )
                    if (state.fact.source.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.fact_source, state.fact.source),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onAnotherFact,
            enabled = state.phase == TimerPhase.REVEALED,
            modifier = Modifier.weight(1f)
        ) { Text(stringResource(R.string.another_fact)) }

        Button(onClick = onReset, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.new_timer))
        }
    }
}
