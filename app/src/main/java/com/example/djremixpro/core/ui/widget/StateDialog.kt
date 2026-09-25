package com.example.djremixpro.core.ui.widget

import androidx.appcompat.app.AlertDialog

/**
 * Dialog driven by UI state, same contract as [StateBottomSheet]: [show] creates it once, [hide] closes it silently,
 * and only a dismissal by the user (back, outside tap, negative button) reports [onUserDismiss].
 */
class StateDialog(private val onUserDismiss: () -> Unit) {

    private var dialog: AlertDialog? = null
    private var closingFromState = false

    val current: AlertDialog? get() = dialog

    /** [create] builds the dialog; [afterShow] runs once it is shown (buttons exist then). */
    fun show(create: () -> AlertDialog, afterShow: (AlertDialog) -> Unit = {}) {
        if (dialog != null) return
        val d = create()
        d.setOnDismissListener {
            val fromState = closingFromState
            closingFromState = false
            dialog = null
            if (!fromState) onUserDismiss()
        }
        dialog = d
        d.show()
        afterShow(d)
    }

    fun hide() {
        val d = dialog ?: return
        closingFromState = true
        d.dismiss()
    }
}
