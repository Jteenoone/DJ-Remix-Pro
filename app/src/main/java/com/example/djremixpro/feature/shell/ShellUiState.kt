package com.example.djremixpro.feature.shell

/** Mini player row; [timeLabel] is "01:17 / 03:12", [progress] in 0..1. */
data class MiniPlayerUi(val id: String, val title: String, val timeLabel: String, val progress: Float)

data class ShellUiState(val miniPlayer: MiniPlayerUi? = null)
