package com.amiunique.amiuniqueapp.utils.fingerprint

import android.content.Context
import android.content.SharedPreferences
import com.amiunique.amiuniqueapp.R
import com.amiunique.amiuniqueapp.presentation.fingerprint.AttributeModel
import org.json.JSONObject
import java.util.UUID

fun selectJSONObjectsInToAttributesModels(
    context: Context,
    jsonObjects: List<JSONObject>
): ArrayList<AttributeModel> {
    val attributes = ArrayList<AttributeModel>()
    val FINGERPRINT_DISPLAY_ATTRIBUTES = mapOf(
        Pair(
            "android.provider.Settings.Secure.ANDROID_ID",
            context.getString(R.string.android_id)
        ),
        Pair(
            "android.os.Build.MODEL",
            context.getString(R.string.device_model)
        ),
        Pair(
            "android.os.Build.BRAND",
            context.getString(R.string.device_brand)
        ),
        Pair(
            "android.os.Build.MANUFACTURER",
            context.getString(R.string.device_manufacturer)
        ),
        Pair(
            "android.os.Build.VERSION.SDK_INT",
            context.getString(R.string.sdk_version)
        ),
        Pair(
            "android.os.Build.VERSION.RELEASE",
            context.getString(R.string.android_version)
        ),
        Pair(
            "android.os.Build.VERSION.SECURITY_PATCH",
            context.getString(R.string.security_patch)
        ),
        Pair(
            "android.os.Build.BOARD",
            context.getString(R.string.board)
        ),
        Pair(
            "android.os.Build.CPU_ABI",
            context.getString(R.string.cpu_abi)
        ),
        Pair(
            "android.os.Build.DEVICE",
            context.getString(R.string.device)
        ),
        Pair(
            "android.os.Build.FINGERPRINT",
            context.getString(R.string.build_fingerprint)
        ),
        Pair(
            "android.os.Build.HARDWARE",
            context.getString(R.string.hardware)
        ),
        Pair(
            "android.os.Build.HOST",
            context.getString(R.string.host)
        ),
        Pair(
            "android.os.Build.ID",
            context.getString(R.string.id)
        ),
        Pair(
            "android.os.Build.TIME",
            context.getString(R.string.build_time)
        ),
        Pair(
            "android.os.Build.getRadioVersion",
            context.getString(R.string.radio_version)
        ),
        Pair(
            "java.util.Locale.getAvailableLocales",
            context.getString(R.string.available_locales)
        ),
        Pair(
            "android.text.format.DateFormat.is24HourFormat",
            context.getString(R.string.hour_format)
        ),
        Pair(
            "android.text.format.DateFormat.getDateFormatOrder",
            context.getString(R.string.date_format_order)
        ),
        Pair(
            "android.provider.Settings.System.SCREEN_BRIGHTNESS",
            context.getString(R.string.screen_brightness)
        ),
        Pair(
            "android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE",
            context.getString(R.string.screen_brightness_mode)
        ),
        Pair(
            "android.provider.Settings.System.FONT_SCALE",
            context.getString(R.string.font_scale)
        ),
        Pair(
            "android.provider.Settings.System.ACCELEROMETER_ROTATION",
            context.getString(R.string.accelerometer_rotation)
        ),
        Pair(
            "android.provider.Settings.System.ADB_ENABLED",
            context.getString(R.string.adb_enabled)
        ),
        Pair(
            "android.provider.Settings.System.AIRPLANE_MODE_ON",
            context.getString(R.string.airplane_mode_on)
        ),
        Pair(
            "android.provider.Settings.System.SCREEN_OFF_TIMEOUT",
            context.getString(R.string.screen_off_timeout)
        ),
        Pair(
            "android.provider.Settings.System.VOLUME_ALARM",
            context.getString(R.string.volume_alarm)
        ),
        Pair(
            "android.provider.Settings.System.VOLUME_MUSIC",
            context.getString(R.string.volume_music)
        ),
        Pair(
            "android.provider.Settings.System.VOLUME_NOTIFICATION",
            context.getString(R.string.volume_notification)
        ),
        Pair(
            "android.provider.Settings.System.VOLUME_RING",
            context.getString(R.string.volume_ring)
        ),
        Pair(
            "android.provider.Settings.Global.AUTO_TIME",
            context.getString(R.string.auto_time)
        ),
        Pair(
            "android.provider.Settings.Global.AUTO_TIME_ZONE",
            context.getString(R.string.auto_time_zone)
        ),
        Pair(
            "android.provider.Settings.Global.WIFI_ON",
            context.getString(R.string.wifi_on)
        ),
        Pair(
            "android.provider.Settings.Global.BLUETOOTH_ON",
            context.getString(R.string.bluetooth_on)
        ),
        Pair(
            "android.provider.Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON",
            context.getString(R.string.wifi_networks_available_notification_on)
        ),
        Pair(
            "android.provider.Settings.Global.WIFI_SLEEP_POLICY",
            context.getString(R.string.wifi_sleep_policy)
        ),
        Pair(
            "android.provider.Settings.Global.LOCK_PATTERN_ENABLED",
            context.getString(R.string.lock_pattern_enabled)
        ),
        Pair(
            "system_uptime",
            context.getString(R.string.system_uptime)
        ),
        Pair(
            "ringtones_list",
            context.getString(R.string.ringtones_list)
        ),
        Pair(
            "ringtones_list_ext",
            context.getString(R.string.ringtones_list_ext)
        ),
        Pair(
            "system_typefaces",
            context.getString(R.string.system_typefaces)
        )
    )

    for ((key,value) in FINGERPRINT_DISPLAY_ATTRIBUTES.entries) {
        // Check if the lowercase key is in the JSONObjects lowercase keys

        if (jsonObjects.any { it.has(key) }) {
            val objValue = jsonObjects.first { it.has(key) }.get(key)
            val attribute = AttributeModel(
                attribute = value,
                description = key,
                value = objValue.toString()
            )
            attributes.add(attribute)
        }
    }
    return attributes
}

fun getOrCreateUUID(sharedPreferences: SharedPreferences): String {
    val uuidKey = "user_uuid"

    // Check if UUID already exists in SharedPreferences
    val existingUUID = sharedPreferences.getString(uuidKey, null)
    if (existingUUID != null)
    // Return existing UUID
        return existingUUID

    // Generate new UUID
    val newUUID = UUID.randomUUID().toString()

    // Save new UUID to SharedPreferences
    with(sharedPreferences.edit()) {
        putString(uuidKey, newUUID)
        apply()
    }

    return newUUID
}

