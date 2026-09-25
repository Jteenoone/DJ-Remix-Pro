package com.example.djremixpro.core.util

import kotlin.coroutines.cancellation.CancellationException

/** Like [runCatching] but lets coroutine cancellation propagate. */
suspend inline fun <T> resultOf(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
