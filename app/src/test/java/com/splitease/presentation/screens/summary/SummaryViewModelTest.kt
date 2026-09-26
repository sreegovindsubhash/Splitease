package com.splitease.presentation.screens.summary

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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeGroupRepo: FakeSummaryGroupRepository
    private lateinit var fakeMemberRepo: FakeSummaryMemberRepository
    private lateinit var fakeExpenseRepo: FakeSummaryExpenseRepository
    private lateinit var fakePaymentRepo: FakeSummaryPaymentRepository

    private val groupId = 1L
    private val defaultGroup = Group(id = groupId, name = "Trip", currencyCode = "INR")
    private val alice = Member(id = 1L, groupId = groupId, name = "Alice")
    private val bob = Member(id = 2L, groupId = groupId, name = "Bob")
    private val charlie = Member(id = 3L, groupId = groupId, name = "Charlie")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGroupRepo = FakeSummaryGroupRepository(defaultGroup)
        fakeMemberRepo = FakeSummaryMemberRepository(listOf(alice, bob))
        fakeExpenseRepo = FakeSummaryExpenseRepository()
        fakePaymentRepo = FakeSummaryPaymentRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(groupId: Long = this.groupId) = SummaryViewModel(
        groupId = groupId,
        groupRepository = fakeGroupRepo,
        memberRepository = fakeMemberRepo,
        expenseRepository = fakeExpenseRepo,
        settlementPaymentRepository = fakePaymentRepo,
        calculateBalances = CalculateMemberBalancesUseCase(),
        getSettlements = GetSettlementsUseCase(CalculateMemberBalancesUseCase()),
    )

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun expense(
        id: Long,
        amount: Long,
        paidBy: Long,
        date: Long = 1_700_000_000_000L,
        description: String = "Test expense $id",
    ) = Expense(
        id = id,
        groupId = groupId,
        description = description,
        amountMinorUnits = amount,
        currencyCode = "INR",
        paidByMemberId = paidBy,
        category = ExpenseCategory.OTHER,
        date = date,
        splitMethod = SplitMethod.EQUAL,
    )

    private fun split(expenseId: Long, memberId: Long, share: Long) =
        ExpenseSplit(expenseId = expenseId, memberId = memberId, shareMinorUnits = share)

    private fun payment(
        debtor: Long,
        creditor: Long,
        amount: Long,
        id: Long = 0L,
    ) = SettlementPayment(
        id = id,
        groupId = groupId,
        debtorMemberId = debtor,
        creditorMemberId = creditor,
        amountMinorUnits = amount,
        paidAt = 1_700_000_000_000L,
    )

    private fun seedAlicePaidForBoth(amount: Long = 100_000L) {
        // Alice paid the full amount; both split equally
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = amount, paidBy = alice.id),
            splits = listOf(
                split(expenseId = 1L, memberId = alice.id, share = amount / 2),
                split(expenseId = 1L, memberId = bob.id, share = amount / 2),
            ),
        )
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialState_isLoading() {
        val vm = buildViewModel()
        assertTrue(vm.uiState.value.isLoading)
    }

    // ── Invalid / unknown groupId ─────────────────────────────────────────────

    @Test
    fun zeroGroupId_setsGroupNotFound() {
        val vm = buildViewModel(groupId = 0L)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.groupNotFound)
    }

    @Test
    fun negativeGroupId_setsGroupNotFound() {
        val vm = buildViewModel(groupId = -1L)
        assertTrue(vm.uiState.value.groupNotFound)
    }

    @Test
    fun unknownGroupId_setsGroupNotFound() = runTest {
        val vm = buildViewModel(groupId = 999L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.groupNotFound)
        assertFalse(vm.uiState.value.isLoading)
    }

    // ── Group not found ───────────────────────────────────────────────────────

    @Test
    fun groupRepositoryReturnsNull_setsGroupNotFound() = runTest {
        fakeGroupRepo.overrideGroup = null
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.groupNotFound)
        assertFalse(vm.uiState.value.isLoading)
    }

    // ── Error state ───────────────────────────────────────────────────────────

    @Test
    fun groupRepositoryThrows_setsErrorMessage() = runTest {
        fakeGroupRepo.shouldThrow = true
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun expenseRepositoryThrows_setsErrorMessage() = runTest {
        fakeExpenseRepo.shouldThrowOnExpenses = true
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

    // ── Retry ─────────────────────────────────────────────────────────────────

    @Test
    fun retry_setsLoadingTrue() = runTest {
        fakeGroupRepo.shouldThrow = true
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)

        fakeGroupRepo.shouldThrow = false
        vm.retry()
        assertTrue(vm.uiState.value.isLoading)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    // ── No expenses ───────────────────────────────────────────────────────────

    @Test
    fun noExpenses_isEmptyTrue() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.groupNotFound)
        assertTrue(state.isEmpty, "Expected isEmpty=true when there are no expenses")
        assertFalse(state.hasExpenses)
    }

    @Test
    fun noExpenses_totalSpentIsZero() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0L, vm.uiState.value.totalSpentMinorUnits)
    }

    @Test
    fun noExpenses_outstandingIsZero() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0L, vm.uiState.value.outstandingAmountMinorUnits)
    }

    @Test
    fun noExpenses_debtsToSettleIsZero() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, vm.uiState.value.debtsToSettleCount)
    }

    @Test
    fun noExpenses_recentExpensesEmpty() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.recentExpenses.isEmpty())
    }

    @Test
    fun noExpenses_outstandingSettlementsEmpty() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.outstandingSettlements.isEmpty())
    }

    // ── One expense ───────────────────────────────────────────────────────────

    @Test
    fun oneExpense_hasExpensesTrue() = runTest {
        seedAlicePaidForBoth(100_000L)
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.hasExpenses)
        assertFalse(vm.uiState.value.isEmpty)
    }

    @Test
    fun oneExpense_totalSpentMatchesExpenseAmount() = runTest {
        seedAlicePaidForBoth(100_000L)
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(100_000L, vm.uiState.value.totalSpentMinorUnits)
    }

    @Test
    fun oneExpense_recentExpensesHasOneItem() = runTest {
        seedAlicePaidForBoth(100_000L)
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.recentExpenses.size)
    }

    // ── Multiple expenses ─────────────────────────────────────────────────────

    @Test
    fun multipleExpenses_totalSpentSumsAllAmounts() = runTest {
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = 60_000L, paidBy = alice.id),
            splits = listOf(
                split(expenseId = 1L, memberId = alice.id, share = 30_000L),
                split(expenseId = 1L, memberId = bob.id, share = 30_000L),
            ),
        )
        fakeExpenseRepo.seed(
            expense(id = 2L, amount = 40_000L, paidBy = bob.id),
            splits = listOf(
                split(expenseId = 2L, memberId = alice.id, share = 20_000L),
                split(expenseId = 2L, memberId = bob.id, share = 20_000L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(100_000L, vm.uiState.value.totalSpentMinorUnits)
    }

    // ── Total spending not multiplied by member count — regression test ────────

    @Test
    fun totalSpent_notMultipliedByMemberCount() = runTest {
        // Three members, one expense paid by Alice totalling 30_000
        // In a buggy JOIN-based query this would show 90_000 (30_000 × 3 members).
        fakeMemberRepo.overrideMembers = listOf(alice, bob, charlie)
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = 30_000L, paidBy = alice.id),
            splits = listOf(
                split(expenseId = 1L, memberId = alice.id, share = 10_000L),
                split(expenseId = 1L, memberId = bob.id, share = 10_000L),
                split(expenseId = 1L, memberId = charlie.id, share = 10_000L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            30_000L,
            vm.uiState.value.totalSpentMinorUnits,
            "Total spent must equal the expense amount, NOT be multiplied by member count",
        )
    }

    // ── Balances ──────────────────────────────────────────────────────────────

    @Test
    fun balances_positiveBalance_aliceIsOwed() = runTest {
        seedAlicePaidForBoth(100_000L)
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val alice = vm.uiState.value.balances.first { it.memberId == 1L }
        assertTrue(alice.netMinorUnits > 0L, "Alice should have a positive balance")
        assertEquals(50_000L, alice.netMinorUnits)
    }

    @Test
    fun balances_negativeBalance_bobOwes() = runTest {
        seedAlicePaidForBoth(100_000L)
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val bob = vm.uiState.value.balances.first { it.memberId == 2L }
        assertTrue(bob.netMinorUnits < 0L, "Bob should have a negative balance")
        assertEquals(-50_000L, bob.netMinorUnits)
    }

    @Test
    fun balances_zeroBalance_bothPaidEqualShares() = runTest {
        // Each person paid their own share — all settled
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = 50_000L, paidBy = alice.id),
            splits = listOf(split(expenseId = 1L, memberId = alice.id, share = 50_000L)),
        )
        fakeExpenseRepo.seed(
            expense(id = 2L, amount = 50_000L, paidBy = bob.id),
            splits = listOf(split(expenseId = 2L, memberId = bob.id, share = 50_000L)),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.uiState.value.balances.forEach { balance ->
            assertEquals(0L, balance.netMinorUnits, "${balance.memberName} should be settled")
        }
    }

    @Test
    fun balances_multipleMembers_allPresent() = runTest {
        fakeMemberRepo.overrideMembers = listOf(alice, bob, charlie)
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = 30_000L, paidBy = alice.id),
            splits = listOf(
                split(expenseId = 1L, memberId = alice.id, share = 10_000L),
                split(expenseId = 1L, memberId = bob.id, share = 10_000L),
                split(expenseId = 1L, memberId = charlie.id, share = 10_000L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, vm.uiState.value.balances.size)
    }

    // ── Outstanding settlements ───────────────────────────────────────────────

    @Test
    fun outstandingSettlements_oneDebt_showsCorrectly() = runTest {
        seedAlicePaidForBoth(100_000L)
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, state.debtsToSettleCount)
        val txn = state.outstandingSettlements.single()
        assertEquals("Bob", txn.fromMemberName)
        assertEquals("Alice", txn.toMemberName)
        assertEquals(50_000L, txn.amountMinorUnits)
        assertEquals(50_000L, state.outstandingAmountMinorUnits)
    }

    @Test
    fun allSettlementsPaid_isAllSettledTrue() = runTest {
        seedAlicePaidForBoth(100_000L)
        // Pre-load the full payment
        fakePaymentRepo.payments = mutableListOf(payment(debtor = 2L, creditor = 1L, amount = 50_000L))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isAllSettled)
        assertEquals(0, state.debtsToSettleCount)
        assertEquals(0L, state.outstandingAmountMinorUnits)
    }

    @Test
    fun partialSettlementPayment_reducesOutstandingAmount() = runTest {
        seedAlicePaidForBoth(100_000L)
        fakePaymentRepo.payments = mutableListOf(payment(debtor = 2L, creditor = 1L, amount = 20_000L))

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isAllSettled)
        assertEquals(1, state.debtsToSettleCount)
        assertEquals(30_000L, state.outstandingSettlements.single().amountMinorUnits)
        assertEquals(30_000L, state.outstandingAmountMinorUnits)
    }

    @Test
    fun noExpenses_isAllSettledFalse() = runTest {
        // No expenses → isEmpty, not isAllSettled
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isAllSettled, "isAllSettled requires expenses to exist")
        assertTrue(vm.uiState.value.isEmpty)
    }

    // ── Recent expenses ───────────────────────────────────────────────────────

    @Test
    fun recentExpenses_limitedToFive() = runTest {
        // Seed 7 expenses; only the latest 5 should appear
        for (i in 1..7) {
            fakeExpenseRepo.seed(
                expense(
                    id = i.toLong(),
                    amount = 10_000L,
                    paidBy = alice.id,
                    date = 1_700_000_000_000L + i * 1_000L,
                ),
                splits = listOf(
                    split(expenseId = i.toLong(), memberId = alice.id, share = 5_000L),
                    split(expenseId = i.toLong(), memberId = bob.id, share = 5_000L),
                ),
            )
        }

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            RECENT_EXPENSE_LIMIT,
            vm.uiState.value.recentExpenses.size,
            "Should show at most $RECENT_EXPENSE_LIMIT recent expenses",
        )
    }

    @Test
    fun recentExpenses_orderedNewestFirst() = runTest {
        // Seed 3 expenses with different dates
        val dates = listOf(1_000L, 3_000L, 2_000L)
        for ((idx, date) in dates.withIndex()) {
            val id = (idx + 1).toLong()
            fakeExpenseRepo.seed(
                expense(id = id, amount = 10_000L, paidBy = alice.id, date = date),
                splits = listOf(
                    split(expenseId = id, memberId = alice.id, share = 5_000L),
                    split(expenseId = id, memberId = bob.id, share = 5_000L),
                ),
            )
        }

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val expenses = vm.uiState.value.recentExpenses
        assertEquals(3, expenses.size)
        // Newest first: 3_000, 2_000, 1_000
        assertTrue(
            expenses[0].dateMillis >= expenses[1].dateMillis,
            "First expense should be newer than second",
        )
        assertTrue(
            expenses[1].dateMillis >= expenses[2].dateMillis,
            "Second expense should be newer than third",
        )
    }

    @Test
    fun recentExpenses_payerNameResolved() = runTest {
        seedAlicePaidForBoth()
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val expense = vm.uiState.value.recentExpenses.single()
        assertEquals("Alice", expense.payerName)
    }

    @Test
    fun recentExpenses_longExpenseName_handledGracefully() = runTest {
        val longName = "A very long expense description that would normally wrap across multiple lines in a UI"
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = 10_000L, paidBy = alice.id, description = longName),
            splits = listOf(
                split(expenseId = 1L, memberId = alice.id, share = 5_000L),
                split(expenseId = 1L, memberId = bob.id, share = 5_000L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(longName, vm.uiState.value.recentExpenses.single().description)
    }

    @Test
    fun recentExpenses_longMemberName_handledGracefully() = runTest {
        val longMemberName = "Thiruvenkatam Subramaniam Raghunathan Krishnamurthy"
        fakeMemberRepo.overrideMembers = listOf(
            Member(id = 99L, groupId = groupId, name = longMemberName),
            bob,
        )
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = 10_000L, paidBy = 99L),
            splits = listOf(
                split(expenseId = 1L, memberId = 99L, share = 5_000L),
                split(expenseId = 1L, memberId = bob.id, share = 5_000L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(longMemberName, vm.uiState.value.recentExpenses.single().payerName)
    }

    // ── Long minor-unit amounts ───────────────────────────────────────────────

    @Test
    fun longMinorUnitAmounts_preservedExactly() = runTest {
        // Use an amount that would lose precision if converted through Double
        val exactAmount = 999_999_999_999L
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = exactAmount, paidBy = alice.id),
            splits = listOf(
                // Odd split: alice gets the extra paise
                split(expenseId = 1L, memberId = alice.id, share = 499_999_999_999L + 1L),
                split(expenseId = 1L, memberId = bob.id, share = 499_999_999_999L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(exactAmount, vm.uiState.value.totalSpentMinorUnits)
    }

    // ── Reactive updates ──────────────────────────────────────────────────────

    @Test
    fun reactiveUpdate_afterAddingExpense_updatesState() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.isEmpty)

        // Reactively add an expense via the flow
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = 60_000L, paidBy = alice.id),
            splits = listOf(
                split(expenseId = 1L, memberId = alice.id, share = 30_000L),
                split(expenseId = 1L, memberId = bob.id, share = 30_000L),
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isEmpty)
        assertTrue(vm.uiState.value.hasExpenses)
        assertEquals(60_000L, vm.uiState.value.totalSpentMinorUnits)
    }

    @Test
    fun reactiveUpdate_afterDeletingExpense_updatesState() = runTest {
        seedAlicePaidForBoth(100_000L)
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(100_000L, vm.uiState.value.totalSpentMinorUnits)

        // Delete all expenses
        fakeExpenseRepo.clearAll()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0L, vm.uiState.value.totalSpentMinorUnits)
        assertFalse(vm.uiState.value.hasExpenses)
    }

    @Test
    fun reactiveUpdate_afterRecordingSettlementPayment_updatesOutstanding() = runTest {
        seedAlicePaidForBoth(100_000L)
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.debtsToSettleCount)
        assertEquals(50_000L, vm.uiState.value.outstandingAmountMinorUnits)

        // Reactively add a full payment
        fakePaymentRepo.paymentsFlow.value = listOf(
            payment(debtor = 2L, creditor = 1L, amount = 50_000L),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isAllSettled)
        assertEquals(0L, vm.uiState.value.outstandingAmountMinorUnits)
    }

    @Test
    fun reactiveUpdate_afterReversingSettlementPayment_restoresOutstanding() = runTest {
        seedAlicePaidForBoth(100_000L)
        // Start with a recorded full payment
        fakePaymentRepo.payments = mutableListOf(payment(debtor = 2L, creditor = 1L, amount = 50_000L, id = 1L))
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isAllSettled)

        // Reactively reverse (remove) the payment
        fakePaymentRepo.paymentsFlow.value = emptyList()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isAllSettled)
        assertEquals(50_000L, vm.uiState.value.outstandingAmountMinorUnits)
    }

    @Test
    fun reactiveUpdate_memberAdded_balancesUpdated() = runTest {
        fakeMemberRepo.overrideMembers = listOf(alice)
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = 10_000L, paidBy = alice.id),
            splits = listOf(split(expenseId = 1L, memberId = alice.id, share = 10_000L)),
        )
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.balances.size)

        // Add bob mid-stream
        fakeMemberRepo.membersFlow.value = listOf(alice, bob)
        testDispatcher.scheduler.advanceUntilIdle()

        // Bob has a zero balance (not in any expense) but appears in the member list
        assertEquals(2, vm.uiState.value.balances.size)
    }

    // ── Group metadata propagation ────────────────────────────────────────────

    @Test
    fun groupName_propagatedToUiState() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Trip", vm.uiState.value.groupName)
    }

    @Test
    fun currencyCode_propagatedToUiState() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("INR", vm.uiState.value.currencyCode)
    }

    // ── Deterministic ordering ────────────────────────────────────────────────

    @Test
    fun recentExpenses_deterministicOrdering() = runTest {
        for (i in 1..3) {
            val id = i.toLong()
            fakeExpenseRepo.seed(
                expense(id = id, amount = 10_000L, paidBy = alice.id, date = id * 1_000L),
                splits = listOf(
                    split(expenseId = id, memberId = alice.id, share = 5_000L),
                    split(expenseId = id, memberId = bob.id, share = 5_000L),
                ),
            )
        }

        val vm1 = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val run1 = vm1.uiState.value.recentExpenses.map { it.id }

        val vm2 = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val run2 = vm2.uiState.value.recentExpenses.map { it.id }

        assertEquals(run1, run2, "Recent expense ordering should be deterministic")
    }

    // ── Summary / Settlement agreement ───────────────────────────────────────

    @Test
    fun outstandingSettlements_matchSettlementScreenLogic() = runTest {
        // Two expenses in a multi-member scenario; verify outstanding matches
        // what the settlement screen would show
        fakeMemberRepo.overrideMembers = listOf(alice, bob, charlie)
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = 30_000L, paidBy = charlie.id),
            splits = listOf(
                split(expenseId = 1L, memberId = alice.id, share = 10_000L),
                split(expenseId = 1L, memberId = bob.id, share = 10_000L),
                split(expenseId = 1L, memberId = charlie.id, share = 10_000L),
            ),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Two people owe Charlie (Alice and Bob) → 2 settlements
        assertEquals(2, vm.uiState.value.debtsToSettleCount)
        assertEquals(20_000L, vm.uiState.value.outstandingAmountMinorUnits)
    }

    @Test
    fun partialPaymentsAcrossMultipleObligations_outstandingCorrect() = runTest {
        fakeMemberRepo.overrideMembers = listOf(alice, bob, charlie)
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = 30_000L, paidBy = charlie.id),
            splits = listOf(
                split(expenseId = 1L, memberId = alice.id, share = 10_000L),
                split(expenseId = 1L, memberId = bob.id, share = 10_000L),
                split(expenseId = 1L, memberId = charlie.id, share = 10_000L),
            ),
        )
        // Alice fully paid her debt
        fakePaymentRepo.payments = mutableListOf(
            payment(debtor = alice.id, creditor = charlie.id, amount = 10_000L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Only Bob still owes Charlie
        assertEquals(1, vm.uiState.value.debtsToSettleCount)
        assertEquals(10_000L, vm.uiState.value.outstandingAmountMinorUnits)
    }

    // ── Adjusted balances — regression tests ──────────────────────────────────
    //
    // These tests verify that the Balances section in the Summary screen shows
    // CURRENT member positions (after settlement payments), not raw expense balances.

    /**
     * Test 1: No payments → adjusted balances equal raw expense balances.
     */
    @Test
    fun adjustedBalances_noPayments_equalRawBalances() = runTest {
        // Alice paid ₹100,000; both share equally → Alice net +50,000, Bob net -50,000
        seedAlicePaidForBoth(100_000L)
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        val aliceAdj = state.adjustedBalances.first { it.memberId == alice.id }
        val bobAdj = state.adjustedBalances.first { it.memberId == bob.id }

        assertEquals(50_000L, aliceAdj.adjustedNetMinorUnits,
            "Without payments Alice's adjusted net should equal her raw net")
        assertEquals(-50_000L, bobAdj.adjustedNetMinorUnits,
            "Without payments Bob's adjusted net should equal his raw net")
    }

    /**
     * Test 2: Full payment → debtor becomes settled, creditor amount decreases.
     */
    @Test
    fun adjustedBalances_fullPayment_debtorSettledCreditorReduced() = runTest {
        // Alice paid ₹100,000; Bob owes Alice ₹50,000.
        // Bob makes the full ₹50,000 payment.
        seedAlicePaidForBoth(100_000L)
        fakePaymentRepo.payments = mutableListOf(
            payment(debtor = bob.id, creditor = alice.id, amount = 50_000L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        val aliceAdj = state.adjustedBalances.first { it.memberId == alice.id }
        val bobAdj = state.adjustedBalances.first { it.memberId == bob.id }

        assertEquals(0L, bobAdj.adjustedNetMinorUnits,
            "After full payment Bob should be settled (adjustedNet = 0)")
        assertEquals(0L, aliceAdj.adjustedNetMinorUnits,
            "After full payment Alice should be settled (adjustedNet = 0)")
    }

    /**
     * Test 3: Partial payment → debtor's owed amount decreases by the payment amount.
     */
    @Test
    fun adjustedBalances_partialPayment_debtorAmountDecreases() = runTest {
        // Bob owes Alice ₹50,000. Bob pays ₹20,000.
        seedAlicePaidForBoth(100_000L)
        fakePaymentRepo.payments = mutableListOf(
            payment(debtor = bob.id, creditor = alice.id, amount = 20_000L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        val bobAdj = state.adjustedBalances.first { it.memberId == bob.id }
        val aliceAdj = state.adjustedBalances.first { it.memberId == alice.id }

        assertEquals(-30_000L, bobAdj.adjustedNetMinorUnits,
            "Bob still owes ₹30,000 after ₹20,000 partial payment")
        assertEquals(30_000L, aliceAdj.adjustedNetMinorUnits,
            "Alice is owed ₹30,000 after receiving ₹20,000")
    }

    /**
     * Test 4: Multiple payments accumulate correctly.
     */
    @Test
    fun adjustedBalances_multiplePayments_accumulateCorrectly() = runTest {
        // Bob owes Alice ₹50,000. Two payments: ₹15,000 + ₹10,000 = ₹25,000 total.
        seedAlicePaidForBoth(100_000L)
        fakePaymentRepo.payments = mutableListOf(
            payment(debtor = bob.id, creditor = alice.id, amount = 15_000L),
            payment(debtor = bob.id, creditor = alice.id, amount = 10_000L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        val bobAdj = state.adjustedBalances.first { it.memberId == bob.id }
        val aliceAdj = state.adjustedBalances.first { it.memberId == alice.id }

        assertEquals(-25_000L, bobAdj.adjustedNetMinorUnits,
            "Bob owes ₹25,000 after two cumulative payments totalling ₹25,000")
        assertEquals(25_000L, aliceAdj.adjustedNetMinorUnits,
            "Alice is owed ₹25,000 after receiving ₹25,000 across two payments")
    }

    /**
     * Test 5: Multiple debtors/creditors — each payment only affects its specified members.
     */
    @Test
    fun adjustedBalances_multipleDebtors_paymentOnlyAffectsSpecifiedPair() = runTest {
        // Charlie paid ₹30,000; Alice owes ₹10,000, Bob owes ₹10,000.
        fakeMemberRepo.overrideMembers = listOf(alice, bob, charlie)
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = 30_000L, paidBy = charlie.id),
            splits = listOf(
                split(expenseId = 1L, memberId = alice.id, share = 10_000L),
                split(expenseId = 1L, memberId = bob.id, share = 10_000L),
                split(expenseId = 1L, memberId = charlie.id, share = 10_000L),
            ),
        )
        // Only Alice pays Charlie ₹10,000
        fakePaymentRepo.payments = mutableListOf(
            payment(debtor = alice.id, creditor = charlie.id, amount = 10_000L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        val aliceAdj = state.adjustedBalances.first { it.memberId == alice.id }
        val bobAdj = state.adjustedBalances.first { it.memberId == bob.id }
        val charlieAdj = state.adjustedBalances.first { it.memberId == charlie.id }

        assertEquals(0L, aliceAdj.adjustedNetMinorUnits,
            "Alice settled after paying her full share")
        assertEquals(-10_000L, bobAdj.adjustedNetMinorUnits,
            "Bob is unaffected by Alice's payment — still owes ₹10,000")
        assertEquals(10_000L, charlieAdj.adjustedNetMinorUnits,
            "Charlie is now owed ₹10,000 (only Bob still owes)")
    }

    /**
     * Test 6: Payment reversal → adjusted balances return to the previous state.
     */
    @Test
    fun adjustedBalances_paymentReversal_restoresPreviousState() = runTest {
        seedAlicePaidForBoth(100_000L)
        // Start with a full payment
        fakePaymentRepo.payments = mutableListOf(
            payment(debtor = bob.id, creditor = alice.id, amount = 50_000L, id = 1L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify settled
        assertEquals(0L, vm.uiState.value.adjustedBalances.first { it.memberId == bob.id }
            .adjustedNetMinorUnits)

        // Reactively reverse the payment
        fakePaymentRepo.paymentsFlow.value = emptyList()
        testDispatcher.scheduler.advanceUntilIdle()

        val bobAdj = vm.uiState.value.adjustedBalances.first { it.memberId == bob.id }
        assertEquals(-50_000L, bobAdj.adjustedNetMinorUnits,
            "After reversal, Bob should owe ₹50,000 again")
        val aliceAdj = vm.uiState.value.adjustedBalances.first { it.memberId == alice.id }
        assertEquals(50_000L, aliceAdj.adjustedNetMinorUnits,
            "After reversal, Alice should be owed ₹50,000 again")
    }

    /**
     * Test 7: Settlement and Summary agree after payments.
     * If Settlement shows "Bob owes Alice ₹30,000", Summary must show
     * Bob owes ₹30,000 and Alice is owed ₹30,000.
     */
    @Test
    fun adjustedBalances_agreeWithSettlementScreen_afterPartialPayment() = runTest {
        // Bob owes Alice ₹50,000; Bob pays ₹20,000.
        seedAlicePaidForBoth(100_000L)
        fakePaymentRepo.payments = mutableListOf(
            payment(debtor = bob.id, creditor = alice.id, amount = 20_000L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value

        // Settlement section must show remaining ₹30,000
        assertEquals(1, state.debtsToSettleCount)
        assertEquals(30_000L, state.outstandingSettlements.single().amountMinorUnits)

        // Balances section must agree: Bob owes ₹30,000, Alice owed ₹30,000
        val bobAdj = state.adjustedBalances.first { it.memberId == bob.id }
        val aliceAdj = state.adjustedBalances.first { it.memberId == alice.id }

        assertEquals(-30_000L, bobAdj.adjustedNetMinorUnits,
            "Summary balance and Settlement must agree: Bob owes ₹30,000")
        assertEquals(30_000L, aliceAdj.adjustedNetMinorUnits,
            "Summary balance and Settlement must agree: Alice owed ₹30,000")
    }

    /**
     * Test 8: Total spent is unchanged by settlement payments.
     */
    @Test
    fun adjustedBalances_totalSpent_unchangedByPayments() = runTest {
        seedAlicePaidForBoth(100_000L)
        fakePaymentRepo.payments = mutableListOf(
            payment(debtor = bob.id, creditor = alice.id, amount = 50_000L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            100_000L,
            vm.uiState.value.totalSpentMinorUnits,
            "Total spent must not be affected by settlement payments",
        )
    }

    /**
     * Test 9: Outstanding amount is consistent with Settlement after payments.
     */
    @Test
    fun adjustedBalances_outstandingConsistentWithSettlement() = runTest {
        // Alice paid ₹60,000; Bob paid ₹40,000 for shared 3-way expense
        fakeMemberRepo.overrideMembers = listOf(alice, bob, charlie)
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = 60_000L, paidBy = alice.id),
            splits = listOf(
                split(expenseId = 1L, memberId = alice.id, share = 20_000L),
                split(expenseId = 1L, memberId = bob.id, share = 20_000L),
                split(expenseId = 1L, memberId = charlie.id, share = 20_000L),
            ),
        )
        // Bob pays Alice ₹20,000 (his full share)
        fakePaymentRepo.payments = mutableListOf(
            payment(debtor = bob.id, creditor = alice.id, amount = 20_000L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value

        // Only Charlie still owes Alice ₹20,000
        assertEquals(1, state.debtsToSettleCount)
        assertEquals(20_000L, state.outstandingAmountMinorUnits)

        // Adjusted balances: Bob = 0, Charlie = -20,000, Alice = +20,000
        val bobAdj = state.adjustedBalances.first { it.memberId == bob.id }
        val charlieAdj = state.adjustedBalances.first { it.memberId == charlie.id }
        val aliceAdj = state.adjustedBalances.first { it.memberId == alice.id }

        assertEquals(0L, bobAdj.adjustedNetMinorUnits, "Bob fully settled")
        assertEquals(-20_000L, charlieAdj.adjustedNetMinorUnits, "Charlie still owes ₹20,000")
        assertEquals(20_000L, aliceAdj.adjustedNetMinorUnits, "Alice owed ₹20,000 by Charlie")
    }

    /**
     * Test 10: Long minor-unit amounts preserved exactly in adjusted balances.
     */
    @Test
    fun adjustedBalances_longMinorUnits_preservedExactly() = runTest {
        // Use amounts that would lose precision through Double arithmetic
        val expenseAmount = 999_999_999_999L
        val halfPlusOne = 500_000_000_000L
        val halfMinusOne = 499_999_999_999L
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = expenseAmount, paidBy = alice.id),
            splits = listOf(
                split(expenseId = 1L, memberId = alice.id, share = halfPlusOne),
                split(expenseId = 1L, memberId = bob.id, share = halfMinusOne),
            ),
        )
        val paymentAmount = 200_000_000_000L
        fakePaymentRepo.payments = mutableListOf(
            payment(debtor = bob.id, creditor = alice.id, amount = paymentAmount),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Bob raw net = -(499_999_999_999) → after payment +200_000_000_000 → -299_999_999_999
        val expectedBobNet = -halfMinusOne + paymentAmount
        // Alice raw net = +(999_999_999_999 - 500_000_000_000) = +499_999_999_999 → -200_000_000_000 → +299_999_999_999
        val expectedAliceNet = (expenseAmount - halfPlusOne) - paymentAmount

        val bobAdj = vm.uiState.value.adjustedBalances.first { it.memberId == bob.id }
        val aliceAdj = vm.uiState.value.adjustedBalances.first { it.memberId == alice.id }

        assertEquals(expectedBobNet, bobAdj.adjustedNetMinorUnits,
            "Long minor-unit Bob adjusted net must be exact")
        assertEquals(expectedAliceNet, aliceAdj.adjustedNetMinorUnits,
            "Long minor-unit Alice adjusted net must be exact")
    }

    /**
     * Test 11: Regression — the exact bug reported.
     *
     * Scenario:
     *   Abhirami paid ₹600 for 3 people (Sanjai, Joswin, Abhirami — ₹200 each).
     *   Joswin  → owes Abhirami ₹200  (no payment)
     *   Sanjai  → owes Abhirami ₹200  (full ₹200 payment recorded)
     *
     * Expected Summary Balances AFTER payment:
     *   Sanjai   — is settled
     *   Joswin   — owes ₹200
     *   Abhirami — is owed ₹200
     *
     * Expected Financial Overview (unchanged by payment):
     *   Total spent : ₹600
     *   Outstanding : ₹200
     *   Debts       : 1
     */
    @Test
    fun regression_sanjaiPaysAbhirami_summaryBalancesUpdatedCorrectly() = runTest {
        val sanjai = Member(id = 10L, groupId = groupId, name = "Sanjai")
        val joswin = Member(id = 11L, groupId = groupId, name = "Joswin")
        val abhirami = Member(id = 12L, groupId = groupId, name = "Abhirami")
        fakeMemberRepo.overrideMembers = listOf(sanjai, joswin, abhirami)

        // Abhirami paid ₹600 (60_000 paise); all three split equally at ₹200 each.
        fakeExpenseRepo.seed(
            expense(id = 1L, amount = 60_000L, paidBy = abhirami.id),
            splits = listOf(
                split(expenseId = 1L, memberId = sanjai.id, share = 20_000L),
                split(expenseId = 1L, memberId = joswin.id, share = 20_000L),
                split(expenseId = 1L, memberId = abhirami.id, share = 20_000L),
            ),
        )

        // Sanjai records a full ₹200 payment to Abhirami.
        fakePaymentRepo.payments = mutableListOf(
            payment(debtor = sanjai.id, creditor = abhirami.id, amount = 20_000L),
        )

        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value

        // ── Financial Overview — unchanged by payment ──────────────────────────
        assertEquals(60_000L, state.totalSpentMinorUnits, "Total spent must be ₹600")
        assertEquals(1, state.debtsToSettleCount, "Only Joswin's debt remains")
        assertEquals(20_000L, state.outstandingAmountMinorUnits, "Outstanding must be ₹200")

        // ── Settlements — Joswin still owes Abhirami ──────────────────────────
        val remaining = state.outstandingSettlements.single()
        assertEquals(joswin.id, remaining.fromMemberId, "Joswin should be the remaining debtor")
        assertEquals(abhirami.id, remaining.toMemberId, "Abhirami should be the remaining creditor")
        assertEquals(20_000L, remaining.amountMinorUnits)

        // ── Adjusted Balances — Sanjai settled, Joswin owes, Abhirami owed ────
        val sanjaiAdj = state.adjustedBalances.first { it.memberId == sanjai.id }
        val joswinAdj = state.adjustedBalances.first { it.memberId == joswin.id }
        val abhiramiAdj = state.adjustedBalances.first { it.memberId == abhirami.id }

        assertEquals(0L, sanjaiAdj.adjustedNetMinorUnits,
            "Sanjai must be settled after paying ₹200")
        assertEquals(-20_000L, joswinAdj.adjustedNetMinorUnits,
            "Joswin must still owe ₹200")
        assertEquals(20_000L, abhiramiAdj.adjustedNetMinorUnits,
            "Abhirami must be owed ₹200 (only Joswin's debt remains)")
    }

    // ── Unit tests for computeAdjustedBalances ────────────────────────────────
    //
    // Because computeAdjustedBalances is internal, these tests invoke it directly
    // for tightly-scoped arithmetic verification without spinning up a ViewModel.

    @Test
    fun computeAdjustedBalances_emptyPayments_returnsRawNets() {
        val raw = listOf(
            memberBalance(id = 1L, name = "A", paid = 100L, owed = 50L),   // net +50
            memberBalance(id = 2L, name = "B", paid = 0L,   owed = 50L),   // net -50
        )
        val result = SummaryViewModel.computeAdjustedBalances(raw, emptyList())

        assertEquals(50L,  result.first { it.memberId == 1L }.adjustedNetMinorUnits)
        assertEquals(-50L, result.first { it.memberId == 2L }.adjustedNetMinorUnits)
    }

    @Test
    fun computeAdjustedBalances_singleFullPayment_bothZero() {
        val raw = listOf(
            memberBalance(id = 1L, name = "A", paid = 200L, owed = 100L),  // net +100
            memberBalance(id = 2L, name = "B", paid = 0L,   owed = 100L),  // net -100
        )
        val payments = listOf(
            SettlementPayment(id = 1L, groupId = 1L,
                debtorMemberId = 2L, creditorMemberId = 1L, amountMinorUnits = 100L),
        )
        val result = SummaryViewModel.computeAdjustedBalances(raw, payments)

        assertEquals(0L, result.first { it.memberId == 1L }.adjustedNetMinorUnits)
        assertEquals(0L, result.first { it.memberId == 2L }.adjustedNetMinorUnits)
    }

    @Test
    fun computeAdjustedBalances_partialPayment_correctResiduals() {
        val raw = listOf(
            memberBalance(id = 1L, name = "A", paid = 200L, owed = 100L),  // net +100
            memberBalance(id = 2L, name = "B", paid = 0L,   owed = 100L),  // net -100
        )
        val payments = listOf(
            SettlementPayment(id = 1L, groupId = 1L,
                debtorMemberId = 2L, creditorMemberId = 1L, amountMinorUnits = 40L),
        )
        val result = SummaryViewModel.computeAdjustedBalances(raw, payments)

        assertEquals(60L,  result.first { it.memberId == 1L }.adjustedNetMinorUnits)
        assertEquals(-60L, result.first { it.memberId == 2L }.adjustedNetMinorUnits)
    }

    @Test
    fun computeAdjustedBalances_preservesOutputOrder() {
        val raw = listOf(
            memberBalance(id = 3L, name = "C", paid = 300L, owed = 100L),  // net +200
            memberBalance(id = 1L, name = "A", paid = 0L,   owed = 100L),  // net -100
            memberBalance(id = 2L, name = "B", paid = 0L,   owed = 100L),  // net -100
        )
        val result = SummaryViewModel.computeAdjustedBalances(raw, emptyList())

        // Order must match rawBalances
        assertEquals(listOf(3L, 1L, 2L), result.map { it.memberId })
    }
}

// ── Test helpers ──────────────────────────────────────────────────────────────

private fun memberBalance(id: Long, name: String, paid: Long, owed: Long) =
    com.splitease.domain.model.MemberBalance(
        memberId = id,
        memberName = name,
        totalPaidMinorUnits = paid,
        totalOwedMinorUnits = owed,
    )

// ── Fake repositories ─────────────────────────────────────────────────────────

private class FakeSummaryGroupRepository(initialGroup: Group) : GroupRepository {
    var overrideGroup: Group? = initialGroup
    var shouldThrow = false

    override fun getGroups(): Flow<List<Group>> = flowOf(listOfNotNull(overrideGroup))
    override fun observeGroupById(id: Long): Flow<Group?> = flowOf(overrideGroup)
    override suspend fun getGroupById(id: Long): Group? {
        if (shouldThrow) throw RuntimeException("Group repository error")
        return if (overrideGroup?.id == id) overrideGroup else null
    }
    override suspend fun createGroup(group: Group): Long = group.id
    override suspend fun updateGroup(group: Group) = Unit
    override suspend fun deleteGroup(group: Group) = Unit
}

private class FakeSummaryMemberRepository(initialMembers: List<Member>) : MemberRepository {
    var overrideMembers: List<Member> = initialMembers
    val membersFlow = MutableStateFlow(initialMembers)

    override fun getMembersForGroup(groupId: Long): Flow<List<Member>> {
        membersFlow.value = overrideMembers
        return membersFlow.map { list -> list.filter { it.groupId == groupId } }
    }
    override suspend fun getMemberById(id: Long): Member? = overrideMembers.firstOrNull { it.id == id }
    override suspend fun addMember(member: Member): Long = member.id
    override suspend fun updateMember(member: Member) = Unit
    override suspend fun deleteMember(member: Member) = Unit
}

private class FakeSummaryExpenseRepository : ExpenseRepository {
    var shouldThrowOnExpenses = false

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    private val _splits = MutableStateFlow<List<ExpenseSplit>>(emptyList())

    fun seed(expense: Expense, splits: List<ExpenseSplit>) {
        _expenses.value = _expenses.value + expense
        _splits.value = _splits.value + splits
    }

    fun clearAll() {
        _expenses.value = emptyList()
        _splits.value = emptyList()
    }

    override fun getExpensesForGroup(groupId: Long): Flow<List<Expense>> =
        if (shouldThrowOnExpenses) flow { throw RuntimeException("Expense error") }
        else _expenses.map { list -> list.filter { it.groupId == groupId } }

    override fun getSplitsForGroup(groupId: Long): Flow<List<ExpenseSplit>> = _splits

    override fun getSplitsForExpense(expenseId: Long): Flow<List<ExpenseSplit>> =
        _splits.map { list -> list.filter { it.expenseId == expenseId } }

    override suspend fun getExpenseById(id: Long): Expense? = _expenses.value.firstOrNull { it.id == id }
    override suspend fun addExpense(expense: Expense, splits: List<ExpenseSplit>): Long = expense.id
    override suspend fun updateExpense(expense: Expense, splits: List<ExpenseSplit>) = Unit
    override suspend fun deleteExpense(expense: Expense) = Unit
}

private class FakeSummaryPaymentRepository : SettlementPaymentRepository {
    var payments: MutableList<SettlementPayment> = mutableListOf()
    val paymentsFlow = MutableStateFlow<List<SettlementPayment>>(emptyList())
    private var nextId = 1L

    override fun getPaymentsForGroup(groupId: Long): Flow<List<SettlementPayment>> {
        paymentsFlow.value = payments.toList()
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
