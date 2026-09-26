package com.splitease.presentation.screens.settlement

import com.splitease.domain.model.SettlementPayment
import com.splitease.domain.model.SettlementTransaction

/**
 * UI state for the Settlement screen.
 *
 * [settlements] are the *remaining* suggested settlements after subtracting recorded payments.
 * [recentPayments] are all recorded payments for the group, ordered newest-first.
 *
 * All monetary amounts are Long minor units — no Double/Float anywhere in this state.
 */
data class SettlementUiState(
    val isLoading: Boolean = true,
    val groupId: Long = 0L,
    val groupName: String = "",
    val currencyCode: String = "INR",
    /** Remaining outstanding settlements after subtracting recorded payments. */
    val settlements: List<SettlementTransaction> = emptyList(),
    /** All recorded payments for this group, newest first. */
    val recentPayments: List<SettlementPayment> = emptyList(),
    /** Member id → display name, used to render payment history cards. */
    val memberNames: Map<Long, String> = emptyMap(),
    val groupNotFound: Boolean = false,
    val errorMessage: String? = null,
    /** True once all data has been loaded, even if there are no expenses. */
    val hasExpenses: Boolean = false,
    /**
     * When non-null, the confirmation dialog is shown for this pending payment.
     * The user may edit [pendingPaymentAmount] before confirming.
     */
    val pendingPaymentDialog: PendingPaymentDialog? = null,
    /** One-shot snackbar message. Null when no message to show. */
    val snackbarMessage: String? = null,
    /** The most recently recorded payment, held for undo until the next payment/reverse. */
    val lastRecordedPayment: SettlementPayment? = null,
) {
    /**
     * True when loading is done, group exists, no error, and there are no expenses at all.
     * Settlement suggestions only make sense once expenses exist.
     */
    val isEmpty: Boolean
        get() = !isLoading && !groupNotFound && errorMessage == null && !hasExpenses

    /**
     * True when loading is done, group exists, no error, there are expenses,
     * and all outstanding balances are zero.
     */
    val isAllSettled: Boolean
        get() = !isLoading && !groupNotFound && errorMessage == null
            && hasExpenses && settlements.isEmpty()

    /** Convenience count used in the summary header. */
    val settlementCount: Int
        get() = settlements.size
}

/**
 * State for the "Mark as paid" confirmation dialog.
 *
 * [settlement] is the suggestion being confirmed.
 * [amountMinorUnits] defaults to the full outstanding amount but may be edited by the user
 * for partial payments.
 * [amountError] is non-null if the entered amount is invalid.
 */
data class PendingPaymentDialog(
    val settlement: SettlementTransaction,
    val amountMinorUnits: Long,
    val amountError: String? = null,
)
