package com.example.cpen321application.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cpen321application.data.remote.RandomFactResponse
import com.example.cpen321application.data.repository.AppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

enum class TimerPhase { IDLE, RUNNING, REVEALING, REVEALED }

data class TimerUiState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val minutesInput: String = "0",
    val secondsInput: String = "10",
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val fact: RandomFactResponse? = null,
    val errorMessage: String? = null
) {
    /** Drives the ring; 0f at the start, 1f when the timer finishes. */
    val progress: Float
        get() = if (totalSeconds <= 0) 0f
        else (totalSeconds - remainingSeconds).toFloat() / totalSeconds

    val canStart: Boolean
        get() = phase == TimerPhase.IDLE && parsedTotalSeconds() in 1..MAX_TOTAL_SECONDS

    fun parsedTotalSeconds(): Int {
        val minutes = minutesInput.toIntOrNull() ?: 0
        val seconds = secondsInput.toIntOrNull() ?: 0
        return minutes * 60 + seconds
    }

    companion object {
        const val MAX_TOTAL_SECONDS = 99 * 60 + 59
    }
}

class TimerViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    /** Digits only, and capped, so the field cannot hold an unusable value. */
    fun onMinutesChange(value: String) {
        if (_uiState.value.phase != TimerPhase.IDLE) return
        _uiState.value = _uiState.value.copy(minutesInput = sanitize(value, max = 99))
    }

    fun onSecondsChange(value: String) {
        if (_uiState.value.phase != TimerPhase.IDLE) return
        _uiState.value = _uiState.value.copy(secondsInput = sanitize(value, max = 59))
    }

    fun start() {
        val total = _uiState.value.parsedTotalSeconds()
        if (total !in 1..TimerUiState.MAX_TOTAL_SECONDS) return

        _uiState.value = _uiState.value.copy(
            phase = TimerPhase.RUNNING,
            remainingSeconds = total,
            totalSeconds = total,
            fact = null,
            errorMessage = null
        )

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            // Counts wall-clock elapsed time rather than summing delays: a
            // delay(1000) loop drifts, and the drift is visible over minutes.
            val endsAt = System.currentTimeMillis() + total * 1000L

            while (isActive) {
                val remainingMs = endsAt - System.currentTimeMillis()
                if (remainingMs <= 0) break

                _uiState.value = _uiState.value.copy(
                    // Rounded up, so the display reaches 0 exactly when the
                    // timer fires rather than a second early.
                    remainingSeconds = ((remainingMs + 999) / 1000).toInt()
                )
                delay(TICK_MS)
            }

            _uiState.value = _uiState.value.copy(remainingSeconds = 0)
            reveal()
        }
    }

    fun reset() {
        countdownJob?.cancel()
        countdownJob = null
        _uiState.value = TimerUiState(
            minutesInput = _uiState.value.minutesInput,
            secondsInput = _uiState.value.secondsInput
        )
    }

    /** Fetches the surprise. Also used by "another fact" after the reveal. */
    fun reveal() {
        _uiState.value = _uiState.value.copy(
            phase = TimerPhase.REVEALING,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    phase = TimerPhase.REVEALED,
                    fact = repository.loadRandomFact()
                )
            } catch (e: Exception) {
                // The timer still counted down correctly, so the screen stays
                // in REVEALED and shows why the fact is missing instead of
                // discarding the result.
                _uiState.value = _uiState.value.copy(
                    phase = TimerPhase.REVEALED,
                    errorMessage = describe(e)
                )
            }
        }
    }

    private fun describe(e: Exception): String = when (e) {
        is HttpException ->
            if (e.code() == 502) "The facts service is unavailable right now."
            else "Could not load a fact (HTTP ${e.code()})."
        is IOException -> "Cannot reach the server. Check your connection."
        else -> e.message ?: "Could not load a fact."
    }

    private fun sanitize(value: String, max: Int): String {
        val digits = value.filter { it.isDigit() }.take(2)
        if (digits.isEmpty()) return ""
        return digits.toInt().coerceAtMost(max).toString()
    }

    companion object {
        // Sub-second so the displayed value never visibly lags the real clock.
        private const val TICK_MS = 200L

        fun format(totalSeconds: Int): String {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

        fun factory(repository: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TimerViewModel(repository) as T
        }
    }
}
