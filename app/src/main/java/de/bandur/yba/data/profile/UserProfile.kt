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

import java.time.LocalDate
import java.time.Period

/**
 * Enum für Geschlecht
 */
enum class Gender {
    MALE, FEMALE
}

/**
 * Data class für Benutzerprofil
 */
data class UserProfile(
    val birthDate: LocalDate? = null,
    val gender: Gender? = null
) {
    /**
     * Überprüft, ob das Profil vollständig ist
     */
    val isComplete: Boolean
        get() = birthDate != null && gender != null

    /**
     * Berechnet das aktuelle Alter basierend auf dem Geburtsdatum
     */
    val age: Int?
        get() = birthDate?.let {
            Period.between(it, LocalDate.now()).years
        }
}