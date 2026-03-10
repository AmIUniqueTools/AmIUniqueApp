package com.amiunique.exadprinterlib

import android.content.ContentResolver
import android.content.Context
import android.util.Log
import java.lang.reflect.Constructor

private const val TAG: String = "InstanceFactory"
class InstanceFactory(private val applicationContext: Context) {
    private val instances: MutableMap<Class<*>, Any?> = HashMap()
    private val instantiatingClasses: MutableSet<Class<*>> = HashSet()
    // These classes may cause native crashes when instantiated
    private val ignoredClasses : Set<String> = setOf(
        "android.graphics.HardwareBufferRenderer",
        "android.media.MediaDrm",
        "android.media.MediaExtractor"
    )
    init {
        // Add the application context to the instances map
        instances[Context::class.java] = this.applicationContext
        // Add the package manager to the instances map
        instances[android.content.pm.PackageManager::class.java] =
            this.applicationContext.packageManager
        // Instantiate System services classes using the SystemServiceFactory
        val systemServiceFactory =
            SystemServiceFactory(this.applicationContext)
        val services: Map<Class<*>, Any?> = systemServiceFactory.getAllServices()
        // Add the system services to the instances map
        instances.putAll(services)
        // Add the Content Resolver to the instances map
        instances[ContentResolver::class.java] = this.applicationContext.contentResolver
    }

    /**
     * This method is used to get an instance of a class
     *
     * @param cls : Class The class to instantiate
     * @return Object : The instance of the class
     */
    fun getInstance(cls: Class<*>): Any? {
        // check if an instance exists in the map
        return if (instances.containsKey(cls)) {
            instances[cls]
        } else {
            // 2) look for an existing instance whose class is a child of `cls`
            val childEntry = instances.entries.firstOrNull { (k, _) -> cls.isAssignableFrom(k) }
            if (childEntry != null) {
                // cache under the abstraction for faster future lookups
                instances[cls] = childEntry.value
                cls.cast(childEntry.value)
            }else{
                createInstance(cls)
            }
        }
    }

    /**
     * This method is used to create an instance of a class
     *
     * @param cls : Class The class to instantiate
     * @return Object : The instance of the class
     */
    private fun createInstance(cls: Class<*>): Any? {
        // Check if the class.name contains any ignored class
        if (ignoredClasses.any { cls.name.contains(it) }) {
            return null
        }
        /*if (ignoredClasses.contains(cls.name)){
            return null
        }*/
        // Check if the class is an interface, annotation, enum, final, static or abstrct
        if (cls.isInterface || cls.isEnum || cls.isAnnotation || cls.isSynthetic || cls.isAnonymousClass
            || isClassAbstract(cls) || isClassStatic(cls)) {
            return null
        }
        // Check if we are already trying to instantiate this class to avoid recursion
        if (instantiatingClasses.contains(cls)) {
            return null
        }
        var instance: Any? = null
        // Add the class to the set to mark it as being instantiated
        instantiatingClasses.add(cls)
        try {
            // If a class has internal class named Builder,
            // we use the builder to create the instance,
            // to do so we create an instance of the builder
            // then we call method build() that will return the instance of the class
            for (declaredClass in cls.declaredClasses) {
                // Check if the class has an internal class named Builder
                if (declaredClass.simpleName == "Builder") {
                    instance = buildObject(cls, declaredClass)
                }
            }

            // Get the constructors of the class and sort them by parameter count
            val constructors: Array<Constructor<*>> =
                sortConstructorsByParameterCount(cls.constructors)
            // Try to instantiate the class with any constructor
            for (constructor in constructors) {
                instance = constructObject(cls, constructor)
                if (instance != null) {
                    break
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error instantiating class " + cls.name, e)
            instance = null
        }
        instances[cls] = instance
        instantiatingClasses.remove(cls)
        return instance
    }

    private fun constructObject(cls: Class<*>, constructor: Constructor<*>): Any? {
        try {
            // Get the parameter types of the constructor
            val parameterTypes = constructor.parameterTypes
            // Create default parameters
            val initArgs = createDefaultParameters(parameterTypes)
            // Create an instance with default parameters
            val instance: Any? = invokeConstructor(constructor, initArgs)
            // Store the instance in the map
            instances[cls] = instance
            instantiatingClasses.remove(cls)
            return instance
        } catch (e: Throwable) {
            Log.e(TAG, "Error instantiating class " + cls.name, e)
            instances[cls] = null
            instantiatingClasses.remove(cls)
            return null
        }
    }

    private fun buildObject(cls: Class<*>, declaredClass: Class<*>): Any? {
        try {
            // Get the instance of the builder
            val builderInstance = getInstance(declaredClass)
            instances[declaredClass] = builderInstance
            instantiatingClasses.remove(declaredClass)
            // Get build method
            val buildMethod = declaredClass.getMethod("build")
            // Get the parameter types of the build method
            val parameterTypes = buildMethod.parameterTypes
            // Create default parameters
            val initArgs = createDefaultParameters(parameterTypes)
            // Invoke the build method with default parameters
            val instance = invokeMethod(buildMethod, builderInstance, initArgs)
            // Store the instance in the map
            instances[cls] = instance
            instantiatingClasses.remove(cls)
            return instance
        } catch (e: Throwable) {
            Log.e(TAG, "Error instantiating class " + cls.name, e)
            instantiatingClasses.remove(declaredClass)
            return null
        }
    }

    /**
     * This method is used to create default parameters for a constructor
     *
     * @param parameterTypes : Class[] The parameter types of the constructor
     * @return Object[] : The default parameters
     */
    fun createDefaultParameters(parameterTypes: Array<Class<*>>): Array<Any?> {
        val parameters = arrayOfNulls<Any>(parameterTypes.size)
        for (i in parameterTypes.indices) {
            parameters[i] = getDefaultParameter(parameterTypes[i])
        }
        return parameters
    }

    /**
     * This method is used to get the default parameter for a class
     * Code inspired from
     * [](https://github.com/IAIK/SCAnDroid/blob/master/code/Android/SCAnDroid/app/src/main/java/at/tugraz/iaik/scandroid/Utils.java)
     * for the primitive classes, I have added the recursion call for creating recursively classes
     *
     * @param aClass : Class The class for which we want to get the default parameter
     * @return Instance
     */
    fun getDefaultParameter(aClass: Class<*>): Any? {
        val className = aClass.name

        //Random random = new Random(); For instance we don't need to generate random values
        if (className == Int::class.javaPrimitiveType!!.name) {
            return 10
            //return random.nextInt(100000);
        } else if (className == IntArray::class.java.name) {
            return intArrayOf(10, 10)
            //return new int[]{random.nextInt(100000), random.nextInt(100000)};
        } else if (className == Long::class.javaPrimitiveType!!.name) {
            return 10L
            //return (long) random.nextInt(100000);
        } else if (className == LongArray::class.java.name) {
            return longArrayOf(10, 10)
            //return new long[]{random.nextInt(100000), random.nextInt(100000)};
        } else if (className == String::class.java.name || className == CharSequence::class.java.name) {
            return "default"
        } else if (className == Array<String>::class.java.name) {
            return arrayOf("default1", "default2")
        } else if (className == Float::class.javaPrimitiveType!!.name) {
            return 10.00f
            //return random.nextFloat();
        } else if (className == FloatArray::class.java.name) {
            return floatArrayOf(1.02f, 13.34f)
        } else if (className == Double::class.javaPrimitiveType!!.name) {
            return 10.00
            //return random.nextDouble();
        } else if (className == DoubleArray::class.java.name) {
            return doubleArrayOf(1.02, 13.34)
        } else if (className == Boolean::class.javaPrimitiveType!!.name) {
            return true
        } else if (className == BooleanArray::class.java.name) {
            return booleanArrayOf(true, false)
        } else if (className == Char::class.javaPrimitiveType!!.name) {
            return 'd'
        } else if (className == CharArray::class.java.name) {
            return charArrayOf('d', 'e')
        } else if (className == Short::class.javaPrimitiveType!!.name) {
            return 1337.toShort()
        } else if (className == ShortArray::class.java.name) {
            return shortArrayOf(1337.toShort(), 1335.toShort())
        } else if (className == Byte::class.javaPrimitiveType!!.name) {
            return 1337.toByte()
        } else if (className == ByteArray::class.java.name) {
            return byteArrayOf(1337.toByte(), 1335.toByte())
        } else if (instances.containsKey(aClass)) {
            return instances[aClass]
        } else if (!instantiatingClasses.contains(aClass)) {
            // Only call getClassInstance if we're not already instantiating this class
            return getInstance(aClass)
        } else {
            Log.e(TAG, "No default found for className = $className")
            return null
        }
    }
}