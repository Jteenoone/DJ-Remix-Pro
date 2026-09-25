package com.example.djremixpro.core.ui.widget

import android.content.Context
import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Bottom sheet driven by UI state: [render] shows it (or updates it when already shown), [hide] closes it without
 * reporting. Only a dismissal by the user (scrim, back, drag) calls [onUserDismiss], so the ViewModel stays the
 * single source of truth. Call [hide] from onDestroyView; the state re-shows the sheet after recreation.
 */
class StateBottomSheet<B : ViewBinding>(
    private val context: Context,
    private val inflate: (LayoutInflater) -> B,
    private val onUserDismiss: () -> Unit,
) {
    private var dialog: BottomSheetDialog? = null
    private var binding: B? = null
    private var closingFromState = false

    val isShowing: Boolean get() = dialog?.isShowing == true

    fun render(bind: (B) -> Unit) {
        val current = binding
        if (dialog != null && current != null) {
            bind(current)
            return
        }
        val b = inflate(LayoutInflater.from(context))
        binding = b
        bind(b)
        val d = BottomSheetDialog(context)
        d.setContentView(b.root)
        d.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        d.behavior.skipCollapsed = true
        d.setOnDismissListener {
            val fromState = closingFromState
            closingFromState = false
            dialog = null
            binding = null
            if (!fromState) onUserDismiss()
        }
        dialog = d
        d.show()
    }

    fun hide() {
        val d = dialog ?: return
        closingFromState = true
        d.dismiss()
    }
}
