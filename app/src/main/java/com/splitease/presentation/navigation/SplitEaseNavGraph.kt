package com.splitease.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.splitease.SplitEaseApplication
import com.splitease.data.repository.ThemePreferenceRepository
import com.splitease.presentation.screens.createGroup.CreateGroupScreen
import com.splitease.presentation.screens.createGroup.CreateGroupViewModel
import com.splitease.presentation.screens.createGroup.CreateGroupViewModelFactory
import com.splitease.presentation.screens.groupDetails.GroupDetailsScreen
import com.splitease.presentation.screens.groupDetails.GroupDetailsViewModel
import com.splitease.presentation.screens.groupDetails.GroupDetailsViewModelFactory
import com.splitease.presentation.screens.groups.GroupsScreen
import com.splitease.presentation.screens.groups.GroupsViewModel
import com.splitease.presentation.screens.groups.GroupsViewModelFactory
import com.splitease.presentation.screens.expenses.AddEditExpenseScreen
import com.splitease.presentation.screens.expenses.AddEditExpenseViewModel
import com.splitease.presentation.screens.expenses.AddEditExpenseViewModelFactory
import com.splitease.presentation.screens.expenses.ExpensesScreen
import com.splitease.presentation.screens.expenses.ExpensesViewModel
import com.splitease.presentation.screens.expenses.ExpensesViewModelFactory
import com.splitease.presentation.screens.balances.BalancesScreen
import com.splitease.presentation.screens.balances.BalancesViewModel
import com.splitease.presentation.screens.balances.BalancesViewModelFactory
import com.splitease.presentation.screens.members.MembersScreen
import com.splitease.presentation.screens.members.MembersViewModel
import com.splitease.presentation.screens.members.MembersViewModelFactory
import com.splitease.presentation.screens.settlement.SettlementScreen
import com.splitease.presentation.screens.settlement.SettlementViewModel
import com.splitease.presentation.screens.settlement.SettlementViewModelFactory
import com.splitease.presentation.screens.onboarding.OnboardingScreen
import com.splitease.presentation.screens.onboarding.OnboardingViewModel
import com.splitease.presentation.screens.onboarding.OnboardingViewModelFactory
import com.splitease.presentation.screens.settings.SettingsScreen
import com.splitease.presentation.screens.summary.SummaryScreen
import com.splitease.presentation.screens.summary.SummaryViewModel
import com.splitease.presentation.screens.summary.SummaryViewModelFactory
import com.splitease.presentation.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun SplitEaseNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Groups.route,
    themePreferenceRepository: ThemePreferenceRepository? = null,
    currentTheme: AppTheme = AppTheme.SYSTEM,
) {
    val context = LocalContext.current
    val app = context.applicationContext as SplitEaseApplication
    val scope = rememberCoroutineScope()

    // Use the repo passed from MainActivity; fall back to the app singleton when
    // the default no-arg overload is called (e.g. from Compose previews).
    val themeRepo = themePreferenceRepository ?: app.themePreferenceRepository

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Screen.Onboarding.route) {
            val viewModel: OnboardingViewModel = viewModel(
                factory = OnboardingViewModelFactory(app.onboardingPreferenceRepository),
            )
            OnboardingScreen(
                viewModel = viewModel,
                onNavigateToCreateGroup = {
                    navController.navigate(Screen.CreateGroup.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onNavigateToGroups = {
                    navController.navigate(Screen.Groups.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Groups.route) {
            val viewModel: GroupsViewModel = viewModel(
                factory = GroupsViewModelFactory(app.groupRepository),
            )
            GroupsScreen(
                viewModel = viewModel,
                onCreateGroup = { navController.navigate(Screen.CreateGroup.route) },
                onGroupClick = { groupId ->
                    navController.navigate(Screen.GroupDetails.createRoute(groupId))
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.CreateGroup.route) {
            val viewModel: CreateGroupViewModel = viewModel(
                factory = CreateGroupViewModelFactory(app.groupRepository),
            )
            CreateGroupScreen(
                viewModel = viewModel,
                onNavigateToGroupDetails = { groupId ->
                    navController.navigate(Screen.GroupDetails.createRoute(groupId)) {
                        popUpTo(Screen.CreateGroup.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.GroupDetails.route,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
            val viewModel: GroupDetailsViewModel = viewModel(
                factory = GroupDetailsViewModelFactory(app.groupRepository, groupId),
            )
            GroupDetailsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMembers = { navController.navigate(Screen.Members.createRoute(groupId)) },
                onNavigateToExpenses = { navController.navigate(Screen.Expenses.createRoute(groupId)) },
                onNavigateToBalances = { navController.navigate(Screen.Balances.createRoute(groupId)) },
                onNavigateToSettlement = { navController.navigate(Screen.Settlement.createRoute(groupId)) },
                onNavigateToSummary = { navController.navigate(Screen.Summary.createRoute(groupId)) },
            )
        }

        composable(
            route = Screen.Members.route,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
            val viewModel: MembersViewModel = viewModel(
                factory = MembersViewModelFactory(app.memberRepository, groupId),
            )
            MembersScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Expenses.route,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
            val viewModel: ExpensesViewModel = viewModel(
                factory = ExpensesViewModelFactory(
                    groupId = groupId,
                    expenseRepository = app.expenseRepository,
                    groupRepository = app.groupRepository,
                    memberRepository = app.memberRepository,
                ),
            )
            ExpensesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onAddExpense = {
                    navController.navigate(Screen.AddEditExpense.createRoute(groupId, 0L))
                },
                onEditExpense = { expenseId ->
                    navController.navigate(Screen.AddEditExpense.createRoute(groupId, expenseId))
                },
            )
        }

        composable(
            route = Screen.AddEditExpense.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.LongType },
                navArgument("editExpenseId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
            val editExpenseId = backStackEntry.arguments?.getLong("editExpenseId") ?: 0L
            val viewModel: AddEditExpenseViewModel = viewModel(
                factory = AddEditExpenseViewModelFactory(
                    groupId = groupId,
                    editExpenseId = editExpenseId,
                    expenseRepository = app.expenseRepository,
                    groupRepository = app.groupRepository,
                    memberRepository = app.memberRepository,
                ),
            )
            AddEditExpenseScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateAfterSave = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Balances.route,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
            val viewModel: BalancesViewModel = viewModel(
                factory = BalancesViewModelFactory(
                    groupId = groupId,
                    groupRepository = app.groupRepository,
                    memberRepository = app.memberRepository,
                    expenseRepository = app.expenseRepository,
                ),
            )
            BalancesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettlement = { navController.navigate(Screen.Settlement.createRoute(groupId)) },
            )
        }

        composable(
            route = Screen.Settlement.route,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
            val viewModel: SettlementViewModel = viewModel(
                factory = SettlementViewModelFactory(
                    groupId = groupId,
                    groupRepository = app.groupRepository,
                    memberRepository = app.memberRepository,
                    expenseRepository = app.expenseRepository,
                    settlementPaymentRepository = app.settlementPaymentRepository,
                ),
            )
            SettlementScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Summary.route,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable
            val viewModel: SummaryViewModel = viewModel(
                factory = SummaryViewModelFactory(
                    groupId = groupId,
                    groupRepository = app.groupRepository,
                    memberRepository = app.memberRepository,
                    expenseRepository = app.expenseRepository,
                    settlementPaymentRepository = app.settlementPaymentRepository,
                ),
            )
            SummaryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBalances = { navController.navigate(Screen.Balances.createRoute(groupId)) },
                onNavigateToExpenses = { navController.navigate(Screen.Expenses.createRoute(groupId)) },
                onNavigateToSettlement = { navController.navigate(Screen.Settlement.createRoute(groupId)) },
                onNavigateToAddExpense = {
                    navController.navigate(Screen.AddEditExpense.createRoute(groupId, 0L))
                },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                currentTheme = currentTheme,
                onThemeSelected = { theme ->
                    scope.launch { themeRepo.setTheme(theme) }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
