package com.geecee.escapelauncher.feature.newwidgets.options

import android.appwidget.AppWidgetProviderInfo
import android.os.Process
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import com.geecee.escapelauncher.feature.newwidgets.WidgetRenderer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.geecee.escapelauncher.core.common.DefaultSettings
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.geecee.escapelauncher.core.ui.composables.EscapeHeader
import com.geecee.escapelauncher.core.ui.composables.SettingsButton
import com.geecee.escapelauncher.core.ui.composables.SettingsSlider
import com.geecee.escapelauncher.core.ui.composables.SettingsSpacer
import com.geecee.escapelauncher.feature.newwidgets.picker.CustomWidgetPicker
import com.geecee.escapelauncher.feature.newwidgets.WidgetAddFlow
import com.geecee.escapelauncher.core.ui.R

/**
 * Widget Setup screen
 *
 * @param onBackClick When back button is pressed
 */
@Composable
fun WidgetOptions(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WidgetOptionsViewModel = hiltViewModel()
) {
    var showCustomPicker by rememberSaveable { mutableStateOf(false) }
    var selectedProviderInfo by rememberSaveable { mutableStateOf<AppWidgetProviderInfo?>(null) }
    var addingFavoritesSearch by rememberSaveable { mutableStateOf(false) }
    val favoritesSearchId by viewModel.favoritesSearchWidgetId.collectAsState(initial = null)
    val searchProvider = remember(viewModel) {
        viewModel.widgetHostManager.manager.getInstalledProvidersForPackage(
            "com.google.android.googlequicksearchbox", Process.myUserHandle()
        ).firstOrNull { it.provider.className.endsWith(".GoogleSearchWidgetProvider") }
    }
    val widgets by viewModel.widgets.collectAsState(initial = emptyList())
    val context = LocalContext.current

    // Modular Widget Addition Flow
    selectedProviderInfo?.let { info ->
        WidgetAddFlow(
            providerInfo = info,
            onWidgetConfigured = { newId ->
                if (addingFavoritesSearch) viewModel.setFavoritesSearchWidget(newId)
                else viewModel.addWidget(newId)
                selectedProviderInfo = null
            },
            onCancel = {
                selectedProviderInfo = null
            }
        )
    }

    if (showCustomPicker) {
        CustomWidgetPicker(
            onWidgetSelected = { info ->
                addingFavoritesSearch = false
                selectedProviderInfo = info
                showCustomPicker = false
            },
            onDismiss = { showCustomPicker = false }
        )
    }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            item(key = "header") { EscapeHeader(onBackClick, stringResource(R.string.widget)) }

            item(key = "add_widget") {
                SettingsButton(
                    label = stringResource(R.string.pond_add_widget),
                    isTopOfGroup = true,
                    isBottomOfGroup = true,
                    onClick = { showCustomPicker = true }
                )
            }

            if (searchProvider != null || favoritesSearchId != null) {
                item(key = "favorites_search") {
                    SettingsSpacer()
                    if (favoritesSearchId == null && searchProvider != null) {
                        SettingsButton(
                            label = stringResource(R.string.pond_add_favorites_search),
                            isTopOfGroup = true,
                            isBottomOfGroup = true,
                            onClick = {
                                addingFavoritesSearch = true
                                selectedProviderInfo = searchProvider
                            }
                        )
                    } else {
                        SettingsButton(
                            label = stringResource(R.string.pond_remove_favorites_search),
                            isTopOfGroup = true,
                            isBottomOfGroup = true,
                            onClick = { viewModel.setFavoritesSearchWidget(null) }
                        )
                    }
                }
            }

            itemsIndexed(widgets, key = { _, widget -> widget.id }) { index, widget ->
                val provider = viewModel.widgetHostManager.manager.getAppWidgetInfo(widget.id)
                SettingsSpacer()
                Text(provider?.loadLabel(context.packageManager) ?: stringResource(R.string.pond_widget_unavailable))
                if (provider != null) {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        WidgetRenderer(
                            widget.id,
                            viewModel.widgetHostManager,
                            modifier = Modifier.offset(x = widget.offset.dp).size(widget.width.dp, widget.height.dp),
                            reportSize = false
                        )
                    }
                    SettingsSlider(
                        label = stringResource(R.string.offset),
                        value = widget.offset,
                        onValueChange = { viewModel.resizeWidget(widget.id, offset = it) },
                        valueRange = -20f..20f,
                        steps = 19,
                        resetButtonContentDescription = stringResource(R.string.reset_to_default),
                        onReset = { viewModel.resizeWidget(widget.id, offset = DefaultSettings.WIDGET_OFFSET) },
                        isTopOfGroup = true
                    )
                    SettingsSlider(
                        label = stringResource(R.string.height),
                        value = widget.height,
                        onValueChange = { viewModel.resizeWidget(widget.id, height = it) },
                        valueRange = 100f..400f,
                        steps = 9,
                        resetButtonContentDescription = stringResource(R.string.reset_to_default),
                        onReset = { viewModel.resizeWidget(widget.id, height = DefaultSettings.WIDGET_HEIGHT) }
                    )
                    SettingsSlider(
                        label = stringResource(R.string.width),
                        value = widget.width,
                        onValueChange = { viewModel.resizeWidget(widget.id, width = it) },
                        valueRange = 100f..400f,
                        steps = 9,
                        resetButtonContentDescription = stringResource(R.string.reset_to_default),
                        onReset = { viewModel.resizeWidget(widget.id, width = DefaultSettings.WIDGET_WIDTH) }
                    )
                }
                if (index > 0) {
                    SettingsButton(label = stringResource(R.string.pond_move_up), onClick = { viewModel.moveWidget(widget.id, -1) })
                }
                if (index < widgets.lastIndex) {
                    SettingsButton(label = stringResource(R.string.pond_move_down), onClick = { viewModel.moveWidget(widget.id, 1) })
                }
                SettingsButton(
                    label = stringResource(R.string.remove_widget),
                    isBottomOfGroup = true,
                    onClick = { viewModel.removeWidget(widget.id) }
                )
            }
            item(key = "bottom_space") { SettingsSpacer() }
        }
    }
}
