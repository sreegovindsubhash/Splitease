package com.splitease.presentation.screens.balances

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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.splitease.domain.model.MemberBalance
import com.splitease.presentation.components.AmountText
import com.splitease.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalancesScreen(
    viewModel: BalancesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettlement: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.onErrorDismissed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.groupName.isNotBlank()) uiState.groupName else "Balances",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Back to group details" },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
            uiState.isLoading -> BalancesLoadingContent(paddingValues)
            uiState.groupNotFound -> BalancesGroupNotFoundContent(paddingValues, onNavigateBack)
            uiState.errorMessage != null && uiState.balances.isEmpty() ->
                BalancesErrorContent(paddingValues, uiState.errorMessage!!, onNavigateBack)
            uiState.isEmpty -> BalancesEmptyContent(paddingValues)
            else -> BalancesListContent(
                uiState = uiState,
                paddingValues = paddingValues,
                onNavigateToSettlement = onNavigateToSettlement,
            )
        }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun BalancesLoadingContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = "Loading balances" },
        )
    }
}

// ── Group not found ───────────────────────────────────────────────────────────

@Composable
private fun BalancesGroupNotFoundContent(
    paddingValues: PaddingValues,
    onNavigateBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.AccountBalance,
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
private fun BalancesErrorContent(
    paddingValues: PaddingValues,
    message: String,
    onNavigateBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp),
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

// ── Empty ─────────────────────────────────────────────────────────────────────

@Composable
private fun BalancesEmptyContent(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text("No balances yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add expenses to see who owes what.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Populated ─────────────────────────────────────────────────────────────────

@Composable
private fun BalancesListContent(
    uiState: BalancesUiState,
    paddingValues: PaddingValues,
    onNavigateToSettlement: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── Summary header card ───────────────────────────────────────────────
        item {
            BalancesSummaryCard(uiState = uiState)
        }

        // ── Per-member cards ──────────────────────────────────────────────────
        // Zip raw balance (for Paid / Share rows) with its adjusted counterpart
        // (for the net row).  The two lists are always in the same member order
        // produced by CalculateMemberBalancesUseCase.
        val adjustedByMember = uiState.adjustedBalances.associateBy { it.memberId }
        items(uiState.balances, key = { it.memberId }) { balance ->
            MemberBalanceCard(
                balance = balance,
                adjustedNet = adjustedByMember[balance.memberId]?.adjustedNetMinorUnits
                    ?: balance.netMinorUnits,
                currencyCode = uiState.currencyCode,
            )
        }

        // ── Settle Up CTA ─────────────────────────────────────────────────────
        item {
            Button(
                onClick = onNavigateToSettlement,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics { contentDescription = "See settlement suggestions" },
            ) {
                Text("Settle Up")
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Summary card ──────────────────────────────────────────────────────────────

@Composable
private fun BalancesSummaryCard(uiState: BalancesUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Group Balances",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total spent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                AmountText(
                    amountMinorUnits = uiState.totalSpendMinorUnits,
                    currencyCode = uiState.currencyCode,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val creditText = when (uiState.creditorsCount) {
                    0 -> "Nobody is owed"
                    1 -> "1 person is owed"
                    else -> "${uiState.creditorsCount} people are owed"
                }
                val debtText = when (uiState.debtorsCount) {
                    0 -> "Nobody owes"
                    1 -> "1 person owes"
                    else -> "${uiState.debtorsCount} people owe"
                }
                Text(
                    text = "$creditText · $debtText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}

// ── Per-member card ───────────────────────────────────────────────────────────

@Composable
private fun MemberBalanceCard(
    balance: MemberBalance,
    /** Adjusted net after settlement payments — drives the net row and accessibility label. */
    adjustedNet: Long,
    currencyCode: String,
) {
    val balanceState = when {
        adjustedNet > 0L -> BalanceState.OWED
        adjustedNet < 0L -> BalanceState.OWES
        else -> BalanceState.SETTLED
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = memberBalanceDescription(balance, adjustedNet, currencyCode)
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Member name
            Text(
                text = balance.memberName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Paid / Share rows
            LabeledAmountRow(
                label = "Paid",
                amountMinorUnits = balance.totalPaidMinorUnits,
                currencyCode = currencyCode,
            )
            LabeledAmountRow(
                label = "Share",
                amountMinorUnits = balance.totalOwedMinorUnits,
                currencyCode = currencyCode,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Net balance — driven by ADJUSTED net (after settlement payments)
            NetBalanceRow(
                balanceState = balanceState,
                netMinorUnits = adjustedNet,
                currencyCode = currencyCode,
            )
        }
    }
}

@Composable
private fun LabeledAmountRow(
    label: String,
    amountMinorUnits: Long,
    currencyCode: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AmountText(
            amountMinorUnits = amountMinorUnits,
            currencyCode = currencyCode,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun NetBalanceRow(
    balanceState: BalanceState,
    netMinorUnits: Long,
    currencyCode: String,
) {
    val (label, icon, iconTint) = when (balanceState) {
        BalanceState.OWED -> Triple(
            "Is owed",
            Icons.Default.ArrowUpward,
            MaterialTheme.colorScheme.tertiary,
        )
        BalanceState.OWES -> Triple(
            "Owes",
            Icons.Default.ArrowDownward,
            MaterialTheme.colorScheme.error,
        )
        BalanceState.SETTLED -> Triple(
            "Settled",
            Icons.Default.CheckCircle,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = iconTint,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (balanceState == BalanceState.SETTLED) {
            Text(
                text = MoneyFormatter.format(0L, currencyCode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // Show absolute value — the label ("Is owed" / "Owes") already communicates direction
            AmountText(
                amountMinorUnits = kotlin.math.abs(netMinorUnits),
                currencyCode = currencyCode,
                style = MaterialTheme.typography.bodyMedium,
                color = if (balanceState == BalanceState.OWED)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.error,
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private enum class BalanceState { OWED, OWES, SETTLED }

private fun memberBalanceDescription(
    balance: MemberBalance,
    adjustedNet: Long,
    currencyCode: String,
): String {
    val netFormatted = MoneyFormatter.format(kotlin.math.abs(adjustedNet), currencyCode)
    val paidFormatted = MoneyFormatter.format(balance.totalPaidMinorUnits, currencyCode)
    val owedFormatted = MoneyFormatter.format(balance.totalOwedMinorUnits, currencyCode)
    val statusText = when {
        adjustedNet > 0L -> "is owed $netFormatted"
        adjustedNet < 0L -> "owes $netFormatted"
        else -> "is settled"
    }
    return "${balance.memberName}: paid $paidFormatted, share $owedFormatted, $statusText"
}
