package com.example.cpen321application.ui.login

import android.content.Context
import com.example.cpen321application.auth.GoogleSignInHelper
import com.example.cpen321application.auth.SignInCancelledException
import com.example.cpen321application.data.remote.UserDto
import com.example.cpen321application.data.repository.AppRepository
import com.example.cpen321application.data.repository.BackendInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val repository: AppRepository = mockk(relaxed = true)
    private val signInHelper: GoogleSignInHelper = mockk()
    private val activityContext: Context = mockk(relaxed = true)

    private val user = UserDto("ada@example.com", "Ada", "Lovelace")
    private val info = BackendInfo(
        serverIp = "34.29.207.92",
        clientIp = "203.0.113.7",
        serverTime = "07:50:12 GMT+00:00",
        developerFirstName = "Linjia",
        developerLastName = "Qi"
    )

    // viewModelScope dispatches on Dispatchers.Main, which does not exist in a
    // plain JVM test, so it is swapped for a controllable test dispatcher.
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = LoginViewModel(repository, signInHelper)

    // Input: a sign-in where Google, the token exchange and the info calls all
    //   succeed
    // Mocked behavior: helper returns an ID token; repository returns the user
    //   and the backend info
    // Expected behavior: state ends signed in, carrying every displayed value,
    //   with no error and no spinner
    // Expected output: isSignedIn true, user and info populated
    @Test
    fun `successful sign-in populates every displayed field`() = runTest {
        coEvery { signInHelper.requestIdToken(any()) } returns "google-id-token"
        coEvery { repository.signIn("google-id-token") } returns user
        coEvery { repository.loadInfo() } returns info

        val vm = viewModel()
        vm.signIn(activityContext)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isSignedIn)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(user, state.signedInUser)
        assertEquals(info, state.info)
        assertNotNull(state.clientTime)
    }

    // Input: the user dismisses the Google account picker
    // Mocked behavior: helper throws SignInCancelledException
    // Expected behavior: treated as a deliberate act, not a failure -- the
    //   screen returns to its initial state with no error shown
    // Expected output: no error, not loading, not signed in
    @Test
    fun `cancelling the picker is not an error`() = runTest {
        coEvery { signInHelper.requestIdToken(any()) } throws SignInCancelledException()

        val vm = viewModel()
        vm.signIn(activityContext)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
        assertFalse(state.isSignedIn)
    }

    // Input: the backend rejects the Google token
    // Mocked behavior: repository.signIn throws HttpException with status 401
    // Expected behavior: the message names the likely cause rather than showing
    //   a raw status code, and the info calls are never attempted
    // Expected output: an error mentioning GOOGLE_CLIENT_ID
    @Test
    fun `401 from the backend explains the likely cause`() = runTest {
        coEvery { signInHelper.requestIdToken(any()) } returns "google-id-token"
        coEvery { repository.signIn(any()) } throws httpException(401)

        val vm = viewModel()
        vm.signIn(activityContext)
        advanceUntilIdle()

        val message = vm.uiState.value.errorMessage
        assertNotNull(message)
        assertTrue(message!!.contains("GOOGLE_CLIENT_ID"))
        coVerify(exactly = 0) { repository.loadInfo() }
    }

    // Input: the server is unreachable
    // Mocked behavior: repository.signIn throws IOException
    // Expected behavior: reported as a connectivity problem, which is the thing
    //   the user can actually act on
    // Expected output: an error mentioning reachability
    @Test
    fun `network failure is reported as unreachable`() = runTest {
        coEvery { signInHelper.requestIdToken(any()) } returns "google-id-token"
        coEvery { repository.signIn(any()) } throws IOException("timeout")

        val vm = viewModel()
        vm.signIn(activityContext)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.errorMessage!!.contains("Cannot reach the server"))
    }

    // Input: a 500 from the backend
    // Mocked behavior: repository.loadInfo throws HttpException with status 503
    // Expected behavior: distinguished from a 401, and the status is surfaced
    // Expected output: an error naming HTTP 503
    @Test
    fun `server error surfaces the status code`() = runTest {
        coEvery { signInHelper.requestIdToken(any()) } returns "google-id-token"
        coEvery { repository.signIn(any()) } returns user
        coEvery { repository.loadInfo() } throws httpException(503)

        val vm = viewModel()
        vm.signIn(activityContext)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.errorMessage!!.contains("503"))
    }

    // Input: sign-out after a successful sign-in
    // Mocked behavior: repository.signOut is relaxed
    // Expected behavior: the stored token is cleared and the screen resets, so
    //   no stale identity remains on display
    // Expected output: initial state; repository.signOut called once
    @Test
    fun `sign-out clears the session and resets the screen`() = runTest {
        coEvery { signInHelper.requestIdToken(any()) } returns "google-id-token"
        coEvery { repository.signIn(any()) } returns user
        coEvery { repository.loadInfo() } returns info
        every { repository.signOut() } returns Unit

        val vm = viewModel()
        vm.signIn(activityContext)
        advanceUntilIdle()
        vm.signOut()

        val state = vm.uiState.value
        assertFalse(state.isSignedIn)
        assertNull(state.signedInUser)
        coVerify(exactly = 1) { repository.signOut() }
    }

    // Input: dismissing a shown error
    // Mocked behavior: helper throws a generic failure first
    // Expected behavior: only the error clears; nothing else about the state
    //   is disturbed
    // Expected output: errorMessage null
    @Test
    fun `dismissing an error clears only the error`() = runTest {
        coEvery { signInHelper.requestIdToken(any()) } throws IllegalStateException("boom")

        val vm = viewModel()
        vm.signIn(activityContext)
        advanceUntilIdle()
        assertEquals("boom", vm.uiState.value.errorMessage)

        vm.dismissError()
        assertNull(vm.uiState.value.errorMessage)
    }

    private fun httpException(code: Int) = HttpException(
        Response.error<Any>(code, "".toResponseBody("application/json".toMediaType()))
    )
}
