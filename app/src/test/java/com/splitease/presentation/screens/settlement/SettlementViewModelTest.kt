package com.splitease.presentation.screens.settlement

import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseCategory
import com.splitease.domain.model.ExpenseSplit
import com.splitease.domain.model.Group
import com.splitease.domain.model.Member
import com.splitease.domain.model.SettlementPayment
import com.splitease.domain.model.SplitMethod
import com.splitease.domain.repository.ExpenseRepository
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.repository.MemberRepository
import com.splitease.domain.repository.SettlementPaymentRepository
import com.splitease.domain.usecase.CalculateMemberBalancesUseCase
import com.splitease.domain.usecase.GetSettlementsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class SettlementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeGroupRepo: FakeGroupRepository
    private lateinit var fakeMemberRepo: FakeMemberRepository
    private lateinit var fakeExpenseRepo: FakeExpenseRepository
    private lateinit var fakePaymentRepo: FakeSettlementPaymentRepository
    private lateinit var viewModel: SettlementViewModel

    private val groupId = 1L

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGroupRepo = FakeGroupRepository()
        fakeMemberRepo = FakeMemberRepository()
        fakeExpenseRepo = FakeExpenseRepository()
        fakePaymentRepo = FakeSettlementPaymentRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): SettlementViewModel = SettlementViewModel(
        groupId = groupId,
        groupRepository = fakeGroupRepo,
        memberRepository = fakeMemberRepo,
        expenseRepository = fakeExpenseRepo,
        settlementPaymentRepository = fakePaymentRepo,
        getSettlements = GetSettlementsUseCase(CalculateMemberBalancesUseCase()),
    )

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun expense(
        id: Long,
        amount: Long,
        paidBy: Long,
    ) = Expense(
        id = id,
        groupId = groupId,
        description = "Test",
        amountMinorUnits = amount,
        currencyCode = "INR",
        paidByMemberId = paidBy,
        category = ExpenseCategory.OTHER,
        date = 1_700_000_000_000L,
        splitMethod = SplitMethod.EQUAL,
    )

    private fun split(expenseId: Long, memberId: Long, share: Long) =
        ExpenseSplit(expenseId = expenseId, memberId = memberId, shareMinorUnits = share)

    private fun member(id: Long, name: String) =
        Member(id = id, groupId = groupId, name = name)

    private fun payment(debtor: Long, creditor: Long, amount: Long, id: Long = 0L) =
        SettlementPayment(
            id = id,
            groupId = groupId,
            debtorMemberId = debtor,
            creditorMemberId = creditor,
            amountMinorUnits = amount,
            paidAt = 1_700_000_000_000L,
        )

    /** Set up a simple 1-debtor-1-creditor scenario: Bob owes Alice 50_000. */
    private fun setupAliceBobScenario() {
        fakeGroupRepo.group = Group(id = groupId, name = "Trip", currencyCode = "INR")
        fakeMemberRepo.members = listOf(member(1L, "Alice"), member(2L, "Bob"))
        fakeExpenseRepo.expenses = listOf(expense(id = 1L, amount = 100_000L, paidBy = 1L))
        fakeExpenseRepo.splits = listOf(
            split(expenseId = 1L, memberId = 1L, share = 50_000L),
            split(expenseId = 1L, memberId = 2L, share = 50_000L),
        )
    }

    // ── Existing tests (preserved) ─────────────────────────────────────────────

    @Test
    fun groupNotFound_setsGroupNotFoundState() = runTest {
        fakeGroupRepo.group = null
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.groupNotFound)
    }

    @Test
    fun noExpenses_isEmpty() = runTest {
        fakeGroupRepo.group = Group(id = groupId, name = "Trip", currencyCode = "INR")
        fakeMemberRepo.members = listOf(member(1L, "Alice"), member(2L, "Bob"))
        fakeExpenseRepo.expenses = emptyList()
        fakeExpenseRepo.splits = emptyList()

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty, "Expected isEmpty when there are no expenses")
        assertFalse(state.isAllSettled)
        assertEquals(0, state.settlementCount)
    }

    @Test
    fun allBalancesZero_isAllSettled() = runTest {
        fakeGroupRepo.group = Group(id = groupId, name = "Trip", currencyCode = "INR")
        fakeMemberRepo.members = listOf(member(1L, "Alice"), member(2L, "Bob"))
        fakeExpenseRepo.expenses = listOf(
            expense(id = 1L, amount = 50000L, paidBy = 1L),
            expense(id = 2L, amount = 50000L, paidBy = 2L),
        )
        fakeExpenseRepo.splits = listOf(
            split(expenseId = 1L, memberId = 1L, share = 25000L),
            split(expenseId = 1L, memberId = 2L, share = 25000L),
            split(expenseId = 2L, memberId = 1L, share = 25000L),
            split(expenseId = 2L, memberId = 2L, share = 25000L),
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertTrue(state.isAllSettled, "Expected isAllSettled when all balances are zero")
        assertEquals(0, state.settlementCount)
    }

    @Test
    fun simpleDebtorCreditor_producesOneSettlement() = runTest {
        setupAliceBobScenario()

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertFalse(state.isAllSettled)
        assertEquals(1, state.settlementCount)

        val txn = state.settlements.single()
        assertEquals("Bob", txn.fromMemberName, "Debtor should be Bob")
        assertEquals("Alice", txn.toMemberName, "Creditor should be Alice")
        assertEquals(50000L, txn.amountMinorUnits)
    }

    @Test
    fun multipleDebtorsAndCreditors_settlementsMinimized() = runTest {
        fakeGroupRepo.group = Group(id = groupId, name = "Trip", currencyCode = "INR")
        fakeMemberRepo.members = listOf(
            member(1L, "Alice"),
            member(2L, "Bob"),
            member(3L, "Charlie"),
        )
        fakeExpenseRepo.expenses = listOf(expense(id = 1L, amount = 3000L, paidBy = 3L))
        fakeExpenseRepo.splits = listOf(
            split(expenseId = 1L, memberId = 1L, share = 1000L),
            split(expenseId = 1L, memberId = 2L, share = 1000L),
            split(expenseId = 1L, memberId = 3L, share = 1000L),
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.settlementCount, "Expected 2 settlements for 2 debtors vs 1 creditor")

        val first = state.settlements[0]
        assertEquals("Alice", first.fromMemberName)
        assertEquals("Charlie", first.toMemberName)
        assertEquals(1000L, first.amountMinorUnits)

        val second = state.settlements[1]
        assertEquals("Bob", second.fromMemberName)
        assertEquals("Charlie", second.toMemberName)
        assertEquals(1000L, second.amountMinorUnits)
    }

    @Test
    fun zeroBalanceMembersIgnored_noExtraSettlements() = runTest {
        fakeGroupRepo.group = Group(id = groupId, name = "Trip", currencyCode = "INR")
        fakeMemberRepo.members = listOf(
            member(1L, "Alice"),
            member(2L, "Bob"),
            member(3L, "Charlie"),
            member(4L, "Dave"),
        )
        fakeExpenseRepo.expenses = listOf(expense(id = 1L, amount = 2000L, paidBy = 4L))
        fakeExpenseRepo.splits = listOf(
            split(expenseId = 1L, memberId = 3L, share = 1000L),
            split(expenseId = 1L, memberId = 4L, share = 1000L),
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.settlementCount)
        val txn = state.settlements.single()
        assertEquals("Charlie", txn.fromMemberName)
        assertEquals("Dave", txn.toMemberName)
        assertEquals(1000L, txn.amountMinorUnits)
    }

    @Test
    fun deterministicOrdering_stableAcrossInvocations() = runTest {
        fakeGroupRepo.group = Group(id = groupId, name = "Trip", currencyCode = "INR")
        fakeMemberRepo.members = listOf(
            member(1L, "Alice"),
            member(2L, "Bob"),
            member(3L, "Charlie"),
        )
        fakeExpenseRepo.expenses = listOf(expense(id = 1L, amount = 3000L, paidBy = 3L))
        fakeExpenseRepo.splits = listOf(
            split(expenseId = 1L, memberId = 1L, share = 1000L),
            split(expenseId = 1L, memberId = 2L, share = 1000L),
            split(expenseId = 1L, memberId = 3L, share = 1000L),
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val firstRun = viewModel.uiState.value.settlements.map { "${it.fromMemberId}-${it.toMemberId}-${it.amountMinorUnits}" }

        val vm2 = SettlementViewModel(
            groupId = groupId,
            groupRepository = fakeGroupRepo,
            memberRepository = fakeMemberRepo,
            expenseRepository = fakeExpenseRepo,
            settlementPaymentRepository = fakePaymentRepo,
            getSettlements = GetSettlementsUseCase(CalculateMemberBalancesUseCase()),
        )
        testDispatcher.scheduler.advanceUntilIdle()
        val secondRun = vm2.uiState.value.settlements.map { "${it.fromMemberId}-${it.toMemberId}-${it.amountMinorUnits}" }

        assertEquals(firstRun, secondRun, "Settlement order must be deterministic")
    }

    @Test
    fun exactLongMinorUnitAmounts_preserved() = runTest {
        fakeGroupRepo.group = Group(id = groupId, name = "Trip", currencyCode = "INR")
        fakeMemberRepo.members = listOf(member(1L, "Alice"), member(2L, "Bob"))
        fakeExpenseRepo.expenses = listOf(expense(id = 1L, amount = 100_001L, paidBy = 1L))
        fakeExpenseRepo.splits = listOf(
            split(expenseId = 1L, memberId = 1L, share = 50_001L),
            split(expenseId = 1L, memberId = 2L, share = 50_000L),
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.settlementCount)
        assertEquals(50_000L, state.settlements.single().amountMinorUnits)
    }

    @Test
    fun longMemberNames_surfaceCorrectlyInSettlements() = runTest {
        val longName1 = "Thiruvenkatam Subramaniam Raghunathan Krishnamurthy"
        val longName2 = "Venkataraman Narayanaswamy Parthasarathy"
        fakeGroupRepo.group = Group(id = groupId, name = "Trip", currencyCode = "INR")
        fakeMemberRepo.members = listOf(member(1L, longName1), member(2L, longName2))
        fakeExpenseRepo.expenses = listOf(expense(id = 1L, amount = 200L, paidBy = 2L))
        fakeExpenseRepo.splits = listOf(
            split(expenseId = 1L, memberId = 1L, share = 100L),
            split(expenseId = 1L, memberId = 2L, share = 100L),
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        val txn = state.settlements.single()
        assertEquals(longName1, txn.fromMemberName)
        assertEquals(longName2, txn.toMemberName)
        assertEquals(100L, txn.amountMinorUnits)
    }

    @Test
    fun invalidGroupId_setsGroupNotFound() = runTest {
        val vm = SettlementViewModel(
            groupId = 0L,
            groupRepository = fakeGroupRepo,
            memberRepository = fakeMemberRepo,
            expenseRepository = fakeExpenseRepo,
            settlementPaymentRepository = fakePaymentRepo,
            getSettlements = GetSettlementsUseCase(CalculateMemberBalancesUseCase()),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.groupNotFound)
    }

    @Test
    fun transactionCountMinimized_oneCreditorMultipleDebtors() = runTest {
        fakeGroupRepo.group = Group(id = groupId, name = "Trip", currencyCode = "INR")
        fakeMemberRepo.members = (1L..5L).map { member(it, "M$it") }
        fakeExpenseRepo.expenses = listOf(expense(id = 1L, amount = 5000L, paidBy = 5L))
        fakeExpenseRepo.splits = (1L..5L).map { split(expenseId = 1L, memberId = it, share = 1000L) }

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(4, state.settlementCount, "Transaction count should equal number of debtors (4)")
        assertTrue(state.settlements.all { it.toMemberId == 5L })
        assertTrue(state.settlements.all { it.amountMinorUnits == 1000L })
    }

    // ── New payment tests ──────────────────────────────────────────────────────

    @Test
    fun fullPaymentRecorded_removesOutstandingSettlement() = runTest {
        setupAliceBobScenario()
        // Pre-load a full payment into the fake repo
        fakePaymentRepo.payments = mutableListOf(payment(debtor = 2L, creditor = 1L, amount = 50_000L))

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isAllSettled, "Full payment should leave nothing outstanding")
        assertEquals(0, state.settlementCount)
        assertEquals(1, state.recentPayments.size)
    }

    @Test
    fun partialPayment_reducesOutstandingAmount() = runTest {
        setupAliceBobScenario()
        fakePaymentRepo.payments = mutableListOf(payment(debtor = 2L, creditor = 1L, amount = 20_000L))

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.settlementCount, "One settlement should remain")
        assertEquals(30_000L, state.settlements.single().amountMinorUnits,
            "Outstanding amount should be reduced by 20_000")
    }

    @Test
    fun multiplePaymentsSameObligation_cumulativeReduction() = runTest {
        setupAliceBobScenario()
        fakePaymentRepo.payments = mutableListOf(
            payment(debtor = 2L, creditor = 1L, amount = 15_000L),
            payment(debtor = 2L, creditor = 1L, amount = 10_000L),
        )

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.settlementCount)
        assertEquals(25_000L, state.settlements.single().amountMinorUnits)
    }

    @Test
    fun onMarkAsPaid_opensDialogWithFullAmount() = runTest {
        setupAliceBobScenario()
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val settlement = viewModel.uiState.value.settlements.single()
        viewModel.onMarkAsPaidClick(settlement)

        val dialog = viewModel.uiState.value.pendingPaymentDialog
        assertNotNull(dialog)
        assertEquals(50_000L, dialog.amountMinorUnits)
        assertNull(dialog.amountError)
    }

    @Test
    fun paymentAmountChange_zeroAmountSetsError() = runTest {
        setupAliceBobScenario()
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val settlement = viewModel.uiState.value.settlements.single()
        viewModel.onMarkAsPaidClick(settlement)
        viewModel.onPaymentAmountChanged(0L)

        val dialog = viewModel.uiState.value.pendingPaymentDialog
        assertNotNull(dialog?.amountError)
    }

    @Test
    fun paymentAmountChange_exceedsOutstandingSetsError() = runTest {
        setupAliceBobScenario()
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val settlement = viewModel.uiState.value.settlements.single()
        viewModel.onMarkAsPaidClick(settlement)
        viewModel.onPaymentAmountChanged(100_000L)  // > 50_000 outstanding

        val dialog = viewModel.uiState.value.pendingPaymentDialog
        assertNotNull(dialog?.amountError)
    }

    @Test
    fun confirmPayment_recordsAndSetsSnackbar() = runTest {
        setupAliceBobScenario()
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val settlement = viewModel.uiState.value.settlements.single()
        viewModel.onMarkAsPaidClick(settlement)
        viewModel.onConfirmPayment()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.pendingPaymentDialog, "Dialog should be dismissed after confirm")
        assertEquals(1, fakePaymentRepo.payments.size, "Payment should have been recorded")
        assertNotNull(state.snackbarMessage)
        assertNotNull(state.lastRecordedPayment)
    }

    @Test
    fun undoLastPayment_reversesPaymentAndClearsLastRecorded() = runTest {
        setupAliceBobScenario()
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Record a payment
        val settlement = viewModel.uiState.value.settlements.single()
        viewModel.onMarkAsPaidClick(settlement)
        viewModel.onConfirmPayment()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakePaymentRepo.payments.size)
        assertNotNull(viewModel.uiState.value.lastRecordedPayment)

        // Undo
        viewModel.onUndoLastPayment()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, fakePaymentRepo.payments.size, "Undo should delete the payment")
        assertNull(viewModel.uiState.value.lastRecordedPayment)
        assertNotNull(viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun reversePayment_removesFromRepo() = runTest {
        setupAliceBobScenario()
        val existingPayment = payment(debtor = 2L, creditor = 1L, amount = 10_000L, id = 99L)
        fakePaymentRepo.payments = mutableListOf(existingPayment)

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onReversePayment(existingPayment)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakePaymentRepo.payments.isEmpty(), "Reversed payment should be deleted")
    }

    @Test
    fun reactiveUpdate_paymentAddedMidStream_updatesSettlements() = runTest {
        setupAliceBobScenario()
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify initial state
        assertEquals(1, viewModel.uiState.value.settlementCount)

        // Reactively add a full payment via the flow
        fakePaymentRepo.paymentsFlow.value = listOf(
            payment(debtor = 2L, creditor = 1L, amount = 50_000L)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAllSettled, "After reactive payment, should be all settled")
    }

    @Test
    fun unrelatedPayment_doesNotAffectDifferentObligation() = runTest {
        fakeGroupRepo.group = Group(id = groupId, name = "Trip", currencyCode = "INR")
        fakeMemberRepo.members = listOf(
            member(1L, "Alice"),
            member(2L, "Bob"),
            member(3L, "Charlie"),
        )
        fakeExpenseRepo.expenses = listOf(expense(id = 1L, amount = 3000L, paidBy = 3L))
        fakeExpenseRepo.splits = listOf(
            split(expenseId = 1L, memberId = 1L, share = 1000L),
            split(expenseId = 1L, memberId = 2L, share = 1000L),
            split(expenseId = 1L, memberId = 3L, share = 1000L),
        )
        // Pay Alice→Charlie; Bob→Charlie should be unaffected
        fakePaymentRepo.payments = mutableListOf(payment(debtor = 1L, creditor = 3L, amount = 1000L))

        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.settlementCount, "Bob still owes Charlie")
        val remaining = state.settlements.single()
        assertEquals(2L, remaining.fromMemberId, "Should be Bob")
        assertEquals(3L, remaining.toMemberId, "Should be Charlie")
        assertEquals(1000L, remaining.amountMinorUnits)
    }
}

// ── Fake repositories ─────────────────────────────────────────────────────────

private class FakeGroupRepository : GroupRepository {
    var group: Group? = Group(id = 1L, name = "Test Group", currencyCode = "INR")

    override fun getGroups(): Flow<List<Group>> = flowOf(listOfNotNull(group))
    override fun observeGroupById(id: Long): Flow<Group?> = flowOf(group)
    override suspend fun getGroupById(id: Long): Group? = group
    override suspend fun createGroup(group: Group): Long = group.id
    override suspend fun updateGroup(group: Group) = Unit
    override suspend fun deleteGroup(group: Group) = Unit
}

private class FakeMemberRepository : MemberRepository {
    var members: List<Member> = emptyList()

    override fun getMembersForGroup(groupId: Long): Flow<List<Member>> = flowOf(members)
    override suspend fun getMemberById(id: Long): Member? = members.find { it.id == id }
    override suspend fun addMember(member: Member): Long = member.id
    override suspend fun updateMember(member: Member) = Unit
    override suspend fun deleteMember(member: Member) = Unit
}

private class FakeExpenseRepository : ExpenseRepository {
    var expenses: List<Expense> = emptyList()
    var splits: List<ExpenseSplit> = emptyList()

    private val expensesFlow = MutableStateFlow(expenses)
    private val splitsFlow = MutableStateFlow(splits)

    override fun getExpensesForGroup(groupId: Long): Flow<List<Expense>> {
        expensesFlow.value = expenses
        return expensesFlow
    }

    override fun getSplitsForGroup(groupId: Long): Flow<List<ExpenseSplit>> {
        splitsFlow.value = splits
        return splitsFlow
    }

    override fun getSplitsForExpense(expenseId: Long): Flow<List<ExpenseSplit>> =
        flowOf(splits.filter { it.expenseId == expenseId })

    override suspend fun getExpenseById(id: Long): Expense? = expenses.find { it.id == id }
    override suspend fun addExpense(expense: Expense, splits: List<ExpenseSplit>): Long = expense.id
    override suspend fun updateExpense(expense: Expense, splits: List<ExpenseSplit>) = Unit
    override suspend fun deleteExpense(expense: Expense) = Unit
}

private class FakeSettlementPaymentRepository : SettlementPaymentRepository {
    var payments: MutableList<SettlementPayment> = mutableListOf()
    val paymentsFlow = MutableStateFlow<List<SettlementPayment>>(emptyList())
    private var nextId = 1L

    override fun getPaymentsForGroup(groupId: Long): Flow<List<SettlementPayment>> {
        paymentsFlow.value = payments
        return paymentsFlow
    }

    override suspend fun recordPayment(payment: SettlementPayment): Long {
        val id = nextId++
        val saved = payment.copy(id = id)
        payments.add(saved)
        paymentsFlow.value = payments.toList()
        return id
    }

    override suspend fun deletePayment(paymentId: Long) {
        payments.removeAll { it.id == paymentId }
        paymentsFlow.value = payments.toList()
    }
}
