package com.flashlight.tvlauncher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flashlight.tvlauncher.data.AppEntry
import com.flashlight.tvlauncher.data.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)

    /** null — список ещё загружается */
    val apps: StateFlow<List<AppEntry>?> = repository.changes()
        .onStart { emit(Unit) }
        .conflate()
        .map { repository.loadApps() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
