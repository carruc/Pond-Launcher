package com.geecee.escapelauncher.core.data.repository.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.geecee.escapelauncher.core.domain.repository.settings.HomeWidget
import com.geecee.escapelauncher.core.common.DefaultSettings
import com.geecee.escapelauncher.core.data.datastore.PreferencesKeys
import com.geecee.escapelauncher.core.domain.repository.settings.WidgetSettingsRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WidgetSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : WidgetSettingsRepository {
    private val widgetsKey = stringPreferencesKey("home_widgets")

    private val favoritesSearchKey = intPreferencesKey("favorites_search_widget_id")

    override val favoritesSearchWidgetId: Flow<Int?> = dataStore.data.map { it[favoritesSearchKey] }

    override suspend fun replaceFavoritesSearchWidget(id: Int?): Int? {
        var previous: Int? = null
        dataStore.edit { preferences ->
            previous = preferences[favoritesSearchKey]
            if (id == null) preferences.remove(favoritesSearchKey)
            else preferences[favoritesSearchKey] = id
        }
        return previous
    }

    override val widgets: Flow<List<HomeWidget>> = dataStore.data.map(::readWidgets)

    override suspend fun updateWidgets(transform: (List<HomeWidget>) -> List<HomeWidget>) {
        dataStore.edit { preferences ->
            preferences[widgetsKey] = transform(readWidgets(preferences)).joinToString(";") {
                "${it.id},${it.width},${it.height},${it.offset}"
            }
        }
    }

    private fun readWidgets(preferences: Preferences): List<HomeWidget> {
        val saved = preferences[widgetsKey]
        if (saved != null) {
            return saved.split(';').mapNotNull { entry ->
                val parts = entry.split(',')
                val id = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                val width = parts.getOrNull(1)?.toFloatOrNull() ?: return@mapNotNull null
                val height = parts.getOrNull(2)?.toFloatOrNull() ?: return@mapNotNull null
                HomeWidget(id, width, height, parts.getOrNull(3)?.toFloatOrNull() ?: DefaultSettings.WIDGET_OFFSET)
            }
        }

        val legacyId = preferences[PreferencesKeys.WIDGET_ID] ?: return emptyList()
        if (legacyId <= 0) return emptyList()
        return listOf(HomeWidget(
            legacyId,
            preferences[PreferencesKeys.WIDGET_WIDTH] ?: DefaultSettings.WIDGET_WIDTH,
            preferences[PreferencesKeys.WIDGET_HEIGHT] ?: DefaultSettings.WIDGET_HEIGHT,
            preferences[PreferencesKeys.WIDGET_OFFSET] ?: DefaultSettings.WIDGET_OFFSET
        ))
    }
}
