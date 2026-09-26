package com.splitease.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.splitease.SplitEaseApplication
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

@Composable
fun SplitEaseNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val app = context.applicationContext as SplitEaseApplication

    NavHost(
        navController = navController,
        startDestination = Screen.Groups.route,
    ) {
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
            )
        }
    }
}
