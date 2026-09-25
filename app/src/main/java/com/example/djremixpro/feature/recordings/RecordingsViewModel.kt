package com.example.djremixpro.feature.recordings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.djremixpro.R
import com.example.djremixpro.app.DJRemixProApp
import com.example.djremixpro.core.data.PlaybackRepository
import com.example.djremixpro.core.data.RecordingRepository
import com.example.djremixpro.core.model.NowPlaying
import com.example.djremixpro.core.model.Recording
import com.example.djremixpro.core.ui.ToastMessage
import com.example.djremixpro.core.ui.ToastTone
import com.example.djremixpro.core.ui.UiText
import com.example.djremixpro.core.util.TimeFormat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** "Bản ghi của tôi" (App.dc.html:122-143, 331-337) with rename/delete dialogs and share toast (D-13). */
class RecordingsViewModel(
    private val recordingRepository: RecordingRepository,
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {

    private data class Local(
        val sheetId: String? = null,
        val renameId: String? = null,
        val deleteId: String? = null,
    )

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<RecordingsUiState> = combine(
        recordingRepository.recordings,
        playbackRepository.nowPlaying,
        local,
        ::buildState,
    ).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        buildState(recordingRepository.recordings.value, playbackRepository.nowPlaying.value, local.value),
    )

    private val _events = Channel<RecordingsEvent>(Channel.BUFFERED)
    val events: Flow<RecordingsEvent> = _events.receiveAsFlow()

    fun onRowClicked(id: String) {
        val recording = find(id) ?: return
        playbackRepository.toggle(recording.id, recording.name, recording.durationSec)
    }

    fun onMenuClicked(id: String) {
        if (find(id) == null) return
        local.update { it.copy(sheetId = id) }
    }

    fun onSheetDismissed() = local.update { it.copy(sheetId = null) }

    fun onRenameClicked() {
        val id = local.value.sheetId ?: return
        local.update { it.copy(sheetId = null, renameId = id) }
    }

    fun onRenameConfirmed(newName: String) {
        val id = local.value.renameId ?: return
        local.update { it.copy(renameId = null) }
        viewModelScope.launch {
            recordingRepository.rename(id, newName)
                .onSuccess { toast(UiText.Res(R.string.toast_recording_renamed)) }
                .onFailure { toastError() }
        }
    }

    fun onRenameDismissed() = local.update { it.copy(renameId = null) }

    fun onShareClicked() {
        local.update { it.copy(sheetId = null) }
        toast(UiText.Res(R.string.toast_share_unavailable))
    }

    fun onDeleteClicked() {
        val id = local.value.sheetId ?: return
        local.update { it.copy(sheetId = null, deleteId = id) }
    }

    fun onDeleteConfirmed() {
        val id = local.value.deleteId ?: return
        local.update { it.copy(deleteId = null) }
        viewModelScope.launch {
            recordingRepository.delete(id)
                .onSuccess {
                    if (playbackRepository.nowPlaying.value?.id == id) playbackRepository.stop()
                    toast(UiText.Res(R.string.toast_recording_deleted), ToastTone.ERROR)
                }
                .onFailure { toastError() }
        }
    }

    fun onDeleteDismissed() = local.update { it.copy(deleteId = null) }

    private fun find(id: String): Recording? = recordingRepository.recordings.value.firstOrNull { it.id == id }

    private fun toast(text: UiText, tone: ToastTone = ToastTone.SUCCESS) {
        _events.trySend(RecordingsEvent.ShowToast(ToastMessage(text, tone)))
    }

    private fun toastError() = toast(UiText.Res(R.string.toast_generic_error), ToastTone.ERROR)

    private fun buildState(recordings: List<Recording>, nowPlaying: NowPlaying?, l: Local): RecordingsUiState {
        fun byId(id: String?) = id?.let { i -> recordings.firstOrNull { it.id == i } }
        return RecordingsUiState(
            items = recordings.map { r ->
                val playing = nowPlaying?.id == r.id
                RecordingRow(
                    id = r.id,
                    name = r.name,
                    meta = UiText.Res(
                        if (playing) R.string.recording_meta_playing else R.string.recording_meta,
                        listOf(TimeFormat.mmss(r.durationSec), TimeFormat.date(r.createdAt.toLocalDate())),
                    ),
                    waveSeed = r.waveSeed,
                    isPlaying = playing,
                )
            },
            isEmpty = recordings.isEmpty(),
            sheet = byId(l.sheetId)?.let {
                RecordingSheetUi(it.id, it.name, "${TimeFormat.mmss(it.durationSec)} · ${TimeFormat.date(it.createdAt.toLocalDate())}")
            },
            renameDialog = byId(l.renameId)?.let { RenameDialogUi(it.id, it.name) },
            deleteConfirm = byId(l.deleteId)?.let { DeleteConfirmUi(it.id, it.name) },
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as DJRemixProApp).container
                RecordingsViewModel(c.recordingRepository, c.playbackRepository)
            }
        }
    }
}
