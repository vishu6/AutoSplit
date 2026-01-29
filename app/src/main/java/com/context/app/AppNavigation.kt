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

    val startRoute = if (isNotificationPermissionGranted(context)) "home" else "welcome"

    NavHost(navController = navController, startDestination = startRoute) {

        composable("welcome") {
            WelcomeScreen(
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }
        
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
                onExpenseClick = { expenseId -> // <-- RENAMED
                    navController.navigate("edit_expense/$expenseId")
                },
                onProfileClick = {
                    navController.navigate("settings")
                }
            )
        }

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
            val groupId = backStackEntry.arguments?.getInt("groupId") ?: 0
            GroupDetailScreen(
                onBack = { navController.popBackStack() },
                onAddExpenseClick = {
                    navController.navigate("add_expense")
                },
                 onSettleUpClick = { 
                    navController.navigate("settle_up/$groupId")
                },
                onExpenseClick = { expenseId -> // <-- RENAMED
                    navController.navigate("edit_expense/$expenseId")
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
        
        composable(
            route = "settle_up/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.IntType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getInt("groupId") ?: 0
            SettleUpScreen(
                groupId = groupId,
                onBack = { navController.popBackStack() },
                onSettled = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}