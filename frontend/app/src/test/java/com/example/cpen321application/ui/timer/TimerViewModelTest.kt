package com.example.cpen321application.ui.timer

import com.example.cpen321application.data.remote.RandomFactResponse
import com.example.cpen321application.data.repository.AppRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class TimerFormatTest {

    // Input: a duration under a minute
    // Expected behavior: both fields zero-padded to two digits
    // Expected output: "00:09"
    @Test
    fun `formats seconds only`() {
        assertEquals("00:09", TimerViewModel.format(9))
    }

    // Input: a duration spanning minutes and seconds
    // Expected behavior: seconds roll over into minutes
    // Expected output: "02:05"
    @Test
    fun `formats minutes and seconds`() {
        assertEquals("02:05", TimerViewModel.format(125))
    }

    // Input: zero
    // Expected behavior: rendered rather than blank, since the countdown ends
    //   on this value
    // Expected output: "00:00"
    @Test
    fun `formats zero`() {
        assertEquals("00:00", TimerViewModel.format(0))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val repository: AppRepository = mockk()
    private val fact = RandomFactResponse(
        text = "Honey never spoils.",
        source = "djtech.net",
        sourceUrl = "https://example.com/fact"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = TimerViewModel(repository)

    // Input: non-numeric text typed into the minutes field
    // Expected behavior: digits only, so the field cannot hold a value the
    //   timer could not parse
    // Expected output: "12"
    @Test
    fun `minutes input keeps only digits`() {
        val vm = viewModel()
        vm.onMinutesChange("1a2b")

        assertEquals("12", vm.uiState.value.minutesInput)
    }

    // Input: a seconds value above 59
    // Expected behavior: clamped, since 90 seconds is not a valid seconds field
    // Expected output: "59"
    @Test
    fun `seconds input is clamped to 59`() {
        val vm = viewModel()
        vm.onSecondsChange("90")

        assertEquals("59", vm.uiState.value.secondsInput)
    }

    // Input: minutes and seconds both zero
    // Expected behavior: start is refused, so a zero-length timer cannot fire
    //   immediately on tap
    // Expected output: canStart false
    @Test
    fun `cannot start a zero-length timer`() {
        val vm = viewModel()
        vm.onMinutesChange("0")
        vm.onSecondsChange("0")

        assertFalse(vm.uiState.value.canStart)
    }

    // Input: a one-minute-thirty timer
    // Expected behavior: the total is computed from both fields
    // Expected output: 90 seconds, canStart true
    @Test
    fun `combines minutes and seconds into a total`() {
        val vm = viewModel()
        vm.onMinutesChange("1")
        vm.onSecondsChange("30")

        assertEquals(90, vm.uiState.value.parsedTotalSeconds())
        assertTrue(vm.uiState.value.canStart)
    }

    // Input: a started timer
    // Expected behavior: phase becomes RUNNING and the remaining time is
    //   seeded with the full duration before the first tick
    // Expected output: RUNNING, 5 seconds remaining
    @Test
    fun `starting seeds the countdown`() = runTest {
        val vm = viewModel()
        vm.onMinutesChange("0")
        vm.onSecondsChange("5")
        vm.start()

        assertEquals(TimerPhase.RUNNING, vm.uiState.value.phase)
        assertEquals(5, vm.uiState.value.remainingSeconds)
        assertEquals(5, vm.uiState.value.totalSeconds)

        vm.reset()
    }

    // Input: a running timer that is cancelled
    // Expected behavior: returns to IDLE and keeps the entered duration, so
    //   the user does not retype it
    // Expected output: IDLE, inputs preserved
    @Test
    fun `reset returns to idle and keeps the inputs`() = runTest {
        val vm = viewModel()
        vm.onMinutesChange("2")
        vm.onSecondsChange("30")
        vm.start()
        vm.reset()

        val state = vm.uiState.value
        assertEquals(TimerPhase.IDLE, state.phase)
        assertEquals("2", state.minutesInput)
        assertEquals("30", state.secondsInput)
    }

    // Input: the reveal, with the backend healthy
    // Mocked behavior: repository returns a fact
    // Expected behavior: the fact is shown and no error is set
    // Expected output: REVEALED with the fact populated
    @Test
    fun `reveal shows the fact`() = runTest {
        coEvery { repository.loadRandomFact() } returns fact

        val vm = viewModel()
        vm.reveal()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TimerPhase.REVEALED, state.phase)
        assertEquals(fact, state.fact)
        assertNull(state.errorMessage)
    }

    // Input: the reveal, with the facts service down
    // Mocked behavior: repository throws HttpException 502
    // Expected behavior: the screen still reaches REVEALED -- the timer did
    //   its job -- and explains why the fact is missing
    // Expected output: REVEALED with an error and no fact
    @Test
    fun `reveal survives an unavailable facts service`() = runTest {
        coEvery { repository.loadRandomFact() } throws HttpException(
            Response.error<Any>(502, "".toResponseBody("application/json".toMediaType()))
        )

        val vm = viewModel()
        vm.reveal()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TimerPhase.REVEALED, state.phase)
        assertNull(state.fact)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("unavailable"))
    }

    // Input: the reveal, with no network
    // Mocked behavior: repository throws IOException
    // Expected behavior: reported as a connectivity problem
    // Expected output: an error mentioning reachability
    @Test
    fun `reveal reports a network failure`() = runTest {
        coEvery { repository.loadRandomFact() } throws IOException("offline")

        val vm = viewModel()
        vm.reveal()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.errorMessage!!.contains("Cannot reach the server"))
    }

    // Input: progress partway through a timer
    // Expected behavior: 0f at the start and 1f at the end, so the ring fills
    //   rather than empties
    // Expected output: 0f, then 0.5f
    @Test
    fun `progress runs from zero to one`() {
        assertEquals(0f, TimerUiState(totalSeconds = 10, remainingSeconds = 10).progress, 0.001f)
        assertEquals(0.5f, TimerUiState(totalSeconds = 10, remainingSeconds = 5).progress, 0.001f)
        assertEquals(1f, TimerUiState(totalSeconds = 10, remainingSeconds = 0).progress, 0.001f)
    }

    // Input: progress before any timer has been set
    // Expected behavior: no division by zero
    // Expected output: 0f
    @Test
    fun `progress is zero when no timer is set`() {
        assertEquals(0f, TimerUiState().progress, 0.001f)
    }
}
