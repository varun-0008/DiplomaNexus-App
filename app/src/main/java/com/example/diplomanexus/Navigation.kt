package com.example.diplomanexus

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.diplomanexus.ui.main.MainScreen
import com.example.diplomanexus.ui.screens.LoginScreen
import com.example.diplomanexus.ui.screens.SplashScreen
import com.example.diplomanexus.ui.screens.VerificationScreen
import com.example.diplomanexus.viewmodel.AppViewModel

@Composable
fun MainNavigation() {
  val viewModel: AppViewModel = viewModel()
  val currentUser by viewModel.currentUser.collectAsState()

  // Determine initial screen (Splash screen showcase first)
  val initialKey = Splash

  Log.e("MainNavigation", "currentUser: $currentUser, initialKey: $initialKey")

  val backStack = rememberNavBackStack(initialKey)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Splash> {
          SplashScreen(
            onFinished = {
              val nextKey = if (currentUser == null) Login else Main
              backStack.add(nextKey)
            },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<Login> {
          LoginScreen(
            viewModel = viewModel,
            onSuccess = {
              backStack.add(Main)
            },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<Verification> {
          VerificationScreen(
            viewModel = viewModel,
            onVerified = {
              backStack.removeLastOrNull()
            },
            onBack = {
              backStack.removeLastOrNull()
            },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<Main> {
          MainScreen(
            viewModel = viewModel,
            onNavigateToVerify = {
              backStack.add(Verification)
            },
            onLogout = {
              backStack.add(Login)
            },
            modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}
