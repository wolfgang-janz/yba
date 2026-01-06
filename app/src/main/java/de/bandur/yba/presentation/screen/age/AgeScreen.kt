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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.units.Percentage
import de.bandur.yba.R
import de.bandur.yba.data.profile.Gender
import de.bandur.yba.data.profile.UserProfile
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Demonstrates the differential changes API.
 */
@Composable
fun AgeScreen(
    permissions: Set<String>,
    permissionsGranted: Boolean,
    latestVo2Max: Vo2MaxRecord?,
    latestRestingHeartRate: RestingHeartRateRecord?,
    latestLeanBodyMass: Percentage?,
    userProfile: UserProfile?,
    uiState: AgeViewModel.UiState,
    onError: (Throwable?) -> Unit = {},
    onPermissionsResult: () -> Unit = {},
    onPermissionsLaunch: (Set<String>) -> Unit = {}
) {

    // Remember the last error ID, such that it is possible to avoid re-launching the error
    // notification for the same error when the screen is recomposed, or configuration changes etc.
    val errorId = rememberSaveable { mutableStateOf(UUID.randomUUID()) }

    // Function to calculate biological age based on VO₂max
    fun calculateBiologicalAge(chronologicalAge: Int, vo2Max: Double, gender: Gender): Double? {
        // Reference VO₂max values based on gender and age
        val referenceVo2Max = when (gender) {
            Gender.MALE -> when (chronologicalAge) {
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

            Gender.FEMALE -> when (chronologicalAge) {
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

        // Average decrease per year: approx. 0.5 ml/kg/min
        val decreasePerYear = 0.5

        // Calculation: Chronological Age + (Reference VO₂max - Current VO₂max) / Decrease per year
        return chronologicalAge + (referenceVo2Max - vo2Max) / decreasePerYear
    }

    // Function to calculate biological age based on resting heart rate
    fun calculateBiologicalAgeFromRestingHeartRate(
        chronologicalAge: Int,
        restingHeartRate: Long,
        gender: Gender
    ): Double? {
        val referenceRestingHeartRate = when (gender) {
            Gender.MALE -> when (chronologicalAge) {
                in 18..25 -> 61
                in 26..35 -> 61
                in 36..45 -> 62
                in 46..55 -> 63
                in 56..65 -> 61
                else -> 61 // 65+
            }

            Gender.FEMALE -> when (chronologicalAge) {
                in 18..25 -> 65
                in 26..35 -> 64
                in 36..45 -> 64
                in 46..55 -> 65
                in 56..65 -> 64
                else -> 64 // 65+
            }
        }

        // An increase of 1 bpm in resting heart rate is associated with roughly 0.4 years of aging.
        val increasePerBpm = 0.4

        // Calculation: Chronological Age + (Current RHR - Reference RHR) * Increase per BPM
        return chronologicalAge + (restingHeartRate - referenceRestingHeartRate) * increasePerBpm
    }

    // Function to calculate biological age based on lean body mass
    fun calculateBiologicalAgeFromLeanBodyMass(
        chronologicalAge: Int,
        leanBodyMassPercentage: Double,
        gender: Gender
    ): Double? {
        val referenceLeanBodyMass = when (gender) {
            Gender.MALE -> when (chronologicalAge) {
                in 18..25 -> 0.85
                in 26..35 -> 0.83
                in 36..45 -> 0.81
                in 46..55 -> 0.79
                in 56..65 -> 0.77
                else -> 0.75 // 65+
            }

            Gender.FEMALE -> when (chronologicalAge) {
                in 18..25 -> 0.75
                in 26..35 -> 0.73
                in 36..45 -> 0.71
                in 46..55 -> 0.69
                in 56..65 -> 0.67
                else -> 0.65 // 65+
            }
        }

        // A decrease of 1% in lean body mass is associated with roughly 1 year of aging.
        val increasePerPercent = 1.0

        // Convert incoming percentage (e.g., 85.0) to a decimal (0.85)
        val currentLbmDecimal = leanBodyMassPercentage / 100.0

        // Calculation: Chronological Age + (Reference LBM - Current LBM) * 100 * Increase per Percent
        return chronologicalAge + (referenceLeanBodyMass - currentLbmDecimal) * 100 * increasePerPercent
    }

    LaunchedEffect(uiState) {
        // If the initial data load has not taken place, attempt to load the data.
        if (uiState is AgeViewModel.UiState.Uninitialized) {
            onPermissionsResult()
        }

        // The [DifferentialChangesViewModel.UiState] provides details of whether the last action
        // was a success or resulted in an error. Where an error occurred, for example in reading
        // and writing to Health Connect, the user is notified, and where the error is one that can
        // be recovered from, an attempt to do so is made.
        if (uiState is AgeViewModel.UiState.Error && errorId.value != uiState.uuid) {
            onError(uiState.exception)
            errorId.value = uiState.uuid
        }
    }

    if (uiState != AgeViewModel.UiState.Uninitialized) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display profile information
            item {
                if (userProfile?.age != null && userProfile.gender != null && userProfile.birthDate != null) {
                    val biologicalAgeVo2Max = latestVo2Max?.let { vo2Max ->
                        calculateBiologicalAge(
                            userProfile.age!!,
                            vo2Max.vo2MillilitersPerMinuteKilogram,
                            userProfile.gender!!
                        )
                    }
                    val biologicalAgeRhr = latestRestingHeartRate?.let { restingHeartRate ->
                        calculateBiologicalAgeFromRestingHeartRate(
                            userProfile.age!!,
                            restingHeartRate.beatsPerMinute,
                            userProfile.gender!!
                        )
                    }
                    val biologicalAgeLbm = latestLeanBodyMass?.let { leanBodyMass ->
                        calculateBiologicalAgeFromLeanBodyMass(
                            userProfile.age!!,
                            leanBodyMass.value,
                            userProfile.gender!!
                        )
                    }

                    val ageComponents = mutableListOf<Pair<Double, Double>>()
                    biologicalAgeVo2Max?.let { ageComponents.add(it to 0.6) }
                    biologicalAgeRhr?.let { ageComponents.add(it to 0.2) }
                    biologicalAgeLbm?.let { ageComponents.add(it to 0.2) }

                    if (ageComponents.isNotEmpty()) {
                        val totalWeight = ageComponents.sumOf { it.second }
                        val weightedSum = ageComponents.sumOf { it.first * it.second }
                        val totalBiologicalAge = weightedSum / totalWeight
                        val ageDifference = totalBiologicalAge - userProfile.age!!
                        AgeCircle(
                            totalBiologicalAge = totalBiologicalAge,
                            ageDifference = ageDifference
                        )
                    }

                    // Calculate and display biological age from VO2max
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
                                val ageDifference = bioAge - currentAge
                                MetricCard(
                                    title = "VO2 MAX",
                                    value = "${String.format("%.2f", vo2Max.vo2MillilitersPerMinuteKilogram)} ml/kg/min",
                                    ageImpact = ageDifference,
                                    sliderValue = vo2Max.vo2MillilitersPerMinuteKilogram.toFloat(),
                                    sliderRange = 15f..70f,
                                    sliderLabelMin = "15",
                                    sliderLabelMax = "70"
                                )
                            }
                        }
                    }
                    // Calculate and display biological age from Resting Heart Rate
                    latestRestingHeartRate?.let { restingHeartRate ->
                        val currentAge = userProfile.age // Local copy to avoid smart cast issues
                        val currentGender = userProfile.gender

                        if (currentAge != null && currentGender != null) {
                            val biologicalAge = calculateBiologicalAgeFromRestingHeartRate(
                                currentAge,
                                restingHeartRate.beatsPerMinute,
                                currentGender
                            )

                            biologicalAge?.let { bioAge ->
                                val ageDifference = bioAge - currentAge
                                MetricCard(
                                    title = "Resting Heart Rate",
                                    value = "${restingHeartRate.beatsPerMinute} bpm",
                                    ageImpact = ageDifference,
                                    sliderValue = restingHeartRate.beatsPerMinute.toFloat(),
                                    sliderRange = 40f..100f,
                                    sliderLabelMin = "40",
                                    sliderLabelMax = "100"
                                )
                            }
                        }
                    }
                    // Calculate and display biological age from Lean Body Mass
                    latestLeanBodyMass?.let { leanBodyMass ->
                        val currentAge = userProfile.age // Local copy to avoid smart cast issues
                        val currentGender = userProfile.gender

                        if (currentAge != null && currentGender != null) {
                            val biologicalAge = calculateBiologicalAgeFromLeanBodyMass(
                                currentAge,
                                leanBodyMass.value,
                                currentGender
                            )

                            biologicalAge?.let { bioAge ->
                                val ageDifference = bioAge - currentAge
                                MetricCard(
                                    title = "Lean Body Mass",
                                    value = "${String.format("%.2f", leanBodyMass.value)}  %",
                                    ageImpact = ageDifference,
                                    sliderValue = leanBodyMass.value.toFloat(),
                                    sliderRange = 40f..100f,
                                    sliderLabelMin = "40",
                                    sliderLabelMax = "100"
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Please go to the profile menu and enter your date of birth and gender.",
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
            }
        }
    }
}

