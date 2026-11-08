package com.geecee.escapelauncher.screens

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Process
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.geecee.escapelauncher.core.model.InstalledApp
import com.geecee.escapelauncher.core.ui.R
import com.geecee.escapelauncher.core.ui.composables.EscapeHeader
import com.geecee.escapelauncher.core.ui.composables.SettingsButton
import com.geecee.escapelauncher.core.ui.composables.SettingsSpacer
import com.geecee.escapelauncher.core.ui.composables.SettingsSlider
import kotlin.math.roundToInt
import com.geecee.escapelauncher.feature.settings.weather.WeatherAppPicker

private enum class UtilitySlot(val key: String, val label: Int) {
    PHONE("phone", R.string.pond_phone),
    MESSAGES("messages", R.string.pond_messages),
    CLOCK("clock", R.string.pond_clock),
    WALLET("wallet", R.string.pond_wallet);

    fun defaultPackage(context: Context): String? = when (this) {
        PHONE -> context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
        MESSAGES -> Telephony.Sms.getDefaultSmsPackage(context)
        CLOCK -> listOf("com.google.android.deskclock", "com.android.deskclock")
            .firstOrNull { context.packageManager.getLaunchIntentForPackage(it) != null }
        WALLET -> "com.google.android.apps.walletnfcrel"
    }
}

private data class UtilityApp(val app: InstalledApp, val activity: LauncherActivityInfo)

@Composable
private fun rememberUtilityApps(): List<UtilityApp> {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(emptyList<UtilityApp>()) }
    LifecycleResumeEffect(context) {
        val launcherApps = checkNotNull(context.getSystemService(LauncherApps::class.java))
        fun refreshApps() {
            apps = launcherApps.getActivityList(null, Process.myUserHandle()).map { activity ->
                UtilityApp(
                    InstalledApp(activity.label.toString(), activity.componentName.packageName, activity.componentName),
                    activity
                )
            }
        }
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: android.os.UserHandle) = refreshApps()
            override fun onPackageAdded(packageName: String, user: android.os.UserHandle) = refreshApps()
            override fun onPackageChanged(packageName: String, user: android.os.UserHandle) = refreshApps()
            override fun onPackagesAvailable(packages: Array<out String>, user: android.os.UserHandle, replacing: Boolean) = refreshApps()
            override fun onPackagesUnavailable(packages: Array<out String>, user: android.os.UserHandle, replacing: Boolean) = refreshApps()
        }
        launcherApps.registerCallback(callback)
        refreshApps()
        onPauseOrDispose { launcherApps.unregisterCallback(callback) }
    }
    return apps
}

private const val ICON_SCALE_KEY = "icon_scale"

private data class UtilityDockPreferences(
    val assignments: Map<UtilitySlot, String?>,
    val iconScale: Float
)

@Composable
private fun rememberUtilityPreferences(preferences: SharedPreferences): UtilityDockPreferences {
    fun readPreferences() = UtilityDockPreferences(
        assignments = UtilitySlot.entries.associateWith { preferences.getString(it.key, null) },
        iconScale = preferences.getFloat(ICON_SCALE_KEY, 1f).coerceIn(1f, 2f)
    )
    var state by remember(preferences) { mutableStateOf(readPreferences()) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            state = readPreferences()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        state = readPreferences()
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

private fun resolveUtility(
    context: Context,
    slot: UtilitySlot,
    assignment: String?,
    apps: List<UtilityApp>
): UtilityApp? = when (assignment) {
    null -> slot.defaultPackage(context)?.let { packageName ->
        val component = context.packageManager.getLaunchIntentForPackage(packageName)?.component
        apps.firstOrNull { it.app.componentName == component }
            ?: apps.firstOrNull { it.app.packageName == packageName }
    }
    "" -> null
    else -> apps.firstOrNull { it.app.componentName == ComponentName.unflattenFromString(assignment) }
}

internal fun Modifier.pondBottomBar(interfaceScale: Float): Modifier =
    fillMaxWidth().navigationBarsPadding()
        .padding(horizontal = 24.dp * interfaceScale, vertical = 4.dp * interfaceScale)

@Composable
fun UtilityDock(interfaceScale: Float, onAppClick: (InstalledApp) -> Unit, onConfigure: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("utility_dock", Context.MODE_PRIVATE) }
    val apps = rememberUtilityApps()
    val settings = rememberUtilityPreferences(preferences)

    BoxWithConstraints(Modifier.pondBottomBar(interfaceScale)) {
        val targetSize = 48.dp
        val baseGap = 12.dp * interfaceScale
        val slotCount = UtilitySlot.entries.size
        val baseWidth = targetSize * slotCount + baseGap * (slotCount - 1)
        val scale = minOf(settings.iconScale, (maxWidth / baseWidth).coerceAtLeast(1f))
        val iconSize = 36.dp * interfaceScale * scale
        val lift = (iconSize - targetSize).coerceAtLeast(0.dp) / 2

        Row(horizontalArrangement = Arrangement.spacedBy(baseGap * scale)) {
            UtilitySlot.entries.forEach { slot ->
                val utility = resolveUtility(context, slot, settings.assignments[slot], apps)
                val label = utility?.app?.displayName
                    ?: stringResource(R.string.pond_setup_utility, stringResource(slot.label))
                Box(
                    modifier = Modifier.width(targetSize * scale).height(targetSize),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        modifier = Modifier.matchParentSize().semantics { contentDescription = label },
                        onClick = { if (utility == null) onConfigure() else onAppClick(utility.app) }
                    ) {}
                    val iconModifier = Modifier.offset(y = -lift).requiredSize(iconSize)
                    if (utility == null) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = iconModifier)
                    } else {
                        val pixels = with(LocalDensity.current) { iconSize.roundToPx() }
                        val icon = remember(utility, pixels) {
                            utility.activity.getIcon(0).toBitmap(pixels, pixels).asImageBitmap()
                        }
                        Image(icon, contentDescription = null, modifier = iconModifier)
                    }
                }
            }
        }
    }
}

@Composable
fun UtilityDockSettings(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("utility_dock", Context.MODE_PRIVATE) }
    val apps = rememberUtilityApps()
    val settings = rememberUtilityPreferences(preferences)
    var editingSlot by remember { mutableStateOf<UtilitySlot?>(null) }

    editingSlot?.let { slot ->
        WeatherAppPicker(
            apps = apps.map { it.app },
            onAppSelected = {
                preferences.edit().putString(slot.key, it.componentName.flattenToString()).apply()
                editingSlot = null
            },
            onDismiss = { editingSlot = null }
        )
    }

    LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp)) {
        item { EscapeHeader(onBackClick, stringResource(R.string.pond_utility_dock)) }
        item(key = "icon_scale") {
            SettingsSlider(
                label = stringResource(R.string.pond_dock_icon_size, (settings.iconScale * 100).roundToInt()),
                value = settings.iconScale,
                onValueChange = { preferences.edit().putFloat(ICON_SCALE_KEY, it).apply() },
                valueRange = 1f..2f,
                steps = 9,
                onReset = { preferences.edit().remove(ICON_SCALE_KEY).apply() },
                resetButtonContentDescription = stringResource(R.string.reset_to_default),
                isTopOfGroup = true,
                isBottomOfGroup = true
            )
            SettingsSpacer()
        }
        UtilitySlot.entries.forEach { slot ->
            item(key = slot.key) {
                val label = stringResource(slot.label)
                val assignment = settings.assignments[slot]
                val app = resolveUtility(context, slot, assignment, apps)
                val status = app?.app?.displayName ?: stringResource(
                    if (assignment == "") R.string.pond_utility_cleared else R.string.pond_utility_unavailable
                )
                SettingsButton(
                    label = stringResource(R.string.pond_utility_assignment, label, status),
                    onClick = { editingSlot = slot },
                    isTopOfGroup = true
                )
                SettingsButton(
                    label = stringResource(R.string.pond_utility_clear, label),
                    onClick = { preferences.edit().putString(slot.key, "").apply() },
                    isDisabled = assignment == ""
                )
                SettingsButton(
                    label = stringResource(R.string.pond_utility_default, label),
                    onClick = { preferences.edit().remove(slot.key).apply() },
                    isDisabled = assignment == null,
                    isBottomOfGroup = true
                )
                SettingsSpacer()
            }
        }
    }
}
