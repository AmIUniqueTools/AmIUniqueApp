package com.amiunique.exadprinterlib

import android.content.Context
import android.util.Log

private const val TAG: String = "SystemServiceFactory"

class SystemServiceFactory(context: Context) {
    private val serviceMap: MutableMap<Class<*>, Any> = HashMap()

    init{
        initializeServices(context)
    }

    private fun initializeServices(context: Context) {
        val fields = Context::class.java.fields

        for (field in fields) {
            try {
                if (field.type == String::class.java && field.name.endsWith("_SERVICE")) {
                    val serviceName = field.get(null) as String
                    try {
                        val service = context.getSystemService(serviceName)

                        if (service != null) {
                            serviceMap[service.javaClass] = service
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error while initializing services", e)
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error while initializing services", e)
            }
        }
    }

    fun getAllServices(): Map<Class<*>, Any> {
        return serviceMap
    }
}