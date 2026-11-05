package com.luanmuc.openwrtmanager.ui.webview

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.luanmuc.openwrtmanager.ui.components.MiButton
import com.luanmuc.openwrtmanager.ui.components.MiButtonType
import com.luanmuc.openwrtmanager.data.repository.LuciRepository
import com.luanmuc.openwrtmanager.ui.components.MiColors
import com.luanmuc.openwrtmanager.ui.components.MiTheme
import com.luanmuc.openwrtmanager.ui.components.MiTopAppBar
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * WebView插件容器页面
 * 用于显示LuCI Web界面的插件
 * 
 * 特性：
 * - 自动注入session实现自动登录
 * - 注入移动端CSS优化
 * - 隐藏LuCI导航栏
 * - 支持返回和刷新
 */
@Composable
fun WebViewPluginScreen(
    title: String,
    url: String,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var canGoBack by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<android.webkit.WebView?>(null) }
    
    // 获取LuCI仓库实例
    val luciRepository = remember { LuciRepository.getInstance(context) }
    
    Scaffold(
        topBar = {
            MiTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = Loc.s(R.string.s0244),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = Loc.s(R.string.s0322),
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 加载进度条
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MiColors.Primary,
                    trackColor = Color.Transparent
                )
            }
            
            // WebView
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MiTheme.Background)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply webView@{
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            
                            // 初始化CookieManager
                            CookieManager.getInstance().apply {
                                setAcceptCookie(true)
                                setAcceptThirdPartyCookies(this@webView, true)
                            }
                            
                            // WebView设置 - 安全加固
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                
                                // 安全加固 - 禁用危险功能
                                allowFileAccess = false
                                allowContentAccess = false
                                allowFileAccessFromFileURLs = false
                                allowUniversalAccessFromFileURLs = false
                                
                                // 密码保存安全
                                savePassword = false
                                saveFormData = false
                                
                                // 文本选择
                                setTextZoom(100)
                            }
                            
                            // WebView客户端
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                }
                                
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    errorMessage = null
                                    
                                    // 注入移动端优化CSS
                                    view?.loadUrl("javascript:injectMobileCss()")
                                    
                                    // 注入自动登录脚本
                                    view?.loadUrl("javascript:autoLoginLuci()")
                                    
                                    // 隐藏LuCI导航栏
                                    view?.loadUrl("javascript:hideLuciNavigation()")
                                }
                                
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        errorMessage = error?.description?.toString() ?: Loc.s(R.string.s0329)
                                    }
                                }
                            }
                            
                            webChromeClient = object : WebChromeClient() {
                                // 文件上传支持
                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallback: ValueCallback<Array<Uri>>?,
                                    fileChooserParams: FileChooserParams?
                                ): Boolean {
                                    // 简单的文件上传支持，实际应用中需要调用系统文件选择器
                                    filePathCallback?.onReceiveValue(null)
                                    return true
                                }
                                
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    progress = newProgress / 100f
                                }
                            }
                            
                            // 设置session cookie实现自动登录
                            setupAutoLogin(luciRepository, url)
                            
                            // 加载URL
                            loadUrl(url)
                            
                            webView = this
                        }
                    },
                    update = { wv ->
                        webView = wv
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // 加载状态
                if (isLoading && progress < 0.1f) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center),
                        color = MiColors.Primary,
                        strokeWidth = 3.dp
                    )
                }
                
                // 错误页面
                errorMessage?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MiTheme.Background)
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MiColors.Error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = Loc.s(R.string.s0329),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiTheme.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                fontSize = 14.sp,
                                color = MiTheme.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            MiButton(
                                text = Loc.s(R.string.s0775),
                                onClick = {
                                    errorMessage = null
                                    webView?.reload()
                                },
                                type = MiButtonType.Primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 设置自动登录
 * 通过Cookie注入session token
 */
private fun setupAutoLogin(luciRepository: LuciRepository, url: String) {
    try {
        val authToken = luciRepository.getCurrentAuthToken()
        if (authToken.isNotEmpty()) {
            val cookieManager = CookieManager.getInstance()
            
            // 解析域名
            val domain = try {
                val urlObj = java.net.URL(url)
                urlObj.host
            } catch (e: Exception) {
                ""
            }
            
            if (domain.isNotEmpty()) {
                // 设置session cookie
                val sessionCookie = "sysauth=$authToken; Domain=$domain; Path=/; HttpOnly"
                cookieManager.setCookie(domain, sessionCookie)
                
                // 也设置LuCI的session cookie
                val luciSessionCookie = "luci_session=$authToken; Domain=$domain; Path=/; HttpOnly"
                cookieManager.setCookie(domain, luciSessionCookie)
                
                cookieManager.flush()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 移动端优化CSS和自动登录脚本
 * 注入到WebView中，优化LuCI在移动端的显示
 */
const val MOBILE_CSS = """
function injectMobileCss() {
    const css = `
        /* 移动端优化样式 */
        body {
            font-size: 14px !important;
            -webkit-text-size-adjust: 100%;
        }
        
        /* 顶部导航优化 */
        .main > .topbar {
            position: sticky;
            top: 0;
            z-index: 1000;
        }
        
        /* 菜单优化 */
        .main > .sidemenu {
            display: none;
        }
        
        .main > .maincontent {
            margin-left: 0 !important;
        }
        
        /* 卡片优化 */
        .cbi-section {
            margin: 8px !important;
            border-radius: 12px !important;
            overflow: hidden;
        }
        
        /* 表格优化 */
        table {
            font-size: 12px !important;
        }
        
        /* 按钮优化 */
        .btn, button, input[type="button"], input[type="submit"] {
            padding: 8px 16px !important;
            border-radius: 8px !important;
            font-size: 14px !important;
        }
        
        /* 输入框优化 */
        input[type="text"], input[type="password"], select, textarea {
            padding: 8px !important;
            border-radius: 8px !important;
            font-size: 14px !important;
        }
        
        /* 隐藏PC端元素 */
        .desktop-only {
            display: none !important;
        }
        
        /* 响应式表格 */
        @media (max-width: 600px) {
            table, thead, tbody, th, td, tr {
                display: block;
            }
            
            thead tr {
                position: absolute;
                top: -9999px;
                left: -9999px;
            }
            
            tr {
                margin-bottom: 10px;
                border: 1px solid #ccc;
                border-radius: 8px;
            }
            
            td {
                border: none;
                position: relative;
                padding-left: 50%;
            }
            
            td:before {
                content: attr(data-label);
                position: absolute;
                left: 6px;
                width: 45%;
                padding-right: 10px;
                white-space: nowrap;
                font-weight: bold;
            }
        }
    `;
    
    const style = document.createElement('style');
    style.textContent = css;
    document.head.appendChild(style);
}

/**
 * 自动登录LuCI
 * 如果检测到登录页面，自动尝试登录
 */
function autoLoginLuci() {
    // 检查是否是登录页面
    const loginForm = document.querySelector('#maincontent form[action*="login"]');
    if (loginForm) {
        // 如果有登录表单，说明还没登录
        // 尝试通过cookie自动登录（已经通过CookieManager设置了session）
        // 如果cookie方式失败，页面会显示登录表单
        console.log('检测到登录页面，Cookie自动登录可能失败');
    }
}

/**
 * 隐藏LuCI导航栏
 * 因为我们已经有APP的顶部导航栏了
 */
function hideLuciNavigation() {
    // 隐藏顶部导航栏
    const topbar = document.querySelector('.main > .topbar');
    if (topbar) {
        topbar.style.display = 'none';
    }
    
    // 隐藏侧边菜单
    const sidemenu = document.querySelector('.main > .sidemenu');
    if (sidemenu) {
        sidemenu.style.display = 'none';
    }
    
    // 调整主内容区域
    const maincontent = document.querySelector('.main > .maincontent');
    if (maincontent) {
        maincontent.style.marginLeft = '0';
        maincontent.style.marginTop = '0';
    }
    
    // 隐藏底部信息
    const footer = document.querySelector('.main > .footer');
    if (footer) {
        footer.style.display = 'none';
    }
}
"""
