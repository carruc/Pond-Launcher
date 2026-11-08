package com.geecee.escapelauncher

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.geecee.escapelauncher.screens.pondBottomBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.geecee.escapelauncher.core.navigation.AppNavKey
import com.geecee.escapelauncher.core.ui.R as UiR
import com.geecee.escapelauncher.feature.newwidgets.WidgetRenderer
import com.geecee.escapelauncher.screens.UtilityDock
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.geecee.escapelauncher.core.common.DefaultSettings
import com.geecee.escapelauncher.core.domain.managedprofiles.ManagedProfileType
import com.geecee.escapelauncher.core.model.InstalledApp
import com.geecee.escapelauncher.core.ui.DefaultSettingsUi
import com.geecee.escapelauncher.core.ui.composables.TabbedScreen
import com.geecee.escapelauncher.core.ui.utils.doHapticFeedBack
import com.geecee.escapelauncher.feature.appslist.AppsListViewModel
import com.geecee.escapelauncher.feature.homescreen.HomeScreen
import com.geecee.escapelauncher.feature.screentime.ScreenTimeDashboard
import com.geecee.escapelauncher.feature.screentime.ScreenTimeViewModel
import com.geecee.escapelauncher.feature.securefolder.SecureFolderButton
import com.geecee.escapelauncher.feature.securefolder.canUseSecureFolder
import com.geecee.escapelauncher.screens.ParentBottomSheetOverlay
import com.geecee.escapelauncher.feature.workapps.WorkApps
import com.geecee.escapelauncher.privatespace.PrivateSpace
import com.geecee.escapelauncher.screens.AppsListTabScreen
import com.geecee.escapelauncher.screens.OpenChallengeOverlay
import com.geecee.escapelauncher.screens.HomeWeatherImpl
import kotlinx.coroutines.launch

/**
 * Main composable for home screen:
 * contains a pager with all the pages inside of it, contains bottom sheet, contains open challenge UI
 */
@Composable
fun MainPagerScreen(
    viewModel: MainPagerScreenViewModel = hiltViewModel(),
    appsListViewModel: AppsListViewModel = hiltViewModel(),
    screenTimeViewModel: ScreenTimeViewModel = hiltViewModel(LocalActivity.current as ComponentActivity),
) {
    val appslistScrollState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val hideScreenTimePage by viewModel.hideScreenTimePage.collectAsState()
    val doubleTapToLock by viewModel.doubleTapToLock.collectAsState(initial = DefaultSettings.DOUBLE_TAP_TO_LOCK)
    val hapticFeedbackEnabled by viewModel.hapticFeedBackEnabled.collectAsState(initial = DefaultSettings.HAPTIC_FEEDBACK)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val homeAlignment by viewModel.homeAlignment.collectAsState(initial = DefaultSettingsUi.HOME_ALIGNMENT)
    val showWeather by viewModel.showWeather.collectAsState(initial = DefaultSettings.SHOW_WEATHER)
    val favoritesSearchId by viewModel.favoritesSearchWidgetId.collectAsState(initial = null)
    val widgets by viewModel.widgets.collectAsState(initial = emptyList())
    val widgetHostManager = viewModel.widgetHostManager
    val interfaceScale by viewModel.interfaceScale.collectAsState(DefaultSettings.INTERFACE_SCALE)
    val showWallpaper by viewModel.showWallpaper.collectAsState(initial = false)

    // Global action callbacks
    val handleAppClick: (InstalledApp) -> Unit = { app ->
        viewModel.openApp(
            app = app,
            overrideChallenge = false,
            onAppOpened = {
                screenTimeViewModel.onAppOpened(it)
                appsListViewModel.onSearchExpandedChanged(false)
                doHapticFeedBack(haptics, hapticFeedbackEnabled)
            }
        )
    }
    val handleAppLongClick: (InstalledApp) -> Unit = { app ->
        viewModel.setBottomSheetApp(app)
        viewModel.setBottomSheetVisible(true)
        doHapticFeedBack(haptics, hapticFeedbackEnabled)
    }

    // Make it that if you go back you go back to main page
    BackHandler(enabled = true) {
        coroutineScope.launch {
            viewModel.animatedGoToMainPage()
        }
    }

    // Generate Apps List tabs
    val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsState()
    val appsListTabs = listOf(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && canUseSecureFolder(context = context)) {
            TabbedScreen(
                title = "Secure Folder", icon = Icons.Default.Lock, content = {
                    SecureFolderButton(
                        modifier = Modifier.fillMaxSize()
                    )
                })
        } else {
            null
        },
        if (isDefaultLauncher && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && viewModel.managedProfileExists(
                ManagedProfileType.PrivateSpace
            )
        ) {
            TabbedScreen(
                title = "Private", icon = Icons.Default.Lock, content = {
                    PrivateSpace(
                        modifier = Modifier.fillMaxSize(),
                        onAppClick = { app -> handleAppClick(app) },
                        onAppLongClick = { app -> handleAppLongClick(app) }
                    )
                })
        } else {
            null
        },
        if (viewModel.isManagedProfileSupported(type = ManagedProfileType.WorkApps) && viewModel.managedProfileExists(
                type = ManagedProfileType.WorkApps
            )
        ) {
            TabbedScreen(
                title = "Work", icon = Icons.Default.Work, content = {
                    WorkApps(
                        modifier = Modifier.fillMaxSize(),
                        onAppClick = { app -> handleAppClick(app) },
                        onAppLongClick = { app -> handleAppLongClick(app) }
                    )
                })
        } else {
            null
        }
    )

    // Figure out where all the pages are
    val screenTimePageIndex = if (!hideScreenTimePage) 0 else -1
    val homePageIndex = if (hideScreenTimePage) 0 else 1
    val favoritesPageIndex = homePageIndex + 1
    val appsListPageIndex = favoritesPageIndex + 1

    // Some apps list stuff
    val isAppsListVisible = viewModel.pagerState.currentPage == appsListPageIndex
    val autoOpenSearch by appsListViewModel.searchAutoOpen.collectAsState(initial = DefaultSettings.SEARCH_AUTO_OPEN)

    // Tidy up apps list when it closes or opens
    LaunchedEffect(isAppsListVisible) {
        if (!isAppsListVisible) {
            appsListViewModel.onSearchExpandedChanged(false)
            appslistScrollState.scrollToItem(0)
            viewModel.setBottomSheetVisible(false)
        } else if (autoOpenSearch) {
            appsListViewModel.onSearchExpandedChanged(true)
        }
    }

    // Home Screen Pages
    HorizontalPager(
        state = viewModel.pagerState,
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = {},
                onLongClickLabel = "",
                onLongClick = {
                    viewModel.openSettings()
                    viewModel.setFirstTimeHelp(false)
                },
                indication = null,
                interactionSource = viewModel.interactionSource,
                onDoubleClick = {
                    // Turn screen off
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if (doubleTapToLock) {
                            viewModel.lockScreen()
                        }
                    }
                })
    ) { page ->
        when (page) {
            screenTimePageIndex -> ScreenTimeDashboard()

            homePageIndex, favoritesPageIndex -> Column(Modifier.fillMaxSize()) {
                val favoritesPage = page == favoritesPageIndex
                HomeScreen(
                    modifier = Modifier.weight(1f).then(
                        if (favoritesPage) Modifier else Modifier.widgetDockFade()
                    ),
                    favoritesPage = favoritesPage,
                    interfaceScale = interfaceScale,
                    onAppClick = handleAppClick,
                    onAppLongClick = handleAppLongClick,
                    widgets = {
                        val availableWidgets = widgets.filter {
                            widgetHostManager.manager.getAppWidgetInfo(it.id) != null
                        }
                        if (availableWidgets.isEmpty()) {
                            item(key = "add_widget") {
                                TextButton(onClick = {
                                    viewModel.navRouter.navigateTo(AppNavKey.Settings.Widget)
                                }) {
                                    Text(stringResource(UiR.string.pond_add_widget))
                                }
                            }
                        }
                        items(availableWidgets, key = { it.id }) { widget ->
                            WidgetRenderer(
                                appWidgetId = widget.id,
                                widgetHostManager = widgetHostManager,
                                modifier = Modifier.offset(x = widget.offset.dp).padding(bottom = 6.dp)
                                    .size(widget.width.dp * interfaceScale, widget.height.dp * interfaceScale)
                            )
                        }
                    },
                    glanceWidgets = {
                        if (showWeather && !viewModel.isFoss) {
                            HomeWeatherImpl(
                                alignment = homeAlignment,
                                shadow = showWallpaper,
                                color = if (showWallpaper) MaterialTheme.colorScheme.primaryFixed else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                if (!favoritesPage) {
                    UtilityDock(
                        interfaceScale = interfaceScale,
                        onAppClick = handleAppClick,
                        onConfigure = { viewModel.navRouter.navigateTo(AppNavKey.Settings.UtilityDock) }
                    )
                } else {
                    favoritesSearchId?.let { id ->
                        val provider = widgetHostManager.manager.getAppWidgetInfo(id)
                        if (provider != null) {
                            val providerHeight = with(LocalDensity.current) { provider.minHeight.toDp() }
                            val searchBarHeight = maxOf(76.dp, providerHeight)
                            Box(Modifier.pondBottomBar(interfaceScale)) {
                                WidgetRenderer(
                                    appWidgetId = id,
                                    widgetHostManager = widgetHostManager,
                                    modifier = Modifier.fillMaxWidth().height(searchBarHeight)
                                )
                            }
                        }
                    }
                }
            }

            appsListPageIndex -> AppsListTabScreen(
                scrollState = appslistScrollState,
                interfaceScale = interfaceScale,
                appsListViewModel = appsListViewModel,
                appsListTabs = appsListTabs,
                handleAppClick = handleAppClick,
                handleAppLongClick = handleAppLongClick,
                hapticFeedbackEnabled = hapticFeedbackEnabled
            )
        }
    }

    // Single Parent-level Bottom Sheet
    ParentBottomSheetOverlay(viewModel = viewModel)

    // Open Challenge
    OpenChallengeOverlay(
        viewModel = viewModel,
        screenTimeViewModel = screenTimeViewModel,
        hapticFeedbackEnabled = hapticFeedbackEnabled
    )
}

private fun Modifier.widgetDockFade(): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithCache {
            val fade = Brush.verticalGradient(
                0f to Color.Black,
                0.25f to Color.Black.copy(alpha = 0.85f),
                0.65f to Color.Black.copy(alpha = 0.35f),
                1f to Color.Transparent,
                startY = (size.height - 48.dp.toPx()).coerceAtLeast(0f),
                endY = size.height
            )
            onDrawWithContent {
                drawContent()
                drawRect(fade, blendMode = BlendMode.DstIn)
            }
        }
