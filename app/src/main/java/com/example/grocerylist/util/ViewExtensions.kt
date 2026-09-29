package com.example.grocerylist.util

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.view.inputmethod.InputMethodManager

/**
 * Extension on [View] to debounce click events.
 * Prevents multiple rapid clicks from triggering redundant transactions or crashes.
 *
 * @param debounceTime Milliseconds window to ignore subsequent clicks. Default: 350ms.
 * @param action Lambda invoked when a non-throttled click is registered.
 */
fun View.throttleClick(debounceTime: Long = 350L, action: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener { v ->
        val currentTime = SystemClock.uptimeMillis()
        if (currentTime - lastClickTime >= debounceTime) {
            lastClickTime = currentTime
            action(v)
        }
    }
}
