package com.splitease.presentation.screens.balances

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BalancesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeGroupRepo: FakeGroupRepo
    private lateinit var fakeMemberRepo: FakeMemberRepo
    private lateinit var fakeExpenseRepo: FakeExpRepo
    private lateinit var fakePaymentRepo: FakeSettlementPaymentRepo

    private val alice = Member(id = 1L, groupId = 1L, name = "Alice")
    private val bob = Member(id = 2L, groupId = 1L, name = "Bob")
    private val group = Group(id = 1L, name = "Trip", currencyCode = "INR")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGroupRepo = FakeGroupRepo(group)
        fakeMemberRepo = FakeMemberRepo(listOf(alice, bob))
        fakeExpenseRepo = FakeExpRepo()
        fakePaymentRepo = FakeSettlementPaymentRepo()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(groupId: Long = 1L) = BalancesViewModel(
        groupId = groupId,
        groupRepository = fakeGroupRepo,
        memberRepository = fakeMemberRepo,
        expenseRepository = fakeExpenseRepo,
        settlementPaymentRepository = fakePaymentRepo,
        calculateBalances = CalculateMemberBalancesUseCase(),
    )

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialState_isLoading() {
        val vm = buildViewModel()
        assertTrue(vm.uiState.value.isLoading)
    }

    // ── Invalid groupId ───────────────────────────────────────────────────────

    @Test
    fun zeroGroupId_setsGroupNotFound() {
        val vm = buildViewModel(groupId = 0L)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.groupNotFound)
    }

    @Test
    fun negativeGroupId_setsGroupNotFound() {
        val vm = buildViewModel(groupId = -5L)
        assertTrue(vm.uiState.value.groupNotFound)
    }

    // ── Group not found ───────────────────────────────────────────────────────

    @Test
    fun unknownGroupId_setsGroupNotFound() = runTest {
        val vm = buildViewModel(groupId = 99L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.groupNotFound)
        assertFalse(vm.uiState.value.isLoading)
    }

    // ── Empty (no expenses) ───────────────────────────────────────────────────

    @Test
    fun noExpenses_stateIsEmpty() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.groupNotFound)
        assertTrue(state.isEmpty, "Expected isEmpty=true when there are no expenses")
    }

    @Test
    fun noExpenses_balancesHaveZeroAmounts() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        // Members with no expenses should all have zero paid and owed
        assertTrue(state.balances.all { it.totalPaidMinorUnits == 0L })
        assertTrue(state.balances.all { it.totalOwedMinorUnits == 0L })
        assertTrue(state.balances.all { it.netMinorUnits == 0L })
    }

    // ── Currency propagation ──────────────────────────────────────────────────

    @Test
    fun currencyCode_isFromGroup() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("INR", vm.uiState.value.currencyCode)
    }

    @Test
    fun groupName_isFromGroup() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Trip", vm.uiState.value.groupName)
    }

    // ── Positive balance ──────────────────────────────────────────────────────

    @Test
    fun alicePaidForBoth_aliceHasPositiveBalance() = runTest {
        // Alice pays ₹9.00; both split equally (₹4.50 each = 450 minor units each)
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 900L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 450L),
                split(expenseId = 1L, memberId = 2L, share = 450L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isEmpty)
        val aliceBalance = state.balances.first { it.memberId == 1L }
        assertEquals(900L, aliceBalance.totalPaidMinorUnits)
        assertEquals(450L, aliceBalance.totalOwedMinorUnits)
        assertEquals(450L, aliceBalance.netMinorUnits, "Alice should be owed 450")
        assertTrue(aliceBalance.netMinorUnits > 0L)
    }

    // ── Negative balance ──────────────────────────────────────────────────────

    @Test
    fun alicePaidForBoth_bobHasNegativeBalance() = runTest {
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 900L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 450L),
                split(expenseId = 1L, memberId = 2L, share = 450L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val bobBalance = vm.uiState.value.balances.first { it.memberId == 2L }
        assertEquals(0L, bobBalance.totalPaidMinorUnits)
        assertEquals(450L, bobBalance.totalOwedMinorUnits)
        assertEquals(-450L, bobBalance.netMinorUnits, "Bob should owe 450")
        assertTrue(bobBalance.netMinorUnits < 0L)
    }

    // ── Zero balance ──────────────────────────────────────────────────────────

    @Test
    fun memberPaidExactlyTheirShare_hasZeroBalance() = runTest {
        // Alice pays ₹5, split: Alice=500 (her whole share), Bob=0 (not in this expense)
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 500L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 500L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val aliceBalance = vm.uiState.value.balances.first { it.memberId == 1L }
        assertEquals(0L, aliceBalance.netMinorUnits, "Alice paid exactly her share — settled")
    }

    // ── Balances sum to zero ──────────────────────────────────────────────────

    @Test
    fun allBalancesSumToZero() = runTest {
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 900L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 450L),
                split(expenseId = 1L, memberId = 2L, share = 450L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val totalNet = vm.uiState.value.balances.sumOf { it.netMinorUnits }
        assertEquals(0L, totalNet, "All net balances must sum to zero")
    }

    // ── Populated state ───────────────────────────────────────────────────────

    @Test
    fun withExpenses_stateIsNotEmpty() = runTest {
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 1000L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 500L),
                split(expenseId = 1L, memberId = 2L, share = 500L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isEmpty)
        assertEquals(2, vm.uiState.value.balances.size)
    }

    @Test
    fun totalSpend_isCorrect() = runTest {
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 1000L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 500L),
                split(expenseId = 1L, memberId = 2L, share = 500L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1000L, vm.uiState.value.totalSpendMinorUnits)
    }

    // ── Summary counters ──────────────────────────────────────────────────────

    @Test
    fun creditorsCount_correctWhenOnePersonOwesMoney() = runTest {
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 900L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 450L),
                split(expenseId = 1L, memberId = 2L, share = 450L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.creditorsCount)
        assertEquals(1, vm.uiState.value.debtorsCount)
    }

    // ── Repository error ──────────────────────────────────────────────────────

    @Test
    fun groupRepositoryError_surfacesErrorMessage() = runTest {
        fakeGroupRepo.shouldThrow = true
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun expenseRepositoryError_surfacesErrorMessage() = runTest {
        fakeExpenseRepo.shouldThrowOnGetExpenses = true
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun onErrorDismissed_clearsErrorMessage() = runTest {
        fakeGroupRepo.shouldThrow = true
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onErrorDismissed()
        assertNull(vm.uiState.value.errorMessage)
    }

    // ── Reactive updates ──────────────────────────────────────────────────────

    @Test
    fun reactiveUpdate_newExpenseUpdatesBalances() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.isEmpty)

        // Emit a new expense into the flow
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 600L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 300L),
                split(expenseId = 1L, memberId = 2L, share = 300L),
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isEmpty)
        assertEquals(600L, vm.uiState.value.totalSpendMinorUnits)
    }
    // ── Settlement-payment adjustment tests ───────────────────────────────────

    /**
     * TC-1  No settlement payments → adjusted balances equal raw expense balances.
     */
    @Test
    fun settlementAdjustment_noPayments_adjustedEqualsRaw() = runTest {
        // Alice paid ₹6 (60000 paise); both share equally → Alice net +30000, Bob net -30000
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 60_000L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 30_000L),
                split(expenseId = 1L, memberId = 2L, share = 30_000L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        val aliceAdj = state.adjustedBalances.first { it.memberId == 1L }
        val bobAdj   = state.adjustedBalances.first { it.memberId == 2L }

        assertEquals(30_000L,  aliceAdj.adjustedNetMinorUnits, "No payments — Alice's adjusted net equals raw net")
        assertEquals(-30_000L, bobAdj.adjustedNetMinorUnits,   "No payments — Bob's adjusted net equals raw net")
    }

    /**
     * TC-2  Full settlement payment → both members become settled (adjusted net = 0).
     */
    @Test
    fun settlementAdjustment_fullPayment_bothMembersSettled() = runTest {
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 60_000L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 30_000L),
                split(expenseId = 1L, memberId = 2L, share = 30_000L),
            ),
        )
        // Bob pays Alice the full ₹30,000
        fakePaymentRepo.paymentsFlow.value = listOf(
            payment(debtor = 2L, creditor = 1L, amount = 30_000L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(0L, state.adjustedBalances.first { it.memberId == 1L }.adjustedNetMinorUnits,
            "After full payment Alice should be settled")
        assertEquals(0L, state.adjustedBalances.first { it.memberId == 2L }.adjustedNetMinorUnits,
            "After full payment Bob should be settled")
    }

    /**
     * TC-3  Partial settlement payment → outstanding amount decreases correctly.
     */
    @Test
    fun settlementAdjustment_partialPayment_outstandingDecreases() = runTest {
        // Bob owes Alice ₹30,000; Bob pays ₹10,000
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 60_000L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 30_000L),
                split(expenseId = 1L, memberId = 2L, share = 30_000L),
            ),
        )
        fakePaymentRepo.paymentsFlow.value = listOf(
            payment(debtor = 2L, creditor = 1L, amount = 10_000L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        val aliceAdj = state.adjustedBalances.first { it.memberId == 1L }
        val bobAdj   = state.adjustedBalances.first { it.memberId == 2L }

        assertEquals(-20_000L, bobAdj.adjustedNetMinorUnits,   "Bob still owes ₹20,000 after ₹10,000 partial payment")
        assertEquals( 20_000L, aliceAdj.adjustedNetMinorUnits, "Alice owed ₹20,000 after receiving ₹10,000")
    }

    /**
     * TC-4  Multiple settlement payments → adjusted balances accumulate correctly.
     */
    @Test
    fun settlementAdjustment_multiplePayments_accumulateCorrectly() = runTest {
        // Bob owes Alice ₹30,000; two payments: ₹10,000 + ₹10,000 = ₹20,000 total
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 60_000L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 30_000L),
                split(expenseId = 1L, memberId = 2L, share = 30_000L),
            ),
        )
        fakePaymentRepo.paymentsFlow.value = listOf(
            payment(debtor = 2L, creditor = 1L, amount = 10_000L),
            payment(debtor = 2L, creditor = 1L, amount = 10_000L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(-10_000L, state.adjustedBalances.first { it.memberId == 2L }.adjustedNetMinorUnits,
            "Bob owes ₹10,000 after two cumulative ₹10,000 payments")
        assertEquals( 10_000L, state.adjustedBalances.first { it.memberId == 1L }.adjustedNetMinorUnits,
            "Alice owed ₹10,000 after receiving ₹20,000 total across two payments")
    }

    /**
     * TC-5  Net adjusted balances still sum to zero after payments.
     */
    @Test
    fun settlementAdjustment_adjustedBalancesSumToZero() = runTest {
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 60_000L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 30_000L),
                split(expenseId = 1L, memberId = 2L, share = 30_000L),
            ),
        )
        fakePaymentRepo.paymentsFlow.value = listOf(
            payment(debtor = 2L, creditor = 1L, amount = 15_000L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val totalAdjustedNet = vm.uiState.value.adjustedBalances.sumOf { it.adjustedNetMinorUnits }
        assertEquals(0L, totalAdjustedNet, "Adjusted net balances must sum to zero")
    }

    /**
     * TC-6  Paid and Share values are unaffected by settlement payments.
     */
    @Test
    fun settlementAdjustment_paidAndShareUnchangedByPayments() = runTest {
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 60_000L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 30_000L),
                split(expenseId = 1L, memberId = 2L, share = 30_000L),
            ),
        )
        fakePaymentRepo.paymentsFlow.value = listOf(
            payment(debtor = 2L, creditor = 1L, amount = 30_000L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val aliceRaw = vm.uiState.value.balances.first { it.memberId == 1L }
        val bobRaw   = vm.uiState.value.balances.first { it.memberId == 2L }

        // Raw ledger values must not be changed by payments
        assertEquals(60_000L, aliceRaw.totalPaidMinorUnits, "Alice paid amount unchanged")
        assertEquals(30_000L, aliceRaw.totalOwedMinorUnits, "Alice share unchanged")
        assertEquals(     0L, bobRaw.totalPaidMinorUnits,   "Bob paid amount unchanged")
        assertEquals(30_000L, bobRaw.totalOwedMinorUnits,   "Bob share unchanged")
    }

    /**
     * TC-7  Summary-card counters reflect adjusted positions, not raw balances.
     */
    @Test
    fun settlementAdjustment_creditorsAndDebtorsCountUpdatedAfterFullPayment() = runTest {
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 60_000L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 30_000L),
                split(expenseId = 1L, memberId = 2L, share = 30_000L),
            ),
        )
        fakePaymentRepo.paymentsFlow.value = listOf(
            payment(debtor = 2L, creditor = 1L, amount = 30_000L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(0, state.creditorsCount, "After full payment no one is owed money")
        assertEquals(0, state.debtorsCount,   "After full payment no one owes money")
    }

    /**
     * TC-8  Reactive update — adding a payment reactively updates adjusted balances.
     */
    @Test
    fun settlementAdjustment_reactivePayment_updatesAdjustedBalances() = runTest {
        fakeExpenseRepo.seedExpense(
            expense(id = 1L, amount = 60_000L, paidBy = 1L),
            splits = listOf(
                split(expenseId = 1L, memberId = 1L, share = 30_000L),
                split(expenseId = 1L, memberId = 2L, share = 30_000L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Before payment Bob owes Alice
        assertEquals(-30_000L, vm.uiState.value.adjustedBalances.first { it.memberId == 2L }.adjustedNetMinorUnits)

        // Record a payment reactively
        fakePaymentRepo.paymentsFlow.value = listOf(
            payment(debtor = 2L, creditor = 1L, amount = 30_000L),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0L, vm.uiState.value.adjustedBalances.first { it.memberId == 2L }.adjustedNetMinorUnits,
            "Adjusted balance should update reactively after payment")
    }
}

// ── Payment helper ────────────────────────────────────────────────────────────

private fun payment(debtor: Long, creditor: Long, amount: Long, id: Long = 0L) =
    SettlementPayment(
        id = id,
        groupId = 1L,
        debtorMemberId = debtor,
        creditorMemberId = creditor,
        amountMinorUnits = amount,
        paidAt = 1_700_000_000_000L,
    )

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun expense(id: Long, amount: Long, paidBy: Long) = Expense(
    id = id,
    groupId = 1L,
    description = "Test",
    amountMinorUnits = amount,
    currencyCode = "INR",
    paidByMemberId = paidBy,
    category = ExpenseCategory.FOOD,
    date = 1_700_000_000_000L,
    splitMethod = SplitMethod.EQUAL,
)

private fun split(expenseId: Long, memberId: Long, share: Long) = ExpenseSplit(
    expenseId = expenseId,
    memberId = memberId,
    shareMinorUnits = share,
)

// ── Fakes ─────────────────────────────────────────────────────────────────────

private class FakeSettlementPaymentRepo : SettlementPaymentRepository {
    val paymentsFlow = MutableStateFlow<List<SettlementPayment>>(emptyList())

    override fun getPaymentsForGroup(groupId: Long): Flow<List<SettlementPayment>> = paymentsFlow
    override suspend fun recordPayment(payment: SettlementPayment): Long = 0L
    override suspend fun deletePayment(paymentId: Long) = Unit
}


private class FakeGroupRepo(private val group: Group) : GroupRepository {
    var shouldThrow = false

    override fun getGroups(): Flow<List<Group>> = flowOf(listOf(group))
    override fun observeGroupById(id: Long): Flow<Group?> = flowOf(if (id == group.id) group else null)
    override suspend fun getGroupById(id: Long): Group? {
        if (shouldThrow) throw RuntimeException("Group repository error")
        return if (id == group.id) group else null
    }
    override suspend fun createGroup(group: Group): Long = 1L
    override suspend fun updateGroup(group: Group) = Unit
    override suspend fun deleteGroup(group: Group) = Unit
}

private class FakeMemberRepo(private val members: List<Member>) : MemberRepository {
    override fun getMembersForGroup(groupId: Long): Flow<List<Member>> =
        flowOf(members.filter { it.groupId == groupId })
    override suspend fun getMemberById(id: Long): Member? = members.firstOrNull { it.id == id }
    override suspend fun addMember(member: Member): Long = 1L
    override suspend fun updateMember(member: Member) = Unit
    override suspend fun deleteMember(member: Member) = Unit
}

private class FakeExpRepo : ExpenseRepository {
    var shouldThrowOnGetExpenses = false

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    private val _splits = MutableStateFlow<List<ExpenseSplit>>(emptyList())

    fun seedExpense(expense: Expense, splits: List<ExpenseSplit>) {
        _expenses.value = _expenses.value + expense
        _splits.value = _splits.value + splits
    }

    override fun getExpensesForGroup(groupId: Long): Flow<List<Expense>> =
        if (shouldThrowOnGetExpenses) flow { throw RuntimeException("Expense repository error") }
        else _expenses.map { list -> list.filter { it.groupId == groupId } }

    override suspend fun getExpenseById(id: Long): Expense? = _expenses.value.firstOrNull { it.id == id }

    override fun getSplitsForExpense(expenseId: Long): Flow<List<ExpenseSplit>> =
        _splits.map { list -> list.filter { it.expenseId == expenseId } }

    override fun getSplitsForGroup(groupId: Long): Flow<List<ExpenseSplit>> = _splits

    override suspend fun addExpense(expense: Expense, splits: List<ExpenseSplit>): Long = 1L
    override suspend fun updateExpense(expense: Expense, splits: List<ExpenseSplit>) = Unit
    override suspend fun deleteExpense(expense: Expense) = Unit
}
