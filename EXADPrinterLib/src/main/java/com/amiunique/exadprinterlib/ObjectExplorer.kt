package com.amiunique.exadprinterlib

import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Array

/**
 * ObjectExplorer is a utility class that allows exploring the content of an object and converting it to a JSONObject.
 */
class ObjectExplorer (private val instanceFactory: InstanceFactory) {
    /**
     * Explore the content of an object and convert it to a JSONObject.
     *
     * @param obj   The object to explore.
     * @param depth The maximum depth to explore.
     * @return A JSONObject representation of the object or a primitive value if the depth is 0 or the object is simple.
     */

    fun exploreObject(obj: Any?, depth: Int): Any {
        return exploreObject(obj, depth, HashSet())
    }

    /**
     * Explore the content of an object and convert it to a JSONObject.
     *
     * @param obj     The object to explore.
     * @param depth   The maximum depth to explore.
     * @param visited A set of visited objects to avoid infinite recursion.
     * @return A JSONObject representation of the object or a primitive value.
     */
    private fun exploreObject(obj: Any?, depth: Int, visited: MutableSet<Any>): Any {
        try {
            // Base case: Stop if depth is 0, object is null, or object has been visited to avoid cycles.
            if (depth == 0 || obj == null || visited.contains(obj)) {
                return obj.toString()
            }

            // Handle primitives, String, Number, and Boolean
            when (obj) {
                is String, is Number, is Boolean -> return obj.toString()
            }

            visited.add(obj)

            // Handle array types
            if (obj::class.java.isArray) {
                val jsonArray = JSONArray()
                val length = Array.getLength(obj)
                for (i in 0 until length) {
                    jsonArray.put(exploreObject(Array.get(obj, i), depth - 1, visited))
                }
                return jsonArray
            }

            // Handle collection types
            if (obj is Collection<*>) {
                val jsonArray = JSONArray()
                for (item in obj) {
                    jsonArray.put(exploreObject(item, depth - 1, visited))
                }
                return jsonArray
            }

            // Handle map types
            if (obj is Map<*, *>) {
                val jsonObject = JSONObject()
                for ((key, value) in obj) {
                    jsonObject.put(key.toString(), exploreObject(value, depth - 1, visited))
                }
                return jsonObject
            }

            // Handle custom object fields
            val fieldsArray = JSONArray()
            val fields = obj.javaClass.fields
            for (field in fields) {
                field.isAccessible = true
                try {
                    fieldsArray.put(
                        JSONObject().put(
                            field.name,
                            exploreObject(field.get(obj), depth - 1, visited)
                        )
                    )
                } catch (e: Throwable) {
                    fieldsArray.put(JSONObject().put(field.name, getStringValueOfException(e)))
                }
            }

            // Handle custom object methods
            val methodsArray = JSONArray()
            val methods = obj.javaClass.methods
            for (method in methods) {
                if (isMethodNameConsidered(method)) {
                    method.isAccessible = true
                    try {
                        val initArgs =
                            instanceFactory.createDefaultParameters(method.parameterTypes)
                        val valueObj = invokeMethod(method, obj, initArgs)

                        if(valueObj != null){
                            methodsArray.put(
                                JSONObject().put(
                                    method.name,
                                    exploreObject(valueObj, depth - 1, visited)
                                )
                            )
                        } else
                            methodsArray.put(
                                JSONObject().put(
                                    method.name,
                                    "null"
                                )
                            )
                    } catch (e: Throwable) {
                        methodsArray.put(
                            JSONObject().put(
                                method.name,
                                getStringValueOfException(e)
                            )
                        )
                    }
                }
            }

            val objJson = JSONObject()
            objJson.put("fields", fieldsArray)
            objJson.put("methods", methodsArray)
            return  objJson
        } catch (e: Throwable) {
            return  getStringValueOfException(e)
        }
    }
}
