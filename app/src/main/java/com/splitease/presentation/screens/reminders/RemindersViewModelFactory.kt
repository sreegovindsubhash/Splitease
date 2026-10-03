package com.splitease.presentation.screens.reminders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.splitease.domain.repository.ReminderRepository
import com.splitease.domain.usecase.AddReminderUseCase
import com.splitease.domain.usecase.CompleteReminderUseCase
import com.splitease.domain.usecase.DeleteReminderUseCase
import com.splitease.domain.usecase.GetRemindersUseCase
import com.splitease.domain.usecase.UpdateReminderUseCase

class RemindersViewModelFactory(
    private val reminderRepository: ReminderRepository,
    private val appContext: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RemindersViewModel::class.java)) {
            return RemindersViewModel(
                getRemindersUseCase = GetRemindersUseCase(reminderRepository),
                addReminderUseCase = AddReminderUseCase(reminderRepository),
                updateReminderUseCase = UpdateReminderUseCase(reminderRepository),
                deleteReminderUseCase = DeleteReminderUseCase(reminderRepository),
                completeReminderUseCase = CompleteReminderUseCase(reminderRepository),
                appContext = appContext,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
