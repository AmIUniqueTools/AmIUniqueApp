package com.amiunique.amiuniqueapp.utils.main

import android.content.res.Resources
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.amiunique.amiuniqueapp.R
import com.amiunique.amiuniqueapp.presentation.about.AboutFragment
import com.amiunique.amiuniqueapp.presentation.fingerprint.FingerprintFragment
import com.amiunique.amiuniqueapp.presentation.home.HomeFragment
import com.amiunique.amiuniqueapp.presentation.privacy_policy.PrivacyPolicyFragment
import com.amiunique.amiuniqueapp.presentation.settings.SettingsFragment

class MyNavigationManager(
    private val supportFragmentManager: FragmentManager,
    private val resources: Resources
) {

    fun navigateToFragment(
        fragment: Fragment,
        toPrevious: Boolean = false,
        push: Boolean = false
    ): String {
        if (toPrevious) loadPreviousFragment()
        else loadFragment(fragment, push)

        return when (fragment) {
            is HomeFragment -> resources.getString(R.string.app_name)
            is FingerprintFragment -> resources.getString(R.string.fingerprint)
            is AboutFragment -> resources.getString(R.string.About)
            is PrivacyPolicyFragment -> resources.getString(R.string.PrivacyPolicy)
            is SettingsFragment -> resources.getString(R.string.Settings)
            else -> resources.getString(R.string.app_title)
        }
    }

    private fun loadFragment(
        fragment: Fragment,
        push: Boolean = false
    ) {
        val transaction = supportFragmentManager.beginTransaction()
        if (push) {
            transaction.add(R.id.container, fragment)
            transaction.addToBackStack(null)
        } else transaction.replace(R.id.container, fragment)
        transaction.commit()
    }

    private fun loadPreviousFragment() {
        supportFragmentManager.popBackStack()
    }
}

