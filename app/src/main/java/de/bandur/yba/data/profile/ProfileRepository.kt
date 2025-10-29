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
package de.bandur.yba.data.profile

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Repository für die Verwaltung der Benutzerprofildaten
 */
class ProfileRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "user_profile", Context.MODE_PRIVATE
    )
    
    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: Flow<UserProfile> = _userProfile.asStateFlow()
    
    companion object {
        private const val KEY_BIRTH_DATE = "birth_date"
        private const val KEY_GENDER = "gender"
    }
    
    /**
     * Lädt das Benutzerprofil aus SharedPreferences
     */
    private fun loadProfile(): UserProfile {
        val birthDateString = sharedPreferences.getString(KEY_BIRTH_DATE, null)
        val birthDate = birthDateString?.let { LocalDate.parse(it) }
        val genderString = sharedPreferences.getString(KEY_GENDER, null)
        val gender = genderString?.let { Gender.valueOf(it) }
        
        return UserProfile(birthDate = birthDate, gender = gender)
    }
    
    /**
     * Speichert das Benutzerprofil in SharedPreferences
     */
    fun saveProfile(profile: UserProfile) {
        with(sharedPreferences.edit()) {
            profile.birthDate?.let { putString(KEY_BIRTH_DATE, it.toString()) } ?: remove(KEY_BIRTH_DATE)
            profile.gender?.let { putString(KEY_GENDER, it.name) } ?: remove(KEY_GENDER)
            apply()
        }
        _userProfile.value = profile
    }
    
    /**
     * Gibt das aktuelle Profil zurück
     */
    fun getCurrentProfile(): UserProfile {
        return _userProfile.value
    }
}