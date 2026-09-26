package com.splitease.presentation.screens.expenses

import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseCategory
import com.splitease.domain.model.ExpenseSplit
import com.splitease.domain.model.Group
import com.splitease.domain.model.Member
import com.splitease.domain.model.SplitMethod
import com.splitease.domain.repository.ExpenseRepository
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.repository.MemberRepository
import com.splitease.domain.usecase.DeleteExpenseUseCase
import com.splitease.domain.usecase.GetExpensesForGroupUseCase
import com.splitease.domain.usecase.GetExpenseByIdUseCase
import com.splitease.domain.usecase.GetMembersForGroupUseCase
import com.splitease.domain.usecase.GetSplitsForExpenseUseCase
import com.splitease.domain.usecase.SaveExpenseUseCase
import com.splitease.domain.usecase.UpdateExpenseUseCase
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditExpenseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeExpenseRepo: FakeExpRepo
    private lateinit var fakeGroupRepo: FakeGrpRepo
    private lateinit var fakeMemberRepo: FakeMbrRepo

    private val alice = Member(id = 1L, groupId = 1L, name = "Alice")
    private val bob = Member(id = 2L, groupId = 1L, name = "Bob")
    private val group = Group(id = 1L, name = "Trip", currencyCode = "INR")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeExpenseRepo = FakeExpRepo()
        fakeGroupRepo = FakeGrpRepo(group)
        fakeMemberRepo = FakeMbrRepo(listOf(alice, bob))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildAddViewModel(groupId: Long = 1L) = AddEditExpenseViewModel(
        groupId = groupId,
        editExpenseId = 0L,
        groupRepository = fakeGroupRepo,
        memberRepository = fakeMemberRepo,
        saveExpenseUseCase = SaveExpenseUseCase(fakeExpenseRepo),
        updateExpenseUseCase = UpdateExpenseUseCase(fakeExpenseRepo),
        getExpenseByIdUseCase = GetExpenseByIdUseCase(fakeExpenseRepo),
        getSplitsForExpenseUseCase = GetSplitsForExpenseUseCase(fakeExpenseRepo),
    )

    // ── Initial load ──────────────────────────────────────────────────────────

    @Test
    fun initialState_isLoading() {
        val vm = buildAddViewModel()
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun afterLoad_allMembersAreParticipants() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(setOf(1L, 2L), state.selectedParticipantIds)
    }

    @Test
    fun afterLoad_currencyCodeIsFromGroup() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("INR", vm.uiState.value.currencyCode)
    }

    // ── Validation: description ───────────────────────────────────────────────

    @Test
    fun blankDescription_isRejected() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onAmountChange("100")
        vm.onPayerSelected(1L)
        vm.onSave()

        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.descriptionError)
        assertNull(vm.uiState.value.savedExpenseId)
    }

    @Test
    fun descriptionIsTrimmedBeforeSave() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onDescriptionChange("  Lunch  ")
        vm.onAmountChange("100")
        vm.onPayerSelected(1L)
        vm.onSave()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Lunch", fakeExpenseRepo.lastSavedExpense?.description)
    }

    // ── Validation: amount ────────────────────────────────────────────────────

    @Test
    fun zeroAmount_isRejected() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onDescriptionChange("Lunch")
        vm.onAmountChange("0")
        vm.onPayerSelected(1L)
        vm.onSave()

        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.amountError)
    }

    @Test
    fun negativeAmount_isRejected() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onDescriptionChange("Lunch")
        vm.onAmountChange("-50")
        vm.onPayerSelected(1L)
        vm.onSave()

        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.amountError)
    }

    @Test
    fun invalidAmountString_isRejected() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onDescriptionChange("Lunch")
        vm.onAmountChange("abc")
        vm.onPayerSelected(1L)
        vm.onSave()

        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.amountError)
    }

    // ── Validation: payer ─────────────────────────────────────────────────────

    @Test
    fun noPayerSelected_isRejected() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onDescriptionChange("Lunch")
        vm.onAmountChange("100")
        // no payer selected
        vm.onSave()

        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.payerError)
    }

    // ── Validation: participants ──────────────────────────────────────────────

    @Test
    fun noParticipants_isRejected() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Deselect all
        vm.onParticipantToggled(1L)
        vm.onParticipantToggled(2L)
        vm.onDescriptionChange("Lunch")
        vm.onAmountChange("100")
        vm.onPayerSelected(1L)
        vm.onSave()

        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.participantsError)
    }

    // ── Equal split ───────────────────────────────────────────────────────────

    @Test
    fun equalSplit_previewReconcilesExactly() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // "100" → 10000 minor units (₹100.00 = 10000 paise)
        vm.onAmountChange("100")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(SplitMethod.EQUAL, state.splitMethod)
        assertTrue(state.isSplitValid)
        assertEquals(10_000L, state.splitPreview.values.sum())
    }

    @Test
    fun equalSplit_withRemainder_reconcilesExactly() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // "1" → 100 minor units (₹1.00 = 100 paise) split between 2 people → 50 + 50
        vm.onAmountChange("1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isSplitValid)
        assertEquals(100L, state.splitPreview.values.sum())
    }

    // ── Exact split ───────────────────────────────────────────────────────────

    @Test
    fun exactSplit_invalidTotal_notSaveable() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onAmountChange("100")
        vm.onSplitMethodSelected(SplitMethod.EXACT)
        vm.onExactAmountChanged(1L, "40")
        vm.onExactAmountChanged(2L, "40")  // 40+40 = 80 ≠ 100
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isSplitValid)
    }

    @Test
    fun exactSplit_validTotal_isSaveable() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // "100" → 10000 minor units; "60" → 6000; "40" → 4000; 6000+4000 = 10000 ✓
        vm.onAmountChange("100")
        vm.onSplitMethodSelected(SplitMethod.EXACT)
        vm.onExactAmountChanged(1L, "60")
        vm.onExactAmountChanged(2L, "40")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isSplitValid)
        assertEquals(10_000L, vm.uiState.value.splitPreview.values.sum())
    }

    // ── Percentage split ──────────────────────────────────────────────────────

    @Test
    fun percentageSplit_notHundredPercent_notSaveable() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onAmountChange("100")
        vm.onSplitMethodSelected(SplitMethod.PERCENTAGE)
        vm.onPercentageChanged(1L, "40")
        vm.onPercentageChanged(2L, "40")  // 40+40 = 80 ≠ 100
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isSplitValid)
    }

    @Test
    fun percentageSplit_exactlyHundred_reconcilesMonetarily() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onAmountChange("100")  // 10000 minor units
        vm.onSplitMethodSelected(SplitMethod.PERCENTAGE)
        vm.onPercentageChanged(1L, "60")
        vm.onPercentageChanged(2L, "40")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isSplitValid)
        assertEquals(10000L, state.splitPreview.values.sum())
    }

    // ── Shares split ──────────────────────────────────────────────────────────

    @Test
    fun sharesSplit_zeroShares_notSaveable() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onAmountChange("100")
        vm.onSplitMethodSelected(SplitMethod.SHARES)
        vm.onSharesChanged(1L, "0")
        vm.onSharesChanged(2L, "1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isSplitValid)
    }

    @Test
    fun sharesSplit_validShares_reconcilesExactly() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onAmountChange("100")  // 10000 minor units
        vm.onSplitMethodSelected(SplitMethod.SHARES)
        vm.onSharesChanged(1L, "1")
        vm.onSharesChanged(2L, "2")  // 1:2 ratio
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isSplitValid)
        assertEquals(10000L, state.splitPreview.values.sum())
    }

    // ── Duplicate submission prevention ───────────────────────────────────────

    @Test
    fun duplicateSubmit_isPrevented() = runTest {
        fakeExpenseRepo.delay = true
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onDescriptionChange("Lunch")
        vm.onAmountChange("100")
        vm.onPayerSelected(1L)

        vm.onSave()
        vm.onSave()   // second call while first is in flight

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeExpenseRepo.saveCallCount)
    }

    // ── Successful save ───────────────────────────────────────────────────────

    @Test
    fun validExpense_equalSplit_savesSuccessfully() = runTest {
        val vm = buildAddViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onDescriptionChange("Dinner")
        vm.onAmountChange("200")
        vm.onPayerSelected(1L)
        vm.onSave()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSaving)
        assertNotNull(state.savedExpenseId)
        assertEquals("Dinner", fakeExpenseRepo.lastSavedExpense?.description)
        // Split must reconcile exactly
        assertEquals(20000L, fakeExpenseRepo.lastSavedSplits.sumOf { it.shareMinorUnits })
    }
}

// ── Fakes ─────────────────────────────────────────────────────────────────────

private class FakeExpRepo : ExpenseRepository {

    var delay = false
    var saveCallCount = 0
    var lastSavedExpense: Expense? = null
    var lastSavedSplits: List<ExpenseSplit> = emptyList()
    var lastUpdatedExpense: Expense? = null
    var lastUpdatedSplits: List<ExpenseSplit> = emptyList()
    var lastDeleted: Expense? = null
    val expenses = MutableStateFlow<List<Expense>>(emptyList())
    val splits = MutableStateFlow<List<ExpenseSplit>>(emptyList())
    private var nextId = 1L

    override fun getExpensesForGroup(groupId: Long): Flow<List<Expense>> = expenses
    override suspend fun getExpenseById(id: Long): Expense? =
        expenses.value.firstOrNull { it.id == id }
    override fun getSplitsForExpense(expenseId: Long): Flow<List<ExpenseSplit>> =
        splits.map { list -> list.filter { it.expenseId == expenseId } }

    override suspend fun addExpense(expense: Expense, splits: List<ExpenseSplit>): Long {
        saveCallCount++
        if (delay) kotlinx.coroutines.delay(10_000)
        lastSavedExpense = expense
        lastSavedSplits = splits
        return nextId++
    }

    override suspend fun updateExpense(expense: Expense, splits: List<ExpenseSplit>) {
        lastUpdatedExpense = expense
        lastUpdatedSplits = splits
    }

    override suspend fun deleteExpense(expense: Expense) {
        lastDeleted = expense
    }
}

private class FakeGrpRepo(private val group: Group) : GroupRepository {

    override fun getGroups(): Flow<List<Group>> = flowOf(listOf(group))
    override fun observeGroupById(id: Long): Flow<Group?> = flowOf(group)
    override suspend fun getGroupById(id: Long): Group? = if (id == group.id) group else null
    override suspend fun createGroup(group: Group): Long = 1L
    override suspend fun updateGroup(group: Group) = Unit
    override suspend fun deleteGroup(group: Group) = Unit
}

private class FakeMbrRepo(private val members: List<Member>) : MemberRepository {

    override fun getMembersForGroup(groupId: Long): Flow<List<Member>> =
        flowOf(members.filter { it.groupId == groupId })

    override suspend fun getMemberById(id: Long): Member? = members.firstOrNull { it.id == id }
    override suspend fun addMember(member: Member): Long = 1L
    override suspend fun updateMember(member: Member) = Unit
    override suspend fun deleteMember(member: Member) = Unit
}
