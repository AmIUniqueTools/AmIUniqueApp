package com.amiunique.amiuniqueapp.presentation.fingerprint

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.amiunique.amiuniqueapp.R
import com.amiunique.amiuniqueapp.network.RetrofitInstance
import com.amiunique.amiuniqueapp.utils.fingerprint.getOrCreateUUID
import com.amiunique.amiuniqueapp.utils.fingerprint.selectJSONObjectsInToAttributesModels
import com.amiunique.exadprinterlib.ClassesJsonReader
import com.amiunique.exadprinterlib.ContentProviderExplorer
import com.amiunique.exadprinterlib.FingerprintExtractor
import com.amiunique.exadprinterlib.InstanceFactory
import com.amiunique.exadprinterlib.SDKExplorer
import com.amiunique.exadprinterlib.ShellCommandsExplorer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FingerprintRepository(private val applicationContext: Context) {
    private val fingerprintLiveData = MutableLiveData<FingerprintModel>()
    private lateinit var fingerprintExtractor: FingerprintExtractor

    // 0 : No loading - 1 - 5000: Fingerprint extraction and Fingerprint processing
    private val fingerprintLoadingStateProgress = MutableLiveData<Int>(0)
    private val fingerprintLoadingStateString = MutableLiveData<String>("")

    private suspend fun extractFP(): List<JSONObject> = withContext(Dispatchers.IO) {
        return@withContext fingerprintExtractor.extractFingerprint()
    }

    private fun saveFingerprintToCache(
        fingerprint: List<JSONObject>,
        cacheFile: File,
        cacheTimestampFile: File
    ) {
        // Save the fingerprint JSON to cache
        cacheFile.writeText(fingerprint.toString()) // Convert the list to JSON string

        // Save the current timestamp
        cacheTimestampFile.writeText(System.currentTimeMillis().toString())
    }

    suspend fun fetchFingerprint() = withContext(Dispatchers.IO) {
        val cacheFile = File(applicationContext.cacheDir, "fingerprint_cache.json")
        val cacheTimestampFile =
            File(applicationContext.cacheDir, "fingerprint_cache_timestamp.txt")
        var cachedTimestamp = -1L
        val currentTime = System.currentTimeMillis()
        var fp = mutableListOf<JSONObject>()
        // Extract the fingerprint attributes

        // Check if the cache exists and is less than 5 minutes old
        if (cacheFile.exists() && cacheTimestampFile.exists()) {
            cachedTimestamp = cacheTimestampFile.readText().toLong()

        }
        // Check if the cached fingerprint is less than 5 minutes old (5 * 60 * 1000 ms)
        if (cachedTimestamp != -1L && (currentTime - cachedTimestamp) < 5 * 60 * 1000) {
            fingerprintLoadingStateString.postValue("Reading cached fingerprint")
            val cachedData = cacheFile.readText()
            // Parse the cached data into a list of JSONObjects
            val jsonArray = JSONArray(cachedData)
            for (i in 0 until jsonArray.length()) {
                fp.add(jsonArray.getJSONObject(i))
            }
            fingerprintLoadingStateProgress.postValue(2500)
        } else {
            val adbExplorer = ShellCommandsExplorer(loadingProgress = fingerprintLoadingStateProgress, loadingString = fingerprintLoadingStateString)
            val classesList = ClassesJsonReader().readFromAssets(applicationContext)
            val instanceFactory = InstanceFactory(applicationContext)
            val sdkExplorer = SDKExplorer(instanceFactory, classesList, fingerprintLoadingStateProgress, fingerprintLoadingStateString)
            val contentProviderExplorer = ContentProviderExplorer(applicationContext, fingerprintLoadingStateProgress, fingerprintLoadingStateString)
            fingerprintExtractor =
                FingerprintExtractor(sdkExplorer, adbExplorer, contentProviderExplorer, applicationContext)

            // Else, extract the fingerprint
            fp = extractFP().toMutableList()

            // Save the new fingerprint and the current timestamp to the cache
            saveFingerprintToCache(fp, cacheFile, cacheTimestampFile)
            fingerprintLoadingStateProgress.postValue(4000)
            fingerprintLoadingStateString.postValue("Compression and sending fingerprint")
            sendFingerprintToBackend(fp)
            fingerprintLoadingStateProgress.postValue(4900)
        }
        // filter out attributes that are not displayed

        val attributes = selectJSONObjectsInToAttributesModels(applicationContext, fp)

        val fingerprintModel = FingerprintModel(attributes)

        fingerprintLoadingStateProgress.postValue(5000)
        fingerprintLiveData.postValue(fingerprintModel)
        // Reset the loading state
        fingerprintLoadingStateProgress.postValue(0)
        fingerprintLoadingStateString.postValue("")
    }

    private suspend fun sendFingerprintToBackend(fp: MutableList<JSONObject>) {
        withContext(Dispatchers.IO) {
            try {
                val (filePart, uuidPart) = createRequestBody(fp)
                val response = RetrofitInstance.api.sendFingerprint(filePart, uuidPart)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // Show success toast
                        Toast.makeText(
                            applicationContext,
                            applicationContext.getString(R.string.fingerprint_sent_successfully),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Show error toast
                        Toast.makeText(
                            applicationContext,
                            applicationContext.getString(R.string.fingerprint_failed_to_send),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // Handle network or server errors
            }
        }
    }

    fun getFingerprintLiveData(): LiveData<FingerprintModel> {
        return fingerprintLiveData
    }

    fun getFingerprintLoadingStateData(): LiveData<Int> {
        return fingerprintLoadingStateProgress
    }

    fun getFingerprintLoadingStateString(): LiveData<String> {
        return fingerprintLoadingStateString
    }

    private fun compressIntoZipFile(data: List<JSONObject>): File {
        // Compress data into a ZIP file
        val zipFile = File.createTempFile("data", ".zip")
        FileOutputStream(zipFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                val byteArrayOutputStream = ByteArrayOutputStream()
                byteArrayOutputStream.write("[".toByteArray())
                data.forEachIndexed { index, jsonObject ->
                    byteArrayOutputStream.write(jsonObject.toString().toByteArray())
                    if (index < data.size - 1) {
                        byteArrayOutputStream.write(",".toByteArray())
                    } else {
                        byteArrayOutputStream.write("]".toByteArray())
                    }
                }

                val zipEntry = ZipEntry("data.json")
                zos.putNextEntry(zipEntry)
                zos.write(byteArrayOutputStream.toByteArray())
                zos.closeEntry()  // Close each entry properly
                zos.flush()       // Flush to ensure all data is written
            }
        }
        return zipFile
    }

    private fun createRequestBody(data: MutableList<JSONObject>): Pair<MultipartBody.Part, RequestBody> {
        val myUuid =
            getOrCreateUUID((PreferenceManager.getDefaultSharedPreferences(applicationContext))) // Generate UUID as a String
        // add the UUID to the data
        data.add(JSONObject().put("uuid", myUuid))
        val zipFile = compressIntoZipFile(data)
        val requestFile = zipFile.asRequestBody("application/zip".toMediaTypeOrNull())

        // Create the multipart body for the file
        val filePart = MultipartBody.Part.createFormData("file", zipFile.name, requestFile)

        // Create the request body for the UUID
        val uuidPart = myUuid.toRequestBody("text/plain".toMediaTypeOrNull())
        return Pair(filePart, uuidPart)
    }
}