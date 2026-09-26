package com.splitease.presentation.screens.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.splitease.domain.model.SettlementTransaction
import com.splitease.presentation.components.AmountText
import com.splitease.util.MoneyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBalances: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onNavigateToSettlement: () -> Unit = {},
    onNavigateToAddExpense: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val title = if (uiState.groupName.isNotBlank()) uiState.groupName else "Summary"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Back to group details"
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
    ) { paddingValues ->
        when {
            uiState.isLoading -> SummaryLoadingContent(paddingValues)
            uiState.groupNotFound -> SummaryGroupNotFoundContent(paddingValues, onNavigateBack)
            uiState.errorMessage != null && uiState.balances.isEmpty() ->
                SummaryErrorContent(
                    paddingValues = paddingValues,
                    message = uiState.errorMessage!!,
                    onRetry = viewModel::retry,
                )
            else -> SummarySuccessContent(
                uiState = uiState,
                paddingValues = paddingValues,
                onNavigateToBalances = onNavigateToBalances,
                onNavigateToExpenses = onNavigateToExpenses,
                onNavigateToSettlement = onNavigateToSettlement,
                onNavigateToAddExpense = onNavigateToAddExpense,
            )
        }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun SummaryLoadingContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = "Loading summary" },
        )
    }
}

// ── Group not found ───────────────────────────────────────────────────────────

@Composable
private fun SummaryGroupNotFoundContent(
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
        Text(
            text = "Group not found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "This group may have been deleted.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .height(48.dp)
                .semantics { contentDescription = "Go back" },
        ) { Text("Go Back") }
    }
}

// ── Error ─────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryErrorContent(
    paddingValues: PaddingValues,
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Something went wrong.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier
                .height(48.dp)
                .semantics { contentDescription = "Retry loading summary" },
        ) { Text("Retry") }
    }
}

// ── Success / main content ────────────────────────────────────────────────────

@Composable
private fun SummarySuccessContent(
    uiState: SummaryUiState,
    paddingValues: PaddingValues,
    onNavigateToBalances: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToSettlement: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Financial Overview (header metrics) ───────────────────────────────
        item {
            OverviewSection(uiState = uiState)
        }

        // ── Balance Overview ──────────────────────────────────────────────────
        item {
            BalancesSection(
                adjustedBalances = uiState.adjustedBalances,
                currencyCode = uiState.currencyCode,
                hasExpenses = uiState.hasExpenses,
                onViewBalances = onNavigateToBalances,
            )
        }

        // ── Outstanding Settlements ───────────────────────────────────────────
        item {
            SettlementsSection(
                uiState = uiState,
                onViewSettlements = onNavigateToSettlement,
            )
        }

        // ── Recent Expenses ───────────────────────────────────────────────────
        item {
            RecentExpensesSection(
                uiState = uiState,
                onViewExpenses = onNavigateToExpenses,
                onAddExpense = onNavigateToAddExpense,
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Overview section ──────────────────────────────────────────────────────────

@Composable
private fun OverviewSection(uiState: SummaryUiState) {
    SectionCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Financial Overview",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MetricItem(
                    label = "Total spent",
                    modifier = Modifier.weight(1f),
                    accessibleLabel = "Total spent: ${
                        MoneyFormatter.format(uiState.totalSpentMinorUnits, uiState.currencyCode)
                    }",
                ) {
                    AmountText(
                        amountMinorUnits = uiState.totalSpentMinorUnits,
                        currencyCode = uiState.currencyCode,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                VerticalDividerLine()

                MetricItem(
                    label = "Outstanding",
                    modifier = Modifier.weight(1f),
                    accessibleLabel = "Outstanding: ${
                        MoneyFormatter.format(uiState.outstandingAmountMinorUnits, uiState.currencyCode)
                    }",
                ) {
                    AmountText(
                        amountMinorUnits = uiState.outstandingAmountMinorUnits,
                        currencyCode = uiState.currencyCode,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (uiState.outstandingAmountMinorUnits > 0L)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface,
                    )
                }

                VerticalDividerLine()

                MetricItem(
                    label = "Debts to settle",
                    modifier = Modifier.weight(1f),
                    accessibleLabel = "${uiState.debtsToSettleCount} debts to settle",
                ) {
                    Text(
                        text = uiState.debtsToSettleCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (uiState.debtsToSettleCount > 0)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Currency sub-label
            if (uiState.currencyCode.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Currency: ${uiState.currencyCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    modifier: Modifier = Modifier,
    accessibleLabel: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleLabel
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        content()
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VerticalDividerLine() {
    VerticalDivider(
        modifier = Modifier.height(40.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

// ── Balances section ──────────────────────────────────────────────────────────

@Composable
private fun BalancesSection(
    adjustedBalances: List<AdjustedMemberBalance>,
    currencyCode: String,
    hasExpenses: Boolean,
    onViewBalances: () -> Unit,
) {
    SectionCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeader(
                title = "Balances",
                actionLabel = "View balances",
                onAction = onViewBalances,
            )

            if (!hasExpenses || adjustedBalances.isEmpty()) {
                EmptyStateText("No balances yet. Add expenses to see who owes what.")
            } else {
                adjustedBalances.forEach { balance ->
                    BalanceSummaryRow(balance = balance, currencyCode = currencyCode)
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BalanceSummaryRow(
    balance: AdjustedMemberBalance,
    currencyCode: String,
) {
    val net = balance.adjustedNetMinorUnits
    val absNet = kotlin.math.abs(net)
    val statusText = when {
        net > 0L -> "is owed ${MoneyFormatter.format(absNet, currencyCode)}"
        net < 0L -> "owes ${MoneyFormatter.format(absNet, currencyCode)}"
        else -> "is settled"
    }
    val accessibleDescription = "${balance.memberName} $statusText"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleDescription
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Member name + status indicator
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Icon communicates direction without relying on color alone
            when {
                net > 0L -> Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                net < 0L -> Icon(
                    imageVector = Icons.Default.TrendingDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                else -> Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column {
                Text(
                    text = balance.memberName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        net > 0L -> MaterialTheme.colorScheme.tertiary
                        net < 0L -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

// ── Settlements section ───────────────────────────────────────────────────────

@Composable
private fun SettlementsSection(
    uiState: SummaryUiState,
    onViewSettlements: () -> Unit,
) {
    SectionCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeader(
                title = "Settlements",
                actionLabel = "View all settlements",
                onAction = onViewSettlements,
            )

            when {
                !uiState.hasExpenses -> {
                    EmptyStateText("No settlements yet. Add expenses to see settlement suggestions.")
                }
                uiState.isAllSettled -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = "Everyone is settled"
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            text = "Everyone is settled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                else -> {
                    uiState.outstandingSettlements.forEach { settlement ->
                        OutstandingSettlementRow(
                            settlement = settlement,
                            currencyCode = uiState.currencyCode,
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OutstandingSettlementRow(
    settlement: SettlementTransaction,
    currencyCode: String,
) {
    val amountFormatted = MoneyFormatter.format(settlement.amountMinorUnits, currencyCode)
    val accessibleDescription =
        "${settlement.fromMemberName} owes ${settlement.toMemberName} $amountFormatted"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleDescription
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Payments,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
                // "[Debtor] owes [Creditor]" — matches spec wording
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = settlement.fromMemberName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = settlement.toMemberName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Text(
                    text = "${settlement.fromMemberName} owes $amountFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AmountText(
            amountMinorUnits = settlement.amountMinorUnits,
            currencyCode = currencyCode,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

// ── Recent expenses section ───────────────────────────────────────────────────

@Composable
private fun RecentExpensesSection(
    uiState: SummaryUiState,
    onViewExpenses: () -> Unit,
    onAddExpense: () -> Unit,
) {
    SectionCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeader(
                title = "Recent expenses",
                actionLabel = "View all expenses",
                onAction = onViewExpenses,
            )

            if (!uiState.hasExpenses) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "No expenses yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Expenses will appear here once they are added to the group.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = onAddExpense,
                        modifier = Modifier
                            .height(48.dp)
                            .semantics { contentDescription = "Add first expense" },
                    ) { Text("Add Expense") }
                }
            } else {
                uiState.recentExpenses.forEach { item ->
                    RecentExpenseRow(item = item, currencyCode = uiState.currencyCode)
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentExpenseRow(
    item: RecentExpenseItem,
    currencyCode: String,
) {
    val dateStr = remember(item.dateMillis) {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(item.dateMillis))
    }
    val amountFormatted = MoneyFormatter.format(item.amountMinorUnits, currencyCode)
    val accessibleDescription =
        "${item.description}, $amountFormatted, paid by ${item.payerName} on $dateStr"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleDescription
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Paid by ${item.payerName} · $dateStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        AmountText(
            amountMinorUnits = item.amountMinorUnits,
            currencyCode = currencyCode,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Shared sub-components ─────────────────────────────────────────────────────

/** Material 3 card used as each section's container. */
@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        content()
    }
}

/**
 * Row with a section title on the left and a text action link on the right.
 * Both elements form a clear 48dp-minimum touch target area via the button.
 */
@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(
            onClick = onAction,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier
                .height(36.dp)
                .semantics { contentDescription = actionLabel },
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun EmptyStateText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
