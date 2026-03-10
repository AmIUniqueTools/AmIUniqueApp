package com.amiunique.exadprinterlib

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "ContentProviderExplorer"
private const val TAG_FP = "FP"

/**
 * ContentProviderExplorer is a utility class for exploring the content providers
 */
class ContentProviderExplorer(private val applicationContext: Context,
                              private val loadingProgress: MutableLiveData<Int>? = null,
                              private val loadingString: MutableLiveData<String>? = null
) {
    private var contentProvidersURIs: Set<String> = HashSet()

    /**
     * This method is used to set the URIs of the ContentProvidersExplorer
     * @param contentProvidersURIs : Set of URIs of the content providers
     */
    fun setContentProvidersURIs(contentProvidersURIs: Set<String>) {
        this.contentProvidersURIs = contentProvidersURIs
    }

    /**
     * This method is used to explore the content providers asynchronously using coroutines.
     *
     * @return A list of JSONObjects containing the content provider results.
     */
    suspend fun exploreContentProviders(): List<JSONObject> = withContext(Dispatchers.IO) {
        val attributesList: MutableList<JSONObject> = ArrayList()

        for (uri in contentProvidersURIs) {
            loadingString?.postValue("Exploring $uri")
            loadingProgress?.postValue(loadingProgress?.value?.plus(1))
            try {
                // Replace with the actual method to query the content provider
                val attributeValue: JSONArray = getContentProviderValue(applicationContext, uri)
                val jsonOutput = JSONObject()
                // Add content provider to output like: "content://settings/global" : {"value" : [{"name":"bluetooth_on","value":"0"}]}
                jsonOutput.put(uri, attributeValue)
                attributesList.add(jsonOutput)
            } catch (e: Throwable) {
                Log.e(TAG, "Error while exploring content providers", e)
            }
        }

        return@withContext attributesList
    }
}
