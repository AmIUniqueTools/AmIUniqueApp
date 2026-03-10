package com.amiunique.amiuniqueapp.presentation.settings

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.amiunique.amiuniqueapp.R
import com.amiunique.amiuniqueapp.utils.settings.MySettingsManager

import com.google.android.material.timepicker.MaterialTimePicker


class SettingsFragment : PreferenceFragmentCompat() {
    private val settingsViewModel: SettingsViewModel by activityViewModels()
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)


        val screenModePreference: SwitchPreferenceCompat? = findPreference(MySettingsManager.KEY_SCREEN_MODE)
        val appLangPreference: ListPreference? = findPreference(MySettingsManager.KEY_APP_LANG)

        val notificationsPreference: SwitchPreferenceCompat? = findPreference(MySettingsManager.KEY_NOTIFICATIONS)
        val notificationsFrequencyDays: SeekBarPreference? = findPreference(MySettingsManager.KEY_FREQUENCY_DAYS)
        val notificationsTriggerTime: Preference? = findPreference(MySettingsManager.KEY_TRIGGER_TIME)

        setUpScreenModePreference(screenModePreference)
        setUpAppLangPreference(appLangPreference)
        setUpNotificationsPreference(notificationsPreference, notificationsFrequencyDays, notificationsTriggerTime)
    }

    private fun setUpScreenModePreference(screenModePreference: SwitchPreferenceCompat?) {
        screenModePreference?.isChecked = settingsViewModel.getIsNightMode()
        screenModePreference?.setOnPreferenceChangeListener { _, newValue ->
            // newValue will be a boolean indicating the new state of the switch
            if (newValue is Boolean) {
                settingsViewModel.setIsNightMode(newValue)
            }
            true // Return true to update the preference's state
        }
    }

    private fun setUpAppLangPreference(appLangPreference: ListPreference?) {
        appLangPreference?.value = settingsViewModel.getAppLang()
        appLangPreference?.setOnPreferenceChangeListener { _, newValue ->
            // newValue will be a boolean indicating the new state of the switch
            if (newValue is String) {
                settingsViewModel.setAppLang(newValue)
            }
            true // Return true to update the preference's state
        }
    }

    private fun setUpNotificationsPreference(notificationsPreference: SwitchPreferenceCompat?,
                                             notificationsFrequencyDays: SeekBarPreference?,
                                             notificationsTriggerDate: Preference?) {
        notificationsPreference?.isChecked = settingsViewModel.getIsConsentedNotifications()
        notificationsPreference?.setOnPreferenceChangeListener { _, newValue ->
            // newValue will be a boolean indicating the new state of the switch
            if (newValue is Boolean) {
                settingsViewModel.setIsConsentedNotifications(newValue)
            }
            true // Return true to update the preference's state
        }

        notificationsFrequencyDays?.value = settingsViewModel.getNotificationsFrequencyDays()
        notificationsFrequencyDays?.setOnPreferenceChangeListener { _, newValue ->
            // newValue will be a boolean indicating the new state of the switch
            val intValue = (newValue as? String)?.toIntOrNull()
            if (intValue != null) {
                settingsViewModel.setNotificationsFrequencyDays(intValue)
            }
            true // Return true to update the preference's state
        }


        notificationsTriggerDate?.summary =settingsViewModel.getNotificationsTriggerTime()

        notificationsTriggerDate?.setOnPreferenceClickListener {
            val (hour, minute) = getHourAndMinute(settingsViewModel.getNotificationsTriggerTime())
            showTimePicker(hour,minute) { pickedTime ->
                // 1) persist/update VM
                settingsViewModel.setNotificationsTriggerTime(pickedTime)

                // 2) update UI immediately (don’t wait to read back)
                notificationsTriggerDate.summary = pickedTime
            }
            true // Return true to update the preference's state
        }

    }
    private fun showTimePicker(initialHour:Int = 11, initialMinute:Int = 0, onPicked: (String) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTitleText(requireContext().getString(R.string.notification_trigger_summary),)
            .setHour(initialHour)
            .setMinute(initialMinute)
            .build()

        picker.addOnPositiveButtonClickListener {
            // Convert the selected time to milliseconds since epoch for consistency
            // return String in "HH:mm" format
            var pickedTime:String = ""
            if (picker.minute < 10) pickedTime = "${picker.hour}:0${picker.minute}"
            else pickedTime = "${picker.hour}:${picker.minute}"
            onPicked(pickedTime)
        }

        picker.show(parentFragmentManager, "triggerDate")
    }

    private fun getHourAndMinute(time:String): Pair<Int, Int> {
        val parts = time.split(":")
        if (parts.size == 2) {
            val hour = parts[0].toIntOrNull() ?: 11
            val minute = parts[1].toIntOrNull() ?: 0
            return Pair(hour, minute)
        }
        return Pair(11, 0) // Default to 11:00 if parsing fails
    }

}