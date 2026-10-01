package com.example.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LocalizationManager {
    private val _currentLanguage = MutableStateFlow(Language.AZ)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    fun setLanguage(language: Language) {
        _currentLanguage.value = language
    }

    fun getString(key: StringKey, language: Language = _currentLanguage.value): String {
        val dict = when (language) {
            Language.AZ -> LocaleAz
            Language.EN -> LocaleEn
            Language.RU -> LocaleRu
        }
        return dict[key] ?: LocaleAz[key] ?: key.name
    }
}

@Composable
fun localizedString(key: StringKey): String {
    val lang by LocalizationManager.currentLanguage.collectAsState()
    return LocalizationManager.getString(key, lang)
}
