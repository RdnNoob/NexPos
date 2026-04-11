package com.nexpos.admin.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.nexpos.admin.ui.screens.auth.LoginScreen
import com.nexpos.admin.ui.screens.auth.RegisterScreen
import com.nexpos.admin.ui.screens.dashboard.DashboardScreen
import com.nexpos.admin.ui.screens.devices.DevicesScreen
import com.nexpos.admin.ui.screens.outlets.CreateOutletScreen
import com.nexpos.admin.ui.screens.outlets.OutletsScreen
import com.nexpos.admin.ui.screens.splash.SplashScreen
import com.nexpos.admin.ui.screens.transactions.TransactionsScreen

sealed class AdminScreen(val route: String) {
    object Splash : AdminScreen("splash")
    object Login : AdminScreen("login")
    object Register : AdminScreen("register")
    object Dashboard : AdminScreen("dashboard")
    object Outlets : AdminScreen("outlets")
    object CreateOutlet : AdminScreen("create_outlet")
    object Devices : AdminScreen("devices")
    object Transactions : AdminScreen("transactions")
    object OutletTransactions : AdminScreen("transactions/{outletId}") {
        fun createRoute(outletId: Int) = "transactions/$outletId"
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AdminNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AdminScreen.Splash.route,
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
            AdminScreen.Splash.route,
            enterTransition = { fadeIn(tween(0)) },
            exitTransition = { fadeOut(tween(400)) }
        ) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(AdminScreen.Login.route) { popUpTo(0) }
                },
                onNavigateToDashboard = {
                    navController.navigate(AdminScreen.Dashboard.route) { popUpTo(0) }
                }
            )
        }
        composable(AdminScreen.Login.route) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(AdminScreen.Dashboard.route) { popUpTo(0) } },
                onNavigateToRegister = { navController.navigate(AdminScreen.Register.route) }
            )
        }
        composable(AdminScreen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(AdminScreen.Dashboard.route) { popUpTo(0) } },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(AdminScreen.Dashboard.route) {
            DashboardScreen(
                onNavigateToOutlets = { navController.navigate(AdminScreen.Outlets.route) },
                onNavigateToDevices = { navController.navigate(AdminScreen.Devices.route) },
                onNavigateToTransactions = { navController.navigate(AdminScreen.Transactions.route) },
                onLogout = { navController.navigate(AdminScreen.Login.route) { popUpTo(0) } }
            )
        }
        composable(AdminScreen.Outlets.route) {
            OutletsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreate = { navController.navigate(AdminScreen.CreateOutlet.route) },
                onNavigateToTransactions = { outletId ->
                    navController.navigate(AdminScreen.OutletTransactions.createRoute(outletId))
                }
            )
        }
        composable(AdminScreen.CreateOutlet.route) {
            CreateOutletScreen(
                onNavigateBack = { navController.popBackStack() },
                onOutletCreated = { navController.popBackStack() }
            )
        }
        composable(AdminScreen.Devices.route) {
            DevicesScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(AdminScreen.Transactions.route) {
            TransactionsScreen(
                outletId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            AdminScreen.OutletTransactions.route,
            arguments = listOf(navArgument("outletId") { type = NavType.IntType })
        ) { backStackEntry ->
            TransactionsScreen(
                outletId = backStackEntry.arguments?.getInt("outletId"),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
