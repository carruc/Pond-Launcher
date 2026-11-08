package com.geecee.escapelauncher.feature.settings.font

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.geecee.escapelauncher.core.ui.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.geecee.escapelauncher.core.ui.composables.SettingsSlider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.geecee.escapelauncher.core.common.DefaultSettings
import com.geecee.escapelauncher.core.theme.ThemeViewModel
import com.geecee.escapelauncher.core.theme.type.getFontFamily
import com.geecee.escapelauncher.core.ui.composables.EscapeHeader
import com.geecee.escapelauncher.core.ui.composables.SettingsButton
import com.geecee.escapelauncher.core.ui.composables.SettingsSpacer


/**
 * Font options in settings
 *
 * @param goBack When back button is pressed
 *
 * @see Settings
 */
@Composable
fun ChooseFont(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    goBack: () -> Unit
) {
    val context = LocalContext.current
    val selectedFont by themeViewModel.font.collectAsState(initial = DefaultSettings.FONT)

    val fontScale by themeViewModel.fontScale.collectAsState(DefaultSettings.FONT_SCALE)

    val interfaceScale by themeViewModel.interfaceScale.collectAsState(DefaultSettings.INTERFACE_SCALE)

    val fontNames = listOf(
        "Outfit",
        "Jost",
        "Inter",
        "Lexend",
        "Google Sans Flex",
        "Work Sans",
        "Poppins",
        "Roboto",
        "Open Sans",
        "Lora",
        "IBM Plex Sans",
        "IBM Plex Serif",
        "JetBrains Mono",
        "System"
    )

    Box(
        Modifier
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
            item { EscapeHeader(goBack, stringResource(R.string.font)) }

            item {
                SettingsSlider(
                    label = stringResource(R.string.pond_font_size, (fontScale * 100).roundToInt()),
                    value = fontScale,
                    onValueChange = themeViewModel::setFontScale,
                    valueRange = 0.8f..1.2f,
                    steps = 7,
                    onReset = { themeViewModel.setFontScale(DefaultSettings.FONT_SCALE) },
                    resetButtonContentDescription = stringResource(R.string.reset_to_default),
                    isTopOfGroup = true,
                    isBottomOfGroup = true
                )
                SettingsSpacer()
            }

            item {
                SettingsSlider(
                    label = stringResource(R.string.pond_interface_size, (interfaceScale * 100).roundToInt()),
                    value = interfaceScale,
                    onValueChange = themeViewModel::setInterfaceScale,
                    valueRange = 0.7f..1f,
                    steps = 5,
                    onReset = { themeViewModel.setInterfaceScale(DefaultSettings.INTERFACE_SCALE) },
                    resetButtonContentDescription = stringResource(R.string.reset_to_default),
                    isTopOfGroup = true,
                    isBottomOfGroup = true
                )
                SettingsSpacer()
            }

            itemsIndexed(fontNames) { index, fontName ->
                SettingsButton(
                    label = fontName,
                    onClick = {
                        themeViewModel.setFont(fontName)
                    },
                    isTopOfGroup = index == 0,
                    isBottomOfGroup = index == fontNames.lastIndex,
                    fontFamily = getFontFamily(context, fontName),
                    isSelected = fontName == selectedFont
                )
            }

            item { SettingsSpacer() }
            item { SettingsSpacer() }
        }
    }
}