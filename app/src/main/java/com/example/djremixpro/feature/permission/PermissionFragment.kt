package com.example.djremixpro.feature.permission

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.djremixpro.R
import com.example.djremixpro.core.ui.ext.collectWhenStarted
import com.example.djremixpro.databinding.FragmentPermissionBinding

/** D-10: asks READ_MEDIA_AUDIO (API 33+) / READ_EXTERNAL_STORAGE (≤ 32); any outcome continues Home. */
class PermissionFragment : Fragment() {

    private var _binding: FragmentPermissionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PermissionViewModel by viewModels { PermissionViewModel.Factory }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onPermissionResult(granted)
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonAllow.setOnClickListener { viewModel.onAllowClicked() }
        binding.buttonSamples.setOnClickListener { viewModel.onUseSamplesClicked() }

        collectWhenStarted(viewModel.uiState) { state ->
            _binding?.let {
                it.buttonAllow.isEnabled = !state.isBusy
                it.buttonSamples.isEnabled = !state.isBusy
            }
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                PermissionEvent.RequestPermission -> requestPermission.launch(audioPermission())
                PermissionEvent.GoHome -> {
                    val nav = findNavController()
                    if (nav.currentDestination?.id == R.id.permissionFragment) {
                        nav.navigate(R.id.action_permission_to_home)
                    }
                }
            }
        }
    }

    private fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
