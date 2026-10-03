package com.splitease.presentation.screens.reminders

import com.splitease.domain.model.Reminder

/**
 * UI state for the Reminders screen.
 */
data class RemindersUiState(
    val isLoading: Boolean = true,
    val activeReminders: List<Reminder> = emptyList(),
    val completedReminders: List<Reminder> = emptyList(),
    val errorMessage: String? = null,
    /** Controls the add/edit dialog. Null = hidden, non-null = shown. */
    val dialogState: ReminderDialogState? = null,
    /** One-shot: set when a save/delete operation completes to show a snackbar. */
    val snackbarMessage: String? = null,
)

/**
 * State driving the Add/Edit reminder dialog.
 *
 * [editingReminder] is the original reminder when editing; null when adding.
 */
data class ReminderDialogState(
    val editingReminder: Reminder? = null,
    val titleInput: String = "",
    val titleError: String? = null,
    /** Display text of the selected date, e.g. "15 Jan 2026". */
    val dateDisplay: String = "",
    /** Display text of the selected time, e.g. "09:30". */
    val timeDisplay: String = "",
    /** Epoch millis assembled from the picker selections. 0 = not yet set. */
    val scheduledAt: Long = 0L,
    val scheduledAtError: String? = null,
    val noteInput: String = "",
    val isSaving: Boolean = false,
) {
    val isEditMode: Boolean get() = editingReminder != null
    val dialogTitle: String get() = if (isEditMode) "Edit Reminder" else "Add Reminder"
}
