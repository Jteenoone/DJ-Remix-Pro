package com.example.djremixpro.feature.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.djremixpro.R
import com.example.djremixpro.core.model.AppSettings
import com.example.djremixpro.core.model.DeckMode
import com.example.djremixpro.core.model.RecordFormat
import com.example.djremixpro.core.model.SettingsOptions
import com.example.djremixpro.core.model.ThemeMode
import com.example.djremixpro.core.ui.ToastHost
import com.example.djremixpro.core.ui.ext.collectWhenStarted
import com.example.djremixpro.core.ui.ext.selectSegment
import com.example.djremixpro.databinding.FragmentSettingsBinding
import com.example.djremixpro.databinding.ItemSettingToggleBinding
import com.example.djremixpro.databinding.ItemSettingValueBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }

    private var current: AppSettings = AppSettings()
    private var firstRender = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        firstRender = true
        val b = binding
        b.segmentDark.setOnClickListener { viewModel.onThemeSelected(ThemeMode.DARK) }
        b.segmentLight.setOnClickListener { viewModel.onThemeSelected(ThemeMode.LIGHT) }
        b.segmentSystem.setOnClickListener { viewModel.onThemeSelected(ThemeMode.SYSTEM) }

        setupValue(b.rowLatency, R.string.settings_latency) {
            choose(R.string.settings_latency, SettingsOptions.latencyMs, current.latencyMs, ::latencyLabel, viewModel::onLatencySelected)
        }
        setupToggle(b.rowPrecue, R.string.settings_precue, R.string.settings_precue_sub, SettingToggle.PRECUE)
        setupValue(b.rowMasterVolume, R.string.settings_master_volume) {
            choose(R.string.settings_master_volume, SettingsOptions.masterVolumePct, current.masterVolumePct, ::percentLabel, viewModel::onMasterVolumeSelected)
        }
        setupValue(b.rowFormat, R.string.settings_format) {
            choose(R.string.settings_format, RecordFormat.entries, current.recordFormat, ::formatLabel, viewModel::onFormatSelected)
        }
        setupValue(b.rowQuality, R.string.settings_quality) {
            choose(R.string.settings_quality, SettingsOptions.recordQualityKbps, current.recordQualityKbps, ::qualityLabel, viewModel::onQualitySelected)
        }
        setupValue(b.rowFolder, R.string.settings_folder) { viewModel.onFolderClicked() }
        setupValue(b.rowPitchRange, R.string.settings_pitch_range) {
            choose(R.string.settings_pitch_range, SettingsOptions.pitchRangePct, current.pitchRangePct, ::pitchRangeLabel, viewModel::onPitchRangeSelected)
        }
        setupValue(b.rowJogMode, R.string.settings_jog_mode) {
            choose(R.string.settings_jog_mode, DeckMode.entries, current.defaultJogMode, ::jogModeLabel, viewModel::onJogModeSelected)
        }
        setupToggle(b.rowHaptic, R.string.settings_haptic, null, SettingToggle.HAPTIC)
        setupToggle(b.rowKeepAwake, R.string.settings_keep_awake, null, SettingToggle.KEEP_SCREEN_ON)
        setupValue(b.rowLanguage, R.string.settings_language) {
            val nav = findNavController()
            if (nav.currentDestination?.id == R.id.settingsFragment) nav.navigate(R.id.action_settings_to_language)
        }
        setupValue(b.rowPermissions, R.string.settings_permissions) { openAppDetails() }
        setupValue(b.rowSupport, R.string.settings_support) { viewModel.onSupportClicked() }
        b.rowPermissions.textValue.isVisible = false
        b.rowSupport.textValue.isVisible = false

        collectWhenStarted(viewModel.uiState, ::render)
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is SettingsEvent.ShowToast -> (requireActivity() as ToastHost).showToast(event.message)
            }
        }
    }

    private fun render(state: SettingsUiState) {
        val b = _binding ?: return
        val s = state.settings
        current = s
        listOf(b.segmentDark, b.segmentLight, b.segmentSystem).selectSegment(
            when (s.themeMode) {
                ThemeMode.DARK -> 0
                ThemeMode.LIGHT -> 1
                ThemeMode.SYSTEM -> 2
            },
        )
        b.rowLatency.textValue.text = latencyLabel(s.latencyMs)
        b.rowMasterVolume.textValue.text = percentLabel(s.masterVolumePct)
        b.rowFormat.textValue.text = formatLabel(s.recordFormat)
        b.rowQuality.textValue.text = qualityLabel(s.recordQualityKbps)
        b.rowFolder.textValue.text = s.saveFolder
        b.rowPitchRange.textValue.text = pitchRangeLabel(s.pitchRangePct)
        b.rowJogMode.textValue.text = jogModeLabel(s.defaultJogMode)
        b.rowLanguage.textValue.text = state.languageName
        val animate = !firstRender
        bindToggle(b.rowPrecue, s.precue, animate)
        bindToggle(b.rowHaptic, s.haptic, animate)
        bindToggle(b.rowKeepAwake, s.keepScreenOn, animate)
        firstRender = false
    }

    private fun setupValue(row: ItemSettingValueBinding, @StringRes label: Int, onClick: () -> Unit) {
        row.textLabel.setText(label)
        row.root.setOnClickListener { onClick() }
    }

    private fun setupToggle(row: ItemSettingToggleBinding, @StringRes label: Int, @StringRes sub: Int?, toggle: SettingToggle) {
        row.textLabel.setText(label)
        row.textSub.isVisible = sub != null
        if (sub != null) row.textSub.setText(sub)
        row.root.setOnClickListener { viewModel.onToggle(toggle) }
        // The row is the switch for TalkBack (role, checked state).
        row.root.accessibilityDelegate = object : View.AccessibilityDelegate() {
            @Suppress("DEPRECATION") // setChecked(Boolean) is the only API below 36
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = "android.widget.Switch"
                info.isCheckable = true
                info.isChecked = host.getTag(R.id.switch_view) == true
            }
        }
    }

    private fun bindToggle(row: ItemSettingToggleBinding, checked: Boolean, animate: Boolean) {
        row.switchView.setChecked(checked, animate)
        row.root.setTag(R.id.switch_view, checked)
    }

    /** D-14: single-choice dialog over the options of [SettingsOptions]. */
    private fun <T> choose(@StringRes title: Int, options: List<T>, selected: T, label: (T) -> String, onSelected: (T) -> Unit) {
        val labels = options.map(label).toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(labels, options.indexOf(selected)) { dialog, which ->
                onSelected(options[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun openAppDetails() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", requireContext().packageName, null))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun latencyLabel(ms: Int) = getString(R.string.settings_latency_value, ms)
    private fun percentLabel(pct: Int) = getString(R.string.settings_percent_value, pct)
    private fun qualityLabel(kbps: Int) = getString(R.string.settings_quality_value, kbps)
    private fun pitchRangeLabel(pct: Int) = getString(R.string.settings_pitch_range_value, pct)
    private fun formatLabel(format: RecordFormat) = getString(
        when (format) {
            RecordFormat.MP3 -> R.string.format_mp3
            RecordFormat.WAV -> R.string.format_wav
        },
    )
    private fun jogModeLabel(mode: DeckMode) = getString(
        when (mode) {
            DeckMode.JOG -> R.string.jog_mode_jog
            DeckMode.PAD -> R.string.jog_mode_pad
        },
    )

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
