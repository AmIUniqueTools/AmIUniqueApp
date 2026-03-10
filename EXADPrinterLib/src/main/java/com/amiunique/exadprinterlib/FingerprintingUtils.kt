package com.amiunique.exadprinterlib

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Arrays
import java.util.Locale
import java.util.regex.Pattern
import android.util.Patterns
import java.security.MessageDigest
private const val TAG = "FingerprintingUtils"

fun shortHash(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(input.toByteArray())
    return hash
        .take(6) // 12 hex chars
        .joinToString("") { "%02x".format(it) }
}

fun maskEmailsInText(value: String?): String? {
    if (value == null) return null

    val emailRegex = Patterns.EMAIL_ADDRESS.toRegex()

    return emailRegex.replace(value) { matchResult ->
        val email = matchResult.value
        "[EMAIL-${shortHash(email)}]"
    }
}


/* METHODS RELATED TO CONTENT PROVIDERS */

/**
 * This method is used to check if a string is a content provider URI
 *
 * @param uriString : String The URI string to check
 * @return boolean : True if the string is a content provider URI, false otherwise
 */
fun isContentProviderUri(uriString: String): Boolean {
    // Regular expression to match content provider URIs
    val uriRegex = "^content://[a-zA-Z\\d._-]+(/[a-zA-Z\\d._-]+)*$"

    // Compile the regex pattern
    val pattern = Pattern.compile(uriRegex)

    // Create a matcher for the input string
    val matcher = pattern.matcher(uriString)

    // Check if the string matches the regular expression
    return matcher.matches()
}

/**
 * This method is used to get the content of a content provider URI
 *
 * @param context   : Context The application context
 * @param uriString : String The URI string of the content provider
 * @return String : The content of the content provider value
 */
suspend fun getContentProviderValue(context: Context, uriString: String): JSONArray =
    withContext(Dispatchers.IO) {
        val jsonArray = JSONArray()

        // Parse URI string to create Uri object
        val uri = Uri.parse(uriString)

        // Get ContentResolver
        val contentResolver = context.contentResolver

        // Query the content provider for its content
        try {
            contentResolver.query(uri, null, null, null, null).use { cursor ->
                if (cursor != null) {
                    // Append columns
                    val columns = cursor.columnNames
                    if (cursor.moveToFirst()) {
                        do {
                            val rowObject = JSONObject()
                            for (column in columns) {
                                val columnIndex = cursor.getColumnIndex(column)
                                var value = cursor.getString(columnIndex)
                                // Check if value contains email, hash the email part
                                value = maskEmailsInText(value)
                                rowObject.put(column, value)
                            }
                            jsonArray.put(rowObject)
                        } while (cursor.moveToNext())
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error querying content provider $uriString", e)
            jsonArray.put(getStringValueOfException(e))
        }

        jsonArray
    }


/* METHODS RELATED TO REFLECTION */
/**
 * This method is used to invoke a method with a timeout
 *
 * @param method   : Method The method to invoke
 * @param instance : Object The instance on which to invoke the method
 * @param args     : Object[] The arguments to pass to the method
 * @return Object : The result of the method invocation
 */
fun invokeMethod(method: Method, instance: Any?, args: Array<Any?>): Any? {
    try {
        return if (isVoid(method)) "MNC" else method.invoke(instance, *args)
    } catch (e: Throwable) {
        Log.e(TAG, "Error invoking method: " + method.name, e)
        return getStringValueOfException(e)
    }
}


/**
 * This method is used to invoke a constructor with a timeout
 *
 * @param constructor : Constructor The constructor to invoke
 * @param args        : Object[] The arguments to pass to the method
 * @return Object : The result of the constructor invocation
 */
fun invokeConstructor(constructor: Constructor<*>, args: Array<Any?>): Any? {
    var instance: Any? = null
    try {
        instance = constructor.newInstance(*args)
    } catch (e: Throwable) {
        Log.e(TAG, "Error invoking constructor: " + constructor.name, e)
    }
    return instance
}


/* METHODS RELATED TO MEMBER SELECTION */ /**
 * This method is used to sort constructors by parameter count and parameter type composition
 *
 * @param constructors: Constructor[] The constructors to sort
 * @return Constructor[] : The sorted constructors
 */
fun sortConstructorsByParameterCount(constructors: Array<Constructor<*>>): Array<Constructor<*>> {
    Arrays.sort(
        constructors
    ) { c1: Constructor<*>, c2: Constructor<*> ->
        val c1AllPrimitive = allPrimitive(c1.parameterTypes)
        val c2AllPrimitive = allPrimitive(c2.parameterTypes)

        // Prioritize constructors with all primitive parameters
        if (c1AllPrimitive && !c2AllPrimitive) {
            return@sort -1
        }
        if (!c1AllPrimitive && c2AllPrimitive) {
            return@sort 1
        }
        Integer.compare(c1.parameterTypes.size, c2.parameterTypes.size)
    }
    return constructors
}

/**
 * This method is used to check if a method is considered for fingerprinting
 *
 * @param method : Method The method to check
 * @return boolean : True if the method is considered, false otherwise
 */
fun isMethodNameConsidered(method: Method): Boolean {
    val methodName = method.name
    val ignoredMethods = arrayOf(
        "getClass", "hashCode", "toString", "notify", "notifyAll", "wait", "equals", "finalize"
    )
    val prefixes = arrayOf(
        "get", "is", "has", "can", "should", "must", "are", "my",
        "contain", "count", "to", "as", "fetch", "retrieve", "query",
        "find", "size", "length", "state", "type", "status", "flag", "id",
        "index", "name", "title", "level", "describe", "semget", "vivoget"
    )
    if (methodName in ignoredMethods) {
        return false
    }
    // Check for prefixes
    for (prefix in prefixes) {
        if (methodName.lowercase(Locale.getDefault()).startsWith(prefix)) {
            return true
        }
    }

    return false
}


/* OTHERS METHODS */ /**
 * This method is used to check if a method is static
 *
 * @param modifiers : int The modifiers of the method
 * @return boolean : True if the method is static, false otherwise
 */
fun isStatic(modifiers: Int): Boolean {
    return Modifier.isStatic(modifiers)
}

/**
 * This method is used to check if a field is a constant
 *
 * @param field
 * @param fieldValue
 * @return
 */
fun isConstant(field: Field, fieldValue: String): Boolean {
    val modifiers = field.modifiers
    if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
        // Check if the field value contains the field name
        if (fieldValue.contains(field.name)) return true

        // Regular expression pattern for constant names
        val CONSTANT_NAME_PATTERN = Pattern.compile("^[a-z_]+$")

        // Check if the field name matches the pattern for constants (UPPERCASE + _)
        return CONSTANT_NAME_PATTERN.matcher(fieldValue).matches()
    }
    return false
}

/***
 * This method is used to check if a method follows the Key-Value Access pattern
 * @param method
 * @return
 */
fun isKeyValueAccessPattern(method: Method): Boolean {
    // Flag to check for the presence of a key-like parameter
    var hasKeyParam = false
    // Flag to check for the presence of a context-like or provider parameter
    var hasContextParam = false

    // Get method parameters
    val paramTypes = method.parameterTypes

    // If method name is different from getString or getInt or getFloat or getLong, return false
    if (method.name != "getString"
        && method.name != "getInt"
        && method.name != "getFloat"
        && method.name != "getLong"
    ) {
        return false
    }

    // If method has less than two parameters, it probably isn't a Key-Value Access method
    if (paramTypes.size != 2) {
        return false
    }

    // If first param is not Context like or provider, return false
    // Check if the parameter could be a context/provider (e.g., ContentResolver, SharedPreferences)
    if (paramTypes[0].simpleName == "ContentResolver" || paramTypes[0].simpleName == "SharedPreferences" || paramTypes[0].simpleName == "Bundle" ||
        paramTypes[0].simpleName.contains("Manager")
    ) {
        hasContextParam = true
    }
    // Check if the parameter could be a key (String or int)
    if (paramTypes[1] == String::class.java || paramTypes[1] == Int::class.javaPrimitiveType) {
        hasKeyParam = true
    }

    // Return true only if both a key-like and a context-like parameter are found
    return hasKeyParam && hasContextParam
}

fun getMethodByName(cls: Class<*>, methodName: String): Method? {
    for (method in cls.declaredMethods) {
        if (method.name == methodName) {
            return method
        }
    }
    return null
}

/**
 * This method is used to check if a method is void
 *
 * @param method : Method The method to check
 * @return boolean : True if the method is void, false otherwise
 */
fun isVoid(method: Method): Boolean {
    return method.returnType == Void.TYPE
}

/**
 * This method is used to check if an object is primitive or a wrapper
 *
 * @param clsObj : Class The object to check
 * @return boolean : True if the object is primitive or a wrapper, false otherwise
 */
fun isPrimitiveOrWrapper(clsObj: Class<*>): Boolean {
    val wrappers: MutableSet<Class<*>> = HashSet()
    wrappers.add(Boolean::class.java)
    wrappers.add(Char::class.java)
    wrappers.add(Byte::class.java)
    wrappers.add(Short::class.java)
    wrappers.add(Int::class.java)
    wrappers.add(Long::class.java)
    wrappers.add(Float::class.java)
    wrappers.add(Double::class.java)
    wrappers.add(Void::class.java)

    return clsObj.isPrimitive || wrappers.contains(clsObj)
}

/**
 * This method is used to get the string value of an exception
 *
 * @param e : Throwable The exception
 * @return String : The string value of the exception
 */
fun getStringValueOfException(e: Throwable): String {
    return "ERR-${e.javaClass.simpleName}"
    /*var value = (e.javaClass.canonicalName?.plus(" : ") ?: "") + e.message
    if (e.cause != null) value = value + " - " + e.cause!!.javaClass.canonicalName
    return value*/
}

/**
 * This method is used to check if all parameters  are primitive
 *
 * @param parameterTypes : Class[] The parameter types
 * @return boolean : True if all parameters are primitive, false otherwise
 */
private fun allPrimitive(parameterTypes: Array<Class<*>>): Boolean {
    for (parameter in parameterTypes) {
        if (!isPrimitiveOrWrapper(parameter)) {
            return false
        }
    }
    return true
}

/**
 * This methods is used to check if a class is abstract or not
 *
 * @param cls : Class The class to check
 * @return boolean : True if the class is abstract, false otherwise
 */
fun isClassAbstract(cls: Class<*>): Boolean {
    return Modifier.isAbstract(cls.modifiers)
}

fun isClassFinal(cls: Class<*>): Boolean {
    return Modifier.isFinal(cls.modifiers)
}

fun isClassStatic(cls: Class<*>): Boolean {
    return Modifier.isStatic(cls.modifiers)
}

fun isClassNative(cls: Class<*>): Boolean {
    return Modifier.isNative(cls.modifiers)
}

fun isDeveloperEnabled(context: Context): Int {
    return when {
        Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN -> {
            Settings.Secure.getInt(context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0)
        }
        Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN -> {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(context.contentResolver,
                Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, 0)
        }
        else -> 0
    }
}

