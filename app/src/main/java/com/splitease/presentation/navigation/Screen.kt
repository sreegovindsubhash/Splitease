package com.splitease.presentation.navigation

/** All navigation destinations in the app. */
sealed class Screen(val route: String) {

    data object Groups : Screen("groups")

    data object CreateGroup : Screen("create_group")

    data object GroupDetails : Screen("group_details/{groupId}") {
        fun createRoute(groupId: Long) = "group_details/$groupId"
    }

    data object Members : Screen("members/{groupId}") {
        fun createRoute(groupId: Long) = "members/$groupId"
    }

    data object Onboarding : Screen("onboarding")
}
