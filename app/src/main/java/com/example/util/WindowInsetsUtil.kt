package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Returns safe bottom padding to lift bottom buttons and dialog action bars
 * above the system navigation bar (3-button or gesture pill) in full-screen Dialogs
 * where standard navigationBarsPadding() may evaluate to 0 due to Dialog window isolation.
 */
@Composable
fun rememberDialogBottomPadding(extraPadding: Dp = 14.dp, fallbackNavHeight: Dp = 48.dp): Dp {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Resolve host Activity to access system window insets
    var ctx: Context? = context
    var hostActivity: Activity? = null
    while (ctx is ContextWrapper) {
        if (ctx is Activity) {
            hostActivity = ctx
            break
        }
        ctx = ctx.baseContext
    }

    val activityInsets = hostActivity?.window?.decorView?.let { decorView ->
        ViewCompat.getRootWindowInsets(decorView)?.getInsets(WindowInsetsCompat.Type.navigationBars())
    }

    val composeNavBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val navBottomDp = when {
        activityInsets != null && activityInsets.bottom > 0 -> {
            with(density) { activityInsets.bottom.toDp() }
        }
        composeNavBottom > 0.dp -> {
            composeNavBottom
        }
        else -> {
            fallbackNavHeight
        }
    }

    return navBottomDp + extraPadding
}
