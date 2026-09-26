package com.splitease.presentation.screens.settlement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.splitease.domain.model.SettlementPayment
import com.splitease.domain.model.SettlementTransaction
import com.splitease.presentation.components.AmountText
import com.splitease.util.MoneyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(
    viewModel: SettlementViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages in snackbar
    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.onErrorDismissed()
        }
    }

    // Show one-shot success/info messages with optional Undo action
    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            // Capture undo availability before consuming the message
            val hasUndo = uiState.lastRecordedPayment != null
            viewModel.onSnackbarMessageConsumed()
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (hasUndo) "Undo" else null,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.onUndoLastPayment()
            }
        }
    }

    // Confirmation dialog
    val dialog = uiState.pendingPaymentDialog
    if (dialog != null) {
        MarkAsPaidDialog(
            dialog = dialog,
            currencyCode = uiState.currencyCode,
            onDismiss = viewModel::onPaymentDialogDismissed,
            onAmountChanged = viewModel::onPaymentAmountChanged,
            onConfirm = viewModel::onConfirmPayment,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.groupName.isNotBlank()) uiState.groupName else "Settle Up",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Navigate back"
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        when {
            uiState.isLoading -> SettlementLoadingContent(paddingValues)
            uiState.groupNotFound -> SettlementGroupNotFoundContent(paddingValues, onNavigateBack)
            uiState.errorMessage != null && uiState.settlements.isEmpty() && uiState.recentPayments.isEmpty() ->
                SettlementErrorContent(paddingValues, uiState.errorMessage!!, onNavigateBack)
            uiState.isEmpty -> SettlementEmptyContent(paddingValues)
            uiState.isAllSettled -> SettlementAllSettledContent(
                paddingValues = paddingValues,
                recentPayments = uiState.recentPayments,
                currencyCode = uiState.currencyCode,
                memberNames = uiState.memberNames,
                onReversePayment = viewModel::onReversePayment,
            )
            else -> SettlementListContent(
                uiState = uiState,
                paddingValues = paddingValues,
                onMarkAsPaid = viewModel::onMarkAsPaidClick,
                onReversePayment = viewModel::onReversePayment,
            )
        }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun SettlementLoadingContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = "Loading settlement suggestions" },
        )
    }
}

// ── Group not found ───────────────────────────────────────────────────────────

@Composable
private fun SettlementGroupNotFoundContent(
    paddingValues: PaddingValues,
    onNavigateBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text("Group not found", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "This group may have been deleted.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNavigateBack,
            modifier = Modifier.height(48.dp).semantics { contentDescription = "Go back" },
        ) { Text("Go Back") }
    }
}

// ── Error ─────────────────────────────────────────────────────────────────────

@Composable
private fun SettlementErrorContent(
    paddingValues: PaddingValues,
    message: String,
    onNavigateBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Something went wrong.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNavigateBack,
            modifier = Modifier.height(48.dp).semantics { contentDescription = "Go back" },
        ) { Text("Go Back") }
    }
}

// ── Empty (no expenses yet) ───────────────────────────────────────────────────

@Composable
private fun SettlementEmptyContent(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No expenses yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Settlement suggestions will appear here once expenses have been added to the group.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── All settled ───────────────────────────────────────────────────────────────

@Composable
private fun SettlementAllSettledContent(
    paddingValues: PaddingValues,
    recentPayments: List<SettlementPayment>,
    currencyCode: String,
    memberNames: Map<Long, String>,
    onReversePayment: (SettlementPayment) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    "Everyone is settled",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "All balances are zero — there is nothing left to pay.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (recentPayments.isNotEmpty()) {
            item { PaymentHistoryHeader() }
            items(recentPayments, key = { it.id }) { payment ->
                PaymentHistoryCard(
                    payment = payment,
                    currencyCode = currencyCode,
                    memberNames = memberNames,
                    onReverse = { onReversePayment(payment) },
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Settlement list ───────────────────────────────────────────────────────────

@Composable
private fun SettlementListContent(
    uiState: SettlementUiState,
    paddingValues: PaddingValues,
    onMarkAsPaid: (SettlementTransaction) -> Unit,
    onReversePayment: (SettlementPayment) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Summary header
        item {
            SettlementSummaryCard(uiState = uiState)
        }

        // One card per outstanding settlement
        items(uiState.settlements, key = { "${it.fromMemberId}-${it.toMemberId}" }) { settlement ->
            SettlementTransactionCard(
                settlement = settlement,
                currencyCode = uiState.currencyCode,
                onMarkAsPaid = { onMarkAsPaid(settlement) },
            )
        }

        // Payment history section
        if (uiState.recentPayments.isNotEmpty()) {
            item { PaymentHistoryHeader() }
            items(uiState.recentPayments, key = { it.id }) { payment ->
                PaymentHistoryCard(
                    payment = payment,
                    currencyCode = uiState.currencyCode,
                    memberNames = uiState.memberNames,
                    onReverse = { onReversePayment(payment) },
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Summary header card ───────────────────────────────────────────────────────

@Composable
private fun SettlementSummaryCard(uiState: SettlementUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Suggested Settlements",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )
            val countText = when (uiState.settlementCount) {
                1 -> "1 debt to settle"
                else -> "${uiState.settlementCount} debts to settle"
            }
            Text(
                text = countText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.semantics { contentDescription = countText },
            )
            Text(
                text = "Tap \"Mark as paid\" on a card to record a payment. " +
                    "Recorded payments are subtracted from the outstanding balance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

// ── Individual settlement card ────────────────────────────────────────────────

@Composable
private fun SettlementTransactionCard(
    settlement: SettlementTransaction,
    currencyCode: String,
    onMarkAsPaid: () -> Unit,
) {
    val amountFormatted = MoneyFormatter.format(settlement.amountMinorUnits, currencyCode)
    // Full accessible description used as the card's content description.
    val accessibleDescription =
        "${settlement.fromMemberName} owes ${settlement.toMemberName} $amountFormatted. " +
            "Button: Mark as paid"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleDescription
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Debtor → Creditor row ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = settlement.fromMemberName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // Arrow — decorative only; direction is communicated by card semantics
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = settlement.toMemberName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── "Owes" label + amount ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Owes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AmountText(
                    amountMinorUnits = settlement.amountMinorUnits,
                    currencyCode = currencyCode,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // ── Mark as paid button ───────────────────────────────────────────
            OutlinedButton(
                onClick = onMarkAsPaid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics {
                        contentDescription =
                            "Mark payment as completed: ${settlement.fromMemberName} pays ${settlement.toMemberName} $amountFormatted"
                    },
            ) {
                Text("Mark as paid")
            }
        }
    }
}

// ── Payment history ───────────────────────────────────────────────────────────

@Composable
private fun PaymentHistoryHeader() {
    Text(
        text = "Recent payments",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun PaymentHistoryCard(
    payment: SettlementPayment,
    currencyCode: String,
    memberNames: Map<Long, String>,
    onReverse: () -> Unit,
) {
    val amountFormatted = MoneyFormatter.format(payment.amountMinorUnits, currencyCode)
    val debtorName = memberNames[payment.debtorMemberId] ?: "Member ${payment.debtorMemberId}"
    val creditorName = memberNames[payment.creditorMemberId] ?: "Member ${payment.creditorMemberId}"
    val dateFormatted = remember(payment.paidAt) {
        SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(payment.paidAt))
    }
    val accessibleDescription =
        "$debtorName paid $creditorName $amountFormatted on $dateFormatted. Button: Remove payment"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleDescription
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$debtorName paid $creditorName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$amountFormatted · $dateFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onReverse,
                modifier = Modifier.semantics {
                    contentDescription = "Remove payment: $debtorName paid $creditorName $amountFormatted"
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Mark as paid dialog ───────────────────────────────────────────────────────

@Composable
private fun MarkAsPaidDialog(
    dialog: PendingPaymentDialog,
    currencyCode: String,
    onDismiss: () -> Unit,
    onAmountChanged: (Long) -> Unit,
    onConfirm: () -> Unit,
) {
    val settlement = dialog.settlement
    val fullAmountFormatted = MoneyFormatter.format(settlement.amountMinorUnits, currencyCode)

    // Local text state for the amount field (user types major units, e.g. "200.50")
    var amountText by rememberSaveable(dialog.settlement.fromMemberId, dialog.settlement.toMemberId) {
        mutableStateOf(formatAsDecimalInput(dialog.amountMinorUnits, currencyCode))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Mark payment as completed?",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${settlement.fromMemberName} owes ${settlement.toMemberName} " +
                        "$fullAmountFormatted outstanding.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Enter the amount paid. Defaults to the full outstanding amount. " +
                        "This will record the payment and update the remaining settlement suggestions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { text ->
                        amountText = text
                        val parsed = parseDecimalInput(text, currencyCode)
                        onAmountChanged(parsed)
                    },
                    label = { Text("Amount") },
                    prefix = { Text(currencySymbol(currencyCode)) },
                    isError = dialog.amountError != null,
                    supportingText = {
                        if (dialog.amountError != null) {
                            Text(
                                text = dialog.amountError,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .semantics { contentDescription = "Payment amount field" },
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics { contentDescription = "Cancel" },
            ) { Text("Cancel") }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = dialog.amountError == null && dialog.amountMinorUnits > 0L,
                modifier = Modifier.semantics { contentDescription = "Confirm mark as paid" },
            ) { Text("Mark as paid") }
        },
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Format Long minor units as a decimal string suitable for an input field (e.g. 20050 → "200.50"). */
private fun formatAsDecimalInput(amountMinorUnits: Long, currencyCode: String): String {
    return try {
        val fractionDigits = java.util.Currency.getInstance(currencyCode).defaultFractionDigits
        if (fractionDigits == 0) {
            amountMinorUnits.toString()
        } else {
            val divisor = pow10(fractionDigits)
            val major = amountMinorUnits / divisor
            val minor = amountMinorUnits % divisor
            "$major.${minor.toString().padStart(fractionDigits, '0')}"
        }
    } catch (_: Exception) {
        amountMinorUnits.toString()
    }
}

/**
 * Parse a user-typed decimal string into Long minor units.
 * Uses only integer arithmetic — no Double/Float.
 * Returns 0 if the input cannot be parsed.
 */
private fun parseDecimalInput(text: String, currencyCode: String): Long {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return 0L
    return try {
        val fractionDigits = java.util.Currency.getInstance(currencyCode).defaultFractionDigits
        if (fractionDigits == 0) {
            trimmed.toLongOrNull() ?: 0L
        } else {
            val dotIndex = trimmed.indexOf('.')
            if (dotIndex < 0) {
                // No decimal point — treat as major units
                val major = trimmed.toLongOrNull() ?: return 0L
                major * pow10(fractionDigits)
            } else {
                val majorStr = trimmed.substring(0, dotIndex)
                val minorStr = trimmed.substring(dotIndex + 1)
                    .take(fractionDigits)
                    .padEnd(fractionDigits, '0')
                val major = if (majorStr.isEmpty()) 0L else majorStr.toLongOrNull() ?: return 0L
                val minor = minorStr.toLongOrNull() ?: return 0L
                major * pow10(fractionDigits) + minor
            }
        }
    } catch (_: Exception) {
        0L
    }
}

private fun currencySymbol(currencyCode: String): String =
    try { java.util.Currency.getInstance(currencyCode).symbol } catch (_: Exception) { currencyCode }

private fun pow10(exp: Int): Long {
    var result = 1L
    repeat(exp) { result *= 10 }
    return result
}
