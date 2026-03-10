package com.amiunique.amiuniqueapp.utils.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import java.util.*

class MySettingsManager(private val sharedPreferences: SharedPreferences) {
    companion object {
        const val KEY_SCREEN_MODE = "screen_mode"
        const val KEY_APP_LANG = "app_lang"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_FREQUENCY_DAYS = "notification_frequency_days"
        const val KEY_TRIGGER_TIME = "notification_trigger_time"
    }

    fun getScreenModeValue(defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(KEY_SCREEN_MODE, defaultValue)
    }

    fun updateScreenMode(isNightMode: Boolean) {
        AppCompatDelegate.setDefaultNightMode(if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
    }

    fun getNotificationsEnabled(defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS, defaultValue)
    }

    fun updateNotificationsEnabled(notificationsEnabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS, notificationsEnabled).apply()
    }

    fun setNotificationsFrequencyDays(frequencyDays: Int) {
        sharedPreferences.edit().putInt(KEY_FREQUENCY_DAYS, frequencyDays).apply()
    }

    fun getNotificationsFrequencyDays(defaultValue: Int = 7): Int {
        return try {
            sharedPreferences.getInt(KEY_FREQUENCY_DAYS, defaultValue)
        } catch (e: ClassCastException) {
            // In case the stored value is a String (from previous versions), we catch the exception and handle it
            defaultValue
        }

    }

    // Store time as "HH:mm" string
    fun setNotificationsTriggerTime(triggerTime: String) {
        sharedPreferences.edit().putString(KEY_TRIGGER_TIME, triggerTime).apply()
    }
    // Retrieve time as "HH:mm" string
    fun getNotificationsTriggerTime(defaultValue: String = "11:00"): String {
        return try {
            sharedPreferences.getString(KEY_TRIGGER_TIME, defaultValue)!!
        } catch (e: ClassCastException) {
            defaultValue
        }
    }


    fun getAppLangValue(): String {
        return sharedPreferences.getString(KEY_APP_LANG, Locale.getDefault().language)!!
    }

    fun updateAppLang(context: Context, language: String): Context? {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
            context.createConfigurationContext(configuration)
        } else {
            val resources = context.resources
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            null
        }
    }

}