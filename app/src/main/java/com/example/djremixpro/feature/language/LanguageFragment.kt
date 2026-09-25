package com.example.djremixpro.feature.language

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.djremixpro.core.ui.ToastHost
import com.example.djremixpro.core.ui.ext.collectWhenStarted
import com.example.djremixpro.databinding.FragmentLanguageBinding

class LanguageFragment : Fragment() {

    private var _binding: FragmentLanguageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LanguageViewModel by viewModels { LanguageViewModel.Factory }
    private val adapter = LanguageAdapter { tag -> viewModel.onLanguageSelected(tag) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerLanguages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLanguages.adapter = adapter
        binding.recyclerLanguages.itemAnimator = null
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

        collectWhenStarted(viewModel.uiState) { state ->
            val b = _binding ?: return@collectWhenStarted
            if (b.editSearch.text.toString() != state.query) b.editSearch.setText(state.query)
            adapter.submitList(state.items)
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is LanguageEvent.ShowToast -> (requireActivity() as ToastHost).showToast(event.message)
            }
        }
    }

    override fun onDestroyView() {
        binding.recyclerLanguages.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
