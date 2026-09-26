package com.splitease.presentation.screens.groupDetails

import com.splitease.domain.model.Group
import com.splitease.domain.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeRepository: FakeGroupRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGroupRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(groupId: Long = 1L) = GroupDetailsViewModel(
        groupRepository = fakeRepository,
        groupId = groupId,
    )

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialState_isLoading() {
        val viewModel = buildViewModel()
        assertIs<GroupDetailsUiState.Loading>(viewModel.uiState.value)
    }

    // ── Success ───────────────────────────────────────────────────────────────

    @Test
    fun whenGroupExists_stateIsSuccess() = runTest {
        val group = makeGroup(id = 1L, name = "Goa Trip")
        fakeRepository.groups[1L] = group

        val viewModel = buildViewModel(groupId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<GroupDetailsUiState.Success>(state)
        assertEquals("Goa Trip", state.group.name)
    }

    @Test
    fun successState_exposesAllGroupFields() = runTest {
        val group = makeGroup(
            id = 2L,
            name = "Hostel 204",
            description = "Second year room",
            currencyCode = "USD",
            memberCount = 3,
            totalAmountMinorUnits = 150_00L,
        )
        fakeRepository.groups[2L] = group

        val viewModel = buildViewModel(groupId = 2L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as GroupDetailsUiState.Success
        assertEquals("Hostel 204", state.group.name)
        assertEquals("Second year room", state.group.description)
        assertEquals("USD", state.group.currencyCode)
        assertEquals(3, state.group.memberCount)
        assertEquals(150_00L, state.group.totalAmountMinorUnits)
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    fun whenGroupDoesNotExist_stateIsNotFound() = runTest {
        // No group inserted for id 99

        val viewModel = buildViewModel(groupId = 99L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<GroupDetailsUiState.NotFound>(viewModel.uiState.value)
    }

    // ── Error ─────────────────────────────────────────────────────────────────

    @Test
    fun whenRepositoryThrows_stateIsError() = runTest {
        fakeRepository.shouldThrow = true

        val viewModel = buildViewModel(groupId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<GroupDetailsUiState.Error>(state)
    }

    @Test
    fun errorMessage_isPopulatedFromException() = runTest {
        fakeRepository.shouldThrow = true
        fakeRepository.errorMessage = "Database is locked"

        val viewModel = buildViewModel(groupId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as GroupDetailsUiState.Error
        assertEquals("Database is locked", state.message)
    }

    // ── Retry ─────────────────────────────────────────────────────────────────

    @Test
    fun retry_resetsToLoadingThenSucceeds() = runTest {
        fakeRepository.shouldThrow = true

        val viewModel = buildViewModel(groupId = 1L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<GroupDetailsUiState.Error>(viewModel.uiState.value)

        // Fix repository, then retry
        fakeRepository.shouldThrow = false
        fakeRepository.groups[1L] = makeGroup(id = 1L, name = "Fixed Group")

        viewModel.retry()
        // Immediately after retry() the state returns to Loading
        assertIs<GroupDetailsUiState.Loading>(viewModel.uiState.value)

        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<GroupDetailsUiState.Success>(viewModel.uiState.value)
    }

    @Test
    fun retry_afterNotFound_canSucceedIfGroupAppearsLater() = runTest {
        val viewModel = buildViewModel(groupId = 5L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertIs<GroupDetailsUiState.NotFound>(viewModel.uiState.value)

        fakeRepository.groups[5L] = makeGroup(id = 5L, name = "Late Group")
        viewModel.retry()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as GroupDetailsUiState.Success
        assertEquals("Late Group", state.group.name)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun makeGroup(
    id: Long,
    name: String,
    description: String = "",
    currencyCode: String = "INR",
    memberCount: Int = 0,
    totalAmountMinorUnits: Long = 0L,
) = Group(
    id = id,
    name = name,
    description = description,
    currencyCode = currencyCode,
    memberCount = memberCount,
    totalAmountMinorUnits = totalAmountMinorUnits,
)

// ── Fake repository ───────────────────────────────────────────────────────────

private class FakeGroupRepository : GroupRepository {

    val groups: MutableMap<Long, Group> = mutableMapOf()
    var shouldThrow: Boolean = false
    var errorMessage: String = "Unexpected error"

    /** Backing StateFlow so tests can trigger reactive updates. */
    private val _groupsFlow = MutableStateFlow<Map<Long, Group>>(emptyMap())

    override fun getGroups(): Flow<List<Group>> = flowOf(groups.values.toList())

    override fun observeGroupById(id: Long): Flow<Group?> {
        if (shouldThrow) throw RuntimeException(errorMessage)
        // Emit from the current map snapshot so the fake behaves like a cold Flow.
        return flowOf(groups[id])
    }

    override suspend fun getGroupById(id: Long): Group? {
        if (shouldThrow) throw RuntimeException(errorMessage)
        return groups[id]
    }

    override suspend fun createGroup(group: Group): Long {
        val newId = (groups.keys.maxOrNull() ?: 0L) + 1L
        groups[newId] = group.copy(id = newId)
        return newId
    }

    override suspend fun updateGroup(group: Group) {
        groups[group.id] = group
    }

    override suspend fun deleteGroup(group: Group) {
        groups.remove(group.id)
    }
}
