package com.tinyai.geekbox.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tinyai.geekbox.core.ui.components.ErrorBox
import com.tinyai.geekbox.core.ui.components.LoadingBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

sealed interface Async<out T> {
    data object Loading : Async<Nothing>
    data class Success<T>(val value: T) : Async<T>
    data class Error(val message: String) : Async<Nothing>
}

@Composable
fun <T> rememberAsync(
    vararg keys: Any?,
    load: suspend () -> T
): Async<T> {
    val loader by rememberUpdatedState(load)
    var state by remember(*keys) { mutableStateOf<Async<T>>(Async.Loading) }
    LaunchedEffect(*keys) {
        state = Async.Loading
        state = try {
            Async.Success(withContext(Dispatchers.IO) { loader() })
        } catch (t: Throwable) {
            Async.Error(t.message ?: "加载失败")
        }
    }
    return state
}

@Composable
fun <T> AsyncContent(
    state: Async<T>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    when (state) {
        is Async.Loading -> LoadingBox(modifier)
        is Async.Error -> ErrorBox(state.message, onRetry, modifier)
        is Async.Success -> content(state.value)
    }
}

@Composable
fun <T> rememberPollingAsync(
    intervalMs: Long,
    enabled: Boolean,
    vararg keys: Any?,
    load: suspend () -> T
): Async<T> {
    val loader by rememberUpdatedState(load)
    var state by remember(*keys) { mutableStateOf<Async<T>>(Async.Loading) }
    LaunchedEffect(enabled, intervalMs, *keys) {
        state = try {
            Async.Success(withContext(Dispatchers.IO) { loader() })
        } catch (t: Throwable) {
            Async.Error(t.message ?: "加载失败")
        }
        if (!enabled) return@LaunchedEffect
        while (true) {
            delay(intervalMs)
            state = try {
                Async.Success(withContext(Dispatchers.IO) { loader() })
            } catch (t: Throwable) {
                Async.Error(t.message ?: "加载失败")
            }
        }
    }
    return state
}
