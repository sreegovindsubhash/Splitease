package com.splitease.presentation.screens.groupDetails

import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseCategory
import com.splitease.domain.model.ExpenseSplit
import com.splitease.domain.model.Group
import com.splitease.domain.model.SplitMethod
import com.splitease.domain.repository.ExpenseRepository
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.usecase.DeleteGroupUseCase
import com.splitease.domain.usecase.SetGroupBudgetUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the Group Budget feature.
 *
 * Covers:
 * - No budget configured
 * - Setting a valid budget
 * - Rejecting zero/negative/invalid budget
 * - Editing an existing budget
 * - Budget persistence/retrieval
 * - Spending calculation
 * - Remaining amount when under budget
 * - Over-budget calculation
 * - Progress calculation and capping at 100%
 * - Expense add/edit/delete affecting spent amount
 * - Settlement payments NOT affecting spent amount
 * - Existing groups remaining valid after database migration (null budget)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupBudgetViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeGroupRepository: FakeBudgetGroupRepository
    private lateinit var fakeExpenseRepository: FakeBudgetExpenseRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGroupRepository = FakeBudgetGroupRepository()
        fakeExpenseRepository = FakeBudgetExpenseRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(groupId: Long = 1L) = GroupDetailsViewModel(
        groupRepository = fakeGroupRepository,
        expenseRepository = fakeExpenseRepository,
        deleteGroupUseCase = DeleteGroupUseCase(fakeGroupRepository),
        setGroupBudgetUseCase = SetGroupBudgetUseCase(fakeGroupRepository),
        groupId = groupId,
    )

    // ─────────────────────────────────────────────────────────────────────────
    // 1. No budget configured
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun noBudget_successState_groupBudgetIsNull() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = null))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as GroupDetailsUiState.Success
        assertNull(state.group.budgetMinorUnits)
    }

    @Test
    fun noBudget_existingGroupAfterMigration_budgetNullAndDataIntact() = runTest {
        // Simulates a group that existed before the budget column was added:
        // budgetMinorUnits = null, but all other fields must be intact.
        val group = makeGroup(
            id = 2L,
            name = "Pre-migration Group",
            currencyCode = "USD",
            budgetMinorUnits = null,
        )
        fakeGroupRepository.setGroup(group)

        val vm = buildViewModel(groupId = 2L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as GroupDetailsUiState.Success
        assertEquals("Pre-migration Group", state.group.name)
        assertEquals("USD", state.group.currencyCode)
        assertNull(state.group.budgetMinorUnits, "Existing groups must have null budget after migration")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Setting a valid budget
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun setValidBudget_budgetIsPersisted() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = null))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val error = vm.onBudgetConfirmed("100.00")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(error, "Expected no validation error for valid input")
        assertEquals(100_00L, fakeGroupRepository.getGroup(1L)?.budgetMinorUnits)
    }

    @Test
    fun setValidBudget_integerInput_parsedCorrectly() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = null))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val error = vm.onBudgetConfirmed("500")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(error)
        assertEquals(500_00L, fakeGroupRepository.getGroup(1L)?.budgetMinorUnits)
    }

    @Test
    fun setValidBudget_stateUpdatesReflectNewBudget() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = null))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onBudgetConfirmed("200.50")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as GroupDetailsUiState.Success
        assertEquals(200_50L, state.group.budgetMinorUnits)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Rejecting zero / negative / invalid budget
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun zeroBudget_isRejectedWithError() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val error = vm.onBudgetConfirmed("0")
        assertTrue(error != null && error.isNotBlank(), "Zero budget must produce a validation error")
    }

    @Test
    fun zeroBudget_00_isRejectedWithError() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val error = vm.onBudgetConfirmed("0.00")
        assertTrue(error != null && error.isNotBlank(), "0.00 must produce a validation error")
    }

    @Test
    fun negativeBudget_isRejectedWithError() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val error = vm.onBudgetConfirmed("-100")
        assertTrue(error != null && error.isNotBlank(), "Negative budget must produce a validation error")
    }

    @Test
    fun emptyInput_isRejectedWithError() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val error = vm.onBudgetConfirmed("")
        assertTrue(error != null && error.isNotBlank(), "Empty input must produce a validation error")
    }

    @Test
    fun nonNumericInput_isRejectedWithError() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val error = vm.onBudgetConfirmed("abc")
        assertTrue(error != null && error.isNotBlank(), "Non-numeric input must produce a validation error")
    }

    @Test
    fun invalidInput_doesNotChangePersisted_budget() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 500_00L))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onBudgetConfirmed("0") // rejected
        testDispatcher.scheduler.advanceUntilIdle()

        // Budget must remain unchanged at 500_00L
        assertEquals(500_00L, fakeGroupRepository.getGroup(1L)?.budgetMinorUnits)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Editing an existing budget
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun editBudget_replacesExistingValue() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 500_00L))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val error = vm.onBudgetConfirmed("750.00")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(error)
        assertEquals(750_00L, fakeGroupRepository.getGroup(1L)?.budgetMinorUnits)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Budget persistence / retrieval
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun budgetPersistence_retrievedCorrectlyFromRepository() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 1_000_00L))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as GroupDetailsUiState.Success
        assertEquals(1_000_00L, state.group.budgetMinorUnits)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Spending calculation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun spendingCalculation_sumOfExpenseAmounts() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 10_000_00L))
        fakeExpenseRepository.setExpenses(
            groupId = 1L,
            expenses = listOf(
                makeExpense(groupId = 1L, amountMinorUnits = 3_000_00L),
                makeExpense(groupId = 1L, amountMinorUnits = 2_500_00L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as GroupDetailsUiState.Success
        assertEquals(5_500_00L, state.totalSpentMinorUnits)
    }

    @Test
    fun spendingCalculation_noExpenses_spentIsZero() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 5_000_00L))
        fakeExpenseRepository.setExpenses(groupId = 1L, expenses = emptyList())

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as GroupDetailsUiState.Success
        assertEquals(0L, state.totalSpentMinorUnits)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Remaining amount when under budget
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun remainingAmount_underBudget_isCorrect() = runTest {
        // budget = 10,000 minor units, spending = 6,750 → remaining = 3,250
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 10_000L))
        fakeExpenseRepository.setExpenses(
            groupId = 1L,
            expenses = listOf(makeExpense(groupId = 1L, amountMinorUnits = 6_750L)),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as GroupDetailsUiState.Success
        val budget = state.group.budgetMinorUnits!!
        val remaining = budget - state.totalSpentMinorUnits
        assertEquals(3_250L, remaining)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. Over-budget calculation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun overBudget_spendingExceedsBudget_overAmountIsCorrect() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 10_000L))
        fakeExpenseRepository.setExpenses(
            groupId = 1L,
            expenses = listOf(makeExpense(groupId = 1L, amountMinorUnits = 12_500L)),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as GroupDetailsUiState.Success
        val budget = state.group.budgetMinorUnits!!
        val overAmount = state.totalSpentMinorUnits - budget
        assertEquals(2_500L, overAmount)
        assertTrue(state.totalSpentMinorUnits > budget)
    }

    @Test
    fun overBudget_exactlyAtBudget_isNotOver() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 10_000L))
        fakeExpenseRepository.setExpenses(
            groupId = 1L,
            expenses = listOf(makeExpense(groupId = 1L, amountMinorUnits = 10_000L)),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as GroupDetailsUiState.Success
        val budget = state.group.budgetMinorUnits!!
        assertTrue(state.totalSpentMinorUnits <= budget, "Exactly at budget must not be over budget")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. Progress calculation and capping at 100%
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun progressCalculation_underBudget_correctFraction() = runTest {
        // budget = 10,000, spending = 6,750 → progress = 0.675
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 10_000L))
        fakeExpenseRepository.setExpenses(
            groupId = 1L,
            expenses = listOf(makeExpense(groupId = 1L, amountMinorUnits = 6_750L)),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as GroupDetailsUiState.Success
        val budget = state.group.budgetMinorUnits!!
        val progress = computeProgress(state.totalSpentMinorUnits, budget)
        assertEquals(0.675f, progress, absoluteTolerance = 0.001f)
    }

    @Test
    fun progressCalculation_atBudget_is100Percent() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 10_000L))
        fakeExpenseRepository.setExpenses(
            groupId = 1L,
            expenses = listOf(makeExpense(groupId = 1L, amountMinorUnits = 10_000L)),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as GroupDetailsUiState.Success
        val budget = state.group.budgetMinorUnits!!
        val progress = computeProgress(state.totalSpentMinorUnits, budget)
        assertEquals(1.0f, progress, absoluteTolerance = 0.001f)
    }

    @Test
    fun progressCalculation_overBudget_cappedAt100Percent() = runTest {
        // spending = 150% of budget → progress must cap at 1f
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 10_000L))
        fakeExpenseRepository.setExpenses(
            groupId = 1L,
            expenses = listOf(makeExpense(groupId = 1L, amountMinorUnits = 15_000L)),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as GroupDetailsUiState.Success
        val budget = state.group.budgetMinorUnits!!
        val progress = computeProgress(state.totalSpentMinorUnits, budget)
        assertEquals(1.0f, progress, absoluteTolerance = 0.001f)
        assertTrue(progress <= 1.0f, "Progress must never exceed 1.0f")
        assertTrue(progress >= 0.0f, "Progress must never be below 0.0f")
    }

    @Test
    fun progressCalculation_noSpending_isZero() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 10_000L))
        fakeExpenseRepository.setExpenses(groupId = 1L, expenses = emptyList())

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as GroupDetailsUiState.Success
        val budget = state.group.budgetMinorUnits!!
        val progress = computeProgress(state.totalSpentMinorUnits, budget)
        assertEquals(0.0f, progress, absoluteTolerance = 0.001f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 10. Expense add/edit/delete affecting spent amount
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun addExpense_spentAmountIncreases() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 50_000L))
        fakeExpenseRepository.setExpenses(
            groupId = 1L,
            expenses = listOf(makeExpense(groupId = 1L, amountMinorUnits = 10_000L)),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val stateBefore = vm.uiState.value as GroupDetailsUiState.Success
        assertEquals(10_000L, stateBefore.totalSpentMinorUnits)

        // Simulate adding a new expense
        fakeExpenseRepository.setExpenses(
            groupId = 1L,
            expenses = listOf(
                makeExpense(groupId = 1L, amountMinorUnits = 10_000L),
                makeExpense(groupId = 1L, amountMinorUnits = 5_000L),
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val stateAfter = vm.uiState.value as GroupDetailsUiState.Success
        assertEquals(15_000L, stateAfter.totalSpentMinorUnits)
    }

    @Test
    fun editExpense_spentAmountUpdates() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 50_000L))
        fakeExpenseRepository.setExpenses(
            groupId = 1L,
            expenses = listOf(makeExpense(id = 1L, groupId = 1L, amountMinorUnits = 10_000L)),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate editing the expense amount
        fakeExpenseRepository.setExpenses(
            groupId = 1L,
            expenses = listOf(makeExpense(id = 1L, groupId = 1L, amountMinorUnits = 20_000L)),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as GroupDetailsUiState.Success
        assertEquals(20_000L, state.totalSpentMinorUnits)
    }

    @Test
    fun deleteExpense_spentAmountDecreases() = runTest {
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 50_000L))
        fakeExpenseRepository.setExpenses(
            groupId = 1L,
            expenses = listOf(
                makeExpense(id = 1L, groupId = 1L, amountMinorUnits = 10_000L),
                makeExpense(id = 2L, groupId = 1L, amountMinorUnits = 5_000L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val stateBefore = vm.uiState.value as GroupDetailsUiState.Success
        assertEquals(15_000L, stateBefore.totalSpentMinorUnits)

        // Simulate deleting the first expense
        fakeExpenseRepository.setExpenses(
            groupId = 1L,
            expenses = listOf(makeExpense(id = 2L, groupId = 1L, amountMinorUnits = 5_000L)),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val stateAfter = vm.uiState.value as GroupDetailsUiState.Success
        assertEquals(5_000L, stateAfter.totalSpentMinorUnits)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 11. Settlement payments NOT affecting spent amount
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun settlementPayments_notIncludedInSpending() = runTest {
        // Only expenses are included; the fake expense repo has no settlement method.
        // The ViewModel calculates spending from getExpensesForGroup only.
        fakeGroupRepository.setGroup(makeGroup(id = 1L, budgetMinorUnits = 20_000L))
        fakeExpenseRepository.setExpenses(
            groupId = 1L,
            expenses = listOf(makeExpense(groupId = 1L, amountMinorUnits = 8_000L)),
        )
        // Settlement payments are in a separate repository and are NOT injected into
        // GroupDetailsViewModel — the spending total is derived solely from expenses.

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as GroupDetailsUiState.Success
        // Spent = 8,000 (only from expenses), not 8,000 + any settlement amount
        assertEquals(8_000L, state.totalSpentMinorUnits)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 12. SetGroupBudgetUseCase — unit tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun setGroupBudgetUseCase_validBudget_persistedCorrectly() = runTest {
        val repo = FakeBudgetGroupRepository()
        val group = makeGroup(id = 1L, budgetMinorUnits = null)
        repo.setGroup(group)

        val useCase = SetGroupBudgetUseCase(repo)
        val updated = useCase(group, 10_000L)

        assertEquals(10_000L, updated.budgetMinorUnits)
        assertEquals(10_000L, repo.getGroup(1L)?.budgetMinorUnits)
    }

    @Test
    fun setGroupBudgetUseCase_zeroBudget_throws() = runTest {
        val repo = FakeBudgetGroupRepository()
        val group = makeGroup(id = 1L)
        repo.setGroup(group)

        val useCase = SetGroupBudgetUseCase(repo)
        try {
            useCase(group, 0L)
            assertTrue(false, "Expected IllegalArgumentException for zero budget")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("greater than zero") == true)
        }
    }

    @Test
    fun setGroupBudgetUseCase_negativeBudget_throws() = runTest {
        val repo = FakeBudgetGroupRepository()
        val group = makeGroup(id = 1L)
        repo.setGroup(group)

        val useCase = SetGroupBudgetUseCase(repo)
        try {
            useCase(group, -1L)
            assertTrue(false, "Expected IllegalArgumentException for negative budget")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("greater than zero") == true)
        }
    }

    @Test
    fun setGroupBudgetUseCase_nullBudget_clearsBudget() = runTest {
        val repo = FakeBudgetGroupRepository()
        val group = makeGroup(id = 1L, budgetMinorUnits = 500_00L)
        repo.setGroup(group)

        val useCase = SetGroupBudgetUseCase(repo)
        val updated = useCase(group, null)

        assertNull(updated.budgetMinorUnits)
        assertNull(repo.getGroup(1L)?.budgetMinorUnits)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: progress calculation (mirrors GroupDetailsScreen logic)
    // ─────────────────────────────────────────────────────────────────────────

    private fun computeProgress(totalSpentMinorUnits: Long, budget: Long): Float {
        val spentCapped = totalSpentMinorUnits.coerceAtMost(budget)
        return if (budget > 0L) {
            (spentCapped.toDouble() / budget.toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun makeGroup(
    id: Long = 1L,
    name: String = "Test Group",
    description: String = "",
    currencyCode: String = "INR",
    memberCount: Int = 0,
    totalAmountMinorUnits: Long = 0L,
    budgetMinorUnits: Long? = null,
) = Group(
    id = id,
    name = name,
    description = description,
    currencyCode = currencyCode,
    memberCount = memberCount,
    totalAmountMinorUnits = totalAmountMinorUnits,
    budgetMinorUnits = budgetMinorUnits,
)

private fun makeExpense(
    id: Long = 0L,
    groupId: Long = 1L,
    amountMinorUnits: Long = 1_000L,
) = Expense(
    id = id,
    groupId = groupId,
    description = "Test expense",
    amountMinorUnits = amountMinorUnits,
    currencyCode = "INR",
    paidByMemberId = 1L,
    category = ExpenseCategory.OTHER,
    date = System.currentTimeMillis(),
    splitMethod = SplitMethod.EQUAL,
)

// ─────────────────────────────────────────────────────────────────────────────
// Fake repositories
// ─────────────────────────────────────────────────────────────────────────────

private class FakeBudgetGroupRepository : GroupRepository {

    private val groups: MutableMap<Long, Group> = mutableMapOf()
    private val groupFlow = MutableStateFlow<Map<Long, Group>>(emptyMap())

    fun setGroup(group: Group) {
        groups[group.id] = group
        groupFlow.value = groups.toMap()
    }

    fun getGroup(id: Long): Group? = groups[id]

    override fun getGroups(): Flow<List<Group>> = flowOf(groups.values.toList())

    override fun observeGroupById(id: Long): Flow<Group?> =
        groupFlow.map { it[id] }

    override suspend fun getGroupById(id: Long): Group? = groups[id]

    override suspend fun createGroup(group: Group): Long {
        val newId = (groups.keys.maxOrNull() ?: 0L) + 1L
        val saved = group.copy(id = newId)
        groups[newId] = saved
        groupFlow.value = groups.toMap()
        return newId
    }

    override suspend fun updateGroup(group: Group) {
        groups[group.id] = group
        groupFlow.value = groups.toMap()
    }

    override suspend fun deleteGroup(group: Group) {
        groups.remove(group.id)
        groupFlow.value = groups.toMap()
    }
}

private class FakeBudgetExpenseRepository : ExpenseRepository {

    private val expensesPerGroup: MutableMap<Long, List<Expense>> = mutableMapOf()
    private val expenseFlows: MutableMap<Long, MutableStateFlow<List<Expense>>> = mutableMapOf()

    fun setExpenses(groupId: Long, expenses: List<Expense>) {
        expensesPerGroup[groupId] = expenses
        getOrCreateFlow(groupId).update { expenses }
    }

    private fun getOrCreateFlow(groupId: Long): MutableStateFlow<List<Expense>> =
        expenseFlows.getOrPut(groupId) {
            MutableStateFlow(expensesPerGroup[groupId] ?: emptyList())
        }

    override fun getExpensesForGroup(groupId: Long): Flow<List<Expense>> =
        getOrCreateFlow(groupId)

    override suspend fun getExpenseById(id: Long): Expense? =
        expensesPerGroup.values.flatten().find { it.id == id }

    override fun getSplitsForExpense(expenseId: Long): Flow<List<ExpenseSplit>> = flowOf(emptyList())

    override fun getSplitsForGroup(groupId: Long): Flow<List<ExpenseSplit>> = flowOf(emptyList())

    override suspend fun addExpense(expense: Expense, splits: List<ExpenseSplit>): Long {
        val list = expensesPerGroup[expense.groupId]?.toMutableList() ?: mutableListOf()
        val newId = (list.maxOfOrNull { it.id } ?: 0L) + 1L
        val saved = expense.copy(id = newId)
        list.add(saved)
        setExpenses(expense.groupId, list)
        return newId
    }

    override suspend fun updateExpense(expense: Expense, splits: List<ExpenseSplit>) {
        val list = expensesPerGroup[expense.groupId]?.toMutableList() ?: mutableListOf()
        val idx = list.indexOfFirst { it.id == expense.id }
        if (idx >= 0) list[idx] = expense else list.add(expense)
        setExpenses(expense.groupId, list)
    }

    override suspend fun deleteExpense(expense: Expense) {
        val list = expensesPerGroup[expense.groupId]?.filter { it.id != expense.id } ?: emptyList()
        setExpenses(expense.groupId, list)
    }
}

