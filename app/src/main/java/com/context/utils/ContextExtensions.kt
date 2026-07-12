package com.context.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Helper to find the nearest Activity from a Context.
 * Useful for launching Google In-App Reviews.
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
