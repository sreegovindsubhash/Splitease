package com.splitease.presentation.screens.balances

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.splitease.domain.repository.ExpenseRepository
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.repository.MemberRepository
import com.splitease.domain.usecase.CalculateMemberBalancesUseCase

class BalancesViewModelFactory(
    private val groupId: Long,
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BalancesViewModel::class.java)) {
            return BalancesViewModel(
                groupId = groupId,
                groupRepository = groupRepository,
                memberRepository = memberRepository,
                expenseRepository = expenseRepository,
                calculateBalances = CalculateMemberBalancesUseCase(),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
