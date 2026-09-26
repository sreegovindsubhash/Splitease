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

    data object Expenses : Screen("expenses/{groupId}") {
        fun createRoute(groupId: Long) = "expenses/$groupId"
    }

    /** editExpenseId = 0 means "add new expense" */
    data object AddEditExpense : Screen("add_edit_expense/{groupId}/{editExpenseId}") {
        fun createRoute(groupId: Long, editExpenseId: Long = 0L) =
            "add_edit_expense/$groupId/$editExpenseId"
    }

    data object Balances : Screen("balances/{groupId}") {
        fun createRoute(groupId: Long) = "balances/$groupId"
    }

    data object Settlement : Screen("settlement/{groupId}") {
        fun createRoute(groupId: Long) = "settlement/$groupId"
    }

    data object Onboarding : Screen("onboarding")
}
