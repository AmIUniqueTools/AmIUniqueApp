package com.amiunique.amiuniqueapp.presentation

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amiunique.amiuniqueapp.presentation.home.HomeFragment

class MainActivityViewModel : ViewModel() {

    private val _currentFragment = MutableLiveData<Fragment>(HomeFragment())
    val currentFragment: LiveData<Fragment> get() = _currentFragment

    private val _isConsentedFingerprint = MutableLiveData<Boolean>(false)
    val isConsentedFingerprint: LiveData<Boolean> get() = _isConsentedFingerprint

    fun setCurrentFragment(fragment: Fragment) {
        _currentFragment.postValue(fragment)
    }

    fun setIsConsentedFingerprint(isConsented: Boolean) {
        _isConsentedFingerprint.postValue(isConsented)
    }
}