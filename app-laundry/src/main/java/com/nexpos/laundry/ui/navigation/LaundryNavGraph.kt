package com.nexpos.laundry.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.nexpos.laundry.ui.screens.auth.LoginDeviceScreen
import com.nexpos.laundry.ui.screens.home.HomeScreen
import com.nexpos.laundry.ui.screens.splash.SplashScreen
import com.nexpos.laundry.ui.screens.transaction.CreateTransactionScreen
import com.nexpos.laundry.ui.screens.transaction.TransactionListScreen

sealed class LaundryScreen(val route: String) {
    object Splash : LaundryScreen("splash")
    object Login : LaundryScreen("login")
    object Home : LaundryScreen("home")
    object CreateTransaction : LaundryScreen("create_transaction")
    object TransactionList : LaundryScreen("transaction_list")
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LaundryNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = LaundryScreen.Splash.route,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) +
                    fadeIn(tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) +
                    fadeOut(tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) +
                    fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) +
                    fadeOut(tween(300))
        }
    ) {
        composable(
            LaundryScreen.Splash.route,
            enterTransition = { fadeIn(tween(0)) },
            exitTransition = { fadeOut(tween(400)) }
        ) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(LaundryScreen.Login.route) { popUpTo(0) }
                },
                onNavigateToHome = {
                    navController.navigate(LaundryScreen.Home.route) { popUpTo(0) }
                }
            )
        }
        composable(LaundryScreen.Login.route) {
            LoginDeviceScreen(
                onLoginSuccess = {
                    navController.navigate(LaundryScreen.Home.route) { popUpTo(0) }
                }
            )
        }
        composable(LaundryScreen.Home.route) {
            HomeScreen(
                onNavigateToCreate = { navController.navigate(LaundryScreen.CreateTransaction.route) },
                onNavigateToList = { navController.navigate(LaundryScreen.TransactionList.route) },
                onLogout = { navController.navigate(LaundryScreen.Login.route) { popUpTo(0) } }
            )
        }
        composable(LaundryScreen.CreateTransaction.route) {
            CreateTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }
        composable(LaundryScreen.TransactionList.route) {
            TransactionListScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
