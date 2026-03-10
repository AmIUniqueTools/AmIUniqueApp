package com.amiunique.amiuniqueapp.presentation

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import com.amiunique.amiuniqueapp.R
import com.amiunique.amiuniqueapp.network.RetrofitInstance
import com.amiunique.amiuniqueapp.presentation.about.AboutFragment
import com.amiunique.amiuniqueapp.presentation.fingerprint.FingerprintFragment
import com.amiunique.amiuniqueapp.presentation.home.HomeFragment
import com.amiunique.amiuniqueapp.presentation.privacy_policy.PrivacyPolicyFragment
import com.amiunique.amiuniqueapp.presentation.settings.SettingsFragment
import com.amiunique.amiuniqueapp.presentation.settings.SettingsViewModel
import com.amiunique.amiuniqueapp.utils.main.MyNavigationManager
import com.amiunique.amiuniqueapp.utils.main.ReminderManager
import com.amiunique.amiuniqueapp.utils.settings.MySettingsManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView


class MainActivity : AppCompatActivity() {
    // Navigation drawer
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private lateinit var navigationView: NavigationView

    // Toolbar
    private lateinit var toolbar: MaterialToolbar

    // Bottom navigation bar
    private lateinit var bottomNav: BottomNavigationView

    // View models
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    // Settings manager
    private val mySettingsManager: MySettingsManager by lazy {
        MySettingsManager(PreferenceManager.getDefaultSharedPreferences(this))
    }

    // Navigation manager
    private val myNavigationManager: MyNavigationManager by lazy {
        MyNavigationManager(supportFragmentManager, resources)
    }

    // Ask for POST_NOTIFICATIONS on Android 13+ only
    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // Permission granted -> trigger now
                ReminderManager.scheduleWeeklyReminder(this,
                    mySettingsManager.getNotificationsTriggerTime(),
                    mySettingsManager.getNotificationsFrequencyDays()
                )
            } else {
                // Denied -> revert toggle silently
                ReminderManager.cancelWeeklyReminder(this)
                settingsViewModel.setIsConsentedNotifications(false)
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setGlobalExceptionHandler()
        // Init the Retrofit instance if Intent param sent
        intent.getStringExtra("API_END_POINT")
            ?.takeIf { it.isNotBlank() }
            ?.let {
                Log.i("API", "Using custom endpoint: $it")
                RetrofitInstance.init(it)
            }

        // Setup toolbar
        setUpToolbar()

        // Setup Navigation drawer
        setUpNavigationDrawer()

        // Setup bottom navigation
        setUpBottomNavigation()

        // Setup settings
        setUpSettings()

        // Setup navigation
        setUpNavigation()

        // Setup isConsentedFingerprint
        setUpIsConsentedFingerprint()

        // Setup isConsentedNotifications
        setUpIsConsentedNotifications()
    }

    private fun setUpIsConsentedNotifications() {

        settingsViewModel.isConsentedNotifications.observe(this) { isConsented ->
            if (isConsented) {
                // 1) Always Ask for permission
                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                ReminderManager.cancelWeeklyReminder(this)
            }
            mySettingsManager.updateNotificationsEnabled(isConsented)
        }


        settingsViewModel.notificationsTriggerTime.observe(this) { triggerTime ->
            mySettingsManager.setNotificationsTriggerTime(triggerTime)
        }

        settingsViewModel.notificationsFrequencyDays.observe(this) { frequency ->
            mySettingsManager.setNotificationsFrequencyDays(frequency)
        }

        // Init isConsentedNotifications
        settingsViewModel.setIsConsentedNotifications(
            mySettingsManager.getNotificationsEnabled()
        )

        settingsViewModel.setNotificationsFrequencyDays(
            mySettingsManager.getNotificationsFrequencyDays()
        )

        settingsViewModel.setNotificationsTriggerTime(
            mySettingsManager.getNotificationsTriggerTime()
        )
    }

    private fun setUpNavigation() {
        // Setup current displayed fragment
        mainActivityViewModel.currentFragment.observe(this) { currentFragment ->
            // Load the fragment
            toolbar.title = myNavigationManager.navigateToFragment(currentFragment)
        }
    }

    private fun setUpIsConsentedFingerprint(){
        mainActivityViewModel.isConsentedFingerprint.observe(this) { isConsented ->
            // Enable Fingerprint button in bottom navigation if consented
            val scanFingerprintBtn = findViewById<BottomNavigationView>(R.id.bottom_navigation)
                .menu.findItem(R.id.page_fingerprint)
            scanFingerprintBtn.isEnabled = isConsented
        }
    }

    private fun setUpSettings() {
        settingsViewModel.isNightMode.observe(this) {
            mySettingsManager.updateScreenMode(it)
        }
        /*settingsViewModel.appLang.observe(this) {
            // Update app language at runtime using mySettingsManager.updateAppLang
            val newContext = mySettingsManager.updateAppLang(this, it)
            if (newContext != null) {

            }
        }*/
        // Init isNightModeValue
        val screenModeDefault =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        settingsViewModel.setIsNightMode(mySettingsManager.getScreenModeValue(screenModeDefault))
        //settingsViewModel.setAppLang(mySettingsManager.getAppLangValue())
    }

    private fun setUpNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        actionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        navigationView = findViewById(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.about -> mainActivityViewModel.setCurrentFragment(AboutFragment())
                R.id.privacy_policy -> mainActivityViewModel.setCurrentFragment(
                    PrivacyPolicyFragment()
                )
                R.id.settings -> mainActivityViewModel.setCurrentFragment(SettingsFragment())
                R.id.website -> openWebsite()
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    /**
     * Open amiunique.org website
     */
    private fun openWebsite() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://amiunique.org")
        startActivity(intent)
    }

    private fun setUpToolbar() {
        toolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)
    }


    private fun setUpBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.page_home -> {
                    mainActivityViewModel.setCurrentFragment(HomeFragment())
                    true
                }
                R.id.page_fingerprint -> {
                    mainActivityViewModel.setCurrentFragment(FingerprintFragment())
                    true
                }
                else -> {
                    true
                }
            }
        }
    }

    // For initializing mode button icon
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //menuInflater.inflate(R.menu.top_bar_menu, menu)
        //val modeItem = menu.findItem(R.id.mode)
        return true
    }

    // Toolbar menu : dark/light mode button, menu button
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle the navigation icon click to open/close the drawer
        return when (item.itemId) {
            R.id.mode -> {
                // Toggle night mode
                true
            }
            else -> {
                if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
                    true
                } else super.onOptionsItemSelected(item)
            }
        }
    }


    private fun setGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log the exception or handle it as needed
            Log.e(
                "GlobalExceptionHandler",
                "Uncaught exception in thread ${thread.name}: ${throwable.message}",
                throwable
            )
        }
    }
}