package com.lock.applock.helper

import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import kotlin.math.roundToInt

class KeyboardToggleListener(
    private val root: View?,
    private val onKeyboardToggleAction: (shown: Boolean) -> Unit
) : ViewTreeObserver.OnGlobalLayoutListener {
    override fun onGlobalLayout() {
        root?.run {
            val heightDiff = rootView.height - height
            val keyboardShown = heightDiff > dpToPx(200f)
            onKeyboardToggleAction.invoke(keyboardShown)

        }
    }
}

fun View.dpToPx(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).roundToInt()