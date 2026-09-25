package com.example.djremixpro.feature.recordings

import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import com.example.djremixpro.core.ui.widget.StateDialog
import com.example.djremixpro.core.ui.widget.designDialog
import com.example.djremixpro.databinding.DialogRenameBinding

/**
 * Rename dialog (D-13) in the design dialog style (dialog_rename.xml). "Lưu" is disabled while the trimmed name is
 * empty and does not close the dialog itself: the ViewModel closes it by clearing RecordingsUiState.renameDialog.
 * "Huỷ", back or an outside tap → [onDismissed].
 */
class RenameDialog(
    private val context: Context,
    private val onConfirm: (String) -> Unit,
    onDismissed: () -> Unit,
) {
    private val dialog = StateDialog(onDismissed)
    private var shownFor: String? = null

    fun render(ui: RenameDialogUi?) {
        if (ui == null) {
            hide()
            return
        }
        if (shownFor == ui.id && dialog.current != null) return
        hide()
        shownFor = ui.id
        val binding = DialogRenameBinding.inflate(LayoutInflater.from(context))
        binding.editName.setText(ui.currentName)
        binding.editName.setSelection(ui.currentName.length)
        fun submit() {
            val name = binding.editName.text?.toString().orEmpty()
            if (name.isNotBlank()) onConfirm(name.trim())
        }
        binding.buttonConfirm.isEnabled = ui.currentName.isNotBlank()
        binding.buttonConfirm.setOnClickListener { submit() }
        binding.editName.doAfterTextChanged { binding.buttonConfirm.isEnabled = !it.isNullOrBlank() }
        binding.editName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }
        dialog.show(
            create = { designDialog(context, binding.root) },
            afterShow = { d ->
                binding.buttonCancel.setOnClickListener { d.dismiss() }
                binding.editName.requestFocus()
                d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            },
        )
    }

    fun hide() {
        shownFor = null
        dialog.hide()
    }
}
