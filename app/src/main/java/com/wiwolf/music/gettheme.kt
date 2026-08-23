package com.wiwolf.music

import android.content.Context
import java.lang.reflect.Method

/**
 * Get a theme id that use in [Context]
 */
fun Context.getThemeId(): Int {
    try {
        val wrapper: Class<*> = this::class.java
        val method: Method = wrapper.getMethod("getThemeResId")
        method.isAccessible = true
        return method.invoke(this) as Int
    } catch (e: Exception) {
        e.printStackTrace()
        return android.R.style.Theme_DeviceDefault
    }
}