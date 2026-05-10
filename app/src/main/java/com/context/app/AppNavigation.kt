package com.context.app

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.context.ui.*
import com.context.utils.*
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType

@Composable
fun AppNavigation(appUpdateManager: AppUpdateManager) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toaster = remember { Toaster(snackbarHostState, scope) }

    val isFirstRun = remember { OnboardingUtils.isFirstRun(context) }
    var isAuthenticated by remember { mutableStateOf(!SecurityUtils.isSecurityEnabled(context)) }

    CompositionLocalProvider(LocalToaster provides toaster) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isAuthenticated) {
                LockScreen(onAuthenticated = { isAuthenticated = true })
            } else {
                val startDestination = if (isFirstRun) "welcome" else "main"
                
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize()
                ) {
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
                                },
                                onUpdateClick = {
                                    UpdateUtils.pendingAppUpdateInfo?.let { updateInfo ->
                                        appUpdateManager.startUpdateFlowForResult(
                                            updateInfo,
                                            AppUpdateType.FLEXIBLE,
                                            context as Activity,
                                            123
                                        )
                                    }
                                },
                                onScanReceiptClick = {
                                    navController.navigate("add_expense?scan=true")
                                },
                                onSetBudgetClick = {
                                    navController.navigate("budget_setup")
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

                    composable(
                        route = "add_expense?scan={scan}",
                        arguments = listOf(
                            navArgument("scan") {
                                type = NavType.BoolType
                                defaultValue = false
                            }
                        )
                    ) { backStackEntry ->
                        val startWithScanner = backStackEntry.arguments?.getBoolean("scan") ?: false
                        AddExpenseScreen(
                            startWithScanner = startWithScanner,
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
                            onBack = { navController.popBackStack() },
                            onBudgetClick = { navController.navigate("budget_setup") }
                        )
                    }

                    composable("budget_setup") {
                        BudgetSetupScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }

            // Global Snackbar Host
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}
