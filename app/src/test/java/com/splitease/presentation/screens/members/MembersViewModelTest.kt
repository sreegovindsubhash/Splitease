package com.splitease.presentation.screens.members

import com.splitease.domain.model.Member
import com.splitease.domain.repository.MemberRepository
import com.splitease.domain.usecase.AddMemberUseCase
import com.splitease.domain.usecase.DeleteMemberUseCase
import com.splitease.domain.usecase.GetMembersForGroupUseCase
import com.splitease.domain.usecase.UpdateMemberUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MembersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeMemberRepository
    private lateinit var viewModel: MembersViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMemberRepository()
        viewModel = buildViewModel()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(groupId: Long = 1L) = MembersViewModel(
        groupId = groupId,
        getMembersForGroupUseCase = GetMembersForGroupUseCase(fakeRepository),
        addMemberUseCase = AddMemberUseCase(fakeRepository),
        updateMemberUseCase = UpdateMemberUseCase(fakeRepository),
        deleteMemberUseCase = DeleteMemberUseCase(fakeRepository),
    )

    // ── Initial / loading state ───────────────────────────────────────────────

    @Test
    fun initialState_isLoading() {
        val freshViewModel = buildViewModel()
        assertTrue(freshViewModel.uiState.value.isLoading)
    }

    @Test
    fun afterLoad_withNoMembers_stateIsEmpty() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertTrue(state.members.isEmpty())
    }

    @Test
    fun afterLoad_withMembers_stateShowsMembers() = runTest {
        fakeRepository.seedMember(Member(id = 1L, groupId = 1L, name = "Alice"))
        fakeRepository.seedMember(Member(id = 2L, groupId = 1L, name = "Bob"))

        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertEquals(2, state.members.size)
    }

    // ── Invalid groupId ───────────────────────────────────────────────────────

    @Test
    fun negativeGroupId_setsGroupNotFound() {
        val vm = buildViewModel(groupId = -1L)
        assertTrue(vm.uiState.value.groupNotFound)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun zeroGroupId_setsGroupNotFound() {
        val vm = buildViewModel(groupId = 0L)
        assertTrue(vm.uiState.value.groupNotFound)
    }

    // ── Add sheet ─────────────────────────────────────────────────────────────

    @Test
    fun openAddSheet_setsSheetOpenAndClearsState() {
        viewModel.onOpenAddSheet()
        val state = viewModel.uiState.value
        assertTrue(state.isSheetOpen)
        assertNull(state.editingMember)
        assertEquals("", state.nameInput)
        assertNull(state.nameError)
    }

    @Test
    fun dismissSheet_closesSheet() {
        viewModel.onOpenAddSheet()
        viewModel.onDismissSheet()
        assertFalse(viewModel.uiState.value.isSheetOpen)
    }

    // ── Validation: blank name ────────────────────────────────────────────────

    @Test
    fun blankMemberName_isRejected() = runTest {
        viewModel.onOpenAddSheet()
        viewModel.onNameInputChange("")
        viewModel.onConfirmSheet()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.nameError, "Expected a name error for blank name")
        assertTrue(state.isSheetOpen, "Sheet should remain open on validation error")
        assertEquals(0, fakeRepository.members.size)
    }

    @Test
    fun whitespaceOnlyMemberName_isRejected() = runTest {
        viewModel.onOpenAddSheet()
        viewModel.onNameInputChange("   ")
        viewModel.onConfirmSheet()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.nameError, "Expected a name error for whitespace-only name")
        assertEquals(0, fakeRepository.members.size)
    }

    // ── Valid member creation ─────────────────────────────────────────────────

    @Test
    fun validMemberName_createsSuccessfully() = runTest {
        viewModel.onOpenAddSheet()
        viewModel.onNameInputChange("Alice")
        viewModel.onConfirmSheet()

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSheetOpen, "Sheet should close after success")
        assertEquals(1, fakeRepository.members.size)
        assertEquals("Alice", fakeRepository.members.values.first().name)
    }

    // ── Name trimming ─────────────────────────────────────────────────────────

    @Test
    fun memberName_isTrimmedBeforeSave() = runTest {
        viewModel.onOpenAddSheet()
        viewModel.onNameInputChange("  Alice  ")
        viewModel.onConfirmSheet()

        testDispatcher.scheduler.advanceUntilIdle()

        val savedName = fakeRepository.members.values.first().name
        assertEquals("Alice", savedName)
    }

    @Test
    fun memberName_trimToBlank_isRejected() = runTest {
        viewModel.onOpenAddSheet()
        viewModel.onNameInputChange("   \t  ")
        viewModel.onConfirmSheet()

        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.nameError)
        assertEquals(0, fakeRepository.members.size)
    }

    // ── Duplicate submission prevention ──────────────────────────────────────

    @Test
    fun duplicateSubmit_isPrevented() = runTest {
        fakeRepository.delay = true
        viewModel.onOpenAddSheet()
        viewModel.onNameInputChange("Alice")

        viewModel.onConfirmSheet()
        // Second call fires before the first completes
        viewModel.onConfirmSheet()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.addCallCount, "Repository should only be called once")
    }

    // ── Edit member ───────────────────────────────────────────────────────────

    @Test
    fun openEditSheet_populatesExistingName() {
        val member = Member(id = 1L, groupId = 1L, name = "Alice")
        viewModel.onOpenEditSheet(member)

        val state = viewModel.uiState.value
        assertTrue(state.isSheetOpen)
        assertEquals("Alice", state.nameInput)
        assertEquals(member, state.editingMember)
    }

    @Test
    fun editMember_validName_updatesSuccessfully() = runTest {
        val member = Member(id = 1L, groupId = 1L, name = "Alice")
        fakeRepository.seedMember(member)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onOpenEditSheet(member)
        viewModel.onNameInputChange("Alicia")
        viewModel.onConfirmSheet()

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSheetOpen)
        assertEquals("Alicia", fakeRepository.members[1L]?.name)
    }

    @Test
    fun editMember_blankName_isRejected() = runTest {
        val member = Member(id = 1L, groupId = 1L, name = "Alice")
        fakeRepository.seedMember(member)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onOpenEditSheet(member)
        viewModel.onNameInputChange("  ")
        viewModel.onConfirmSheet()

        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.nameError)
        // Original name should be unchanged
        assertEquals("Alice", fakeRepository.members[1L]?.name)
    }

    @Test
    fun editMember_nameIsTrimmed() = runTest {
        val member = Member(id = 1L, groupId = 1L, name = "Alice")
        fakeRepository.seedMember(member)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onOpenEditSheet(member)
        viewModel.onNameInputChange("  Alicia  ")
        viewModel.onConfirmSheet()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Alicia", fakeRepository.members[1L]?.name)
    }

    // ── Delete member ─────────────────────────────────────────────────────────

    @Test
    fun requestDelete_showsConfirmationDialog() {
        val member = Member(id = 1L, groupId = 1L, name = "Alice")
        viewModel.onRequestDelete(member)
        assertEquals(member, viewModel.uiState.value.memberPendingDelete)
    }

    @Test
    fun dismissDeleteConfirmation_clearsPendingDelete() {
        val member = Member(id = 1L, groupId = 1L, name = "Alice")
        viewModel.onRequestDelete(member)
        viewModel.onDismissDeleteConfirmation()
        assertNull(viewModel.uiState.value.memberPendingDelete)
    }

    @Test
    fun confirmDelete_removesMemberFromRepository() = runTest {
        val member = Member(id = 1L, groupId = 1L, name = "Alice")
        fakeRepository.seedMember(member)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onRequestDelete(member)
        viewModel.onConfirmDelete()

        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.memberPendingDelete)
        assertFalse(fakeRepository.members.containsKey(1L))
    }

    // ── ViewModel state transitions ───────────────────────────────────────────

    @Test
    fun nameInputChange_clearsPreviousNameError() = runTest {
        viewModel.onOpenAddSheet()
        viewModel.onConfirmSheet() // triggers blank-name error
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.nameError)

        viewModel.onNameInputChange("Alice")
        assertNull(viewModel.uiState.value.nameError)
    }

    @Test
    fun repositoryError_surfacesAsErrorMessage() = runTest {
        fakeRepository.shouldThrowOnAdd = true
        viewModel.onOpenAddSheet()
        viewModel.onNameInputChange("Alice")
        viewModel.onConfirmSheet()

        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onErrorDismissed_clearsErrorMessage() = runTest {
        fakeRepository.shouldThrowOnAdd = true
        viewModel.onOpenAddSheet()
        viewModel.onNameInputChange("Alice")
        viewModel.onConfirmSheet()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onErrorDismissed()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}

// ── Fake repository ───────────────────────────────────────────────────────────

private class FakeMemberRepository : MemberRepository {

    val members: MutableMap<Long, Member> = mutableMapOf()
    private val membersFlow = MutableStateFlow<List<Member>>(emptyList())
    private var nextId = 1L
    var addCallCount = 0
    var delay = false
    var shouldThrowOnAdd = false

    fun seedMember(member: Member) {
        members[member.id] = member
        membersFlow.value = members.values.toList()
    }

    override fun getMembersForGroup(groupId: Long): Flow<List<Member>> =
        membersFlow.map { list -> list.filter { it.groupId == groupId } }

    override suspend fun getMemberById(id: Long): Member? = members[id]

    override suspend fun addMember(member: Member): Long {
        addCallCount++
        if (delay) kotlinx.coroutines.delay(10_000)
        if (shouldThrowOnAdd) throw RuntimeException("Database error")
        val id = nextId++
        val saved = member.copy(id = id)
        members[id] = saved
        membersFlow.value = members.values.toList()
        return id
    }

    override suspend fun updateMember(member: Member) {
        members[member.id] = member
        membersFlow.value = members.values.toList()
    }

    override suspend fun deleteMember(member: Member) {
        members.remove(member.id)
        membersFlow.value = members.values.toList()
    }
}
