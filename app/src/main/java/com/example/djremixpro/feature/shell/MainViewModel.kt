package com.example.djremixpro.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.djremixpro.app.DJRemixProApp
import com.example.djremixpro.core.data.PlaybackRepository
import com.example.djremixpro.core.model.NowPlaying
import com.example.djremixpro.core.util.TimeFormat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(private val playbackRepository: PlaybackRepository) : ViewModel() {

    val uiState: StateFlow<ShellUiState> = playbackRepository.nowPlaying
        .map { ShellUiState(miniPlayer = it?.toUi()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ShellUiState(playbackRepository.nowPlaying.value?.toUi()))

    fun onMiniPlayerStop() = playbackRepository.stop()

    private fun NowPlaying.toUi() = MiniPlayerUi(
        id = id,
        title = title,
        timeLabel = "${TimeFormat.mmss(positionSec)} / ${TimeFormat.mmss(durationSec)}",
        progress = if (durationSec > 0) (positionSec.toFloat() / durationSec).coerceIn(0f, 1f) else 0f,
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DJRemixProApp
                MainViewModel(app.container.playbackRepository)
            }
        }
    }
}
