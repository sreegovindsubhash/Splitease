package com.splitease.presentation.screens.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.splitease.domain.repository.ExpenseRepository
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.repository.MemberRepository
import com.splitease.domain.repository.SettlementPaymentRepository
import com.splitease.domain.usecase.CalculateMemberBalancesUseCase
import com.splitease.domain.usecase.GetSettlementsUseCase

class SummaryViewModelFactory(
    private val groupId: Long,
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementPaymentRepository: SettlementPaymentRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SummaryViewModel::class.java)) {
            return SummaryViewModel(
                groupId = groupId,
                groupRepository = groupRepository,
                memberRepository = memberRepository,
                expenseRepository = expenseRepository,
                settlementPaymentRepository = settlementPaymentRepository,
                calculateBalances = CalculateMemberBalancesUseCase(),
                getSettlements = GetSettlementsUseCase(CalculateMemberBalancesUseCase()),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
