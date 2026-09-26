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
    }
}
