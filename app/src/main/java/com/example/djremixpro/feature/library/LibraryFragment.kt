package com.example.djremixpro.feature.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.djremixpro.core.model.LibraryTab
import com.example.djremixpro.core.ui.ToastHost
import com.example.djremixpro.core.ui.ext.collectWhenStarted
import com.example.djremixpro.databinding.FragmentLibraryBinding

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LibraryViewModel by viewModels { LibraryViewModel.Factory }

    private val adapter = SongAdapter { id -> viewModel.onSongClicked(id) }
    private var songSheet: SongSheet? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSongs.adapter = adapter
        binding.recyclerSongs.itemAnimator = null

        binding.editSearch.doAfterTextChanged { viewModel.onQueryChange(it?.toString().orEmpty()) }
        binding.editSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                v.clearFocus()
                requireContext().getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }
        chips().forEach { (tab, chip) -> chip.setOnClickListener { viewModel.onTabSelected(tab) } }
        binding.buttonSort.setOnClickListener { viewModel.onCycleSort() }

        songSheet = SongSheet(
            requireContext(),
            onLoad = { deck -> viewModel.onLoadToDeck(deck) },
            onPreview = { viewModel.onPreview() },
            onDismissed = { viewModel.onSheetDismissed() },
        )

        collectWhenStarted(viewModel.uiState, ::render)
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is LibraryEvent.ShowToast -> (requireActivity() as ToastHost).showToast(event.message)
            }
        }
    }

    private fun chips(): List<Pair<LibraryTab, TextView>> = listOf(
        LibraryTab.SONGS to binding.chipSongs,
        LibraryTab.PLAYLISTS to binding.chipPlaylists,
        LibraryTab.ALBUMS to binding.chipAlbums,
        LibraryTab.ARTISTS to binding.chipArtists,
        LibraryTab.FOLDERS to binding.chipFolders,
    )

    private fun render(state: LibraryUiState) {
        val b = _binding ?: return
        val context = requireContext()
        // Keep the cursor: only write the text back when it differs (e.g. after process death).
        if (b.editSearch.text.toString() != state.query) b.editSearch.setText(state.query)
        chips().forEach { (tab, chip) -> chip.isSelected = tab == state.tab }
        b.textCount.text = state.countLabel.resolve(context)
        b.textSort.text = state.sortLabel.resolve(context)
        adapter.submitList(state.songs)
        b.textEmpty.isVisible = state.songs.isEmpty() && state.query.isNotBlank()
        songSheet?.render(state.sheet)
    }

    override fun onDestroyView() {
        songSheet?.hide()
        songSheet = null
        binding.recyclerSongs.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
