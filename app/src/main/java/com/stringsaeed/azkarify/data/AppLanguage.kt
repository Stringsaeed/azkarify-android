package com.stringsaeed.azkarify.data

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

fun Context.appLanguage(): String {
    val platformLanguage =
        if (Build.VERSION.SDK_INT >= 33)
            getSystemService(LocaleManager::class.java)?.applicationLocales?.get(0)?.language
        else null
    val saved = getSharedPreferences("azkarify", 0).all["language"] as? String
    return (platformLanguage ?: saved ?: resources.configuration.locales[0].language).let {
        if (it == "ar") "ar" else "en"
    }
}

fun Context.applyAppLanguage(language: String) {
    require(language == "en" || language == "ar")
    if (Build.VERSION.SDK_INT >= 33) {
        val manager = getSystemService(LocaleManager::class.java)
        if (manager.applicationLocales.toLanguageTags() != language)
            manager.applicationLocales = LocaleList.forLanguageTags(language)
    }
}
