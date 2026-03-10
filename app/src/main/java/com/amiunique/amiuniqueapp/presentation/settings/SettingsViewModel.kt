package com.amiunique.amiuniqueapp.presentation.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Date

class SettingsViewModel : ViewModel() {
    private var _isNightMode = MutableLiveData(false)
    val isNightMode: LiveData<Boolean> get() = _isNightMode

    private var _appLang = MutableLiveData("en")
    val appLang: LiveData<String> get() = _appLang

    private val _isConsentedNotifications = MutableLiveData<Boolean>(false)
    val isConsentedNotifications: LiveData<Boolean> get() = _isConsentedNotifications

    private val _notificationsFrequencyDays = MutableLiveData<Int>(7)
    val notificationsFrequencyDays: LiveData<Int> get() = _notificationsFrequencyDays

    private val _notificationsTriggerTime = MutableLiveData<String>("11:00")
    val notificationsTriggerTime: LiveData<String> get() = _notificationsTriggerTime


    fun setIsNightMode(isNightMode: Boolean) {
        _isNightMode.postValue(isNightMode)
    }

    fun getIsNightMode(): Boolean {
        return isNightMode.value!!
    }

    fun setAppLang(appLang: String) {
        _appLang.postValue(appLang)
    }

    fun getAppLang(): String {
        return appLang.value!!
    }

    fun getIsConsentedNotifications(): Boolean {
        return isConsentedNotifications.value!!
    }

    fun setIsConsentedNotifications(isConsented: Boolean) {
        _isConsentedNotifications.postValue(isConsented)
    }

    fun getNotificationsFrequencyDays(): Int {
        return notificationsFrequencyDays.value!!
    }

    fun setNotificationsFrequencyDays(frequencyDays: Int) {
        _notificationsFrequencyDays.postValue(frequencyDays)
    }

    fun getNotificationsTriggerTime(): String {
        return notificationsTriggerTime.value!!
    }

    fun setNotificationsTriggerTime(triggerTime: String) {
        _notificationsTriggerTime.postValue(triggerTime)
    }
}