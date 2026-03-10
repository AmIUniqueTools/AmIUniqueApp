package com.amiunique.amiuniqueapp.presentation.home


import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.amiunique.amiuniqueapp.R
import com.amiunique.amiuniqueapp.presentation.fingerprint.FingerprintFragment
import com.amiunique.amiuniqueapp.presentation.MainActivityViewModel
import com.amiunique.amiuniqueapp.presentation.privacy_policy.PrivacyPolicyFragment
import com.amiunique.amiuniqueapp.presentation.settings.SettingsFragment
import com.amiunique.amiuniqueapp.presentation.settings.SettingsViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {
    private lateinit var scanFingerprintBtn: Button
    private lateinit var checkoxConsent: CheckBox
    private lateinit var notifConsent: CheckBox
    private lateinit var privacyPolicyLink: TextView
    private lateinit var settingsLink: TextView
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val settingsViewModel: SettingsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_home, container, false)
        // Setup Fingerprints card
        setUpFingerprintButton(view)
        setUpNotificationChecker(view)

        // Setup Privacy Policy link
        privacyPolicyLink = view.findViewById(R.id.openPrivacyPolicy)

        privacyPolicyLink.setOnClickListener {
            mainActivityViewModel.setCurrentFragment(PrivacyPolicyFragment())
        }

        // Setup Settings link
        settingsLink = view.findViewById(R.id.openSettings)
        settingsLink.setOnClickListener {
            mainActivityViewModel.setCurrentFragment(SettingsFragment())
        }
        return view
    }

    private fun setUpFingerprintButton(view: View) {
        scanFingerprintBtn = view.findViewById(R.id.scanFingerprint)
        checkoxConsent = view.findViewById(R.id.consentFp)
        checkoxConsent.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Checkbox is checked
                mainActivityViewModel.setIsConsentedFingerprint(true)
                scanFingerprintBtn.isEnabled = true
            } else {
                // Checkbox is unchecked
                mainActivityViewModel.setIsConsentedFingerprint(false)
                scanFingerprintBtn.isEnabled = false
            }
        }
        // Show fingerprint fragment when clicking on card
        scanFingerprintBtn.setOnClickListener {
            mainActivityViewModel.setCurrentFragment(FingerprintFragment())
            updateBottomNavigationMenu(R.id.page_fingerprint,requireActivity().findViewById(R.id.bottom_navigation))
        }
    }

    private fun updateBottomNavigationMenu(itemId: Int, bottomNavigationView: BottomNavigationView) {
        val menu: Menu = bottomNavigationView.menu
        val item: MenuItem = menu.findItem(itemId)
        item.isChecked = true
    }

    private fun setUpNotificationChecker(view: View){
        notifConsent = view.findViewById(R.id.notifConsent)
        notifConsent.setOnClickListener {
            settingsViewModel.setIsConsentedNotifications(notifConsent.isChecked)
        }
        settingsViewModel.isConsentedNotifications.observe(viewLifecycleOwner) { isConsented ->
            if (isConsented != notifConsent.isChecked) {
                notifConsent.isChecked = isConsented
            }
        }
    }
}