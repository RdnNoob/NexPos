package com.nexpos.laundry.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.nexpos.laundry.ui.screens.auth.LoginDeviceScreen
import com.nexpos.laundry.ui.screens.home.HomeScreen
import com.nexpos.laundry.ui.screens.transaction.CreateTransactionScreen
import com.nexpos.laundry.ui.screens.transaction.TransactionListScreen

sealed class LaundryScreen(val route: String) {
    object Login : LaundryScreen("login")
    object Home : LaundryScreen("home")
    object CreateTransaction : LaundryScreen("create_transaction")
    object TransactionList : LaundryScreen("transaction_list")
}

@Composable
fun LaundryNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = LaundryScreen.Login.route) {
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
