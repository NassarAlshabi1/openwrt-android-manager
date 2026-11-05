package com.luanmuc.openwrtmanager.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * Localization helper.
 *
 * Provides localized strings for non-composable code (ViewModels, repositories,
 * services, widgets) and applies the user-selected locale to the Activity context.
 *
 * Supported languages: English (default) and Arabic (RTL).
 */
object Loc {

    private const val PREFS_NAME = "settings_prefs"
    private const val KEY_LANGUAGE = "language"

    @Volatile
    private var base: Context? = null

    @Volatile
    private var localized: Context? = null

    @Volatile
    private var cachedLang: String? = null

    /**
     * Must be called as early as possible (Application.onCreate / attachBaseContext).
     */
    fun init(context: Context) {
        base = context.applicationContext
    }

    /**
     * Raw persisted language enum name (SYSTEM / ENGLISH / ARABIC).
     */
    fun storedLanguageName(): String {
        val b = base ?: return "SYSTEM"
        return try {
            b.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, "SYSTEM") ?: "SYSTEM"
        } catch (e: Exception) {
            "SYSTEM"
        }
    }

    /**
     * The locale the app should currently use.
     * SYSTEM follows the device locale list.
     */
    fun currentLocale(): Locale {
        return when (storedLanguageName()) {
            "ARABIC" -> Locale("ar")
            "ENGLISH" -> Locale("en")
            else -> LocaleList.getDefault().get(0) ?: Locale.getDefault()
        }
    }

    fun isRtl(): Boolean {
        return currentLocale().language == "ar"
    }

    private fun ctx(): Context {
        val b = base ?: throw IllegalStateException("Loc not initialized")
        val lang = storedLanguageName()
        var c = localized
        if (c == null || cachedLang != lang) {
            val locale = currentLocale()
            val conf = Configuration(b.resources.configuration)
            conf.setLocale(locale)
            conf.setLayoutDirection(locale)
            c = b.createConfigurationContext(conf)
            localized = c
            cachedLang = lang
        }
        return c!!
    }

    /**
     * Get a localized string. Safe to call from anywhere after [init];
     * returns an empty string if called before initialization.
     */
    fun s(res: Int): String = try {
        ctx().getString(res)
    } catch (e: Exception) {
        ""
    }

    /**
     * Get a localized formatted string with positional arguments (%1$s ...).
     */
    fun s(res: Int, vararg args: Any?): String = try {
        val safeArgs = args.map { it ?: "null" }.toTypedArray()
        ctx().getString(res, *safeArgs)
    } catch (e: Exception) {
        ""
    }

    /**
     * Wraps the Activity base context with the selected locale
     * (call from Activity.attachBaseContext).
     */
    fun wrap(context: Context): Context {
        if (!isInitialized()) return context
        return try {
            val locale = currentLocale()
            val conf = Configuration(context.resources.configuration)
            conf.setLocale(locale)
            conf.setLayoutDirection(locale)
            context.createConfigurationContext(conf)
        } catch (e: Exception) {
            context
        }
    }

    fun isInitialized(): Boolean = base != null

    /**
     * Clears the cached localized context (call after the language changes).
     */
    fun refresh() {
        localized = null
        cachedLang = null
    }
}
