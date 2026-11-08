package com.geecee.escapelauncher.core.domain.repository.settings

import kotlinx.coroutines.flow.Flow

data class HomeWidget(val id: Int, val width: Float, val height: Float, val offset: Float = 0f)

interface WidgetSettingsRepository {
    val favoritesSearchWidgetId: Flow<Int?>
    suspend fun replaceFavoritesSearchWidget(id: Int?): Int?
    val widgets: Flow<List<HomeWidget>>
    suspend fun updateWidgets(transform: (List<HomeWidget>) -> List<HomeWidget>)
}
