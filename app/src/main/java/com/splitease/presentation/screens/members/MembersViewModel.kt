package com.splitease.presentation.screens.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.domain.model.Member
import com.splitease.domain.usecase.AddMemberUseCase
import com.splitease.domain.usecase.DeleteMemberUseCase
import com.splitease.domain.usecase.GetMembersForGroupUseCase
import com.splitease.domain.usecase.UpdateMemberUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MembersViewModel(
    private val groupId: Long,
    private val getMembersForGroupUseCase: GetMembersForGroupUseCase,
    private val addMemberUseCase: AddMemberUseCase,
    private val updateMemberUseCase: UpdateMemberUseCase,
    private val deleteMemberUseCase: DeleteMemberUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MembersUiState(groupId = groupId))
    val uiState: StateFlow<MembersUiState> = _uiState.asStateFlow()

    init {
        if (groupId <= 0L) {
            _uiState.update { it.copy(isLoading = false, groupNotFound = true) }
        } else {
            observeMembers()
        }
    }

    private fun observeMembers() {
        viewModelScope.launch {
            getMembersForGroupUseCase(groupId)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Failed to load members.",
                        )
                    }
                }
                .collect { members ->
                    _uiState.update {
                        it.copy(isLoading = false, members = members, errorMessage = null)
                    }
                }
        }
    }

    // ── Add / Edit sheet ──────────────────────────────────────────────────────

    fun onOpenAddSheet() {
        _uiState.update {
            it.copy(
                isSheetOpen = true,
                editingMember = null,
                nameInput = "",
                nameError = null,
                isSaving = false,
            )
        }
    }

    fun onOpenEditSheet(member: Member) {
        _uiState.update {
            it.copy(
                isSheetOpen = true,
                editingMember = member,
                nameInput = member.name,
                nameError = null,
                isSaving = false,
            )
        }
    }

    fun onDismissSheet() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSheetOpen = false, nameError = null) }
    }

    fun onNameInputChange(value: String) {
        _uiState.update { it.copy(nameInput = value, nameError = null) }
    }

    fun onConfirmSheet() {
        val state = _uiState.value
        if (state.isSaving) return

        val trimmed = state.nameInput.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(nameError = "Member name is required") }
            return
        }

        _uiState.update { it.copy(isSaving = true, nameError = null) }

        viewModelScope.launch {
            try {
                val existing = state.editingMember
                if (existing == null) {
                    addMemberUseCase(groupId = groupId, name = trimmed)
                } else {
                    updateMemberUseCase(member = existing, newName = trimmed)
                }
                _uiState.update { it.copy(isSaving = false, isSheetOpen = false) }
            } catch (e: IllegalArgumentException) {
                _uiState.update {
                    it.copy(isSaving = false, nameError = e.message ?: "Invalid name")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save member. Please try again.",
                    )
                }
            }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun onRequestDelete(member: Member) {
        _uiState.update { it.copy(memberPendingDelete = member) }
    }

    fun onDismissDeleteConfirmation() {
        _uiState.update { it.copy(memberPendingDelete = null) }
    }

    fun onConfirmDelete() {
        val member = _uiState.value.memberPendingDelete ?: return
        _uiState.update { it.copy(memberPendingDelete = null) }

        viewModelScope.launch {
            try {
                deleteMemberUseCase(member)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Failed to remove member. Please try again.",
                    )
                }
            }
        }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
