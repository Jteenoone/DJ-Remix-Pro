package com.example.djremixpro.feature.splash

enum class SplashDestination { ONBOARDING, HOME }

/** [destination] stays null until settings are read and the minimum splash time has passed. */
data class SplashUiState(val destination: SplashDestination? = null)
