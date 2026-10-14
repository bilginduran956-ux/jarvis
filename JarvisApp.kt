package com.jarvis.android.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.android.core.model.AiMode
import com.jarvis.android.core.model.ConversationRole
import com.jarvis.android.core.model.DeviceMetric
import com.jarvis.android.core.model.DeviceSnapshot
import com.jarvis.android.core.model.JarvisPreferences
import com.jarvis.android.core.model.JarvisStatus
import com.jarvis.android.core.model.MetricLevel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class JarvisDestination(val label: String) {
    HOME("ANA"),
    COMMAND("KOMUT"),
    SYSTEM("SİSTEM"),
    TOOLS("ARAÇLAR"),
    SECURITY("GÜVENLİK"),
    MEMORY("HAFIZA"),
    SETTINGS("AYARLAR")
}

@Composable
fun JarvisApp(
    state: JarvisUiState,
    onSubmitCommand: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onRefreshSystem: () -> Unit,
    onSavePreferences: (JarvisPreferences) -> Unit,
    onSaveMemory: (String) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    var destination by rememberSaveable { mutableStateOf(JarvisDestination.HOME) }
    var booting by rememberSaveable { mutableStateOf(true) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    JarvisTheme {
        LaunchedEffect(Unit) {
            delay(1700)
            booting = false
        }
        if (booting) {
            BootScreen()
            return@JarvisTheme
        }
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = DeepNavy,
                    drawerContentColor = Ink,
                    modifier = Modifier.widthIn(max = 310.dp)
                ) {
                    DrawerContent(
                        selected = destination,
                        onSelect = {
                            destination = it
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                containerColor = Void,
                bottomBar = {
                    JarvisBottomBar(
                        destination = destination,
                        onSelect = { destination = it },
                        onVoiceClick = onVoiceClick
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Void)
                        .padding(padding)
                ) {
                    HudGrid()
                    Column(modifier = Modifier.fillMaxSize()) {
                        AppHeader(
                            status = state.status,
                            onMenu = { scope.launch { drawerState.open() } },
                            onRefresh = onRefreshSystem
                        )
                        when (destination) {
                            JarvisDestination.HOME -> HomeScreen(
                                state = state,
                                onSubmitCommand = onSubmitCommand,
                                onVoiceClick = onVoiceClick,
                                onOpen = { destination = it }
                            )
                            JarvisDestination.COMMAND -> CommandScreen(
                                state = state,
                                onSubmitCommand = onSubmitCommand,
                                onVoiceClick = onVoiceClick
                            )
                            JarvisDestination.SYSTEM -> SystemScreen(
                                snapshot = state.snapshot,
                                onRefresh = onRefreshSystem,
                                onSubmitCommand = onSubmitCommand
                            )
                            JarvisDestination.TOOLS -> ToolsScreen(
                                onSubmitCommand = onSubmitCommand,
                                onOpen = { destination = it }
                            )
                            JarvisDestination.SECURITY -> SecurityScreen(
                                onSubmitCommand = onSubmitCommand,
                                onRequestCameraPermission = onRequestCameraPermission,
                                onRequestNotificationPermission = onRequestNotificationPermission
                            )
                            JarvisDestination.MEMORY -> MemoryScreen(state, onSaveMemory)
                            JarvisDestination.SETTINGS -> SettingsScreen(state.preferences, onSavePreferences)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HudGrid() {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.18f)
    ) {
        val step = 28.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(Cyan.copy(alpha = 0.16f), start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 0.5.dp.toPx())
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(Cyan.copy(alpha = 0.16f), start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 0.5.dp.toPx())
            y += step
        }
    }
}

@Composable
private fun DrawerContent(selected: JarvisDestination, onSelect: (JarvisDestination) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = Cyan.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Cyan.copy(alpha = 0.45f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("J", color = Cyan, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
            }
            Spacer(Modifier.width(11.dp))
            Column {
                Text("JARVIS", fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("KOMUTA ÇEKİRDEĞİ", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
            }
        }
        Spacer(Modifier.height(28.dp))
        DrawerItem("Ana ekran", Icons.Default.Home, JarvisDestination.HOME, selected, onSelect)
        DrawerItem("Komut merkezi", Icons.Default.Mic, JarvisDestination.COMMAND, selected, onSelect)
        DrawerItem("Sistem", Icons.Default.Memory, JarvisDestination.SYSTEM, selected, onSelect)
        DrawerItem("Araçlar", Icons.Default.Build, JarvisDestination.TOOLS, selected, onSelect)
        DrawerItem("Güvenlik", Icons.Default.Security, JarvisDestination.SECURITY, selected, onSelect)
        DrawerItem("Hafıza", Icons.Default.Note, JarvisDestination.MEMORY, selected, onSelect)
        DrawerItem("Ayarlar", Icons.Default.Settings, JarvisDestination.SETTINGS, selected, onSelect)
        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = Cyan.copy(alpha = 0.14f))
        Spacer(Modifier.height(14.dp))
        Text(
            "Yerel çekirdek etkin\nDış erişimler onaylıdır",
            color = Muted,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    destination: JarvisDestination,
    selected: JarvisDestination,
    onSelect: (JarvisDestination) -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = selected == destination,
        onClick = { onSelect(destination) },
        modifier = Modifier.padding(vertical = 2.dp),
        colors = androidx.compose.material3.NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Cyan.copy(alpha = 0.13f),
            selectedIconColor = Cyan,
            selectedTextColor = Cyan,
            unselectedIconColor = Muted,
            unselectedTextColor = Ink
        )
    )
}

@Composable
private fun AppHeader(status: JarvisStatus, onMenu: () -> Unit, onRefresh: () -> Unit) {
    val date = remember(status) {
        SimpleDateFormat("EEE, dd MMMM · HH:mm", Locale("tr", "TR")).format(Date()).uppercase(Locale("tr", "TR"))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 14.dp, top = 10.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Default.Menu, contentDescription = "Menüyü aç", tint = Cyan)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("JARVIS", color = Ink, fontWeight = FontWeight.Black, letterSpacing = 4.sp, fontSize = 16.sp)
            Text(date, color = Muted, fontSize = 10.sp, letterSpacing = 0.6.sp)
        }
        StatusTag(status)
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Sistem verisini yenile", tint = Muted)
        }
    }
}

@Composable
private fun StatusTag(status: JarvisStatus) {
    val color = statusColor(status)
    Surface(
        color = color.copy(alpha = 0.11f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.42f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Spacer(Modifier.width(5.dp))
            Text(status.label, color = color, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 0.6.sp)
        }
    }
}

@Composable
private fun JarvisBottomBar(
    destination: JarvisDestination,
    onSelect: (JarvisDestination) -> Unit,
    onVoiceClick: () -> Unit
) {
    NavigationBar(containerColor = DeepNavy.copy(alpha = 0.96f), contentColor = Ink) {
        BottomItem("ANA", Icons.Default.Home, destination == JarvisDestination.HOME) { onSelect(JarvisDestination.HOME) }
        BottomItem("KOMUT", Icons.Default.Mic, destination == JarvisDestination.COMMAND) { onSelect(JarvisDestination.COMMAND) }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .size(52.dp)
                    .clickable(onClick = onVoiceClick),
                color = Cyan,
                shape = CircleShape,
                shadowElevation = 10.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Mic, contentDescription = "JARVIS'i dinlemeye başlat", tint = Void)
                }
            }
        }
        BottomItem("SİSTEM", Icons.Default.Memory, destination == JarvisDestination.SYSTEM) { onSelect(JarvisDestination.SYSTEM) }
        BottomItem("ARAÇLAR", Icons.Default.Build, destination == JarvisDestination.TOOLS) { onSelect(JarvisDestination.TOOLS) }
    }
}

@Composable
private fun RowScope.BottomItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = text) },
        label = { Text(text, fontSize = 8.sp, maxLines = 1) },
        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
            selectedIconColor = Cyan,
            selectedTextColor = Cyan,
            indicatorColor = Cyan.copy(alpha = 0.12f),
            unselectedIconColor = Muted,
            unselectedTextColor = Muted
        )
    )
}

@Composable
private fun BootScreen() {
    val modules = listOf("AI", "SES", "KOMUT", "HAFIZA", "SİSTEM", "DOSYA", "AĞ", "GÜVENLİK", "OTOMASYON")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void),
        contentAlignment = Alignment.Center
    ) {
        HudGrid()
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CoreOrb(JarvisStatus.PLANNING, animationsEnabled = true)
            Spacer(Modifier.height(12.dp))
            Text("JARVIS", fontWeight = FontWeight.Black, fontSize = 26.sp, letterSpacing = 6.sp, color = Cyan)
            Text("BAŞLATILIYOR…", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(24.dp))
            modules.forEach { module ->
                Row(
                    modifier = Modifier.width(220.dp).padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(module, color = Muted, letterSpacing = 1.3.sp, fontSize = 10.sp)
                    Text("✓", color = Mint, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(19.dp))
            Text("JARVIS CORE AKTİF", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        }
    }
}

@Composable
private fun HomeScreen(
    state: JarvisUiState,
    onSubmitCommand: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onOpen: (JarvisDestination) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            Text("Merhaba, " + state.preferences.operatorName + ".", fontSize = 25.sp, fontWeight = FontWeight.Medium)
            Text("Telefonunuzu tek merkezden yönetmeye hazırım.", color = Muted, fontSize = 13.sp)
        }
        item {
            CorePanel(
                status = state.status,
                detail = state.statusDetail,
                animationsEnabled = state.preferences.animationsEnabled,
                onVoiceClick = onVoiceClick
            )
        }
        item {
            CommandComposer(
                enabled = !state.isProcessing,
                onSubmit = onSubmitCommand,
                onVoiceClick = onVoiceClick
            )
        }
        item {
            SectionTitle("HIZLI KOMUTLAR", "Tüm araçlar")
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { QuickCommand("YouTube", Icons.Default.Apps) { onSubmitCommand("YouTube'u aç") } }
                item { QuickCommand("Kamera", Icons.Default.Search) { onSubmitCommand("Kamerayı aç") } }
                item { QuickCommand("Fener", Icons.Default.BatteryFull) { onSubmitCommand("Feneri aç") } }
                item { QuickCommand("Müzik", Icons.Default.Mic) { onSubmitCommand("Müziği başlat") } }
                item { QuickCommand("Dosyalar", Icons.Default.Folder) { onSubmitCommand("Dosyaları aç") } }
                item { QuickCommand("Ayarlar", Icons.Default.Settings) { onSubmitCommand("Ayarları aç") } }
            }
        }
        item { SectionTitle("SİSTEM DURUMU", "Canlı yerel veri") }
        item { SystemMetricsGrid(state.snapshot) }
        item { NetworkPanel(state.snapshot) }
        item {
            JarvisPanel {
                Text("SON İŞLEM", color = Muted, fontSize = 10.sp, letterSpacing = 1.1.sp)
                Spacer(Modifier.height(5.dp))
                Text(state.lastPlanSummary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                state.lastError?.let {
                    Spacer(Modifier.height(5.dp))
                    Text(it, color = Amber, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onOpen(JarvisDestination.COMMAND) }, contentPadding = PaddingValues(0.dp)) {
                    Text("Komut geçmişini aç")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
        item { Spacer(Modifier.height(7.dp)) }
    }
}

@Composable
private fun CorePanel(
    status: JarvisStatus,
    detail: String,
    animationsEnabled: Boolean,
    onVoiceClick: () -> Unit
) {
    JarvisPanel(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("JARVIS CORE", color = Cyan, fontWeight = FontWeight.Bold, letterSpacing = 2.6.sp, fontSize = 11.sp)
            CoreOrb(status = status, animationsEnabled = animationsEnabled, modifier = Modifier.padding(vertical = 8.dp))
            StatusTag(status)
            Spacer(Modifier.height(10.dp))
            Text(detail, color = Ink, textAlign = TextAlign.Center, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(12.dp))
            AssistChip(
                onClick = onVoiceClick,
                label = { Text("JARVIS SES · DİNLEMEYE BAŞLA") },
                leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(17.dp)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Cyan.copy(alpha = 0.12f),
                    labelColor = Cyan,
                    leadingIconContentColor = Cyan
                ),
                border = BorderStroke(1.dp, Cyan.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
private fun CoreOrb(status: JarvisStatus, animationsEnabled: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "jarvis-core")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animationsEnabled) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (status == JarvisStatus.LISTENING) 1800 else 6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "core-rotation"
    )
    val breath by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = if (animationsEnabled) 1.08f else 0.88f,
        animationSpec = InfiniteRepeatableSpec(tween(1300), RepeatMode.Reverse),
        label = "core-breath"
    )
    val color = statusColor(status)
    Box(
        modifier = modifier.size(184.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val min = size.minDimension
            drawCircle(color.copy(alpha = 0.04f), radius = min * 0.47f, center = center)
            drawCircle(color.copy(alpha = 0.68f), radius = min * 0.43f, center = center, style = Stroke(1.5.dp.toPx()))
            drawCircle(Cyan.copy(alpha = 0.35f), radius = min * 0.34f, center = center, style = Stroke(1.dp.toPx()))
            drawArc(
                color = Turquoise,
                startAngle = 12f,
                sweepAngle = 96f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(min * .10f, min * .10f),
                size = androidx.compose.ui.geometry.Size(min * .80f, min * .80f),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = Mint,
                startAngle = 207f,
                sweepAngle = 47f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(min * .17f, min * .17f),
                size = androidx.compose.ui.geometry.Size(min * .66f, min * .66f),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Surface(
            modifier = Modifier.size((71 * breath).dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.72f)),
            shadowElevation = 10.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("J", color = color, fontSize = 31.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun CommandScreen(
    state: JarvisUiState,
    onSubmitCommand: (String) -> Unit,
    onVoiceClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text("KOMUT MERKEZİ", fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
        Text("Sesli ve yazılı komut geçmişiniz", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(11.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 10.dp)
        ) {
            if (state.conversation.isEmpty()) {
                item {
                    EmptyState("Henüz komut yok", "Bir komut yazarak veya mikrofonla konuşarak başlayın.")
                }
            } else {
                items(state.conversation.takeLast(40), key = { it.id }) { entry ->
                    ConversationBubble(entry.role, entry.text)
                }
            }
        }
        CommandComposer(
            enabled = !state.isProcessing,
            onSubmit = onSubmitCommand,
            onVoiceClick = onVoiceClick
        )
        Spacer(Modifier.height(11.dp))
    }
}

@Composable
private fun ConversationBubble(role: ConversationRole, value: String) {
    val isUser = role == ConversationRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 15.dp,
                topEnd = 15.dp,
                bottomStart = if (isUser) 15.dp else 3.dp,
                bottomEnd = if (isUser) 3.dp else 15.dp
            ),
            color = if (isUser) Turquoise.copy(alpha = 0.17f) else Panel,
            border = BorderStroke(1.dp, if (isUser) Turquoise.copy(alpha = 0.28f) else Cyan.copy(alpha = 0.16f)),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(if (isUser) "SİZ" else "JARVIS", color = if (isUser) Turquoise else Cyan, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(value, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun CommandComposer(
    enabled: Boolean,
    onSubmit: (String) -> Unit,
    onVoiceClick: () -> Unit
) {
    var value by rememberSaveable { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.weight(1f),
            enabled = enabled,
            singleLine = true,
            placeholder = { Text("JARVIS, yap…", color = Muted) },
            shape = RoundedCornerShape(15.dp),
            trailingIcon = {
                IconButton(onClick = {
                    if (value.isNotBlank()) {
                        onSubmit(value)
                        value = ""
                    }
                }, enabled = enabled && value.isNotBlank()) {
                    Icon(Icons.Default.Send, contentDescription = "Komutu işle", tint = Cyan)
                }
            }
        )
        Spacer(Modifier.width(7.dp))
        Surface(
            shape = CircleShape,
            color = Cyan,
            modifier = Modifier
                .size(52.dp)
                .clickable(enabled = enabled, onClick = onVoiceClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Mic, contentDescription = "Sesli komut ver", tint = Void)
            }
        }
    }
}

@Composable
private fun SystemScreen(
    snapshot: DeviceSnapshot?,
    onRefresh: () -> Unit,
    onSubmitCommand: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SİSTEM ANALİZİ", fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
                    Text("Resmî Android API'lerinden alınan anlık durum", color = Muted, fontSize = 11.sp)
                }
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Yenile")
                }
            }
        }
        item { SystemMetricsGrid(snapshot) }
        item { NetworkPanel(snapshot) }
        item {
            JarvisPanel {
                Text("SİSTEM RAPORU", color = Cyan, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, fontSize = 11.sp)
                Spacer(Modifier.height(7.dp))
                Text(
                    "CPU ve GPU için genel Android API'si canlı yüzde verisi sağlamaz. JARVIS, erişilmeyen veriyi tahmin etmek yerine bunu açıkça gösterir.",
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onSubmitCommand("Telefonumu kontrol et") }, contentPadding = PaddingValues(0.dp)) {
                    Text("Tam raporu JARVIS ile yenile")
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 5.dp).size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SystemMetricsGrid(snapshot: DeviceSnapshot?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("CPU", snapshot?.cpu, Modifier.weight(1f))
            MetricCard("RAM", snapshot?.ram, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("DEPOLAMA", snapshot?.storage, Modifier.weight(1f))
            MetricCard("PİL", snapshot?.battery, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("SICAKLIK", snapshot?.temperature, Modifier.weight(1f))
            MetricCard("GPU", snapshot?.gpu, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(label: String, metric: DeviceMetric?, modifier: Modifier = Modifier) {
    val color = metric?.let { metricColor(it.level) } ?: Muted
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = 0.93f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(7.dp))
            Text(metric?.value ?: "Yükleniyor", color = color, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(metric?.detail ?: "Sistem verisi alınıyor", color = Muted, fontSize = 10.sp, lineHeight = 14.sp, minLines = 2)
        }
    }
}

@Composable
private fun NetworkPanel(snapshot: DeviceSnapshot?) {
    JarvisPanel {
        Text("AĞ DURUMU", color = Cyan, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        val network = snapshot?.network
        NetworkRow("Wi‑Fi", network?.wifi ?: "YÜKLENİYOR", Icons.Default.Wifi)
        NetworkRow("Mobil veri", network?.mobile ?: "YÜKLENİYOR", Icons.Default.Search)
        NetworkRow("Bluetooth", network?.bluetooth ?: "YÜKLENİYOR", Icons.Default.Bluetooth)
        NetworkRow("NFC", network?.nfc ?: "YÜKLENİYOR", Icons.Default.Apps)
        NetworkRow("VPN", network?.vpn ?: "YÜKLENİYOR", Icons.Default.Security)
    }
}

@Composable
private fun NetworkRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Muted, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(9.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text(
            value,
            color = if (value in setOf("BAĞLI", "AKTİF", "AÇIK")) Mint else Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun ToolsScreen(onSubmitCommand: (String) -> Unit, onOpen: (JarvisDestination) -> Unit) {
    val tools = listOf(
        Triple("Uygulama Merkezi", "Yüklü uygulamaları açın veya arayın", "YouTube'u aç"),
        Triple("Dosya Merkezi", "Dosya seçici ve depolama araçları", "Dosyaları aç"),
        Triple("Kamera Merkezi", "Resmî kamera uygulamasını başlatın", "Kamerayı aç"),
        Triple("Medya Merkezi", "Müzik ve medya ses kontrolü", "Müziği başlat"),
        Triple("Ağ Merkezi", "Wi‑Fi, Bluetooth ve ağ sistem panelleri", "Wi-Fi'yi aç ve Bluetooth'u aç"),
        Triple("Alarm Merkezi", "Alarm uygulamasında yeni alarm hazırlayın", "Alarm kur: 07:00")
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("ARAÇ MERKEZİ", fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
            Text("Telefonun resmî araçlarına güvenli JARVIS köprüleri", color = Muted, fontSize = 12.sp)
        }
        items(tools) { tool ->
            ToolCard(tool.first, tool.second, Icons.Default.Build) { onSubmitCommand(tool.third) }
        }
        item {
            ToolCard("Güvenlik Merkezi", "İzinleri ve güvenli erişim sınırlarını inceleyin", Icons.Default.Security) {
                onOpen(JarvisDestination.SECURITY)
            }
        }
        item {
            ToolCard("Hafıza Merkezi", "Yerel notları ve kalıcı kayıtları yönetin", Icons.Default.Note) {
                onOpen(JarvisDestination.MEMORY)
            }
        }
        item {
            ToolCard("Ayarlar", "Ses, AI modu ve görsel tercihleri değiştirin", Icons.Default.Settings) {
                onOpen(JarvisDestination.SETTINGS)
            }
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    JarvisPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = Cyan.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Cyan.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Cyan)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(description, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SecurityScreen(
    onSubmitCommand: (String) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("GÜVENLİK MERKEZİ", fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
            Text("JARVIS yalnızca Android'in onaylı erişim sınırları içinde çalışır.", color = Muted, fontSize = 12.sp)
        }
        item {
            JarvisPanel {
                Text("ERİŞİM FELSEFESİ", color = Cyan, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, fontSize = 10.sp)
                Spacer(Modifier.height(7.dp))
                Text(
                    "Mikrofon, kamera, bildirim, Bluetooth ve dosya erişimleri uygulama veya sistem düzeyinde sizden izin ister. " +
                        "JARVIS bu izinleri gizlice açmaz; Android'in resmî paneline yönlendirir.",
                    color = Ink,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
        item { PermissionRow("Mikrofon", "Sesli komut başlatıldığında istenir", Icons.Default.Mic) }
        item { PermissionRow("Kamera / fener", "Feneri kullanmak için Kamera izni gerekir", Icons.Default.Search) }
        item { PermissionRow("Bildirim erişimi", "Bildirim özeti için sistem tarafından etkinleştirilir", Icons.Default.Notifications) }
        item { PermissionRow("Dosyalar", "Android dosya seçicisi üzerinden seçtiğiniz dosyalar", Icons.Default.Folder) }
        item {
            ToolCard("Kamera / fener izni", "Android Kamera izni iletişim kutusunu aç", Icons.Default.Search) {
                onRequestCameraPermission()
            }
        }
        item {
            ToolCard("Bildirim izni", "Android bildirim izni iletişim kutusunu aç", Icons.Default.Notifications) {
                onRequestNotificationPermission()
            }
        }
        item {
            ToolCard("Bildirim erişim paneli", "Android'in resmî bildirim erişim ekranını aç", Icons.Default.Notifications) {
                onSubmitCommand("Bildirim erişimi")
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(description, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MemoryScreen(state: JarvisUiState, onSaveMemory: (String) -> Unit) {
    var note by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("JARVIS HAFIZASI", fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
            Text("Kullanıcının açıkça kaydettiği yerel bilgiler", color = Muted, fontSize = 12.sp)
        }
        item {
            JarvisPanel {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Hatırlanacak bilgi") },
                    placeholder = { Text("Örn. Doktor randevum her ayın ilk pazartesisi") },
                    minLines = 2
                )
                Spacer(Modifier.height(7.dp))
                TextButton(
                    onClick = {
                        onSaveMemory(note)
                        note = ""
                    },
                    enabled = note.isNotBlank()
                ) {
                    Text("HAFIZAYA KAYDET")
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 5.dp).size(16.dp))
                }
            }
        }
        if (state.notes.isEmpty()) {
            item { EmptyState("Hafıza boş", "“Bunu hatırla: …” komutuyla veya yukarıdaki alandan bir kayıt ekleyin.") }
        } else {
            itemsIndexed(state.notes.reversed(), key = { _, noteItem -> noteItem.id }) { _, item ->
                JarvisPanel {
                    Text(item.value, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Yerel hafıza kaydı", color = Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    preferences: JarvisPreferences,
    onSave: (JarvisPreferences) -> Unit
) {
    var operatorName by rememberSaveable(preferences.operatorName) { mutableStateOf(preferences.operatorName) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("JARVIS AYARLARI", fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
            Text("Tercihler cihazdaki JARVIS alanında saklanır.", color = Muted, fontSize = 12.sp)
        }
        item {
            JarvisPanel {
                Text("OPERATÖR PROFİLİ", color = Cyan, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, fontSize = 10.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = operatorName,
                    onValueChange = { operatorName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("JARVIS size nasıl hitap etsin?") },
                    singleLine = true
                )
                TextButton(
                    onClick = {
                        onSave(preferences.copy(operatorName = operatorName.ifBlank { "Komutan" }))
                    }
                ) { Text("PROFİLİ KAYDET") }
            }
        }
        item {
            JarvisPanel {
                SettingSwitch(
                    title = "Sesli JARVIS yanıtları",
                    detail = "Komut sonuçlarını Türkçe seslendirsin",
                    checked = preferences.speechEnabled
                ) { onSave(preferences.copy(speechEnabled = it)) }
                HorizontalDivider(modifier = Modifier.padding(vertical = 11.dp), color = Cyan.copy(alpha = 0.12f))
                SettingSwitch(
                    title = "HUD animasyonları",
                    detail = "JARVIS CORE halkaları ve görsel hareket",
                    checked = preferences.animationsEnabled
                ) { onSave(preferences.copy(animationsEnabled = it)) }
            }
        }
        item {
            JarvisPanel {
                Text("AI MODU", color = Cyan, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, fontSize = 10.sp)
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    AiMode.entries.forEach { mode ->
                        FilterChip(
                            selected = preferences.selectedAiMode == mode,
                            onClick = { onSave(preferences.copy(selectedAiMode = mode)) },
                            label = { Text(mode.label, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    "Bu sürümde yerel komut motoru aktiftir. Çevrimiçi AI seçimi için güvenli sağlayıcı ve kullanıcı anahtarı entegrasyonu ayrı kurulum adımıdır.",
                    color = Muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
        item {
            JarvisPanel {
                Text("UYANDIRMA KELİMESİ", color = Cyan, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, fontSize = 10.sp)
                Spacer(Modifier.height(7.dp))
                Text(
                    "“JARVIS” sürekli uyandırma dinlemesi varsayılan olarak kapalıdır. Android'de bunu güvenli ve pil dostu biçimde sunmak için foreground service, kalıcı bildirim ve cihaz üstü keyword modeli gerekir.",
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, detail: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(detail, color = Muted, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun QuickCommand(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = PanelSoft.copy(alpha = 0.85f),
            labelColor = Ink,
            leadingIconContentColor = Cyan
        ),
        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.16f))
    )
}

@Composable
private fun SectionTitle(title: String, trailing: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, color = Cyan, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(trailing, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun EmptyState(title: String, detail: String) {
    JarvisPanel {
        Text(title, fontWeight = FontWeight.SemiBold, color = Cyan)
        Spacer(Modifier.height(4.dp))
        Text(detail, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun JarvisPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Panel.copy(alpha = 0.93f),
        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.17f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(15.dp), content = content)
    }
}

private fun statusColor(status: JarvisStatus): Color = when (status) {
    JarvisStatus.READY, JarvisStatus.COMPLETE -> Mint
    JarvisStatus.LISTENING -> Cyan
    JarvisStatus.UNDERSTANDING, JarvisStatus.PLANNING, JarvisStatus.WORKING, JarvisStatus.VERIFYING -> Turquoise
    JarvisStatus.WARNING -> Amber
    JarvisStatus.ERROR -> Rose
}

private fun metricColor(level: MetricLevel): Color = when (level) {
    MetricLevel.NORMAL -> Mint
    MetricLevel.WARNING -> Amber
    MetricLevel.CRITICAL -> Rose
    MetricLevel.UNKNOWN -> Muted
}
