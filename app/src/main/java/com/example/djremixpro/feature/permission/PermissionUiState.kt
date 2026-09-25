package com.example.djremixpro.feature.permission

data class PermissionUiState(val isBusy: Boolean = false)

sealed interface PermissionEvent {
    data object RequestPermission : PermissionEvent
    data object GoHome : PermissionEvent
}
