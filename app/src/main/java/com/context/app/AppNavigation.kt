package com.context.app

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.context.ui.*
import com.context.utils.isNotificationPermissionGranted

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()

    // DECIDE START DESTINATION
    val startRoute = if (isNotificationPermissionGranted(context)) "home" else "welcome"

    NavHost(navController = navController, startDestination = startRoute) {

        // 1. Welcome Screen
        composable("welcome") {
            WelcomeScreen(
                onNavigateToHome = {
                    // Navigate to home and clear back stack so user can't "back" into welcome
                    navController.navigate("home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        // 2. Home Screen (Your existing code)
        composable("home") {
            HomeScreen(
                onNavigateToGroup = { groupId -> 
                    navController.navigate("group_detail/$groupId") 
                },
                onCreateGroupClick = { 
                    navController.navigate("create_group") 
                },
                onAddExpenseClick = {
                    navController.navigate("add_expense")
                },
                onEditExpenseClick = { expenseId ->
                    navController.navigate("edit_expense/$expenseId")
                }
            )
        }

        // ... rest of your routes (add_expense, etc.) ...
        composable("create_group") {
            CreateGroupScreen(
                onBack = { navController.popBackStack() },
                onGroupCreated = { navController.popBackStack() } 
            )
        }

        composable(
            route = "group_detail/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.IntType })
        ) { backStackEntry ->
            GroupDetailScreen(
                onBack = { navController.popBackStack() },
                onAddExpenseClick = {
                    navController.navigate("add_expense")
                }
            )
        }

        composable("add_expense") {
            AddExpenseScreen(
                onBack = { navController.popBackStack() },
                onExpenseAdded = { navController.popBackStack() }
            )
        }

        composable(
            route = "edit_expense/{expenseId}",
            arguments = listOf(navArgument("expenseId") { type = NavType.IntType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getInt("expenseId") ?: 0
            EditExpenseScreen(
                expenseId = expenseId,
                onBack = { navController.popBackStack() },
                onExpenseUpdated = { navController.popBackStack() }
            )
        }
    }
}