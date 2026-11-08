package com.geecee.escapelauncher.feature.homescreen

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.geecee.escapelauncher.core.ui.utils.escapeShadow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.geecee.escapelauncher.core.common.DefaultSettings
import com.geecee.escapelauncher.core.common.formatScreenTime
import com.geecee.escapelauncher.core.model.InstalledApp
import com.geecee.escapelauncher.core.ui.DefaultSettingsUi
import com.geecee.escapelauncher.core.ui.R
import com.geecee.escapelauncher.core.ui.composables.FirstTimeHelp
import com.geecee.escapelauncher.core.ui.composables.HomeScreenItem
import com.geecee.escapelauncher.core.ui.utils.doHapticFeedBack
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

private val pondBullets = listOf(
    R.drawable.pond_leaf,
    R.drawable.pond_lily,
    R.drawable.pond_flower,
    R.drawable.pond_hexagon
)

private val pondAccents = listOf(
    Color(0xFF73BA56),
    Color(0xFF28BCA7),
    Color(0xFFF188B3),
    Color(0xFFA285EA)
)

@Composable
private fun pondGlyphColor(index: Int, showWallpaper: Boolean): Color {
    val scheme = MaterialTheme.colorScheme
    val themeColor = when (index) {
        0, 3 -> if (showWallpaper) scheme.primaryFixed else scheme.primary
        1 -> if (showWallpaper) scheme.secondaryFixed else scheme.secondary
        else -> if (showWallpaper) scheme.tertiaryFixed else scheme.tertiary
    }
    return lerp(themeColor, pondAccents[index], 0.65f)
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    glanceWidgets: @Composable (FlowRowScope.() -> Unit) = {},
    widgets: LazyListScope.() -> Unit = {},
    favoritesPage: Boolean = false,
    interfaceScale: Float = DefaultSettings.INTERFACE_SCALE,
    onAppClick: (app: InstalledApp) -> Unit = {},
    onAppLongClick: (app: InstalledApp) -> Unit = {},
    homeViewModel: NewHomeScreenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val twelveHourClock by homeViewModel.twelveHourClock.collectAsState(initial = DefaultSettings.TWELVE_HOUR_CLOCK)
    val showClock by homeViewModel.showClock.collectAsState(initial = DefaultSettings.SHOW_CLOCK)
    val bigClock by homeViewModel.bigClock.collectAsState(initial = DefaultSettings.BIG_CLOCK)
    val timeParts by homeViewModel.timeParts.collectAsState()
    val showDate by homeViewModel.showDate.collectAsState(initial = DefaultSettings.SHOW_DATE)
    val showScreenTimeHome by homeViewModel.showScreenTimeHome.collectAsState(initial = DefaultSettings.SHOW_SCREEN_TIME_HOME)
    val showScreenTimeApp by homeViewModel.showScreenTimeApp.collectAsState(initial = DefaultSettings.SHOW_SCREEN_TIME_APP)
    val firstTimeHelp by homeViewModel.firstTimeHelp.collectAsState(initial = false)
    val homeAlignment by homeViewModel.homeAlignment.collectAsState(initial = DefaultSettingsUi.HOME_ALIGNMENT)
    val homeVAlignment by homeViewModel.homeVAlignment.collectAsState(initial = DefaultSettingsUi.HOME_V_ALIGNMENT)
    val hapticFeedbackEnabled by homeViewModel.hapticFeedBackEnabled.collectAsState(initial = DefaultSettings.HAPTIC_FEEDBACK)
    val appUsageList by homeViewModel.appUsageUiList.collectAsState()
    val favoriteApps by homeViewModel.favoriteApps.collectAsState()
    val showWallpaper by homeViewModel.showWallpaper.collectAsState(initial = false)
    val totalUsage by homeViewModel.totalUsage.collectAsState()

    val (hour, minute, _) = timeParts

    LaunchedEffect(twelveHourClock) {
        homeViewModel.startClockTicker(twelveHourClock)
    }

    val scrollState = rememberLazyListState()

    // This is for the swipe down to get to quick settings thing
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            var totalDrag = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0) {
                    totalDrag += available.y

                    if (totalDrag > 150f) {
                        try {
                            @SuppressLint("WrongConstant") val service =
                                context.getSystemService("statusbar")

                            val statusBarManager = Class.forName("android.app.StatusBarManager")
                            val expand = statusBarManager.getMethod("expandNotificationsPanel")
                            expand.invoke(service)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        totalDrag = 0f
                        return available
                    }
                } else {
                    totalDrag = 0f
                }
                return Offset.Zero
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = scrollState,
            verticalArrangement = homeVAlignment,
            horizontalAlignment = homeAlignment,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp * interfaceScale)
                .nestedScroll(nestedScrollConnection)
        ) {
            //Top padding
            item {
                Spacer(Modifier.height((if (favoritesPage) 90.dp else 54.dp) * interfaceScale))
            }

            if (!favoritesPage) {
                item {
                    val scheme = MaterialTheme.colorScheme
                    val color = if (showWallpaper) scheme.primaryFixed else scheme.primary
                    val shadow = escapeShadow(scheme.scrim, showWallpaper)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp * interfaceScale)
                    ) {
                        if (showClock) {
                            Text(
                                text = "%02d:%02d".format(hour, minute),
                                modifier = Modifier.alignByBaseline().clickable {
                                    try {
                                        context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS))
                                    } catch (e: Exception) {
                                        homeViewModel.logException(e)
                                    }
                                }.heightIn(min = 48.dp).wrapContentHeight(),
                                color = color,
                                style = (if (bigClock) MaterialTheme.typography.bodyLarge
                                    else MaterialTheme.typography.bodyMedium).copy(shadow = shadow),
                                maxLines = 1
                            )
                        }
                        if (showDate) {
                            val dateFormat =
                                remember { SimpleDateFormat("dd MMM yy", Locale.getDefault()) }
                            var dateText by remember { mutableStateOf(dateFormat.format(Date())) }

                            LaunchedEffect(Unit) {
                                while (true) {
                                    val calendar = Calendar.getInstance()
                                    val now = calendar.timeInMillis
                                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                                    calendar.set(Calendar.MINUTE, 0)
                                    calendar.set(Calendar.SECOND, 0)
                                    calendar.set(Calendar.MILLISECOND, 0)
                                    val delayMillis = calendar.timeInMillis - now
                                    delay(delayMillis.milliseconds)
                                    dateText = dateFormat.format(Date())
                                }
                            }
                            Text(
                                text = dateText,
                                modifier = Modifier.weight(1f).alignByBaseline().clickable {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_MAIN).apply {
                                            addCategory(Intent.CATEGORY_APP_CALENDAR)
                                        })
                                    } catch (e: Exception) {
                                        homeViewModel.logException(e)
                                    }
                                }.heightIn(min = 48.dp).wrapContentHeight(),
                                color = color.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.bodySmall.let { it.copy(fontSize = it.fontSize * 0.8f, shadow = shadow) },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        if (showScreenTimeHome) {
                            Row(
                                modifier = Modifier.alignBy(FirstBaseline),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = "Screen time",
                                    modifier = Modifier.size(18.dp * interfaceScale).padding(end = 2.dp),
                                    tint = color
                                )
                                Text(
                                    text = formatScreenTime(totalUsage),
                                    color = color,
                                    style = MaterialTheme.typography.bodySmall.copy(shadow = shadow),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp * interfaceScale)) {
                        glanceWidgets()
                    }
                }

                item {
                    Spacer(Modifier.height(10.dp * interfaceScale))
                }

                widgets()
            }

            //Apps
            if (favoritesPage) itemsIndexed(favoriteApps, key = { _, app -> app.packageName }) { index, app ->
                val screenTime = remember(appUsageList) {
                    homeViewModel.getScreenTime(app.packageName)
                }

                HomeScreenItem(
                    appName = app.displayName,
                    interfaceScale = interfaceScale,
                    leadingContent = {
                        val glyph = index % pondBullets.size
                        Icon(
                            painter = painterResource(pondBullets[glyph]),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 10.dp * interfaceScale).size(12.dp * interfaceScale),
                            tint = pondGlyphColor(glyph, showWallpaper)
                        )
                    },
                    screenTime = formatScreenTime(screenTime),
                    onAppClick = {
                        onAppClick(app)
                        doHapticFeedBack(haptics, hapticFeedbackEnabled)
                    },
                    onAppLongClick = {
                        onAppLongClick(app)
                        doHapticFeedBack(hapticFeedback = haptics, enabled = hapticFeedbackEnabled)
                    },
                    showScreenTime = showScreenTimeApp,
                    modifier = Modifier,
                    alignment = homeAlignment,
                    shadow = showWallpaper,
                    color = if (showWallpaper) MaterialTheme.colorScheme.primaryFixed else MaterialTheme.colorScheme.primary,
                    screenTimeColor = if (showWallpaper) MaterialTheme.colorScheme.secondaryFixed else MaterialTheme.colorScheme.primary,
                    screenTimeAlpha = if (showWallpaper) 1f else 0.5f
                )
            }

            item {
                AnimatedVisibility(
                    visible = firstTimeHelp && !favoritesPage,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(Modifier.height(15.dp))

                        FirstTimeHelp(
                            swipeForAllAppsText = stringResource(R.string.pond_swipe_help),
                            holdForSettingsText = stringResource(R.string.hold_for_settings)
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(90.dp * interfaceScale))
            }
        }
    }
}