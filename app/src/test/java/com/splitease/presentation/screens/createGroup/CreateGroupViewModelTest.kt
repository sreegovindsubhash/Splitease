package com.splitease.presentation.screens.createGroup

import com.splitease.domain.model.Group
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.usecase.CreateGroupUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateGroupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeRepository: FakeGroupRepository
    private lateinit var viewModel: CreateGroupViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGroupRepository()
        viewModel = CreateGroupViewModel(CreateGroupUseCase(fakeRepository))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun blankGroupName_isRejected() = runTest {
        viewModel.onNameChange("")
        viewModel.onCreateGroup()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.nameError, "Expected a name error for blank name")
        assertNull(state.savedGroupId, "No group should have been saved")
        assertFalse(state.isSaving)
    }

    @Test
    fun whitespaceOnlyGroupName_isRejected() = runTest {
        viewModel.onNameChange("   ")
        viewModel.onCreateGroup()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.nameError, "Expected a name error for whitespace-only name")
        assertNull(state.savedGroupId, "No group should have been saved")
    }

    @Test
    fun validGroupName_createsSuccessfully() = runTest {
        viewModel.onNameChange("Goa Trip")
        viewModel.onCreateGroup()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.nameError)
        assertNull(state.errorMessage)
        assertNotNull(state.savedGroupId, "Expected savedGroupId to be set after creation")
        assertEquals(1L, state.savedGroupId)
    }

    @Test
    fun nameAndDescription_areTrimmed() = runTest {
        viewModel.onNameChange("  Goa Trip  ")
        viewModel.onDescriptionChange("  Beach holiday  ")
        viewModel.onCreateGroup()

        testDispatcher.scheduler.advanceUntilIdle()

        val savedGroup = fakeRepository.lastSaved
        assertNotNull(savedGroup)
        assertEquals("Goa Trip", savedGroup.name)
        assertEquals("Beach holiday", savedGroup.description)
    }

    @Test
    fun duplicateSubmit_isPrevented() = runTest {
        // Slow repository so the first call is still in-flight when the second arrives
        fakeRepository.delay = true
        viewModel.onNameChange("Trip")

        viewModel.onCreateGroup()
        // Second call fires before the first completes
        viewModel.onCreateGroup()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.createCallCount, "Repository should only be called once")
    }
}

// ── Fake ─────────────────────────────────────────────────────────────────────

private class FakeGroupRepository : GroupRepository {

    var delay: Boolean = false
    var createCallCount: Int = 0
    var lastSaved: Group? = null
    private var nextId: Long = 1L

    override fun getGroups(): Flow<List<Group>> = flowOf(emptyList())

    override suspend fun getGroupById(id: Long): Group? = null

    override suspend fun createGroup(group: Group): Long {
        createCallCount++
        if (delay) {
            // Suspend long enough to keep the save in-flight
            kotlinx.coroutines.delay(10_000)
        }
        lastSaved = group
        return nextId++
    }

    override suspend fun updateGroup(group: Group) = Unit

    override suspend fun deleteGroup(group: Group) = Unit
}
