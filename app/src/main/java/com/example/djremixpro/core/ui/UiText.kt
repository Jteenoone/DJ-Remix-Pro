package com.example.djremixpro.core.ui

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Text produced by ViewModels without a Context. Args may themselves be [UiText]; they are resolved first.
 * A [Plural] without args uses [Plural.count] as its only format argument.
 */
sealed interface UiText {
    data class Res(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Plural(@param:PluralsRes val id: Int, val count: Int, val args: List<Any> = emptyList()) : UiText
    data class Raw(val value: String) : UiText

    fun resolve(context: Context): String = when (this) {
        is Raw -> value
        is Res -> if (args.isEmpty()) context.getString(id) else context.getString(id, *resolveArgs(context, args))
        is Plural -> context.resources.getQuantityString(
            id, count, *resolveArgs(context, args.ifEmpty { listOf(count) }),
        )
    }

    private fun resolveArgs(context: Context, args: List<Any>): Array<Any> =
        args.map { if (it is UiText) it.resolve(context) else it }.toTypedArray()
}
