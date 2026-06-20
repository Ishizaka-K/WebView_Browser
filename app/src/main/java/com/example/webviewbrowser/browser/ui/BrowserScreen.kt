package com.example.webviewbrowser.browser.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.webviewbrowser.browser.webview.WebViewCapture
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.webviewbrowser.browser.BrowserEffect
import com.example.webviewbrowser.browser.BrowserIntent
import com.example.webviewbrowser.browser.BrowserViewModel
import com.example.webviewbrowser.home.ui.HomeScreen
import com.example.webviewbrowser.settings.SettingsViewModel
import com.example.webviewbrowser.settings.WebSettingsApplier
import com.example.webviewbrowser.tabs.ui.TabsSheet
import kotlinx.coroutines.launch

/**
 * ブラウザのメイン画面（Chrome 風）。
 * 上バーに オムニボックス + ホーム + タブ + メニュー を集約。下バーは持たない。
 * 戻る/進む/共有/新規タブ/検索などはメニューから操作する。
 */
@Composable
fun BrowserScreen(
    onOpenBookmarks: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: BrowserViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appSettings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showTabs by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val omniboxFocus = remember { FocusRequester() }

    fun openTabs() {
        val tab = state.activeTab
        val window = context.findActivity()?.window
        if (tab != null && !tab.isHome && window != null) {
            val webView = viewModel.acquireWebView(tab.id)
            scope.launch {
                WebViewCapture.capture(window, webView)?.let { viewModel.setThumbnail(tab.id, it) }
                showTabs = true
            }
        } else {
            showTabs = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BrowserEffect.ShareUrl -> {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, effect.url)
                        putExtra(Intent.EXTRA_SUBJECT, effect.title ?: effect.url)
                    }
                    context.startActivity(Intent.createChooser(send, null))
                }
                is BrowserEffect.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(effect.message) }
                is BrowserEffect.RequestDownload -> scope.launch {
                    snackbarHostState.showSnackbar("ダウンロード要求: ${effect.event.url}")
                }
            }
        }
    }

    val activeTab = state.activeTab

    BackHandler(enabled = true) {
        when {
            state.find.active -> viewModel.onIntent(BrowserIntent.ToggleFind(false))
            showTabs -> showTabs = false
            activeTab?.canGoBack == true -> viewModel.onIntent(BrowserIntent.GoBack)
            state.tabCount > 1 && activeTab != null -> viewModel.onIntent(BrowserIntent.CloseTab(activeTab.id))
            else -> context.findActivity()?.finish()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (state.find.active) {
                FindInPageBar(
                    state = state.find,
                    onQueryChange = { viewModel.onIntent(BrowserIntent.FindQueryChanged(it)) },
                    onNext = { viewModel.onIntent(BrowserIntent.FindNext(true)) },
                    onPrev = { viewModel.onIntent(BrowserIntent.FindNext(false)) },
                    onClose = { viewModel.onIntent(BrowserIntent.ToggleFind(false)) },
                )
            } else {
                BrowserTopBar(
                    text = state.addressInput,
                    isLoading = activeTab?.isLoading == true,
                    progress = activeTab?.progress ?: 0,
                    tabCount = state.tabCount,
                    canGoBack = activeTab?.canGoBack == true,
                    canGoForward = activeTab?.canGoForward == true,
                    canShare = activeTab?.isHome == false,
                    menuExpanded = showMenu,
                    onMenuToggle = { showMenu = it },
                    onTextChange = { viewModel.onIntent(BrowserIntent.AddressInputChanged(it)) },
                    onSubmit = { viewModel.onIntent(BrowserIntent.SubmitAddress(it)) },
                    onReloadOrStop = {
                        if (activeTab?.isLoading == true) viewModel.onIntent(BrowserIntent.StopLoading)
                        else viewModel.onIntent(BrowserIntent.Reload)
                    },
                    onHome = { viewModel.onIntent(BrowserIntent.GoHome) },
                    onShowTabs = { openTabs() },
                    focusRequester = omniboxFocus,
                    onBack = { viewModel.onIntent(BrowserIntent.GoBack) },
                    onForward = { viewModel.onIntent(BrowserIntent.GoForward) },
                    onNewTab = { viewModel.onIntent(BrowserIntent.NewTab()) },
                    onShare = { viewModel.onIntent(BrowserIntent.ShareCurrent) },
                    onFind = { viewModel.onIntent(BrowserIntent.ToggleFind(true)) },
                    onAddBookmark = { viewModel.onIntent(BrowserIntent.ToggleBookmarkCurrent) },
                    onOpenBookmarks = onOpenBookmarks,
                    onOpenHistory = onOpenHistory,
                    onOpenDownloads = onOpenDownloads,
                    onOpenSettings = onOpenSettings,
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (activeTab == null || activeTab.isHome) {
                HomeScreen(
                    onOpenUrl = { viewModel.onIntent(BrowserIntent.SubmitAddress(it)) },
                    onStartSearch = { omniboxFocus.requestFocus() },
                )
            } else {
                val activeTabId = activeTab.id
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx -> FrameLayout(ctx) },
                    update = { container ->
                        val webView = viewModel.acquireWebView(activeTabId)
                        WebSettingsApplier.apply(webView, appSettings)
                        (webView.parent as? ViewGroup)?.removeView(webView)
                        container.removeAllViews()
                        container.addView(webView)
                    },
                    onRelease = { container -> container.removeAllViews() },
                )
            }

            if (showTabs) {
                TabsSheet(
                    tabs = state.tabs,
                    activeTabId = state.activeTabId,
                    thumbnailProvider = { id -> viewModel.tabThumbnail(id)?.asImageBitmap() },
                    faviconProvider = { id -> viewModel.tabFavicon(id)?.asImageBitmap() },
                    onSwitch = { viewModel.onIntent(BrowserIntent.SwitchTab(it)) },
                    onClose = { viewModel.onIntent(BrowserIntent.CloseTab(it)) },
                    onMoveTab = { fromIndex, toIndex -> viewModel.moveTab(fromIndex, toIndex) },
                    onNewTab = { viewModel.onIntent(BrowserIntent.NewTab()) },
                    onDismiss = { showTabs = false },
                )
            }
        }
    }
}

@Composable
private fun BrowserTopBar(
    text: String,
    isLoading: Boolean,
    progress: Int,
    tabCount: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    canShare: Boolean,
    menuExpanded: Boolean,
    focusRequester: FocusRequester,
    onMenuToggle: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onReloadOrStop: () -> Unit,
    onHome: () -> Unit,
    onShowTabs: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onNewTab: () -> Unit,
    onShare: () -> Unit,
    onFind: () -> Unit,
    onAddBookmark: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    AddressBar(
        text = text,
        isLoading = isLoading,
        progress = progress,
        onTextChange = onTextChange,
        onSubmit = onSubmit,
        onReloadOrStop = onReloadOrStop,
        focusRequester = focusRequester,
        trailingMenu = {
            IconButton(onClick = onHome) { Icon(Icons.Default.Home, contentDescription = "ホーム") }
            TabCountButton(count = tabCount, onClick = onShowTabs)
            Box {
                IconButton(onClick = { onMenuToggle(true) }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "メニュー")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenuToggle(false) }) {
                    DropdownMenuItem(
                        text = { Text("戻る") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                        enabled = canGoBack,
                        onClick = { onMenuToggle(false); onBack() },
                    )
                    DropdownMenuItem(
                        text = { Text("進む") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                        enabled = canGoForward,
                        onClick = { onMenuToggle(false); onForward() },
                    )
                    DropdownMenuItem(
                        text = { Text("新しいタブ") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = { onMenuToggle(false); onNewTab() },
                    )
                    DropdownMenuItem(
                        text = { Text("共有") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        enabled = canShare,
                        onClick = { onMenuToggle(false); onShare() },
                    )
                    DropdownMenuItem(
                        text = { Text("ページ内検索") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        onClick = { onMenuToggle(false); onFind() },
                    )
                    DropdownMenuItem(
                        text = { Text("ブックマークに追加") },
                        leadingIcon = { Icon(Icons.Default.StarBorder, contentDescription = null) },
                        enabled = canShare,
                        onClick = { onMenuToggle(false); onAddBookmark() },
                    )
                    DropdownMenuItem(
                        text = { Text("ブックマーク") },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                        onClick = { onMenuToggle(false); onOpenBookmarks() },
                    )
                    DropdownMenuItem(
                        text = { Text("履歴") },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                        onClick = { onMenuToggle(false); onOpenHistory() },
                    )
                    DropdownMenuItem(
                        text = { Text("ダウンロード") },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                        onClick = { onMenuToggle(false); onOpenDownloads() },
                    )
                    DropdownMenuItem(
                        text = { Text("設定") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = { onMenuToggle(false); onOpenSettings() },
                    )
                }
            }
        },
    )
}

@Composable
private fun TabCountButton(count: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
