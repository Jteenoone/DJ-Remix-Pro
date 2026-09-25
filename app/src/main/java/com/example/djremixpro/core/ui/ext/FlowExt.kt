package com.example.djremixpro.core.ui.ext

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Collects [flow] while this owner is at least STARTED (restarts on every START). */
fun <T> LifecycleOwner.collectWhenStarted(flow: Flow<T>, action: suspend (T) -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { action(it) }
        }
    }
}

/** Fragment variant bound to the view lifecycle; call from onViewCreated. */
fun <T> Fragment.collectWhenStarted(flow: Flow<T>, action: suspend (T) -> Unit) {
    viewLifecycleOwner.collectWhenStarted(flow, action)
}
