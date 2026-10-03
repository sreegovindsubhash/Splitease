package com.splitease.presentation.screens.reminders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.domain.model.Reminder
import com.splitease.domain.usecase.AddReminderUseCase
import com.splitease.domain.usecase.CompleteReminderUseCase
import com.splitease.domain.usecase.DeleteReminderUseCase
import com.splitease.domain.usecase.GetRemindersUseCase
import com.splitease.domain.usecase.UpdateReminderUseCase
import com.splitease.notifications.ReminderNotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class RemindersViewModel(
    private val getRemindersUseCase: GetRemindersUseCase,
    private val addReminderUseCase: AddReminderUseCase,
    private val updateReminderUseCase: UpdateReminderUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase,
    private val completeReminderUseCase: CompleteReminderUseCase,
    private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    init {
        getRemindersUseCase()
            .onEach { all ->
                val (completed, active) = all.partition { it.isCompleted }
                _uiState.update { it.copy(isLoading = false, activeReminders = active, completedReminders = completed) }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error") }
            }
            .launchIn(viewModelScope)
    }

    // ── Dialog open / close ───────────────────────────────────────────────────

    fun onAddReminderClick() {
        _uiState.update { it.copy(dialogState = ReminderDialogState()) }
    }

    fun onEditReminderClick(reminder: Reminder) {
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(reminder.scheduledAt), ZoneId.systemDefault())
        _uiState.update {
            it.copy(
                dialogState = ReminderDialogState(
                    editingReminder = reminder,
                    titleInput = reminder.title,
                    dateDisplay = zdt.format(dateFormatter),
                    timeDisplay = zdt.format(timeFormatter),
                    scheduledAt = reminder.scheduledAt,
                    noteInput = reminder.note,
                ),
            )
        }
    }

    fun onDismissDialog() {
        _uiState.update { it.copy(dialogState = null) }
    }

    // ── Dialog field changes ──────────────────────────────────────────────────

    fun onTitleChange(value: String) {
        _uiState.update { state ->
            state.copy(
                dialogState = state.dialogState?.copy(
                    titleInput = value,
                    titleError = if (value.isBlank()) "Title is required" else null,
                ),
            )
        }
    }

    fun onNoteChange(value: String) {
        _uiState.update { state ->
            state.copy(dialogState = state.dialogState?.copy(noteInput = value))
        }
    }

    /**
     * Called by the UI after the user picks a date from the DatePicker.
     * [epochDay] is the day returned by the Material3 DatePickerState (epoch day, not millis).
     */
    fun onDateSelected(epochDay: Long) {
        val date = LocalDate.ofEpochDay(epochDay)
        _uiState.update { state ->
            val dialog = state.dialogState ?: return@update state
            val existing = assembleScheduledAt(date, extractTime(dialog.scheduledAt))
            state.copy(
                dialogState = dialog.copy(
                    dateDisplay = date.format(dateFormatter),
                    scheduledAt = existing,
                    scheduledAtError = null,
                ),
            )
        }
    }

    /**
     * Called by the UI after the user picks a time from the TimePicker.
     * [hour] and [minute] are 0-based values from the TimePicker state.
     */
    fun onTimeSelected(hour: Int, minute: Int) {
        val time = LocalTime.of(hour, minute)
        _uiState.update { state ->
            val dialog = state.dialogState ?: return@update state
            val existing = assembleScheduledAt(extractDate(dialog.scheduledAt), time)
            state.copy(
                dialogState = dialog.copy(
                    timeDisplay = time.format(timeFormatter),
                    scheduledAt = existing,
                    scheduledAtError = null,
                ),
            )
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun onSave() {
        val dialog = _uiState.value.dialogState ?: return
        val titleError = if (dialog.titleInput.isBlank()) "Title is required" else null
        val scheduledAtError = when {
            dialog.scheduledAt <= 0L -> "Please select a date and time"
            else -> null
        }

        if (titleError != null || scheduledAtError != null) {
            _uiState.update { it.copy(dialogState = dialog.copy(titleError = titleError, scheduledAtError = scheduledAtError)) }
            return
        }

        _uiState.update { it.copy(dialogState = dialog.copy(isSaving = true)) }

        viewModelScope.launch {
            try {
                if (dialog.isEditMode) {
                    val updated = dialog.editingReminder!!.copy(
                        title = dialog.titleInput.trim(),
                        scheduledAt = dialog.scheduledAt,
                        note = dialog.noteInput.trim(),
                    )
                    // Cancel old alarm before updating so stale alarm is removed.
                    ReminderNotificationScheduler.cancel(appContext, updated.id)
                    updateReminderUseCase(updated)
                    // Re-schedule with updated time only if still active and future.
                    if (!updated.isCompleted) {
                        ReminderNotificationScheduler.schedule(appContext, updated)
                    }
                    _uiState.update { it.copy(dialogState = null, snackbarMessage = "Reminder updated") }
                } else {
                    val reminder = Reminder(
                        title = dialog.titleInput.trim(),
                        scheduledAt = dialog.scheduledAt,
                        note = dialog.noteInput.trim(),
                    )
                    val id = addReminderUseCase(reminder)
                    ReminderNotificationScheduler.schedule(appContext, reminder.copy(id = id))
                    _uiState.update { it.copy(dialogState = null, snackbarMessage = "Reminder added") }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(dialogState = dialog.copy(isSaving = false), snackbarMessage = e.message)
                }
            }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun onDeleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            ReminderNotificationScheduler.cancel(appContext, reminder.id)
            deleteReminderUseCase(reminder)
            _uiState.update { it.copy(snackbarMessage = "Reminder deleted") }
        }
    }

    // ── Complete ──────────────────────────────────────────────────────────────

    fun onCompleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            ReminderNotificationScheduler.cancel(appContext, reminder.id)
            completeReminderUseCase(reminder)
        }
    }

    // ── Snackbar consumed ─────────────────────────────────────────────────────

    fun onSnackbarMessageConsumed() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun assembleScheduledAt(date: LocalDate, time: LocalTime): Long =
        ZonedDateTime.of(date, time, ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun extractDate(epochMs: Long): LocalDate =
        if (epochMs <= 0L) LocalDate.now()
        else Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun extractTime(epochMs: Long): LocalTime =
        if (epochMs <= 0L) LocalTime.of(9, 0)
        else Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalTime()
}
