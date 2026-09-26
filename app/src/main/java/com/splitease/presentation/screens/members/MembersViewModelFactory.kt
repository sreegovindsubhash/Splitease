package com.splitease.presentation.screens.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.splitease.domain.repository.MemberRepository
import com.splitease.domain.usecase.AddMemberUseCase
import com.splitease.domain.usecase.DeleteMemberUseCase
import com.splitease.domain.usecase.GetMembersForGroupUseCase
import com.splitease.domain.usecase.UpdateMemberUseCase

class MembersViewModelFactory(
    private val memberRepository: MemberRepository,
    private val groupId: Long,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MembersViewModel::class.java)) {
            return MembersViewModel(
                groupId = groupId,
                getMembersForGroupUseCase = GetMembersForGroupUseCase(memberRepository),
                addMemberUseCase = AddMemberUseCase(memberRepository),
                updateMemberUseCase = UpdateMemberUseCase(memberRepository),
                deleteMemberUseCase = DeleteMemberUseCase(memberRepository),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
