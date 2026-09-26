package com.splitease.presentation.screens.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.splitease.domain.model.ExpenseCategory
import com.splitease.domain.model.Member
import com.splitease.domain.model.SplitMethod
import com.splitease.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditExpenseScreen(
    viewModel: AddEditExpenseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateAfterSave: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Navigate after successful save
    LaunchedEffect(uiState.savedExpenseId) {
        if (uiState.savedExpenseId != null) {
            viewModel.onNavigationConsumed()
            onNavigateAfterSave()
        }
    }

    // Surface repository/generic errors as snackbar
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
                        text = uiState.formTitle,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = "Loading expense form" },
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Description ───────────────────────────────────────────────────
            SectionLabel("Description")
            OutlinedTextField(
                value = uiState.descriptionInput,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("What was it for? *") },
                placeholder = { Text("e.g. Lunch at dhaba") },
                supportingText = {
                    if (uiState.descriptionError != null) {
                        ErrorText(uiState.descriptionError!!)
                    } else {
                        Text("Required")
                    }
                },
                isError = uiState.descriptionError != null,
                singleLine = true,
                enabled = !uiState.isSaving,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Description field" },
            )

            // ── Amount ────────────────────────────────────────────────────────
            SectionLabel("Amount")
            OutlinedTextField(
                value = uiState.amountInput,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Amount *") },
                placeholder = { Text("0.00") },
                prefix = { Text(currencySymbol(uiState.currencyCode)) },
                supportingText = {
                    if (uiState.amountError != null) {
                        ErrorText(uiState.amountError!!)
                    } else {
                        Text("Required · ${uiState.currencyCode}")
                    }
                },
                isError = uiState.amountError != null,
                singleLine = true,
                enabled = !uiState.isSaving,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Amount field" },
            )

            // ── Category ──────────────────────────────────────────────────────
            SectionLabel("Category")
            CategoryDropdown(
                selected = uiState.category,
                onSelected = viewModel::onCategorySelected,
                enabled = !uiState.isSaving,
            )

            // ── Payer ─────────────────────────────────────────────────────────
            SectionLabel("Paid by")
            if (!uiState.hasMembers) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        text = "This group has no members. Add members before recording expenses.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            } else {
                PayerSelector(
                    members = uiState.groupMembers,
                    selectedPayerId = uiState.selectedPayerId,
                    onPayerSelected = viewModel::onPayerSelected,
                    enabled = !uiState.isSaving,
                    error = uiState.payerError,
                )
            }

            // ── Participants ──────────────────────────────────────────────────
            SectionLabel("Participants")
            ParticipantsSelector(
                members = uiState.groupMembers,
                selectedIds = uiState.selectedParticipantIds,
                onToggle = viewModel::onParticipantToggled,
                enabled = !uiState.isSaving,
                error = uiState.participantsError,
            )

            // ── Split method ──────────────────────────────────────────────────
            SectionLabel("Split method")
            SplitMethodSelector(
                selected = uiState.splitMethod,
                onSelected = viewModel::onSplitMethodSelected,
                enabled = !uiState.isSaving,
            )

            // ── Split inputs ──────────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            SplitInputSection(
                uiState = uiState,
                onExactChanged = viewModel::onExactAmountChanged,
                onPercentageChanged = viewModel::onPercentageChanged,
                onSharesChanged = viewModel::onSharesChanged,
            )

            // ── Split validation summary ──────────────────────────────────────
            if (uiState.splitValidationMessage.isNotBlank() || uiState.splitError != null) {
                SplitValidationCard(uiState = uiState)
            }

            // ── Note (optional) ───────────────────────────────────────────────
            SectionLabel("Note (optional)")
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("Note") },
                placeholder = { Text("Any extra context") },
                supportingText = { Text("Optional") },
                singleLine = false,
                maxLines = 3,
                enabled = !uiState.isSaving,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Note field" },
            )

            Spacer(Modifier.height(16.dp))

            // ── Save button ───────────────────────────────────────────────────
            Button(
                onClick = viewModel::onSave,
                enabled = !uiState.isSaving && uiState.hasMembers,
                modifier = Modifier.fillMaxWidth().height(48.dp)
                    .semantics { contentDescription = "Save expense" },
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(if (uiState.isEditMode) "Save Changes" else "Add Expense")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun ErrorText(message: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Error,
            contentDescription = "Error",
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(4.dp))
        Text(text = message, color = MaterialTheme.colorScheme.error)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: ExpenseCategory,
    onSelected: (ExpenseCategory) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        OutlinedTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                .semantics { contentDescription = "Category: ${selected.displayName()}" },
            enabled = enabled,
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ExpenseCategory.entries.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.displayName()) },
                    onClick = {
                        onSelected(cat)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PayerSelector(
    members: List<Member>,
    selectedPayerId: Long,
    onPayerSelected: (Long) -> Unit,
    enabled: Boolean,
    error: String?,
) {
    Column {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            members.forEach { member ->
                val isSelected = member.id == selectedPayerId
                FilterChip(
                    selected = isSelected,
                    onClick = { if (enabled) onPayerSelected(member.id) },
                    label = { Text(member.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.semantics {
                        contentDescription = member.name
                        stateDescription = if (isSelected) "selected as payer" else "not selected as payer"
                        role = Role.RadioButton
                    },
                )
            }
        }
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            ErrorText(error)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParticipantsSelector(
    members: List<Member>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    enabled: Boolean,
    error: String?,
) {
    Column {
        if (members.isEmpty()) {
            Text(
                "No members in this group.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val selectedCount = selectedIds.size
            Text(
                "$selectedCount of ${members.size} selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                members.forEach { member ->
                    val isSelected = member.id in selectedIds
                    FilterChip(
                        selected = isSelected,
                        onClick = { if (enabled) onToggle(member.id) },
                        label = { Text(member.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.semantics {
                            contentDescription = member.name
                            stateDescription = if (isSelected) "included" else "not included"
                            role = Role.Checkbox
                        },
                    )
                }
            }
        }
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            ErrorText(error)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SplitMethodSelector(
    selected: SplitMethod,
    onSelected: (SplitMethod) -> Unit,
    enabled: Boolean,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SplitMethod.entries.forEach { method ->
            val isSelected = method == selected
            FilterChip(
                selected = isSelected,
                onClick = { if (enabled) onSelected(method) },
                label = { Text(method.displayName()) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                modifier = Modifier.semantics {
                    contentDescription = method.displayName()
                    stateDescription = if (isSelected) "selected" else "not selected"
                    role = Role.RadioButton
                },
            )
        }
    }
}

@Composable
private fun SplitInputSection(
    uiState: AddEditExpenseUiState,
    onExactChanged: (Long, String) -> Unit,
    onPercentageChanged: (Long, String) -> Unit,
    onSharesChanged: (Long, String) -> Unit,
) {
    val participants = uiState.selectedParticipants
    if (participants.isEmpty()) return

    when (uiState.splitMethod) {
        SplitMethod.EQUAL -> EqualSplitPreview(
            participants = participants,
            preview = uiState.splitPreview,
            currencyCode = uiState.currencyCode,
        )
        SplitMethod.EXACT -> ExactSplitInputs(
            participants = participants,
            inputs = uiState.exactAmountInputs,
            preview = uiState.splitPreview,
            currencyCode = uiState.currencyCode,
            onChanged = onExactChanged,
            enabled = !uiState.isSaving,
        )
        SplitMethod.PERCENTAGE -> PercentageSplitInputs(
            participants = participants,
            inputs = uiState.percentageInputs,
            preview = uiState.splitPreview,
            currencyCode = uiState.currencyCode,
            onChanged = onPercentageChanged,
            enabled = !uiState.isSaving,
        )
        SplitMethod.SHARES -> SharesSplitInputs(
            participants = participants,
            inputs = uiState.sharesInputs,
            preview = uiState.splitPreview,
            currencyCode = uiState.currencyCode,
            onChanged = onSharesChanged,
            enabled = !uiState.isSaving,
        )
    }
}

@Composable
private fun EqualSplitPreview(
    participants: List<Member>,
    preview: Map<Long, Long>,
    currencyCode: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Each participant pays:", style = MaterialTheme.typography.labelMedium)
            participants.forEach { member ->
                val share = preview[member.id] ?: 0L
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = MoneyFormatter.format(share, currencyCode),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.semantics {
                            contentDescription = "${member.name} owes ${MoneyFormatter.format(share, currencyCode)}"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExactSplitInputs(
    participants: List<Member>,
    inputs: Map<Long, String>,
    preview: Map<Long, Long>,
    currencyCode: String,
    onChanged: (Long, String) -> Unit,
    enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        participants.forEach { member ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = inputs[member.id] ?: "",
                    onValueChange = { onChanged(member.id, it) },
                    placeholder = { Text("0.00") },
                    prefix = { Text(currencySymbol(currencyCode)) },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(130.dp).semantics {
                        contentDescription = "Amount for ${member.name}"
                    },
                )
            }
        }
    }
}

@Composable
private fun PercentageSplitInputs(
    participants: List<Member>,
    inputs: Map<Long, String>,
    preview: Map<Long, Long>,
    currencyCode: String,
    onChanged: (Long, String) -> Unit,
    enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        participants.forEach { member ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = inputs[member.id] ?: "",
                    onValueChange = { onChanged(member.id, it) },
                    placeholder = { Text("0.00") },
                    suffix = { Text("%") },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(130.dp).semantics {
                        contentDescription = "Percentage for ${member.name}"
                    },
                )
                if (preview[member.id] != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = MoneyFormatter.format(preview[member.id]!!, currencyCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "${member.name} owes ${MoneyFormatter.format(preview[member.id]!!, currencyCode)}"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SharesSplitInputs(
    participants: List<Member>,
    inputs: Map<Long, String>,
    preview: Map<Long, Long>,
    currencyCode: String,
    onChanged: (Long, String) -> Unit,
    enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        participants.forEach { member ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = inputs[member.id] ?: "1",
                    onValueChange = { onChanged(member.id, it) },
                    placeholder = { Text("1") },
                    suffix = { Text("share") },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(130.dp).semantics {
                        contentDescription = "Shares for ${member.name}"
                    },
                )
                if (preview[member.id] != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = MoneyFormatter.format(preview[member.id]!!, currencyCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "${member.name} owes ${MoneyFormatter.format(preview[member.id]!!, currencyCode)}"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitValidationCard(uiState: AddEditExpenseUiState) {
    val isValid = uiState.isSplitValid
    val containerColor = if (isValid) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (isValid) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = if (isValid) "Split valid" else "Split invalid",
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = uiState.splitValidationMessage,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                modifier = Modifier.semantics {
                    contentDescription = "Split status: ${uiState.splitValidationMessage}"
                },
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun currencySymbol(code: String): String =
    try { java.util.Currency.getInstance(code).symbol } catch (_: Exception) { code }

private fun ExpenseCategory.displayName(): String = when (this) {
    ExpenseCategory.FOOD -> "Food"
    ExpenseCategory.TRANSPORT -> "Transport"
    ExpenseCategory.ACCOMMODATION -> "Accommodation"
    ExpenseCategory.SHOPPING -> "Shopping"
    ExpenseCategory.ENTERTAINMENT -> "Entertainment"
    ExpenseCategory.UTILITIES -> "Utilities"
    ExpenseCategory.EDUCATION -> "Education"
    ExpenseCategory.OTHER -> "Other"
}

private fun SplitMethod.displayName(): String = when (this) {
    SplitMethod.EQUAL -> "Equal"
    SplitMethod.EXACT -> "Exact"
    SplitMethod.PERCENTAGE -> "Percentage"
    SplitMethod.SHARES -> "Shares"
}
