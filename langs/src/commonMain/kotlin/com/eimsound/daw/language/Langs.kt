package com.eimsound.daw.language

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.eimsound.daw.language.langs.EnStrings

val Locales = mapOf(
    "en" to EnStrings
)

val langs by mutableStateOf(EnStrings)
