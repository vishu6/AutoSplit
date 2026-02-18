package com.context.app

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.context.ui.*
import com.context.utils.OnboardingUtils

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()

    val isFirstRun = remember { OnboardingUtils.isFirstRun(context) }
    val startDestination = if (isFirstRun) "welcome" else "main"

    NavHost(navController = navController, startDestination = startDestination) {

        composable("welcome") {
            WelcomeScreen(
                onEnableClicked = {
                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    context.startActivity(intent)
                },
                onSkipClicked = {
                    OnboardingUtils.setOnboardingCompleted(context)
                    navController.navigate("main") { 
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }
        
        navigation(startDestination = "home", route = "main") { 
            composable("home") {
                val parentEntry = remember(it) { navController.getBackStackEntry("main") }
                val homeViewModel: HomeViewModel = hiltViewModel(parentEntry)
                HomeScreen(
                    homeViewModel = homeViewModel,
                    onNavigateToGroup = { groupId -> 
                        navController.navigate("group_detail/$groupId") 
                    },
                    onCreateGroupClick = { 
                        navController.navigate("create_group") 
                    },
                    onAddExpenseClick = {
                        navController.navigate("add_expense")
                    },
                    onExpenseClick = { expenseId ->
                        navController.navigate("edit_expense/$expenseId")
                    },
                    onProfileClick = {
                        navController.navigate("settings")
                    },
                    onViewAllClick = {
                        navController.navigate("transactions")
                    }
                )
            }

            composable("transactions") {
                val parentEntry = remember(it) { navController.getBackStackEntry("main") }
                val homeViewModel: HomeViewModel = hiltViewModel(parentEntry)
                AllTransactionsScreen(
                    homeViewModel = homeViewModel,
                    onBack = { navController.popBackStack() },
                    onExpenseClick = { expenseId ->
                        navController.navigate("edit_expense/$expenseId")
                    }
                )
            }
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
                onExpenseClick = { expenseId ->
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