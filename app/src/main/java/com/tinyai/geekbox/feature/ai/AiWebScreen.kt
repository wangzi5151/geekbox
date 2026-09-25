@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tinyai.geekbox.feature.ai

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.tinyai.geekbox.core.ui.components.ScreenScaffold

private const val DESKTOP_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

private fun engineUa(context: Context): String =
    WebSettings.getDefaultUserAgent(context)
        .replace("; wv", "")
        .replace(" Version/4.0", "")
        .replace("Version/4.0 ", "")

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AiWebScreen(title: String, url: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var desktop by remember { mutableStateOf(false) }
    var compatInput by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var currentUrl by remember { mutableStateOf(url) }
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        fileCallback = null
    }

    val handleBack: () -> Unit = {
        val wv = webViewRef.value
        if (canGoBack && wv != null) wv.goBack() else onBack()
    }

    fun openExternal(target: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target))) }
    }

    BackHandler { handleBack() }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef.value?.stopLoading()
            webViewRef.value?.destroy()
            webViewRef.value = null
        }
    }

    ScreenScaffold(
        title,
        onBack = handleBack,
        actions = {
            IconButton(onClick = { webViewRef.value?.reload() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "刷新")
            }
            IconButton(onClick = {
                compatInput = !compatInput
                webViewRef.value?.setLayerType(
                    if (compatInput) View.LAYER_TYPE_SOFTWARE else View.LAYER_TYPE_HARDWARE,
                    null
                )
                Toast.makeText(
                    context,
                    if (compatInput) "输入兼容模式：开（修复文字顺序异常）" else "输入兼容模式：关",
                    Toast.LENGTH_SHORT
                ).show()
            }) {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = "输入兼容模式",
                    tint = if (compatInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = {
                desktop = !desktop
                webViewRef.value?.settings?.userAgentString = if (desktop) DESKTOP_UA else engineUa(context)
                webViewRef.value?.reload()
            }) {
                Icon(if (desktop) Icons.Filled.Smartphone else Icons.Filled.DesktopWindows, contentDescription = "PC/手机版")
            }
            IconButton(onClick = { BrowserLauncher.openTab(context, webViewRef.value?.url ?: url) }) {
                Icon(Icons.Filled.OpenInNew, contentDescription = "系统浏览器打开")
            }
            IconButton(onClick = {
                webViewRef.value?.clearCache(true)
                webViewRef.value?.clearHistory()
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                Toast.makeText(context, "已清除缓存与 Cookie", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = "清除数据")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                if (progress in 1..99) {
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                }
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            if (compatInput) setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                            isFocusable = true
                            isFocusableInTouchMode = true
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.userAgentString = engineUa(ctx)
                            settings.allowFileAccess = false
                            settings.allowContentAccess = true
                            settings.setSupportMultipleWindows(true)
                            settings.setNeedInitialFocus(true)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                settings.safeBrowsingEnabled = true
                            }
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    val u = request.url
                                    val scheme = u.scheme ?: return false
                                    if (scheme == "http" || scheme == "https") {
                                        currentUrl = u.toString()
                                        return false
                                    }
                                    openExternal(u.toString())
                                    return true
                                }

                                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                    progress = 5
                                    errorText = null
                                }

                                override fun onPageFinished(view: WebView, url: String?) {
                                    progress = 100
                                    canGoBack = view.canGoBack()
                                    if (url != null) currentUrl = url
                                }

                                override fun onReceivedError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    error: WebResourceError
                                ) {
                                    if (request.isForMainFrame) {
                                        errorText = "加载失败：${error.description}"
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView, newProgress: Int) {
                                    progress = newProgress
                                }

                                override fun onShowFileChooser(
                                    view: WebView,
                                    callback: ValueCallback<Array<Uri>>,
                                    params: FileChooserParams
                                ): Boolean {
                                    fileCallback?.onReceiveValue(null)
                                    fileCallback = callback
                                    return try {
                                        fileLauncher.launch(params.createIntent())
                                        true
                                    } catch (_: Throwable) {
                                        fileCallback = null
                                        false
                                    }
                                }

                                override fun onCreateWindow(
                                    view: WebView,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: Message
                                ): Boolean {
                                    val temp = WebView(context)
                                    temp.webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                                            val target = request.url.toString()
                                            if (target.startsWith("http")) webViewRef.value?.loadUrl(target)
                                            else openExternal(target)
                                            runCatching { v.destroy() }
                                            return true
                                        }
                                    }
                                    (resultMsg.obj as WebView.WebViewTransport).webView = temp
                                    resultMsg.sendToTarget()
                                    return true
                                }
                            }
                            loadUrl(url)
                        }.also { webViewRef.value = it }
                    }
                )
            }

            errorText?.let { message ->
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "可尝试：切换「输入兼容模式」、切换 PC/手机版、或直接用系统浏览器打开。",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { errorText = null; webViewRef.value?.reload() }) { Text("重试") }
                            OutlinedButton(onClick = { BrowserLauncher.openTab(context, currentUrl) }) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("系统浏览器打开")
                            }
                        }
                    }
                }
            }
        }
    }
}
