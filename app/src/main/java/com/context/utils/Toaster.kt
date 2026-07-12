package com.context.utils

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class Toaster(
    private val snackbarHostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    fun show(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }
}

val LocalToaster = staticCompositionLocalOf<Toaster> {
    error("No Toaster provided")
}
