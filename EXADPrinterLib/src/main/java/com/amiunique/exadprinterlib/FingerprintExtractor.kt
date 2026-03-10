package com.amiunique.exadprinterlib

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val TAG = "FingerprintExtractor"

class FingerprintExtractor(
    private val sdkExplorer: SDKExplorer,
    private val shellCommandsExplorer: ShellCommandsExplorer,
    private val contentProviderExplorer: ContentProviderExplorer,
    private val context: Context
) {
    /**
     * Extracts the fingerprint synchronously.
     * @return A list of JSONObject containing the extracted fingerprint results.
     */
    suspend fun extractFingerprint(): List<JSONObject> = withContext(Dispatchers.IO) {
        val mergedResults = mutableListOf<JSONObject>()
        // init execution timer for the fingerprinting process
        val startTime = System.currentTimeMillis()
        try {
            // Explore ADB asynchronously
            val adbResults = shellCommandsExplorer.exploreADB()

            // Explore SDK asynchronously
            val sdkResults = sdkExplorer.exploreSDK()

            // Explore content providers asynchronously
            contentProviderExplorer.setContentProvidersURIs(sdkExplorer.getContentProvidersURIs())
            val contentResults = contentProviderExplorer.exploreContentProviders()

            mergedResults.addAll(adbResults)
            mergedResults.addAll(sdkResults)
            mergedResults.addAll(contentResults)

            // add isDeveloperModeEnabled to the fingerprint
            mergedResults.add(JSONObject().apply {
                put("isDeveloperModeEnabled", isDeveloperEnabled(context))
            })
            // add isDeviceRooted to the fingerprint
            mergedResults.add(JSONObject().apply {
                put("isDeviceRooted", isDeviceRooted(context.packageManager))
            })
        } catch (e: Throwable) {
            Log.e(TAG, "Error while extracting fingerprint", e)
        }
        // Log the execution time
        val endTime = System.currentTimeMillis()
        Log.d(TAG, "Fingerprinting took ${endTime - startTime}ms")
        mergedResults.add(JSONObject().apply {
            put("execution_time", endTime - startTime)
        })
        // Return the merged results
        mergedResults
    }
}