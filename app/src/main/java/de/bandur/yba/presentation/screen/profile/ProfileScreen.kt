/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.bandur.yba.presentation.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.bandur.yba.R
import de.bandur.yba.data.profile.Gender
import java.util.UUID

@Composable
fun ProfileScreen(
    onError: (Throwable?) -> Unit = {},
    onSaved: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val birthDateInput by viewModel.birthDateInput.collectAsState()
    val selectedGender by viewModel.selectedGender.collectAsState()

    // Remember the last error ID, such that it is possible to avoid re-launching the error
    // notification for the same error when the screen is recomposed, or configuration changes etc.
    val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }
    val savedId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }

    LaunchedEffect(uiState) {
        when (val currentState = uiState) {
            is ProfileViewModel.UiState.Error -> {
                if (errorId.value != currentState.uuid) {
                    onError(currentState.exception)
                    errorId.value = currentState.uuid
                }
            }
            is ProfileViewModel.UiState.Saved -> {
                if (savedId.value != currentState.uuid) {
                    onSaved()
                    savedId.value = currentState.uuid
                }
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Geburtsdatum Input
                OutlinedTextField(
                    value = birthDateInput,
                    onValueChange = viewModel::updateBirthDate,
                    label = { Text(stringResource(R.string.profile_birth_date_label)) },
                    isError = birthDateInput.isNotEmpty() && !viewModel.isValidBirthDate(birthDateInput),
                    placeholder = { Text("01.01.1990") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (birthDateInput.isNotEmpty() && !viewModel.isValidBirthDate(birthDateInput)) {
                    Text(
                        text = stringResource(R.string.profile_birth_date_error),
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Geschlecht Auswahl
                Text(
                    text = stringResource(R.string.profile_gender_label),
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Männlich RadioButton
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedGender == Gender.MALE,
                            onClick = { viewModel.updateGender(Gender.MALE) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedGender == Gender.MALE,
                        onClick = null
                    )
                    Text(
                        text = stringResource(R.string.profile_gender_male),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Weiblich RadioButton
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedGender == Gender.FEMALE,
                            onClick = { viewModel.updateGender(Gender.FEMALE) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedGender == Gender.FEMALE,
                        onClick = null
                    )
                    Text(
                        text = stringResource(R.string.profile_gender_female),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (selectedGender == null && uiState is ProfileViewModel.UiState.Error) {
                    Text(
                        text = stringResource(R.string.profile_gender_error),
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Speichern Button
        Button(
            onClick = { viewModel.saveProfile() },
            enabled = viewModel.isValidBirthDate(birthDateInput) && selectedGender != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.profile_save_button))
        }

        // Status-Anzeige
        when (val currentState = uiState) {
            is ProfileViewModel.UiState.Loading -> {
                Text(
                    text = "Lädt...",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            is ProfileViewModel.UiState.Saved -> {
                Text(
                    text = stringResource(R.string.profile_saved_message),
                    color = MaterialTheme.colors.primary,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            else -> {}
        }
    }
}