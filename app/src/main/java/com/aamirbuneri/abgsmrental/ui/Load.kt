package com.aamirbuneri.abgsmrental.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aamirbuneri.abgsmrental.data.ApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ok<T>(val data: T) : Load<T>
    data class Err(val error: ApiException) : Load<Nothing>
}

val <T> Load<T>.dataOrNull: T? get() = (this as? Load.Ok<T>)?.data

/**
 * Loads one thing from the site and keeps it. The first load shows skeletons; later refreshes keep
 * the old data on screen and only show a small error if they fail.
 */
class DataVM<T>(private val fetch: suspend () -> T) : ViewModel() {
    var state: Load<T> by mutableStateOf<Load<T>>(Load.Loading)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var refreshError by mutableStateOf<String?>(null)
        private set

    private var job: Job? = null

    init {
        refresh()
    }

    fun refresh(pull: Boolean = false) {
        job?.cancel()
        job = viewModelScope.launch {
            val hasData = state is Load.Ok
            if (pull) refreshing = true
            if (!hasData) state = Load.Loading
            try {
                state = Load.Ok(fetch())
                refreshError = null
            } catch (e: ApiException) {
                if (hasData) refreshError = e.message else state = Load.Err(e)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val err = ApiException(0, e.message ?: "Something went wrong.")
                if (hasData) refreshError = err.message else state = Load.Err(err)
            } finally {
                refreshing = false
            }
        }
    }

    /** Replace the shown data (after an action returned a fresh copy). */
    fun set(value: T) {
        state = Load.Ok(value)
    }
}
