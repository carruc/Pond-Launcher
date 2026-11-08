package com.geecee.escapelauncher.feature.newwidgets.options

import com.geecee.escapelauncher.core.common.DefaultSettings
import com.geecee.escapelauncher.core.domain.repository.settings.HomeWidget
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geecee.escapelauncher.core.domain.repository.settings.WidgetSettingsRepository
import com.geecee.escapelauncher.feature.newwidgets.WidgetHostManager
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class WidgetOptionsViewModel @Inject constructor(
    private val widgetSettingsRepository: WidgetSettingsRepository,
    val widgetHostManager: WidgetHostManager
) : ViewModel() {
    val favoritesSearchWidgetId = widgetSettingsRepository.favoritesSearchWidgetId

    fun setFavoritesSearchWidget(id: Int?) {
        viewModelScope.launch {
            val previous = widgetSettingsRepository.replaceFavoritesSearchWidget(id)
            if (previous != null && previous != id) widgetHostManager.deleteWidgetId(previous)
        }
    }

    val widgets = widgetSettingsRepository.widgets

    fun addWidget(id: Int) = update { widgets ->
        if (widgets.any { it.id == id }) widgets
        else widgets + HomeWidget(id, DefaultSettings.WIDGET_WIDTH, DefaultSettings.WIDGET_HEIGHT)
    }

    fun resizeWidget(id: Int, width: Float? = null, height: Float? = null, offset: Float? = null) = update { widgets ->
        widgets.map {
            if (it.id == id) it.copy(width = width ?: it.width, height = height ?: it.height, offset = offset ?: it.offset)
            else it
        }
    }

    fun moveWidget(id: Int, direction: Int) = update { widgets ->
        val from = widgets.indexOfFirst { it.id == id }
        val to = from + direction
        if (from < 0 || to !in widgets.indices) widgets
        else widgets.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun removeWidget(id: Int) {
        viewModelScope.launch {
            widgetSettingsRepository.updateWidgets { widgets -> widgets.filterNot { it.id == id } }
            widgetHostManager.deleteWidgetId(id)
        }
    }

    private fun update(transform: (List<HomeWidget>) -> List<HomeWidget>) {
        viewModelScope.launch { widgetSettingsRepository.updateWidgets(transform) }
    }
}
