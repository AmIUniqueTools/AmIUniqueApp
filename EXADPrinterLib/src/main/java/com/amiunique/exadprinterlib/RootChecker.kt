package com.amiunique.exadprinterlib

import android.content.pm.PackageManager
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


fun isDeviceRooted(packageManager: PackageManager): Boolean {
    return checkForKnownRootFiles() ||
            checkForSUBinary() ||
            checkForBusyBox() ||
            checkForRootManagementApps(packageManager) ||
            !verifySystemIntegrity() ||
            searchForMagisk(packageManager)
}
/**
 * Code by https://github.com/Somchandra17/RootAppChecker
 */
private fun checkForKnownRootFiles(): Boolean {
    val paths = arrayOf(
        "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
        "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
        "/system/bin/failsafe/su", "/data/local/su"
    )
    for (path in paths) {
        if (File(path).exists()) return true
    }
    return false
}

private fun checkForSUBinary(): Boolean {
    return checkCommand("which su")
}

private fun checkForBusyBox(): Boolean {
    return checkCommand("which busybox")
}

private fun checkForRootManagementApps(packageManager: PackageManager): Boolean {
    return packageExists(packageManager,"com.noshufou.android.su") ||
            packageExists(packageManager,"com.thirdparty.superuser") ||
            packageExists(packageManager,"eu.chainfire.supersu") ||
            packageExists(packageManager,"com.topjohnwu.magisk")
}

private fun verifySystemIntegrity(): Boolean {
    val buildTags = Build.TAGS
    return buildTags == null || !buildTags.contains("test-keys")
}

private fun searchForMagisk(packageManager: PackageManager): Boolean {
    return File("/sbin/.magisk").exists() ||
            File("/sbin/.core/mirror").exists() ||
            packageExists(packageManager ,"com.topjohnwu.magisk")
}

private fun checkCommand(command: String): Boolean {
    var process: Process? = null
    try {
        process = Runtime.getRuntime().exec(command)
        val `in` = BufferedReader(InputStreamReader(process.inputStream))
        return `in`.readLine() != null
    } catch (e: Exception) {
        return false
    } finally {
        process?.destroy()
    }
}

private fun packageExists(packageManager: PackageManager, targetPackage: String): Boolean {
    try {
        packageManager.getPackageInfo(targetPackage, 0)
        return true
    } catch (e: PackageManager.NameNotFoundException) {
        return false
    }
}
