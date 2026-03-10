package com.amiunique.exadprinterlib

import android.util.Log
import androidx.lifecycle.MutableLiveData

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.reflect.Field
import java.lang.reflect.Method

private const val TAG = "SDKExplorer"
private const val TAG_FP = "FP"
private const val MAX_DEPTH = 1

/**
 * This class is responsible for exploring the SDK.
 */
class SDKExplorer(
    private val instanceFactory: InstanceFactory,
    private val classesList: List<String>,
    private val loadingProgress: MutableLiveData<Int>? = null,
    private val loadingString: MutableLiveData<String>? = null
) {
    private val contentProvidersURIs = mutableSetOf<String>()
    private val uiClasses: Set<String> = setOf(
        "android.webkit.WebView",
    )
    private val blackListClasses: Set<String> = setOf("android.content.ClipboardManager", "android.text.ClipboardManager",)
    private val blackListMethods: Set<String> = setOf(
        "getPrimaryClip", "hasPrimaryClip", "getPrimaryClipDescription", "getText", "getTextDescription", "hasText",
    )

    private val objectExplorer: ObjectExplorer = ObjectExplorer(instanceFactory)

    /**
     * This method is used to get the URIs of the content providers.
     *
     * @return Set of URIs of the content providers
     */
    fun getContentProvidersURIs(): Set<String> {
        return contentProvidersURIs
    }

    /**
     * This method is used to explore the SDK asynchronously using coroutines.
     *
     */
    suspend fun exploreSDK(): List<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val attributesMutableList = mutableListOf<JSONObject>()
            for (className in classesList) {
                loadingString?.postValue("Exploring $className")
                loadingProgress?.postValue(loadingProgress?.value?.plus(1))
                val taskResult = exploreClass(className)
                attributesMutableList.addAll(taskResult)
            }
            return@withContext attributesMutableList
        } catch (e: Throwable) {
            Log.e(TAG, "Error while exploring SDK", e)
            return@withContext emptyList()
        }
    }

    /**
     * Explores the class in a coroutine.
     */
    private suspend fun exploreClass(className: String): List<JSONObject> =
        withContext(Dispatchers.IO) {
            val attributesList = mutableListOf<JSONObject>()
            try {
                val cls = Class.forName(className)
                if (uiClasses.any { className.contains(it) }) {
                    withContext(Dispatchers.Main) {
                        val instance = instanceFactory.getInstance(cls)
                        attributesList.addAll(getAllClassFields(cls, instance))
                        attributesList.addAll(getAllClassMethods(cls, instance))
                    }
                } else {
                    val instance = instanceFactory.getInstance(cls)
                    attributesList.addAll(getAllClassFields(cls, instance))
                    attributesList.addAll(getAllClassMethods(cls, instance))
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error while exploring class $className", e)
            }
            return@withContext attributesList
        }

    /**
     * This method is used to get all the fields of the class.
     *
     * @param cls      : Class Class to explore
     * @param instance : Object Instance of the class
     * @return List of fields of the class
     */
    private suspend fun getAllClassFields(cls: Class<*>, instance: Any?): List<JSONObject> =
        withContext(Dispatchers.IO) {
            val attributesList = mutableListOf<JSONObject>()
            try {
                val fields: Array<Field> = cls.fields
                for (field in fields) {
                    field.isAccessible = true
                    val modifiers = field.modifiers
                    val attributeName = "${cls.canonicalName}.${field.name}"
                    val jsonOutput = JSONObject()
                    try {
                        val valueObj: Any?
                        val attributeValue: Any?
                        val objInstance = if (isStatic(modifiers)) null else instance
                        valueObj = field.get(objInstance)
                        if (isContentProviderUri(valueObj.toString())) {
                            contentProvidersURIs.add(valueObj.toString())
                        }
                        attributeValue = valueObj?.let {
                            getAttributeValue(
                                field,
                                it,
                                getMethodByName(cls, "getString"),
                                instance
                            )
                        } ?: "FNC"
                        jsonOutput.put(attributeName, attributeValue)
                        if (isContentProviderUri(attributeValue.toString())) {
                            contentProvidersURIs.add(attributeValue.toString())
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error while exploring field $attributeName", e)
                        jsonOutput.put(attributeName, getStringValueOfException(e))
                    }
                    if (jsonOutput.length() > 0) attributesList.add(jsonOutput)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error while exploring fields of class ${cls.canonicalName}", e)
            }
            return@withContext attributesList
        }

    private fun getAttributeValue(
        field: Field,
        valueObj: Any?,
        method: Method?,
        instance: Any?
    ): Any {
        return if (field.name.equals(valueObj.toString(), ignoreCase = true)) {
            if (method != null && isKeyValueAccessPattern(method)) {
                val parameterTypes = method.parameterTypes
                val param1 = instanceFactory.getDefaultParameter(parameterTypes[0])
                method.invoke(instance, param1, valueObj.toString())
            } else {
                objectExplorer.exploreObject(valueObj, MAX_DEPTH)
            }
        } else {
            objectExplorer.exploreObject(valueObj, MAX_DEPTH)
        }
    }

    /**
     * This method is used to get all the methods of the class.
     *
     * @param cls      : Class Class to explore
     * @param instance : Object Instance of the class
     * @return List of methods of the class
     */
    private suspend fun getAllClassMethods(cls: Class<*>, instance: Any?): List<JSONObject> =
        withContext(Dispatchers.IO) {
            val attributesList = mutableListOf<JSONObject>()
            try {

                val methods: Array<Method> = cls.methods
                for (method in methods) {
                    method.isAccessible = true
                    val modifiers = method.modifiers
                    val attributeName = "${cls.canonicalName}.${method.name}"
                    val jsonOutput = JSONObject()
                    if (!isMethodNameConsidered(method) || isVoid(method)) {
                        jsonOutput.put(attributeName, "MNC")
                    } else if (blackListClasses.contains(cls.canonicalName) && blackListMethods.contains(method.name)) {
                        jsonOutput.put(attributeName, "MNC")
                    } else if (!isStatic(modifiers) && instance == null) {
                        jsonOutput.put(attributeName, "MNC")
                    } else {
                        try {
                            val objInstance = if (isStatic(modifiers)) null else instance
                            val initArgs =
                                instanceFactory.createDefaultParameters(method.parameterTypes)
                            val valueObj = invokeMethod(method, objInstance, initArgs)
                            val attributeValue = objectExplorer.exploreObject(valueObj, MAX_DEPTH)
                            jsonOutput.put(attributeName, attributeValue)
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error while exploring method $attributeName", e)
                            jsonOutput.put(attributeName, getStringValueOfException(e))
                        }
                    }
                    if (jsonOutput.length() > 0) attributesList.add(jsonOutput)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error while exploring methods of class ${cls.canonicalName}", e)
            }
            return@withContext attributesList
        }
}