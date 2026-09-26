package com.splitease.presentation.screens.editGroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGroupScreen(
    viewModel: EditGroupViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Navigate back once the save completes
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.onSavedConsumed()
            onNavigateBack()
        }
    }

    // Show errors in the snackbar
    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Group",
                        style = MaterialTheme.typography.titleLarge,
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
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Group name — required
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Group name *") },
                placeholder = { Text("e.g. Goa Trip") },
                supportingText = {
                    val error = uiState.nameError
                    if (error != null) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        Text("Required")
                    }
                },
                isError = uiState.nameError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Group name field" },
                enabled = !uiState.isSaving && !uiState.isLoading,
            )

            // Description — optional
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description") },
                placeholder = { Text("e.g. Split expenses for the trip") },
                supportingText = { Text("Optional") },
                singleLine = false,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.onSave()
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Group description field" },
                enabled = !uiState.isSaving && !uiState.isLoading,
            )

            // Currency — read-only, informational only
            OutlinedTextField(
                value = uiState.currencyCode,
                onValueChange = {},
                label = { Text("Currency") },
                supportingText = { Text("Cannot be changed after group creation") },
                singleLine = true,
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Currency code — read only" },
                enabled = false,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save button
            Button(
                onClick = viewModel::onSave,
                enabled = !uiState.isSaving && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics { contentDescription = "Save group button" },
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .height(20.dp)
                            .align(Alignment.CenterVertically),
                    )
                } else {
                    Text(
                        text = "Save Changes",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
