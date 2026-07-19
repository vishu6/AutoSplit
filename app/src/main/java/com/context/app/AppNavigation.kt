package com.context.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.context.sync.GroupSyncManager
import com.context.ui.*
import com.context.utils.*
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AppNavigation(
    appUpdateManager: AppUpdateManager,
    groupSyncManager: GroupSyncManager = hiltViewModel<HomeViewModel>().groupSyncManager
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toaster = remember { Toaster(snackbarHostState, scope) }

    val isFirstRun = remember { OnboardingUtils.isFirstRun(context) }
    var isAuthenticated by remember { mutableStateOf(!SecurityUtils.isSecurityEnabled(context)) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var pendingJoin by remember { mutableStateOf<GroupSyncManager.JoinResult?>(null) }

    // Listen for global join events (via deep links or manual join)
    LaunchedEffect(Unit) {
        groupSyncManager.joinEvents.collectLatest { result ->
            if (result.groupId != 0) {
                // Navigate to the joined group
                navController.navigate("group_detail/${result.groupId}") {
                    launchSingleTop = true
                }
                // Show success feedback
                toaster.show("Joined group: ${result.groupName}")
            } else if (result.remoteId != null) {
                // Intercept for "Claim Profile" dialog if there is a candidate name
                if (result.candidateName != null) {
                    pendingJoin = result
                } else {
                    // No candidate to merge, just proceed with normal join
                    groupSyncManager.confirmJoin(result, shouldMerge = false)
                }
            }
        }
    }

    // Claim Profile Dialog
    if (pendingJoin != null) {
        val result = pendingJoin!!
        AlertDialog(
            onDismissRequest = { pendingJoin = null },
            title = { Text("Claim your profile") },
            text = {
                Text("${result.inviterName ?: "A friend"} added someone named '${result.candidateName}' to this group. Is this you, or would you like to join as a new member?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        groupSyncManager.confirmJoin(result, shouldMerge = true)
                        pendingJoin = null
                    }
                ) {
                    Text("Yes, that's me")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        groupSyncManager.confirmJoin(result, shouldMerge = false)
                        pendingJoin = null
                    }
                ) {
                    Text("No, join as new")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                PermissionUtils.setWeeklySummaryEnabled(context, false)
            }
        }
    )

    LaunchedEffect(isAuthenticated, currentRoute) {
        if (isAuthenticated && currentRoute == "home" && PermissionUtils.isWeeklySummaryEnabled(context)) {
            if (!PermissionUtils.areNotificationsEnabled(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    PermissionUtils.setWeeklySummaryEnabled(context, false)
                }
            }
        }
    }

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
                        ),
                        deepLinks = listOf(
                            navDeepLink { uriPattern = "cleave://add" }
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
                            onBack = { navController.popBackStack() },
                            onSettled = { navController.popBackStack() }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onBudgetClick = { navController.navigate("budget_setup") },
                            onManageCategoriesClick = { navController.navigate("manage_categories") }
                        )
                    }

                    composable("budget_setup") {
                        BudgetSetupScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("manage_categories") {
                        ManageCategoriesScreen(
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
