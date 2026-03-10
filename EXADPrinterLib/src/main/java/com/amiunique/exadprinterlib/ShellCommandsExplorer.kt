package com.amiunique.exadprinterlib

import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

private const val TAG: String = "ShellCommandsExplorer"
private const val TAG_FP: String = "FP"

class ShellCommandsExplorer(private val commandsMap: Map<String, String> = DEFAULT_COMMANDS,
                            private val loadingProgress: MutableLiveData<Int>? = null,
                            private val loadingString: MutableLiveData<String>? = null
) {
    companion object {
        val DEFAULT_COMMANDS: Map<String, String> = hashMapOf(
            // **CPU and Hardware Information**
            "cpu_information" to "lscpu",
            "memory_information" to "free -m",
            "device_tree" to "lshw",
            "storage_information" to "lsblk",
            "acpi_battery" to "acpi -V",
            "nproc" to "nproc",
            "lsmod" to "lsmod",
            "lspci" to "lspci",
            "lsusb" to "lsusb",

            // **File System and Storage**
            "system_root_structure" to "ls -l",
            "system_typefaces" to "ls -l /system/fonts",
            "ringtones_list" to "ls -l /system/media/audio/ringtones",
            "ringtones_list_ext" to "ls -l /system_ext/media/audio/ringtones",
            "df" to "df -h",

            // **Kernel and OS Information**
            "kernel_information" to "uname -a",
            "distribution_information" to "lsb_release -a",
            "system_uptime" to "uptime -s",
            "sysctl" to "sysctl -a",
            "system_conf_vars" to "getconf -a",

            // **System and User Information**
            "installed_packages" to "dpkg -l",
            "running_processes" to "ps aux",
            "user_accounts" to "cat /etc/passwd",
            "groups" to "cat /etc/group",
            "hostname" to "hostname",
            "hwclock" to "hwclock",
            "tty" to "tty",
            "ssty_active" to "stty -a",

            // **System Logs**
            "dmesg_first_1000_lines" to "dmesg -T | head -n 1000",
            "dmesg_last_1000_lines" to "dmesg -T | tail -n 1000",
            "authentication_logs" to "cat /var/log/auth.log",

            // **Network Information**
            "network_interfaces" to "ifconfig",
            "routing_table" to "route",
            "routing_table_n" to "route -n",
            "netstat" to "netstat",
            "arp_cache" to "arp -a",

            // **Network Properties (Getprop)**
            "getprop_net_dns1" to "getprop net.dns1",
            "getprop_net_dns2" to "getprop net.dns2",
            "getprop_net_dns3" to "getprop net.dns3",
            "getprop_net_dns4" to "getprop net.dns4",

            // **Android System Services**
            "dumpsys" to "dumpsys",

            // **Android System Properties**
            "getprop" to "getprop",

            // **Memory and CPU Information**
            "meminfo" to "cat /proc/meminfo",
            "cpuinfo" to "cat /proc/cpuinfo",

            // **Package Information**
            "package_list" to "pm list packages",
            "permission_list" to "pm list permissions -g",
            "feature_list" to "pm list features",
            "sessions_list" to "pm list staged-sessions"
        )
    }

    /**
     * Executes a given system command asynchronously using a coroutine and returns its output as a JSONArray.
     *
     * @param command The system command to execute.
     * @return The output of the executed command as a JSONArray.
     */
    private suspend fun commandExecutor(command: String): JSONArray = withContext(Dispatchers.IO) {
        val outputArray = JSONArray()
        var process: Process? = null
        var reader: BufferedReader? = null
        try {
            process = Runtime.getRuntime().exec(command)
            reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                outputArray.put(line)
            }
            process.waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command: $command", e)
            outputArray.put(getStringValueOfException(e))
        } finally {
            try {
                reader?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to close reader", e)
            }
            process?.destroy()
        }
        outputArray
    }

    /**
     * Suspended method to explore the device by executing a set of predefined system commands asynchronously.
     *
     * @return A list of JSONObjects containing the results of the commands.
     */
    suspend fun exploreADB(): List<JSONObject> = withContext(Dispatchers.IO) {
        val results: MutableList<JSONObject> = ArrayList()
        try {
            for ((attributeName, value) in commandsMap) {
                loadingString?.postValue("Executing $value")
                loadingProgress?.postValue(loadingProgress?.value?.plus(1))
                val attributeValue = commandExecutor(value)
                val jsonOutput = JSONObject().apply {
                    put(attributeName, attributeValue)
                }
                results.add(jsonOutput)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error while exploring device", e)
        }
        results
    }
}
