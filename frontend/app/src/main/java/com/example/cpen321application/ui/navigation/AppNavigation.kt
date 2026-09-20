package com.example.cpen321application.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cpen321application.auth.GoogleSignInHelper
import com.example.cpen321application.data.remote.PixelSocketClient
import com.example.cpen321application.data.repository.AppRepository
import com.example.cpen321application.ui.home.HomeScreen
import com.example.cpen321application.ui.login.LoginScreen
import com.example.cpen321application.ui.login.LoginViewModel
import com.example.cpen321application.ui.pixels.PixelArtScreen
import com.example.cpen321application.ui.pixels.PixelArtViewModel
import com.example.cpen321application.ui.timer.TimerScreen
import com.example.cpen321application.ui.timer.TimerViewModel

object Routes {
    const val HOME = "home"
    const val LOGIN = "login"
    const val PIXELS = "pixels"
    const val TIMER = "timer"
}

@Composable
fun AppNavigation(
    repository: AppRepository,
    googleSignInHelper: GoogleSignInHelper,
    pixelSocketClient: PixelSocketClient,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onButtonOneClick = { navController.navigate(Routes.LOGIN) },
                onButtonTwoClick = { navController.navigate(Routes.PIXELS) },
                onButtonThreeClick = { navController.navigate(Routes.TIMER) }
            )
        }

        composable(Routes.LOGIN) {
            val viewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.factory(repository, googleSignInHelper)
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LoginScreen(
                state = state,
                onSignIn = viewModel::signIn,
                onSignOut = viewModel::signOut,
                onDismissError = viewModel::dismissError,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PIXELS) {
            // Scoped to this destination, so the socket opens on entry and is
            // torn down when the user navigates away.
            val viewModel: PixelArtViewModel = viewModel(
                factory = PixelArtViewModel.factory(pixelSocketClient)
            )

            PixelArtScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TIMER) {
            val viewModel: TimerViewModel = viewModel(
                factory = TimerViewModel.factory(repository)
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            TimerScreen(
                state = state,
                onMinutesChange = viewModel::onMinutesChange,
                onSecondsChange = viewModel::onSecondsChange,
                onStart = viewModel::start,
                onReset = viewModel::reset,
                onAnotherFact = viewModel::reveal,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
