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
import com.example.cpen321application.data.repository.AppRepository
import com.example.cpen321application.ui.home.HomeScreen
import com.example.cpen321application.ui.login.LoginScreen
import com.example.cpen321application.ui.login.LoginViewModel

object Routes {
    const val HOME = "home"
    const val LOGIN = "login"
}

@Composable
fun AppNavigation(
    repository: AppRepository,
    googleSignInHelper: GoogleSignInHelper,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(onButtonOneClick = { navController.navigate(Routes.LOGIN) })
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
    }
}
