package com.example.djremixpro.core.ui

import android.os.Bundle
import androidx.annotation.IdRes

/** Accent of the toast check icon: SUCCESS/DECK_B = md_deck_b, DECK_A = md_deck_a, ERROR = md_rec (App.dc.html:279). */
enum class ToastTone { SUCCESS, DECK_A, DECK_B, ERROR }

data class ToastMessage(val text: UiText, val tone: ToastTone = ToastTone.SUCCESS)

/** Implemented by MainActivity and MixerActivity. */
interface ToastHost {
    fun showToast(message: ToastMessage)
}

/** Implemented by MainActivity: switches bottom-nav tabs the same way a tab tap does. */
interface ShellNavigator {
    fun openTab(@IdRes destinationId: Int, args: Bundle? = null)
}
