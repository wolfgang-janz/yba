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
package de.bandur.yba.data

import android.content.Context
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

// The minimum android level that can use Health Connect
const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1

/**
 * Demonstrates reading and writing from Health Connect.
 */
class HealthConnectManager(private val context: Context) {
  private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

  var availability = mutableStateOf(HealthConnectAvailability.NOT_SUPPORTED)
    private set

  init {
    checkAvailability()
  }

  suspend fun getLatestVo2Max(): Vo2MaxRecord? {
    val now = Instant.now()
    val lastMonth = now.minusSeconds(30L * 24 * 60 * 60) // 30 days ago

    val response = healthConnectClient.readRecords(
      ReadRecordsRequest(
        recordType = Vo2MaxRecord::class,
        timeRangeFilter = TimeRangeFilter.between(lastMonth, now)
      )
    )
    return response.records.lastOrNull()
  }

  suspend fun getLatestRestingHeartRate(): RestingHeartRateRecord? {
    val now = Instant.now()
    val lastMonth = now.minusSeconds(30L * 24 * 60 * 60) // 30 days ago

    val response = healthConnectClient.readRecords(
      ReadRecordsRequest(
        recordType = RestingHeartRateRecord::class,
        timeRangeFilter = TimeRangeFilter.between(lastMonth, now)
      )
    )
    return response.records.lastOrNull()
  }

  suspend fun getLatestHRV(): HeartRateVariabilityRmssdRecord? {
    val now = Instant.now()
    val lastMonth = now.minusSeconds(30L * 24 * 60 * 60) // 30 days ago

    val response = healthConnectClient.readRecords(
      ReadRecordsRequest(
        recordType = HeartRateVariabilityRmssdRecord::class,
        timeRangeFilter = TimeRangeFilter.between(lastMonth, now)
      )
    )
    return response.records.lastOrNull()
  }

  fun checkAvailability() {
    availability.value = when {
      HealthConnectClient.getSdkStatus(context) == SDK_AVAILABLE -> HealthConnectAvailability.INSTALLED
      isSupported() -> HealthConnectAvailability.NOT_INSTALLED
      else -> HealthConnectAvailability.NOT_SUPPORTED
    }
  }

  /**
   * Determines whether all the specified permissions are already granted. It is recommended to
   * call [PermissionController.getGrantedPermissions] first in the permissions flow, as if the
   * permissions are already granted then there is no need to request permissions via
   * [PermissionController.createRequestPermissionResultContract].
   */
  suspend fun hasAllPermissions(permissions: Set<String>): Boolean {
    return healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
  }

  fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
    return PermissionController.createRequestPermissionResultContract()
  }

  private fun isSupported() = Build.VERSION.SDK_INT >= MIN_SUPPORTED_SDK

}

/**
 * Health Connect requires that the underlying Health Connect APK is installed on the device.
 * [HealthConnectAvailability] represents whether this APK is indeed installed, whether it is not
 * installed but supported on the device, or whether the device is not supported (based on Android
 * version).
 */
enum class HealthConnectAvailability {
  INSTALLED,
  NOT_INSTALLED,
  NOT_SUPPORTED
}
