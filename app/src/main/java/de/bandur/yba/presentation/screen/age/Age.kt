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
package de.bandur.yba.presentation.screen.age

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.Vo2MaxRecord
import de.bandur.yba.R
import java.util.UUID

/**
 * Demonstrates the differential changes API.
 */
@Composable
fun DifferentialChangesScreen(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    latestVo2Max: Vo2MaxRecord?,
    userProfile: de.bandur.yba.data.profile.UserProfile?,
    uiState: DifferentialChangesViewModel.UiState,
    onError: (Throwable?) -> Unit = {},
    onPermissionsResult: () -> Unit = {},
    onPermissionsLaunch: (Set<String>) -> Unit = {}
) {

    // Remember the last error ID, such that it is possible to avoid re-launching the error
    // notification for the same error when the screen is recomposed, or configuration changes etc.
    val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }

    // Funktion zur Berechnung des biologischen Alters basierend auf VO₂max
    fun calculateBiologicalAge(chronologicalAge: Int, vo2Max: Double, gender: de.bandur.yba.data.profile.Gender): Double? {
        // Referenz VO₂max Werte basierend auf Geschlecht und Alter
        val referenceVo2Max = when (gender) {
            de.bandur.yba.data.profile.Gender.MALE -> when (chronologicalAge) {
                in 18..25 -> 47.0
                in 26..35 -> 44.0
                in 36..45 -> 41.0
                in 46..55 -> 38.0
                in 56..65 -> 35.0
                in 66..75 -> 32.0
                in 76..85 -> 29.0
                in 86..99 -> 26.0
                else -> return null
            }
            de.bandur.yba.data.profile.Gender.FEMALE -> when (chronologicalAge) {
                in 18..25 -> 38.0
                in 26..35 -> 34.0
                in 36..45 -> 31.0
                in 46..55 -> 28.0
                in 56..65 -> 25.0
                in 66..75 -> 22.0
                in 76..85 -> 19.0
                in 86..99 -> 16.0
                else -> return null
            }
        }
        
        // Durchschnittliche Abnahme pro Jahr: ca. 0.5 ml/kg/min
        val decreasePerYear = 0.5
        
        // Berechnung: Chronologisches Alter + (Referenz VO₂max - Aktueller VO₂max) / Abnahme pro Jahr
        return chronologicalAge + (referenceVo2Max - vo2Max) / decreasePerYear
    }

    LaunchedEffect(uiState) {
        // If the initial data load has not taken place, attempt to load the data.
        if (uiState is DifferentialChangesViewModel.UiState.Uninitialized) {
            onPermissionsResult()
        }

        // The [DifferentialChangesViewModel.UiState] provides details of whether the last action
        // was a success or resulted in an error. Where an error occurred, for example in reading
        // and writing to Health Connect, the user is notified, and where the error is one that can
        // be recovered from, an attempt to do so is made.
        if (uiState is DifferentialChangesViewModel.UiState.Error && errorId.value != uiState.uuid) {
            onError(uiState.exception)
            errorId.value = uiState.uuid
        }
    }

    if (uiState != DifferentialChangesViewModel.UiState.Uninitialized) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profil-Informationen anzeigen
            item {
                if (userProfile?.age != null && userProfile.gender != null && userProfile.birthDate != null) {
                    Text(
                        text = "Profil: ${userProfile.age} Jahre (geboren ${userProfile.birthDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))}), ${
                            when (userProfile.gender) {
                                de.bandur.yba.data.profile.Gender.MALE -> "Männlich"
                                de.bandur.yba.data.profile.Gender.FEMALE -> "Weiblich"
                            }
                        }",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(8.dp)
                    )
                    
                    // Biologisches Alter berechnen und anzeigen
                    latestVo2Max?.let { vo2Max ->
                        val currentAge = userProfile.age // Local copy to avoid smart cast issues
                        val currentGender = userProfile.gender
                        
                        if (currentAge != null && currentGender != null) {
                            val biologicalAge = calculateBiologicalAge(
                                currentAge, 
                                vo2Max.vo2MillilitersPerMinuteKilogram,
                                currentGender
                            )
                            
                            biologicalAge?.let { bioAge ->
                                Text(
                                    text = "Biologisches Alter: ${String.format("%.1f", bioAge)} Jahre",
                                    style = MaterialTheme.typography.h5,
                                    modifier = Modifier.padding(8.dp)
                                )
                                
                                val ageDifference = bioAge - currentAge
                                val differenceColor = if (ageDifference > 0) {
                                    MaterialTheme.colors.error
                                } else {
                                    MaterialTheme.colors.primary
                                }
                                
                                val differenceText = if (ageDifference > 0) {
                                    "Du bist ${String.format("%.1f", ageDifference)} Jahre älter als dein chronologisches Alter"
                                } else {
                                    "Du bist ${String.format("%.1f", kotlin.math.abs(ageDifference))} Jahre jünger als dein chronologisches Alter"
                                }
                                
                                Text(
                                    text = differenceText,
                                    color = differenceColor,
                                    style = MaterialTheme.typography.body1,
                                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Bitte gehen Sie zum Profil-Menü und geben Sie Ihr Geburtsdatum und Geschlecht ein.",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (!permissionsGranted) {
                item {
                    Button(
                        onClick = {
                            onPermissionsLaunch(permissions)
                        }
                    ) {
                        Text(text = stringResource(R.string.permissions_button_label))
                    }
                }
            } else {
                item {
                    val vo2MaxText = latestVo2Max?.let { record ->
                        "Latest VO2 Max: ${String.format("%.1f", record.vo2MillilitersPerMinuteKilogram)} ml/kg/min"
                    } ?: "Latest VO2 Max: Not available"
                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = vo2MaxText
                    )
                }
            }
        }
    }
}