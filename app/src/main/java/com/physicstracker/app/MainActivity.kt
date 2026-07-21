package com.physicstracker.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.physicstracker.app.ui.TrackingScreen
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PhysicsTrackerColorScheme = darkColorScheme(
    primary = Color(0xFF58D5F7),
    onPrimary = Color.Black,
    secondary = Color(0xFFF2C66D),
    onSecondary = Color.Black,
    background = Color(0xFF091017),
    onBackground = Color.White,
    surface = Color(0xFF121B24),
    onSurface = Color.White
)

private fun formatProjectTimestamp(timestampMs: Long): String {
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        .format(Date(timestampMs))
}

private data class HomeLink(
    val title: String,
    val subtitle: String,
    val url: String,
    val accent: Color
)

@Composable
private fun LandingLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val ringColor = Color(0xFF12C4A8)
        val accentColor = Color(0xFFF55D1A)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White, Color(0xFFF4F8FB)),
                start = Offset.Zero,
                end = Offset(w, h)
            ),
            cornerRadius = CornerRadius(w * 0.28f, h * 0.28f)
        )
        drawRoundRect(
            color = Color(0xFF0E2030).copy(alpha = 0.18f),
            cornerRadius = CornerRadius(w * 0.28f, h * 0.28f),
            style = Stroke(width = size.minDimension * 0.03f)
        )
        val center = Offset(w * 0.5f, h * 0.42f)
        rotate(-38f, center) {
            drawOval(
                color = ringColor,
                topLeft = Offset(center.x - w * 0.28f, center.y - h * 0.1f),
                size = Size(w * 0.56f, h * 0.2f),
                style = Stroke(width = size.minDimension * 0.032f)
            )
        }
        rotate(38f, center) {
            drawOval(
                color = ringColor,
                topLeft = Offset(center.x - w * 0.28f, center.y - h * 0.1f),
                size = Size(w * 0.56f, h * 0.2f),
                style = Stroke(width = size.minDimension * 0.032f)
            )
        }
        drawCircle(color = accentColor, radius = size.minDimension * 0.055f, center = center)
        val textY = h * 0.78f
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.rgb(9, 16, 23)
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = size.minDimension * 0.17f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                letterSpacing = 0.08f
            }
            canvas.nativeCanvas.drawText("PHYZX", w * 0.5f, textY, paint)
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = if (getSharedPreferences("physics_tracker_settings", MODE_PRIVATE)
                .getBoolean("force_landscape", true)
        ) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            val viewModel: com.physicstracker.app.ui.VideoPlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val projects by viewModel.projects.collectAsState()
            var selectedVideoUriString by rememberSaveable { mutableStateOf<String?>(null) }
            var projectName by rememberSaveable { mutableStateOf("") }
            var showNameDialog by rememberSaveable { mutableStateOf(false) }
            var activeProjectName by rememberSaveable { mutableStateOf<String?>(null) }
            var isProjectActive by rememberSaveable { mutableStateOf(false) }
            var selectedProjectAction by remember { mutableStateOf<com.physicstracker.app.data.ProjectEntity?>(null) }
            var renameProjectDraft by rememberSaveable { mutableStateOf("") }
            var showDeleteProjectConfirm by rememberSaveable { mutableStateOf(false) }
            var homeTab by rememberSaveable { mutableStateOf("experiments") }
            var openInfoUrl by rememberSaveable { mutableStateOf<String?>(null) }
            val appVersion = remember {
                runCatching { packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown" }
                    .getOrDefault("unknown")
            }
            val homeLinks = remember {
                listOf(
                    HomeLink(
                        "2D Nonlinear Simple Pendulum Simulation",
                        "Prof. OSKS Sastri",
                        "https://sastriosks.github.io/2D-Nonlinear-Simple-Pendulum-Simulation/",
                        Color(0xFF58D5F7)
                    ),
                    HomeLink(
                        "Projectile Motion",
                        "Ayushi Awasthi",
                        "https://projectile-motion-9hvyj5sn93tifpiu4ezlbf.streamlit.app/",
                        Color(0xFF12C4A8)
                    ),
                    HomeLink(
                        "Schrodinger Solver",
                        "Author",
                        "https://schrodinger-solver-web.streamlit.app/",
                        Color(0xFFF2C66D)
                    )
                )
            }
            
            val pickMedia = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {}
                    selectedVideoUriString = uri.toString()
                    showNameDialog = true
                }
            }

            MaterialTheme(colorScheme = PhysicsTrackerColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isProjectActive) {
                        if (openInfoUrl != null) {
                            var infoWebView by remember { mutableStateOf<WebView?>(null) }
                            var infoCanGoBack by remember { mutableStateOf(false) }
                            BackHandler(enabled = true) {
                                val webView = infoWebView
                                if (webView != null && webView.canGoBack()) {
                                    webView.goBack()
                                    infoCanGoBack = webView.canGoBack()
                                } else {
                                    openInfoUrl = null
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                        .background(Color.Black.copy(alpha = 0.08f))
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            val webView = infoWebView
                                            if (webView != null && webView.canGoBack()) {
                                                webView.goBack()
                                                infoCanGoBack = webView.canGoBack()
                                            }
                                        },
                                        enabled = infoCanGoBack,
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("Back")
                                    }
                                    TextButton(
                                        onClick = { openInfoUrl = null },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("Close")
                                    }
                                }
                                AndroidView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    factory = { context ->
                                        WebView(context).apply {
                                            val cookieManager = CookieManager.getInstance()
                                            cookieManager.setAcceptCookie(true)
                                            cookieManager.setAcceptThirdPartyCookies(this, true)
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.databaseEnabled = true
                                            settings.loadsImagesAutomatically = true
                                            settings.useWideViewPort = true
                                            settings.loadWithOverviewMode = true
                                            settings.builtInZoomControls = true
                                            settings.displayZoomControls = false
                                            settings.javaScriptCanOpenWindowsAutomatically = true
                                            settings.setSupportMultipleWindows(true)
                                            settings.allowContentAccess = true
                                            settings.allowFileAccess = true
                                            settings.mediaPlaybackRequiresUserGesture = false
                                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                            setBackgroundColor(android.graphics.Color.WHITE)
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            webChromeClient = object : WebChromeClient() {
                                                override fun onCreateWindow(
                                                    view: WebView?,
                                                    isDialog: Boolean,
                                                    isUserGesture: Boolean,
                                                    resultMsg: Message?
                                                ): Boolean {
                                                    val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                                    transport.webView = this@apply
                                                    resultMsg.sendToTarget()
                                                    return true
                                                }
                                            }
                                            webViewClient = object : WebViewClient() {
                                                override fun shouldOverrideUrlLoading(
                                                    view: WebView?,
                                                    request: WebResourceRequest?
                                                ): Boolean {
                                                    val target = request?.url ?: return false
                                                    val scheme = target.scheme?.lowercase()
                                                    return if (scheme == "http" || scheme == "https") {
                                                        false
                                                    } else {
                                                        runCatching {
                                                            context.startActivity(Intent(Intent.ACTION_VIEW, target))
                                                        }
                                                        true
                                                    }
                                                }

                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    super.onPageFinished(view, url)
                                                    infoCanGoBack = view?.canGoBack() == true
                                                }
                                            }
                                            infoWebView = this
                                            loadUrl(openInfoUrl!!)
                                        }
                                    },
                                    update = { webView ->
                                        infoWebView = webView
                                        infoCanGoBack = webView.canGoBack()
                                        val target = openInfoUrl
                                        if (target != null && target != webView.url) {
                                            webView.loadUrl(target)
                                        }
                                    }
                                )
                            }
                        } else Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF071018), Color(0xFF0B1620), Color(0xFF091017))
                                    )
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .align(Alignment.TopCenter)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = (-36).dp, y = 18.dp)
                                        .size(210.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFF103448).copy(alpha = 0.34f))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 28.dp, y = 42.dp)
                                        .size(184.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFF1F5C52).copy(alpha = 0.22f))
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 18.dp, vertical = 20.dp)
                            ) {
                                Surface(
                                    color = Color(0xFF101A24).copy(alpha = 0.92f),
                                    shape = RoundedCornerShape(30.dp),
                                    tonalElevation = 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(30.dp))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 22.dp, vertical = 22.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "PHYZX",
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.headlineMedium,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = "PhysicsTracker",
                                                    color = Color.White.copy(alpha = 0.72f),
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            LandingLogo(
                                                modifier = Modifier
                                                    .size(88.dp)
                                                    .aspectRatio(1f)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            FilterChip(
                                                selected = homeTab == "experiments",
                                                onClick = { homeTab = "experiments" },
                                                label = { Text("Experiments") },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(52.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = Color.Black,
                                                    containerColor = Color(0xFF16222E),
                                                    labelColor = Color.White
                                                )
                                            )
                                            FilterChip(
                                                selected = homeTab == "info",
                                                onClick = { homeTab = "info" },
                                                label = { Text("More") },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(52.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                                    selectedLabelColor = Color.Black,
                                                    containerColor = Color(0xFF16222E),
                                                    labelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                if (homeTab == "experiments") {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        item {
                                            Surface(
                                                color = Color(0xFF101821).copy(alpha = 0.62f),
                                                shape = RoundedCornerShape(30.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(30.dp))
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 18.dp, vertical = 18.dp),
                                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "EXPERIMENTS",
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.titleLarge,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                        Surface(
                                                            color = Color(0xFFF2C66D).copy(alpha = 0.08f),
                                                            shape = RoundedCornerShape(999.dp)
                                                        ) {
                                                            Text(
                                                                text = projects.size.toString(),
                                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                                color = MaterialTheme.colorScheme.secondary,
                                                                style = MaterialTheme.typography.labelLarge,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }
                                                    }
                                                    FilledTonalButton(
                                                        onClick = {
                                                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                                        },
                                                        colors = ButtonDefaults.filledTonalButtonColors(
                                                            containerColor = Color(0xFF17394A).copy(alpha = 0.82f),
                                                            contentColor = Color(0xFFBDEFFF)
                                                        ),
                                                        shape = RoundedCornerShape(24.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(84.dp),
                                                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp)
                                                    ) {
                                                        Text(
                                                            "NEW EXPERIMENT",
                                                            fontFamily = FontFamily.Monospace,
                                                            style = MaterialTheme.typography.titleMedium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (projects.isEmpty()) {
                                            item {
                                                Surface(
                                                    color = Color(0xFF101821).copy(alpha = 0.78f),
                                                    shape = RoundedCornerShape(24.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Spacer(modifier = Modifier.height(96.dp))
                                                }
                                            }
                                        } else {
                                            items(projects) { project ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .combinedClickable(
                                                            onClick = {
                                                                viewModel.loadProject(project)
                                                                activeProjectName = project.name
                                                                selectedVideoUriString = project.videoUri
                                                                isProjectActive = true
                                                            },
                                                            onLongClick = {
                                                                selectedProjectAction = project
                                                                renameProjectDraft = project.name
                                                            }
                                                        ),
                                                    shape = RoundedCornerShape(24.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = Color(0xFF101821).copy(alpha = 0.92f)
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 18.dp, vertical = 16.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(52.dp)
                                                                .clip(RoundedCornerShape(18.dp))
                                                                .background(
                                                                    Brush.linearGradient(
                                                                        colors = listOf(Color(0xFF58D5F7).copy(alpha = 0.22f), Color(0xFF12C4A8).copy(alpha = 0.14f))
                                                                    )
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = project.name.take(1).uppercase(),
                                                                color = Color(0xFF8CE8FF),
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }
                                                        Column(
                                                            modifier = Modifier.weight(1f),
                                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                                        ) {
                                                            Text(
                                                                text = project.name,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = formatProjectTimestamp(project.updatedAt),
                                                                color = MaterialTheme.colorScheme.primary,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        homeLinks.forEach { link ->
                                            Surface(
                                                color = Color(0xFF101821).copy(alpha = 0.86f),
                                                shape = RoundedCornerShape(22.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .combinedClickable(
                                                        onClick = { openInfoUrl = link.url },
                                                        onLongClick = { openInfoUrl = link.url }
                                                    )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 18.dp, vertical = 16.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            link.title,
                                                            color = link.accent,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontFamily = FontFamily.Monospace,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            link.subtitle,
                                                            color = Color.White.copy(alpha = 0.88f),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontFamily = FontFamily.Monospace,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Surface(
                                                        color = link.accent.copy(alpha = 0.14f),
                                                        shape = RoundedCornerShape(999.dp)
                                                    ) {
                                                        Text(
                                                            "Open",
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                            color = link.accent,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Surface(
                                            color = Color(0xFF101821).copy(alpha = 0.78f),
                                            shape = RoundedCornerShape(22.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 18.dp, vertical = 16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("APP", color = Color(0xFF8CE8FF), style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace)
                                                Text("v$appVersion", color = Color.White, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showNameDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    showNameDialog = false
                                    selectedVideoUriString = null
                                    projectName = ""
                                },
                                title = { Text("Name this experiment") },
                                text = {
                                    TextField(
                                        value = projectName,
                                        onValueChange = { projectName = it },
                                        placeholder = { Text("Pendulum trial 01") }
                                    )
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        val uri = selectedVideoUriString
                                        val normalizedName = projectName.trim()
                                        if (normalizedName.isNotBlank() && uri != null) {
                                            activeProjectName = normalizedName
                                            isProjectActive = true
                                            showNameDialog = false
                                            projectName = normalizedName
                                        }
                                    }) { Text("Open Workspace") }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        showNameDialog = false
                                        selectedVideoUriString = null
                                        projectName = ""
                                    }) { Text("Cancel") }
                                }
                            )
                        }

                        selectedProjectAction?.let { project ->
                            AlertDialog(
                                onDismissRequest = { selectedProjectAction = null },
                                title = { Text(project.name) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        TextField(
                                            value = renameProjectDraft,
                                            onValueChange = { renameProjectDraft = it },
                                            placeholder = { Text("Experiment name") }
                                        )
                                        Text(
                                            "Long-press on a saved experiment to rename or delete it.",
                                            color = Color.White.copy(alpha = 0.68f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        viewModel.renameProject(project.id, renameProjectDraft)
                                        selectedProjectAction = null
                                    }) { Text("Rename") }
                                },
                                dismissButton = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = {
                                            showDeleteProjectConfirm = true
                                        }) { Text("Delete", color = Color.Red) }
                                        TextButton(onClick = { selectedProjectAction = null }) { Text("Close") }
                                    }
                                }
                            )
                        }

                        if (showDeleteProjectConfirm) {
                            val project = selectedProjectAction
                            if (project != null) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteProjectConfirm = false },
                                    title = { Text("Delete experiment?") },
                                    text = {
                                        Text("This will permanently delete '${project.name}' and all saved tracking data.")
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                viewModel.deleteProject(project)
                                                showDeleteProjectConfirm = false
                                                selectedProjectAction = null
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                        ) { Text("Delete", color = Color.White) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteProjectConfirm = false }) { Text("Cancel") }
                                    }
                                )
                            }
                        }
                    } else {
                        TrackingScreen(
                            videoUri = selectedVideoUriString ?: "",
                            projectName = activeProjectName ?: "",
                            onBack = {
                                isProjectActive = false
                                activeProjectName = null
                                selectedVideoUriString = null
                                projectName = ""
                            },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
