package com.splitease.presentation.screens.reminders

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.splitease.domain.model.Reminder
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Request POST_NOTIFICATIONS permission on Android 13+.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { /* Result ignored — notification permission is advisory */ }
        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.onSnackbarMessageConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Reminders",
                        style = MaterialTheme.typography.titleLarge,
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
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onAddReminderClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.semantics { contentDescription = "Add reminder" },
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingState(paddingValues)
            uiState.activeReminders.isEmpty() && uiState.completedReminders.isEmpty() ->
                EmptyState(paddingValues, onAddClick = viewModel::onAddReminderClick)
            else -> RemindersList(
                activeReminders = uiState.activeReminders,
                completedReminders = uiState.completedReminders,
                onEdit = viewModel::onEditReminderClick,
                onDelete = viewModel::onDeleteReminder,
                onComplete = viewModel::onCompleteReminder,
                paddingValues = paddingValues,
            )
        }
    }

    // Add/Edit dialog
    uiState.dialogState?.let { dialog ->
        ReminderDialog(
            state = dialog,
            onTitleChange = viewModel::onTitleChange,
            onNoteChange = viewModel::onNoteChange,
            onDateSelected = viewModel::onDateSelected,
            onTimeSelected = viewModel::onTimeSelected,
            onSave = viewModel::onSave,
            onDismiss = viewModel::onDismissDialog,
        )
    }
}

// ── Loading ────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingState(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = "Loading reminders" },
        )
    }
}

// ── Empty ──────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(paddingValues: PaddingValues, onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No reminders yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap + to add a reminder and get notified when it's time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── List ───────────────────────────────────────────────────────────────────────

@Composable
private fun RemindersList(
    activeReminders: List<Reminder>,
    completedReminders: List<Reminder>,
    onEdit: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    onComplete: (Reminder) -> Unit,
    paddingValues: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (activeReminders.isNotEmpty()) {
            item {
                SectionHeader(text = "Upcoming")
            }
            items(activeReminders, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onEdit = { onEdit(reminder) },
                    onDelete = { onDelete(reminder) },
                    onComplete = { onComplete(reminder) },
                )
            }
        }
        if (completedReminders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(text = "Completed")
            }
            items(completedReminders, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onEdit = { onEdit(reminder) },
                    onDelete = { onDelete(reminder) },
                    onComplete = null,
                )
            }
        }
        // Bottom padding for FAB
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onComplete: (() -> Unit)?,
) {
    val completedAlpha = if (reminder.isCompleted) 0.6f else 1f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = buildString {
                    append(reminder.title)
                    append(", scheduled ")
                    append(formatEpochForAccessibility(reminder.scheduledAt))
                    if (reminder.isCompleted) append(", completed")
                    if (reminder.note.isNotBlank()) { append(", note: "); append(reminder.note) }
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (reminder.isCompleted) 0.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Completion icon
            if (reminder.isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = completedAlpha),
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = completedAlpha),
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatEpochForDisplay(reminder.scheduledAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = completedAlpha),
                    )
                }
                if (reminder.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = reminder.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = completedAlpha),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // Actions
            if (onComplete != null) {
                IconButton(
                    onClick = onComplete,
                    modifier = Modifier.semantics { contentDescription = "Mark as completed: ${reminder.title}" },
                ) {
                    Icon(
                        Icons.Default.AlarmOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (!reminder.isCompleted) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.semantics { contentDescription = "Edit reminder: ${reminder.title}" },
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.semantics { contentDescription = "Delete reminder: ${reminder.title}" },
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// ── Add / Edit dialog ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDialog(
    state: ReminderDialogState,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDateSelected: (Long) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        title = { Text(state.dialogTitle) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Title
                OutlinedTextField(
                    value = state.titleInput,
                    onValueChange = onTitleChange,
                    label = { Text("Title *") },
                    placeholder = { Text("e.g. Pay electricity bill") },
                    supportingText = {
                        if (state.titleError != null) {
                            ErrorText(state.titleError)
                        } else {
                            Text("Required")
                        }
                    },
                    isError = state.titleError != null,
                    singleLine = true,
                    enabled = !state.isSaving,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Reminder title field" },
                )

                // Date picker trigger
                OutlinedTextField(
                    value = state.dateDisplay,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date *") },
                    placeholder = { Text("Select a date") },
                    supportingText = {
                        if (state.scheduledAtError != null) {
                            ErrorText(state.scheduledAtError)
                        }
                    },
                    isError = state.scheduledAtError != null,
                    trailingIcon = {
                        IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.semantics { contentDescription = "Pick date" },
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    },
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Date: ${state.dateDisplay.ifBlank { "not selected" }}" },
                )

                // Time picker trigger
                OutlinedTextField(
                    value = state.timeDisplay,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Time *") },
                    placeholder = { Text("Select a time") },
                    supportingText = {
                        if (state.scheduledAtError != null && state.dateDisplay.isNotBlank()) {
                            ErrorText(state.scheduledAtError)
                        }
                    },
                    isError = state.scheduledAtError != null && state.dateDisplay.isNotBlank(),
                    trailingIcon = {
                        IconButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.semantics { contentDescription = "Pick time" },
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                        }
                    },
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Time: ${state.timeDisplay.ifBlank { "not selected" }}" },
                )

                // Note (optional)
                OutlinedTextField(
                    value = state.noteInput,
                    onValueChange = onNoteChange,
                    label = { Text("Note (optional)") },
                    placeholder = { Text("Any extra context") },
                    supportingText = { Text("Optional") },
                    singleLine = false,
                    maxLines = 3,
                    enabled = !state.isSaving,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Note field" },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier.semantics { contentDescription = if (state.isEditMode) "Save changes" else "Add reminder" },
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(if (state.isEditMode) "Save" else "Add")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isSaving,
            ) {
                Text("Cancel")
            }
        },
    )

    // Date picker dialog
    if (showDatePicker) {
        val initialEpochDay = if (state.scheduledAt > 0L) {
            ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(state.scheduledAt),
                ZoneId.systemDefault(),
            ).toLocalDate().toEpochDay()
        } else {
            null
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEpochDay?.let { it * 86_400_000L },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // DatePickerState gives UTC midnight millis; convert to epoch day
                            onDateSelected(millis / 86_400_000L)
                        }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val initialHour: Int
        val initialMinute: Int
        if (state.scheduledAt > 0L) {
            val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(state.scheduledAt), ZoneId.systemDefault())
            initialHour = zdt.hour
            initialMinute = zdt.minute
        } else {
            initialHour = 9
            initialMinute = 0
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select time") },
            text = {
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.semantics { contentDescription = "Time picker" },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
        )
    }
}

// ── Helper composable ─────────────────────────────────────────────────────────

@Composable
private fun ErrorText(message: String) {
    Text(text = message, color = MaterialTheme.colorScheme.error)
}

// ── Formatting helpers ────────────────────────────────────────────────────────

private val DISPLAY_FORMATTER = java.time.format.DateTimeFormatter.ofPattern(
    "d MMM yyyy, HH:mm",
    java.util.Locale.getDefault(),
)

private val ACCESSIBILITY_FORMATTER = java.time.format.DateTimeFormatter.ofPattern(
    "EEEE d MMMM yyyy 'at' HH:mm",
    java.util.Locale.getDefault(),
)

private fun formatEpochForDisplay(epochMs: Long): String =
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault())
        .format(DISPLAY_FORMATTER)

private fun formatEpochForAccessibility(epochMs: Long): String =
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault())
        .format(ACCESSIBILITY_FORMATTER)
