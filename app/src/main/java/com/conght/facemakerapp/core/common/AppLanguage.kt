package com.conght.facemakerapp.core.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.conght.facemakerapp.R

/** Ngôn ngữ app hỗ trợ, theo thứ tự hiển thị trong Figma. */
enum class AppLanguage(val code: String, @param:StringRes val nameRes: Int, @param:DrawableRes val flagRes: Int) {
    HINDI("hi", R.string.language_hindi, R.drawable.ic_flag_hi),
    SPANISH("es", R.string.language_spanish, R.drawable.ic_flag_es),
    FRENCH("fr", R.string.language_french, R.drawable.ic_flag_fr),
    ENGLISH("en", R.string.language_english, R.drawable.ic_flag_en),
    PORTUGUESE("pt", R.string.language_portuguese, R.drawable.ic_flag_pt),
    INDONESIAN("in", R.string.language_indonesian, R.drawable.ic_flag_in);

    companion object {
        val DEFAULT = ENGLISH

        /** "id" (mã mới của tiếng Indonesia) -> "in" (mã resource values-in). */
        fun fromCode(code: String?): AppLanguage? {
            val normalized = code?.substringBefore('-')?.substringBefore('_')?.lowercase()
                ?.let { if (it == "id") "in" else it }
            return entries.firstOrNull { it.code == normalized }
        }

        /** Ngôn ngữ app đang dùng (nếu đã chọn), ngược lại ngôn ngữ máy nếu hỗ trợ, cuối cùng English. */
        fun current(): AppLanguage {
            val appLocales = AppCompatDelegate.getApplicationLocales()
            val locale = if (!appLocales.isEmpty) appLocales[0] else LocaleListCompat.getAdjustedDefault()[0]
            return fromCode(locale?.language) ?: DEFAULT
        }

        /** Áp ngôn ngữ cho app (Activity tự recreate nếu khác ngôn ngữ hiện tại). */
        fun apply(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.code))
        }
    }
}
