package com.amiunique.exadprinterlib

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.io.IOException

private const val TAG = "ClassesJsonReader"
private const val DEFAULT_CLASSES_LIST_PATH = "classes.json"

class ClassesJsonReader {

    suspend fun readFromAssets(
        context: Context,
        path: String = DEFAULT_CLASSES_LIST_PATH
    ): List<String> {
        val json = withContext(Dispatchers.IO) {
            try {
                context.assets.open(path).bufferedReader().use { it.readText() }
            } catch (e: IOException) {
                Log.e(TAG, "Error reading JSON from assets", e)
                null
            }
        }

        return json?.let { parseJsonArray(it) } ?: emptyList()
    }

    suspend fun readFromFile(file: File): List<String> {
        val json = withContext(Dispatchers.IO) {
            try {
                file.bufferedReader().use { it.readText() }
            } catch (e: IOException) {
                Log.e(TAG, "Error reading JSON from file", e)
                null
            }
        }

        return json?.let { parseJsonArray(it) } ?: emptyList()
    }

    fun parseJsonArray(json: String): List<String> {
        return try {
            val jsonArray = JSONArray(json)
            List(jsonArray.length()) { i -> jsonArray.getString(i) }
        } catch (e: JSONException) {
            Log.e(TAG, "Error parsing JSON", e)
            emptyList()
        }
    }
}