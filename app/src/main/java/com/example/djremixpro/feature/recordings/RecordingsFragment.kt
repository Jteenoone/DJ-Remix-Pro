package com.example.djremixpro.feature.recordings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.djremixpro.R
import com.example.djremixpro.core.model.MixerArgs
import com.example.djremixpro.core.model.MixerEntry
import com.example.djremixpro.core.ui.ToastHost
import com.example.djremixpro.core.ui.ext.collectWhenStarted
import com.example.djremixpro.core.ui.widget.StateDialog
import com.example.djremixpro.core.ui.widget.designDialog
import com.example.djremixpro.databinding.DialogDeleteBinding
import com.example.djremixpro.databinding.FragmentRecordingsBinding

class RecordingsFragment : Fragment() {

    private var _binding: FragmentRecordingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecordingsViewModel by viewModels { RecordingsViewModel.Factory }

    private val adapter = RecordingAdapter(
        onRowClick = { viewModel.onRowClicked(it) },
        onMenuClick = { viewModel.onMenuClicked(it) },
    )
    private var sheet: RecordingSheet? = null
    private var renameDialog: RenameDialog? = null
    private var deleteDialog: StateDialog? = null
    private var deleteShownFor: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecordingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerRecordings.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRecordings.adapter = adapter
        binding.buttonOpenMixer.setOnClickListener {
            findNavController().navigate(
                R.id.action_global_mixer,
                Bundle().apply {
                    putString(MixerArgs.ENTRY, MixerEntry.DEFAULT.name)
                    putInt(MixerArgs.LESSON_ID, -1)
                },
            )
        }
        val context = requireContext()
        sheet = RecordingSheet(
            context,
            onRename = { viewModel.onRenameClicked() },
            onShare = { viewModel.onShareClicked() },
            onDelete = { viewModel.onDeleteClicked() },
            onDismissed = { viewModel.onSheetDismissed() },
        )
        renameDialog = RenameDialog(
            context,
            onConfirm = { viewModel.onRenameConfirmed(it) },
            onDismissed = { viewModel.onRenameDismissed() },
        )
        deleteDialog = StateDialog { viewModel.onDeleteDismissed() }

        collectWhenStarted(viewModel.uiState, ::render)
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is RecordingsEvent.ShowToast -> (requireActivity() as ToastHost).showToast(event.message)
            }
        }
    }

    private fun render(state: RecordingsUiState) {
        val b = _binding ?: return
        b.recyclerRecordings.isVisible = !state.isEmpty
        b.layoutEmpty.isVisible = state.isEmpty
        adapter.submitList(state.items)
        sheet?.render(state.sheet)
        renameDialog?.render(state.renameDialog)
        renderDeleteConfirm(state.deleteConfirm)
    }

    /** D-13: confirm before deleting (dialog_delete.xml, "Xoá" filled in rec); the ViewModel closes it. */
    private fun renderDeleteConfirm(ui: DeleteConfirmUi?) {
        val dialog = deleteDialog ?: return
        if (ui == null) {
            deleteShownFor = null
            dialog.hide()
            return
        }
        if (deleteShownFor == ui.id && dialog.current != null) return
        dialog.hide()
        deleteShownFor = ui.id
        val content = DialogDeleteBinding.inflate(layoutInflater)
        content.textBody.text = getString(R.string.recording_delete_body, ui.name)
        content.buttonConfirm.setOnClickListener { viewModel.onDeleteConfirmed() }
        dialog.show(
            create = { designDialog(requireContext(), content.root) },
            afterShow = { d -> content.buttonCancel.setOnClickListener { d.dismiss() } },
        )
    }

    override fun onDestroyView() {
        sheet?.hide()
        renameDialog?.hide()
        deleteDialog?.hide()
        sheet = null
        renameDialog = null
        deleteDialog = null
        deleteShownFor = null
        binding.recyclerRecordings.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
