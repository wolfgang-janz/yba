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

import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.bandur.yba.data.HealthConnectManager
import java.util.UUID
import kotlinx.coroutines.launch

class AgeViewModel(private val healthConnectManager: HealthConnectManager) :
    ViewModel() {

    internal val changesDataTypes = setOf(
        Vo2MaxRecord::class,
        HeartRateVariabilityRmssdRecord::class,
        RestingHeartRateRecord::class
    )

    val permissions = changesDataTypes.map { HealthPermission.getReadPermission(it) }.toSet()

    var permissionsGranted = mutableStateOf(false)
        private set

    var uiState: UiState by mutableStateOf(UiState.Uninitialized)
        private set

    var latestVo2Max: MutableState<Vo2MaxRecord?> = mutableStateOf(null)
        private set

    var latestHRV: MutableState<HeartRateVariabilityRmssdRecord?> = mutableStateOf(null)
        private set

    var latestRestingHeartRate: MutableState<RestingHeartRateRecord?> = mutableStateOf(null)
        private set


    val permissionsLauncher = healthConnectManager.requestPermissionsActivityContract()

    fun initialLoad() {
        viewModelScope.launch {
            permissionsGranted.value = healthConnectManager.hasAllPermissions(permissions)
            if (permissionsGranted.value) {
                try {
                    latestVo2Max.value = healthConnectManager.getLatestVo2Max()
                    Log.i(TAG, "Latest VO2 Max: ${latestVo2Max.value}")

                    latestHRV.value = healthConnectManager.getLatestHRV()
                    Log.i(TAG, "Latest HRV: ${latestHRV.value}")

                    latestRestingHeartRate.value = healthConnectManager.getLatestRestingHeartRate()
                    Log.i(TAG, "Latest Resting Heart Rate: ${latestRestingHeartRate.value}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load health data: ${e.message}")
                    latestVo2Max.value = null
                    latestHRV.value = null
                    latestRestingHeartRate.value = null
                }
            }
            uiState = UiState.Done
        }
    }


    sealed class UiState {
        object Uninitialized : UiState()
        object Done : UiState()

        // A random UUID is used in each Error object to allow errors to be uniquely identified,
        // and recomposition won't result in multiple snackbars.
        data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
    }
}

class AgeViewModelFactory(
    private val healthConnectManager: HealthConnectManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AgeViewModel(
                healthConnectManager = healthConnectManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
