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
package de.bandur.yba.presentation.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import de.bandur.yba.data.HealthConnectManager
import de.bandur.yba.presentation.screen.WelcomeScreen
import de.bandur.yba.presentation.screen.age.AgeScreen
import de.bandur.yba.presentation.screen.age.AgeViewModel
import de.bandur.yba.presentation.screen.age.AgeViewModelFactory
import de.bandur.yba.presentation.screen.privacypolicy.PrivacyPolicyScreen
import de.bandur.yba.presentation.screen.profile.ProfileScreen
import de.bandur.yba.presentation.screen.profile.ProfileViewModel
import de.bandur.yba.presentation.screen.profile.ProfileViewModelFactory
import de.bandur.yba.showExceptionSnackbar

/**
 * Provides the navigation in the app.
 */
@Composable
fun HealthConnectNavigation(
  navController: NavHostController,
  healthConnectManager: HealthConnectManager,
  scaffoldState: ScaffoldState,
) {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val profileRepository = de.bandur.yba.data.profile.ProfileRepository(context)
  val userProfile by profileRepository.userProfile.collectAsState(
    initial = de.bandur.yba.data.profile.UserProfile()
  )

  val startDestination = Screen.Permission.route

  NavHost(navController = navController, startDestination = startDestination) {
    val availability by healthConnectManager.availability
    composable(Screen.WelcomeScreen.route) {
      WelcomeScreen(
        healthConnectAvailability = availability,
        onResumeAvailabilityCheck = {
          healthConnectManager.checkAvailability()
        }
      )
    }
    composable(
      route = Screen.PrivacyPolicy.route,
      deepLinks = listOf(
        navDeepLink {
          action = "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"
        }
      )
    ) {
      PrivacyPolicyScreen()
    }
    composable(Screen.Age.route) {
      val viewModel: AgeViewModel = viewModel(
        factory = AgeViewModelFactory(
          healthConnectManager = healthConnectManager
        )
      )
      val latestVo2Max by viewModel.latestVo2Max
      val latestRestingHeartRate by viewModel.latestRestingHeartRate
      val permissionsGranted by viewModel.permissionsGranted
      val permissions = viewModel.permissions
      val onPermissionsResult = { viewModel.initialLoad() }
      val permissionsLauncher =
        rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
          onPermissionsResult()
        }
      AgeScreen(
        permissionsGranted = permissionsGranted,
        permissions = permissions,
        latestVo2Max = latestVo2Max,
        latestRestingHeartRate = latestRestingHeartRate,
        userProfile = userProfile,
        uiState = viewModel.uiState,
        onError = { exception ->
          showExceptionSnackbar(scaffoldState, scope, exception)
        },
        onPermissionsResult = {
          viewModel.initialLoad()
        },
        onPermissionsLaunch = { values ->
          permissionsLauncher.launch(values)
        }
      )
    }
    composable(Screen.Profile.route) {
      val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(context)
      )
      ProfileScreen(
        onError = { exception ->
          showExceptionSnackbar(scaffoldState, scope, exception)
        },
        onSaved = {
          showExceptionSnackbar(scaffoldState, scope, null, "Profil gespeichert!")
          navController.navigate(Screen.WelcomeScreen.route) {
            popUpTo(Screen.Profile.route) { inclusive = true }
          }
        },
        viewModel = viewModel
      )
    }
    composable(Screen.Permission.route) {
      val viewModel: AgeViewModel = viewModel(
        factory = AgeViewModelFactory(
          healthConnectManager = healthConnectManager
        )
      )
      val permissionsGranted by viewModel.permissionsGranted
      val permissions = viewModel.permissions
      val onPermissionsResult = {
        val destination = if (userProfile.isComplete) {
          Screen.WelcomeScreen.route
        } else {
          Screen.Profile.route
        }
        navController.navigate(destination) {
          popUpTo(Screen.Permission.route) { inclusive = true }
        }
      }
      val permissionsLauncher =
        rememberLauncherForActivityResult(viewModel.permissionsLauncher) {
          onPermissionsResult()
        }

      LaunchedEffect(Unit) {
        if (!permissionsGranted) {
          permissionsLauncher.launch(permissions)
        } else {
          onPermissionsResult()
        }
      }
      // Show a loading indicator or a splash screen while waiting for permission result
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        CircularProgressIndicator()
      }
    }
  }
}
