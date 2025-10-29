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

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.bandur.yba.data.profile.Gender
import de.bandur.yba.data.profile.ProfileRepository
import de.bandur.yba.data.profile.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

class ProfileViewModel(private val profileRepository: ProfileRepository) : ViewModel() {

    sealed class UiState {
        object Uninitialized : UiState()
        object Loading : UiState()
        data class Success(val profile: UserProfile) : UiState()
        data class Error(val exception: Throwable, val uuid: UUID = UUID.randomUUID()) : UiState()
        data class Saved(val profile: UserProfile, val uuid: UUID = UUID.randomUUID()) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Uninitialized)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _birthDateInput = MutableStateFlow("")
    val birthDateInput: StateFlow<String> = _birthDateInput.asStateFlow()

    private val _selectedGender = MutableStateFlow<Gender?>(null)
    val selectedGender: StateFlow<Gender?> = _selectedGender.asStateFlow()
    
    companion object {
        val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val profile = profileRepository.getCurrentProfile()
                _birthDateInput.value = profile.birthDate?.format(DATE_FORMATTER) ?: ""
                _selectedGender.value = profile.gender
                _uiState.value = UiState.Success(profile)
            } catch (exception: Exception) {
                _uiState.value = UiState.Error(exception)
            }
        }
    }

    fun updateBirthDate(birthDate: String) {
        _birthDateInput.value = birthDate
    }

    fun updateGender(gender: Gender) {
        _selectedGender.value = gender
    }

    fun saveProfile() {
        viewModelScope.launch {
            try {
                val birthDate = parseBirthDate(_birthDateInput.value)
                
                // Validierung
                if (birthDate == null) {
                    _uiState.value = UiState.Error(IllegalArgumentException("Ungültiges Geburtsdatum"))
                    return@launch
                }
                
                val age = java.time.Period.between(birthDate, LocalDate.now()).years
                if (age < 18 || age > 99) {
                    _uiState.value = UiState.Error(IllegalArgumentException("Alter muss zwischen 18 und 99 Jahren liegen"))
                    return@launch
                }
                
                if (_selectedGender.value == null) {
                    _uiState.value = UiState.Error(IllegalArgumentException("Geschlecht nicht ausgewählt"))
                    return@launch
                }

                val profile = UserProfile(
                    birthDate = birthDate,
                    gender = _selectedGender.value
                )
                
                profileRepository.saveProfile(profile)
                _uiState.value = UiState.Saved(profile)
            } catch (exception: Exception) {
                _uiState.value = UiState.Error(exception)
            }
        }
    }

    fun isValidBirthDate(birthDateString: String): Boolean {
        val birthDate = parseBirthDate(birthDateString) ?: return false
        val age = java.time.Period.between(birthDate, LocalDate.now()).years
        return age in 18..99
    }
    
    private fun parseBirthDate(birthDateString: String): LocalDate? {
        return try {
            LocalDate.parse(birthDateString, DATE_FORMATTER)
        } catch (e: DateTimeParseException) {
            null
        }
    }
}

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(ProfileRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}