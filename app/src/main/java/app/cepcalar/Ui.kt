@file:OptIn(androidx.media3.common.util.UnstableApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.cepcalar

import android.Manifest
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import android.content.Intent
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import android.speech.RecognizerIntent
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs


fun tintFor(s: String, sat: Float = 0.35f, v: Float = 0.30f): Color =
    Color.hsv((abs(s.hashCode()) % 360).toFloat(), sat, v)

fun speedLabel(v: Float): String =
    (if (v == v.toInt().toFloat()) v.toInt().toString() else "%.2f".format(v).trimEnd('0').replace('.', ',')) + "x"

fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

object ThumbCache {
    val cache = LruCache<String, ImageBitmap>(80)
}

@Composable
fun Art(song: Song?, modifier: Modifier = Modifier, corner: Dp = 6.dp, letter: Boolean = true) {
    val ctx = LocalContext.current
    val title = song?.title ?: ""
    Box(
        modifier.clip(RoundedCornerShape(corner)).background(tintFor(title, 0.38f, 0.28f)),
        contentAlignment = Alignment.Center
    ) {
        if (letter) {
            Text(
                (title.trim().firstOrNull()?.uppercase() ?: "♪"),
                color = TXT.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 22.sp
            )
        }
        if (song != null) {
            if (song.artUrl != null) {
                AsyncImage(
                    model = song.artUrl, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
            } else if (!song.remote) {
                val bmp by produceState<ImageBitmap?>(null, song.mediaId) {
                    // Şarkı değişince eski kapak kalmasın: önce önbelleğe (yoksa boşa) sıfırla, sonra yükle.
                    val cached = ThumbCache.cache.get(song.mediaId)
                    value = cached
                    if (cached == null) {
                        value = withContext(Dispatchers.IO) {
                            runCatching {
                                ctx.contentResolver.loadThumbnail(song.uri, Size(320, 320), null).asImageBitmap()
                            }.getOrNull()
                        }?.also { ThumbCache.cache.put(song.mediaId, it) }
                    }
                }
                bmp?.let {
                    Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun CepApp(vm: MusicVM) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var full by rememberSaveable { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf(false) }
    val systemDark = isSystemInDarkTheme()
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            nowMs = System.currentTimeMillis()
        }
    }
    val dark = when (vm.themeMode) {
        THEME_LIGHT -> false
        THEME_DARK -> true
        THEME_SUN -> vm.isNightBySun(nowMs)
        else -> systemDark
    }
    val pal = palFor(dark, vm.accent)
    val perms = remember {
        if (Build.VERSION.SDK_INT >= 33)
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.scan()
    }
    val view = LocalView.current
    SideEffect {
        view.keepScreenOn = vm.voiceOn
        // Durum/gezinme çubuğu simgeleri: açık temada koyu, koyu temada açık (oynatıcı hep koyu)
        val act = view.context as? Activity
        if (act != null) {
            val ctl = WindowCompat.getInsetsController(act.window, view)
            val lightBars = !dark && !full
            ctl.isAppearanceLightStatusBars = lightBars
            ctl.isAppearanceLightNavigationBars = lightBars
        }
    }
    LaunchedEffect(Unit) { if (!vm.checkPermission()) launcher.launch(perms) }
    LaunchedEffect(Unit) {
        while (true) {
            vm.tick()
            delay(400)
        }
    }
    LaunchedEffect(vm.openFull) {
        if (vm.openFull) {
            full = true
            vm.openFull = false
        }
    }
    LaunchedEffect(vm.goTab) {
        vm.goTab?.let { tab = it; full = false; settings = false; vm.goTab = null }
    }
    // Sekme değişince açık detay sayfası kapanır; kitaplık sekmesine yeniden dokununca da liste başa döner.
    val go: (Int) -> Unit = { t ->
        if (t != 3 || tab == 3) vm.browse = null
        tab = t
    }
    BackHandler(enabled = vm.browse != null && tab == 3 && !full && !settings) { vm.browse = null }
    BackHandler(enabled = full && !settings) { full = false }
    BackHandler(enabled = settings) { settings = false }

    CompositionLocalProvider(LocalPal provides pal) {
        MaterialTheme(
            colorScheme = if (dark) darkColorScheme(primary = pal.accent, background = pal.bg, surface = pal.card)
            else lightColorScheme(primary = pal.accent, background = pal.bg, surface = pal.card)
        ) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to pal.bg2, 0.4f to pal.bg, 1f to pal.bg))) {
                Column(Modifier.fillMaxSize().imePadding()) {
                    Box(Modifier.weight(1f).then(if (tab == 0) Modifier else Modifier.statusBarsPadding())) {
                        when (tab) {
                            0 -> Home(
                                vm, onGo = go, onSettings = { settings = true },
                                onOpenPlayer = { full = true }
                            ) { launcher.launch(perms) }
                            1 -> SearchTab(vm)
                            2 -> AssistantTab(vm) { settings = true }
                            3 -> LibraryTab(vm)
                            else -> LikedTab(vm)
                        }
                    }
                    if (vm.current != null && tab != 0) MiniPlayer(vm) { full = true }
                    NavBar(tab) { go(it) }
                }
                AnimatedVisibility(
                    visible = full,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) { FullPlayer(vm) { full = false } }
                AnimatedVisibility(
                    visible = settings,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) { SettingsScreen(vm) { settings = false } }
                vm.badgeBanner?.let { b ->
                    LaunchedEffect(b) {
                        delay(4500)
                        if (vm.badgeBanner == b) vm.badgeBanner = null
                    }
                    Row(
                        Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp, start = 16.dp, end = 16.dp)
                            .clip(RoundedCornerShape(24.dp)).background(ACC).padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = INK)
                        Text(b, color = INK, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

// ---------- Ana sayfa ----------

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(if (selected) ACC else CHIP)
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(text, color = if (selected) INK else TXT, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

@Composable
fun Hint(t: String, modifier: Modifier = Modifier) {
    Text(t, color = MUTED, fontSize = 14.sp, modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp))
}

@Composable
fun rememberVoiceToggle(vm: MusicVM): () -> Unit {
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.setVoiceOn(true) else vm.voiceNote = "Mikrofon izni verilmedi."
    }
    return {
        if (vm.voiceOn) vm.setVoiceOn(false)
        else if (vm.hasMic()) vm.setVoiceOn(true)
        else perm.launch(Manifest.permission.RECORD_AUDIO)
    }
}

@Composable
fun RoundBtn(on: Boolean, desc: String, icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).border(1.dp, EDGE, CircleShape)
            .background(if (on) ACC else CARD).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = if (on) INK else ACC) }
}

@Composable
fun PlaylistCard(title: String, sub: String, cover: Song?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.width(150.dp).height(190.dp).clip(shape).border(1.dp, EDGE, shape).clickable(onClick = onClick)
    ) {
        Art(cover, Modifier.fillMaxSize(), 0.dp, letter = false)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE0B0907)), startY = 160f)
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text("• $sub", color = IMG_MUTED, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
fun StationCard(a: Artist, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(Modifier.width(150.dp).height(190.dp).clip(shape).border(1.dp, EDGE, shape).background(CARD).clickable(onClick = onClick)) {
        AsyncImage(
            model = a.pic, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE0B0907)), startY = 160f)
            )
        )
        Text(
            "RADYO", color = PLAYER_ACC, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
        )
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(a.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text("• ve benzerleri", color = IMG_MUTED, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
fun SongRow(s: Song, vm: MusicVM, sub: String? = null, rank: Int? = null, onClick: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val now = vm.current?.mediaId == s.mediaId
    val rp = if (sub == null && s.isLong) vm.resume[s.statKey]?.first else null
    val baseSub = sub ?: s.artist.ifEmpty { "Sanatçı bilinmiyor" }
    val shownSub = if (rp != null && rp > 5000L) "${fmt(rp)}'den devam · $baseSub" else baseSub
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) {
            Text(
                "$rank", color = if (rank == 1) ACC else MUTED, fontWeight = FontWeight.Bold,
                modifier = Modifier.width(30.dp), fontSize = 16.sp
            )
        }
        Art(s, Modifier.size(56.dp))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                s.title, color = if (now) ACC else TXT, fontSize = 17.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (s.isVideo) {
                    Text(
                        "KLİP", color = MUTED, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 6.dp).border(1.dp, MUTED, RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp)
                    )
                }
                Text(
                    shownSub, color = MUTED, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (s.remote) {
            val liked = vm.isLiked(s)
            IconButton(onClick = { vm.toggleLike(s) }) {
                Icon(
                    if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    if (liked) "Beğenmekten vazgeç" else "Beğen",
                    tint = if (liked) ACC else TXT
                )
            }
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Daha fazla", tint = TXT)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Sıradakine ekle") }, onClick = {
                    vm.addNext(s)
                    menu = false
                })
                if (!s.remote) {
                    if (s.artist.isNotBlank()) {
                        DropdownMenuItem(text = { Text("Sanatçı sayfası") }, onClick = {
                            vm.openBrowse(0, s.artist)
                            menu = false
                        })
                    }
                    if (s.album.isNotBlank()) {
                        DropdownMenuItem(text = { Text("Albüm sayfası") }, onClick = {
                            vm.openBrowse(1, s.album)
                            menu = false
                        })
                    }
                    DropdownMenuItem(text = { Text("Benzer parçaları bul") }, onClick = {
                        vm.openBrowse(3, s.mediaId)
                        menu = false
                    })
                }
                if (s.remote) {
                    val q = android.net.Uri.encode("${s.artist} ${s.title}")
                    DropdownMenuItem(text = { Text("YouTube Music'te ara") }, onClick = {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://music.youtube.com/search?q=$q"))) }
                        menu = false
                    })
                    DropdownMenuItem(text = { Text("Spotify'da ara") }, onClick = {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://open.spotify.com/search/$q"))) }
                        menu = false
                    })
                }
                if (!s.link.isNullOrBlank()) {
                    DropdownMenuItem(text = { Text("Deezer'da aç") }, onClick = {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(s.link))) }
                        menu = false
                    })
                }
            }
        }
    }
}

// ---------- Ara / Kitaplık / En çok ----------

@Composable
fun Header(t: String, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(t, color = TXT, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun SearchTab(vm: MusicVM) {
    var q by rememberSaveable { mutableStateOf("") }
    val res = remember(q, vm.songs) {
        if (q.isBlank()) emptyList()
        else vm.songs.filter { it.title.contains(q, true) || it.artist.contains(q, true) }
    }
    Column(Modifier.fillMaxSize()) {
        Header("Ara")
        TextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            placeholder = { Text("Ne dinlemek istiyorsun?") },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = MUTED) },
            shape = RoundedCornerShape(8.dp),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (q.isNotBlank() && res.isEmpty()) Hint("Sonuç yok.")
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(res) { s -> SongRow(s, vm) { vm.play(res, res.indexOf(s)) } }
        }
    }
}

@Composable
fun LibraryTab(vm: MusicVM) {
    val b = vm.browse
    if (b != null) {
        BrowseScreen(vm, b)
        return
    }
    val shown = remember(vm.songs, vm.recoTracks) { vm.mixed(vm.songs, 5) }
    Column(Modifier.fillMaxSize()) {
        Header("Kitaplığım") {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(ACC).clickable { vm.shuffleAll(vm.songs) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Shuffle, "Karışık çal", tint = INK) }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Chip("Tüm parçalar", vm.libMode == 0) { vm.libMode = 0 }
            Chip("En çok dinlenen", vm.libMode == 1) { vm.libMode = 1 }
            Chip("Sanatçılar", vm.libMode == 2) { vm.libMode = 2 }
            Chip("Albümler", vm.libMode == 3) { vm.libMode = 3 }
            Chip("Klasörler", vm.libMode == 4) { vm.libMode = 4 }
        }
        when (vm.libMode) {
            1 -> TopContent(vm)
            2 -> ArtistList(vm)
            3 -> AlbumList(vm)
            4 -> FolderList(vm)
            else -> {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${vm.songs.size} parça · Tüm videoları göster", color = MUTED, fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = vm.allVideos, onCheckedChange = { vm.setShowAllVideos(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = ACC, checkedThumbColor = INK)
                    )
                }
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(shown) { s ->
                        val c = vm.count(s)
                        SongRow(
                            s, vm,
                            sub = (if (c > 0) "$c kez dinlendi · " else "") + s.artist.ifEmpty { "Sanatçı bilinmiyor" }
                        ) { vm.play(shown, shown.indexOf(s)) }
                    }
                }
            }
        }
    }
}

// ---------- Sanatçı / albüm / klasör gezinmesi ----------

@Composable
fun Avatar(seed: String, round: Boolean, icon: ImageVector? = null) {
    Box(
        Modifier.size(52.dp).clip(if (round) CircleShape else RoundedCornerShape(10.dp))
            .background(tintFor(seed, 0.38f, 0.28f)),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) Icon(icon, null, tint = TXT.copy(alpha = 0.85f))
        else Text(
            seed.trim().firstOrNull()?.uppercase() ?: "♪", color = TXT.copy(alpha = 0.85f),
            fontWeight = FontWeight.Bold, fontSize = 20.sp
        )
    }
}

@Composable
fun ListRow(title: String, sub: String, onClick: () -> Unit, lead: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        lead()
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, color = TXT, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = MUTED, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ArtistList(vm: MusicVM) {
    val rows = remember(vm.songs, vm.stats) {
        vm.songs.filter { it.artist.isNotBlank() }.groupBy { it.artist.lowercase() }.values.map { l ->
            Triple(l.first().artist, l.size, l.sumOf { vm.count(it) })
        }.sortedBy { it.first.lowercase() }
    }
    if (rows.isEmpty()) Hint("Sanatçı bilgisi olan parça bulunamadı.")
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(rows, key = { it.first.lowercase() }) { (name, n, plays) ->
            ListRow(
                name, "$n parça" + if (plays > 0) " · $plays kez dinlendi" else "",
                { vm.browse = Browse(0, name) }
            ) { Avatar(name, true) }
        }
    }
}

@Composable
fun AlbumList(vm: MusicVM) {
    class Row3(val album: String, val artist: String, val count: Int, val cover: Song)
    val rows = remember(vm.songs) {
        vm.songs.filter { it.album.isNotBlank() }.groupBy { it.album }.map { (a, l) ->
            val art = l.map { it.artist }.filter { it.isNotBlank() }.groupingBy { it }.eachCount()
                .maxByOrNull { it.value }?.key ?: ""
            Row3(a, art, l.size, l.first())
        }.sortedBy { it.album.lowercase() }
    }
    if (rows.isEmpty()) Hint("Albüm bilgisi olan parça bulunamadı.")
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(rows, key = { it.album }) { r ->
            ListRow(
                r.album, (if (r.artist.isNotEmpty()) "${r.artist} · " else "") + "${r.count} parça",
                { vm.browse = Browse(1, r.album) }
            ) { Art(r.cover, Modifier.size(52.dp), 10.dp, letter = false) }
        }
    }
}

@Composable
fun FolderList(vm: MusicVM) {
    val rows = remember(vm.songs) {
        vm.songs.filter { it.folder.isNotBlank() }.groupBy { it.folder }.map { (f, l) -> Triple(f, l.size, l.count { it.isVideo }) }
            .sortedBy { it.first.lowercase() }
    }
    if (rows.isEmpty()) Hint("Klasör bilgisi bulunamadı.")
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(rows, key = { it.first }) { (path, n, v) ->
            ListRow(
                path.substringAfterLast('/'),
                "$path · $n parça" + if (v > 0) " ($v klip)" else "",
                { vm.browse = Browse(2, path) }
            ) { Avatar(path, false, Icons.Filled.Folder) }
        }
    }
}

@Composable
fun BrowseScreen(vm: MusicVM, b: Browse) {
    val list = remember(b, vm.songs) { vm.tracksOf(b) }
    val title = when (b.kind) {
        0, 1 -> b.key
        2 -> b.key.substringAfterLast('/')
        else -> "Benzer parçalar"
    }
    val plays = list.sumOf { vm.count(it) }
    val albums = if (b.kind == 0) list.map { it.album }.filter { it.isNotBlank() }.distinct() else emptyList()
    val subtitle = when (b.kind) {
        0 -> "${list.size} parça" + (if (albums.isNotEmpty()) " · ${albums.size} albüm" else "") +
            (if (plays > 0) " · $plays kez dinlendi" else "")
        1 -> (list.map { it.artist }.firstOrNull { it.isNotBlank() }?.let { "$it · " } ?: "") + "${list.size} parça"
        2 -> "${b.key} · ${list.size} parça"
        else -> vm.songs.firstOrNull { it.mediaId == b.key }?.let { "${it.title} parçasına benzeyenler" } ?: ""
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.browse = null }) { Icon(Icons.Filled.ArrowBack, "Geri", tint = TXT) }
            Column(Modifier.weight(1f)) {
                Text(title, color = TXT, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = MUTED, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (list.isEmpty()) {
            Hint(if (b.kind == 3) "Bu parçaya benzer başka parça bulunamadı." else "Bu sayfada parça yok.")
        } else {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { vm.play(list, 0) },
                    colors = ButtonDefaults.buttonColors(containerColor = ACC, contentColor = INK)
                ) { Icon(Icons.Filled.PlayArrow, null); Text("Çal") }
                Button(
                    onClick = { vm.playShuffled(list) },
                    colors = ButtonDefaults.buttonColors(containerColor = CHIP, contentColor = TXT)
                ) { Icon(Icons.Filled.Shuffle, null); Text("Karışık", modifier = Modifier.padding(start = 4.dp)) }
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            if (b.kind == 0) {
                val top = list.filter { vm.count(it) > 0 }.sortedByDescending { vm.count(it) }.take(5)
                if (top.isNotEmpty()) {
                    item { SectionTitle("En çok dinlediklerin") }
                    itemsIndexed(top) { i, s ->
                        SongRow(s, vm, rank = i + 1, sub = "${vm.count(s)} kez · " + s.album.ifEmpty { "Albüm bilgisi yok" }) {
                            vm.play(top, i)
                        }
                    }
                }
                if (albums.isNotEmpty()) {
                    item { SectionTitle("Albümler") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(albums) { al ->
                                val tr = list.filter { it.album == al }
                                PlaylistCard(al, "${tr.size} parça", tr.firstOrNull()) { vm.browse = Browse(1, al) }
                            }
                        }
                    }
                    item { SectionTitle("Tüm parçalar") }
                }
            }
            itemsIndexed(list) { i, s -> SongRow(s, vm) { vm.play(list, i) } }
        }
    }
}

@Composable
fun TopContent(vm: MusicVM) {
    val ranked = remember(vm.songs, vm.stats) {
        vm.songs.filter { vm.count(it) > 0 }.sortedByDescending { vm.count(it) }.take(50)
    }
    Column(Modifier.fillMaxSize()) {
        if (ranked.isEmpty()) Hint("Henüz dinleme kaydı yok. Bir parçayı yaklaşık 30 saniye dinleyince sayılır.")
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            itemsIndexed(ranked) { i, s ->
                SongRow(
                    s, vm, rank = i + 1,
                    sub = "${vm.count(s)} kez · " + s.artist.ifEmpty { "Sanatçı bilinmiyor" }
                ) { vm.play(ranked, i) }
            }
        }
    }
}

// ---------- Alt çubuklar ----------

@Composable
fun MiniPlayer(vm: MusicVM, onOpen: () -> Unit) {
    val s = vm.current ?: return
    val frac = if (vm.duration > 0) (vm.position.toFloat() / vm.duration).coerceIn(0f, 1f) else 0f
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(shape).border(1.dp, EDGE, shape).background(CARD).clickable(onClick = onOpen)
    ) {
        Row(
            Modifier.padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Art(s, Modifier.size(50.dp), 10.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(s.title, color = TXT, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(s.artist.ifEmpty { "Sanatçı bilinmiyor" }, color = MUTED, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { vm.prev() }) {
                Icon(Icons.Filled.SkipPrevious, "Önceki", tint = TXT, modifier = Modifier.size(26.dp))
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).border(2.dp, ACC, CircleShape).clickable { vm.toggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (vm.isPlaying) "Duraklat" else "Oynat", tint = TXT, modifier = Modifier.size(26.dp)
                )
            }
            IconButton(onClick = { vm.next() }) {
                Icon(Icons.Filled.SkipNext, "Sonraki", tint = TXT, modifier = Modifier.size(26.dp))
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 14.dp, vertical = 4.dp)
                .fillMaxWidth().height(2.dp).background(EDGE)
        ) { Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(ACC)) }
    }
}

// ---------- Şimdi çalıyor ----------

@Composable
fun PlayerMedia(vm: MusicVM, s: Song, landscape: Boolean) {
    if (s.isVideo) {
        AndroidView(
            factory = { c ->
                PlayerView(c).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    player = vm.controller
                }
            },
            update = { it.player = vm.controller },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
        )
    } else if (landscape) {
        Art(s, Modifier.fillMaxHeight(0.9f).aspectRatio(1f), 8.dp)
    } else {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Art(s, Modifier.size(220.dp), 8.dp) }
    }
}

@Composable
fun PlayerInfo(vm: MusicVM, s: Song) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                s.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(s.artist.ifEmpty { "Sanatçı bilinmiyor" }, color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
        }
        if (s.remote) {
            val liked = vm.isLiked(s)
            IconButton(onClick = { vm.toggleLike(s) }) {
                Icon(
                    if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    if (liked) "Beğenmekten vazgeç" else "Beğen",
                    tint = if (liked) PLAYER_ACC else Color.White, modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun PlayerControls(vm: MusicVM) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val dur = vm.duration.coerceAtLeast(1L)
    val frac = drag ?: (vm.position.toFloat() / dur).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = frac,
            onValueChange = { drag = it },
            onValueChangeFinished = {
                drag?.let { vm.seekTo((it * dur).toLong()) }
                drag = null
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White, activeTrackColor = Color.White,
                inactiveTrackColor = Color(0x55FFFFFF)
            )
        )
        var speedMenu by remember { mutableStateOf(false) }
        Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(fmt((frac * dur).toLong()), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Box {
                val pill = RoundedCornerShape(50)
                Text(
                    "Hız " + speedLabel(vm.speed),
                    color = if (vm.speed != 1f) PLAYER_ACC else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(pill).border(1.dp, Color(0x55FFFFFF), pill)
                        .clickable { speedMenu = true }.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                    listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { v ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    speedLabel(v) + if (v == 1f) " (normal)" else "",
                                    fontWeight = if (v == vm.speed) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = { vm.setSpeed(v); speedMenu = false }
                        )
                    }
                }
            }
            Text(fmt(vm.duration), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.toggleShuffle() }) {
                Icon(Icons.Filled.Shuffle, "Karıştır", tint = if (vm.shuffle) PLAYER_ACC else Color.White)
            }
            IconButton(onClick = { vm.prev() }) {
                Icon(Icons.Filled.SkipPrevious, "Önceki", tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(Color.White).clickable { vm.toggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (vm.isPlaying) "Duraklat" else "Oynat", tint = DARK_INK, modifier = Modifier.size(36.dp)
                )
            }
            IconButton(onClick = { vm.next() }) {
                Icon(Icons.Filled.SkipNext, "Sonraki", tint = Color.White, modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = { vm.cycleRepeat() }) {
                Icon(
                    if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    "Tekrarla",
                    tint = if (vm.repeatMode == Player.REPEAT_MODE_OFF) Color.White else PLAYER_ACC
                )
            }
        }
    }
}

@Composable
fun PlayerLyrics(vm: MusicVM, s: Song, modifier: Modifier, onEdit: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Color(0x33000000))) {
        val ly = vm.lyrics
        if (ly == null) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    when {
                        vm.lyricsLoading -> "Sözler aranıyor…"
                        s.remote -> "Bu 30 saniyelik önizlemede söz yok."
                        !vm.online -> "Sözler için internete bağlan. Bulunan sözler telefona kaydedilir."
                        else -> "Bu parça için söz bulunamadı."
                    },
                    color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp
                )
                if (!s.remote && !vm.lyricsLoading) {
                    TextButton(onClick = onEdit) { Text("Sözleri kendin yaz", color = PLAYER_ACC, fontWeight = FontWeight.SemiBold) }
                }
            }
        } else {
            LyricsList(ly, vm.position) { vm.seekTo(it) }
            if (!s.remote) {
                IconButton(onClick = onEdit, modifier = Modifier.align(Alignment.TopEnd).size(40.dp)) {
                    Icon(Icons.Filled.Edit, "Sözleri düzenle", tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun LyricsEditDialog(vm: MusicVM, onClose: () -> Unit) {
    var text by remember { mutableStateOf(vm.rawLyrics()) }
    AlertDialog(
        onDismissRequest = onClose, containerColor = CARD, titleContentColor = TXT, textContentColor = TXT,
        title = { Text("Sözler", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Düz metin yazabilir ya da [01:23.45] gibi zaman damgalı satırlar yapıştırabilirsin. " +
                        "Zaman damgalıysa sözler şarkıyla akar. Boş kaydedersen sözler silinir.",
                    color = MUTED, fontSize = 13.sp
                )
                TextField(
                    value = text, onValueChange = { text = it },
                    placeholder = { Text("Sözleri buraya yaz") },
                    shape = RoundedCornerShape(12.dp), colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth().height(220.dp).padding(top = 10.dp)
                )
                if (vm.online) {
                    TextButton(onClick = { vm.refetchLyrics(); onClose() }) {
                        Text("Kayıtlıyı sil, internetten yeniden ara", color = ACC, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.saveLyricsText(text); onClose() }) { Text("Kaydet", color = ACC, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Vazgeç", color = MUTED) } },
    )
}

@Composable
fun FullPlayer(vm: MusicVM, onClose: () -> Unit) {
    val s = vm.current
    val ctx = LocalContext.current
    // Kapak renginden tema: kapak bulunamazsa ya da ayar kapalıysa parça adından türeyen renk
    val coverArgb by produceState<Int?>(null, s?.mediaId, vm.coverTheme) {
        value = if (vm.coverTheme && s != null) Cover.tint(ctx, s) else null
    }
    val target = coverArgb?.let { Color(it) } ?: tintFor(s?.title ?: "", 0.45f, 0.38f)
    val tint by animateColorAsState(target, tween(600), label = "tint")
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var more by remember { mutableStateOf(false) }
    var showTrim by remember { mutableStateOf(false) }
    var showLyricEdit by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(tint, DARK_BG)))
            .pointerInput(Unit) {}.statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.KeyboardArrowDown, "Kapat", tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Text(
                "ŞİMDİ ÇALIYOR", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { vm.shareNowPlaying() }, enabled = s != null) {
                Icon(Icons.Filled.Share, "Şimdi çalanı paylaş", tint = Color.White)
            }
            Box {
                IconButton(onClick = { more = true }, enabled = s != null) {
                    Icon(Icons.Filled.MoreVert, "Daha fazla", tint = Color.White)
                }
                DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                    if (s != null && !s.remote) {
                        DropdownMenuItem(text = { Text("Kırp / zil sesi yap") }, onClick = { more = false; showTrim = true })
                        DropdownMenuItem(text = { Text("Sözleri düzenle") }, onClick = { more = false; showLyricEdit = true })
                        if (s.artist.isNotBlank()) {
                            DropdownMenuItem(text = { Text("Sanatçı sayfası") }, onClick = { more = false; vm.openBrowse(0, s.artist) })
                        }
                        if (s.album.isNotBlank()) {
                            DropdownMenuItem(text = { Text("Albüm sayfası") }, onClick = { more = false; vm.openBrowse(1, s.album) })
                        }
                        DropdownMenuItem(text = { Text("Benzer parçaları bul") }, onClick = { more = false; vm.openBrowse(3, s.mediaId) })
                    } else {
                        DropdownMenuItem(text = { Text("Kırpma ve söz yalnızca telefondaki parçalarda") }, enabled = false, onClick = {})
                    }
                }
            }
        }
        if (s != null) {
            if (landscape) {
                // Yatay: solda kapak/video, sağda kaydırılabilir bilgi + kontroller + sözler (tuşlar hep erişilebilir)
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(0.9f).fillMaxHeight().padding(end = 16.dp), contentAlignment = Alignment.Center) {
                        PlayerMedia(vm, s, true)
                    }
                    Column(Modifier.weight(1.1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        PlayerInfo(vm, s)
                        PlayerControls(vm)
                        PlayerLyrics(vm, s, Modifier.fillMaxWidth().height(180.dp).padding(vertical = 8.dp)) { showLyricEdit = true }
                    }
                }
            } else {
                PlayerMedia(vm, s, false)
                PlayerInfo(vm, s)
                PlayerControls(vm)
                PlayerLyrics(vm, s, Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp, bottom = 12.dp)) { showLyricEdit = true }
            }
        }
    }
    if (showTrim && s != null) TrimDialog(vm, s) { showTrim = false }
    if (showLyricEdit && s != null && !s.remote) LyricsEditDialog(vm) { showLyricEdit = false }
}

@Composable
fun LyricsList(ly: Lyrics, pos: Long, onSeek: (Long) -> Unit) {
    val state = remember(ly) { LazyListState() }
    val active = if (ly.synced) ly.lines.indexOfLast { it.timeMs <= pos + 200 } else -1
    LaunchedEffect(active, ly) {
        if (active >= 0) state.animateScrollToItem(active, -140)
    }
    LazyColumn(state = state, contentPadding = PaddingValues(start = 16.dp, end = 44.dp, top = 16.dp, bottom = 16.dp)) {
        itemsIndexed(ly.lines) { i, l ->
            Text(
                l.text.ifBlank { "♪" },
                fontSize = if (ly.synced) 22.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (!ly.synced || i == active) Color.White else Color.White.copy(alpha = 0.45f),
                modifier = Modifier.padding(vertical = 6.dp).clickable(enabled = ly.synced) { onSeek(l.timeMs) }
            )
        }
    }
}

// ---------- Asistan ----------

@Composable
fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = CHIP, unfocusedContainerColor = CHIP,
    focusedTextColor = TXT, unfocusedTextColor = TXT, cursorColor = ACC,
    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
    focusedPlaceholderColor = MUTED, unfocusedPlaceholderColor = MUTED,
    focusedLabelColor = MUTED, unfocusedLabelColor = MUTED,
)

@Composable
fun Bubble(m: ChatMsg) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Text(
            m.text, color = if (m.fromUser) INK else TXT, fontSize = 16.sp,
            modifier = Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(16.dp))
                .background(if (m.fromUser) ACC else CHIP).padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun AssistantTab(vm: MusicVM, onSettings: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val toggleVoice = rememberVoiceToggle(vm)
    val listState = rememberLazyListState()
    LaunchedEffect(vm.chat.size, vm.aiBusy) {
        val n = vm.chat.size + (if (vm.aiBusy) 1 else 0)
        if (n > 0) listState.animateScrollToItem(n - 1)
    }

    Column(Modifier.fillMaxSize()) {
        Header("Asistan") {
            IconButton(onClick = { vm.toggleMute() }) {
                Icon(
                    if (vm.voiceMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    if (vm.voiceMuted) "Asistan sesini aç" else "Asistan sesini kapat",
                    tint = if (vm.voiceMuted) MUTED else ACC
                )
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, "Ayarlar", tint = TXT)
            }
        }
        run {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (vm.chat.isEmpty()) {
                    item {
                        Hint(
                            "Ruh halini ya da ne dinlemek istediğini söyle. Telefonundaki listene bakıp sana göre çalarım, internet ya da anahtar gerekmez. " +
                                "Mesela: \"Ruh halim kötü, iyileştirecek oyun havaları aç\" ya da \"Hüzünlü şarkılar aç\"."
                        )
                    }
                }
                items(vm.chat) { m -> Bubble(m) }
                if (vm.aiBusy) item { Bubble(ChatMsg(false, "Listene bakıyorum…")) }
            }
            Text(
                vm.voiceNote ?: when {
                    vm.speaking -> "Konuşuyorum…"
                    vm.voiceOn && vm.listening -> "Dinliyorum, konuşabilirsin…"
                    vm.voiceOn -> "Sohbet modu açık"
                    else -> "Sohbet modu kapalı. Mikrofon simgesine dokununca eller serbest konuşabilirsin."
                },
                color = if (vm.voiceNote != null) Color(0xFFFF8A70) else ACC, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Ruh halim kötü, iyileştir", "Hüzünlü şarkılar aç", "Oyun havası, heyecanlı",
                    "Sakin bir şeyler", "Bana enerji ver"
                ).forEach { q -> Chip(q, false) { vm.ask(q) } }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = text, onValueChange = { text = it }, maxLines = 3,
                    placeholder = { Text("Ruh halin ne? Ne çalayım?") },
                    shape = RoundedCornerShape(24.dp), colors = fieldColors(),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = toggleVoice) {
                    Icon(
                        if (vm.voiceOn) Icons.Filled.Mic else Icons.Filled.MicOff,
                        if (vm.voiceOn) "Sohbet modunu kapat" else "Sohbet modunu aç",
                        tint = if (vm.voiceOn) ACC else MUTED
                    )
                }
                IconButton(onClick = { vm.ask(text); text = "" }, enabled = !vm.aiBusy) {
                    Icon(Icons.Filled.Send, "Gönder", tint = if (vm.aiBusy) MUTED else ACC)
                }
            }
        }
    }
}

// ---------- Beğenilenler ----------

private fun saveTextTo(ctx: android.content.Context, uri: android.net.Uri, text: String): Boolean = runCatching {
    ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
}.getOrDefault(false)

@Composable
fun LikedTab(vm: MusicVM) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val txtSave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) vm.toast(if (saveTextTo(ctx, uri, Export.text(vm.likes))) "Liste kaydedildi." else "Kaydedilemedi.")
    }
    val csvSave = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.toast(if (saveTextTo(ctx, uri, Export.csv(vm.likes))) "Liste kaydedildi." else "Kaydedilemedi.")
    }
    Column(Modifier.fillMaxSize()) {
        Header("Beğendiklerin") {
            if (vm.likes.isNotEmpty()) {
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.Share, "Listeyi dışa aktar", tint = TXT) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Metin olarak paylaş") }, onClick = {
                            menu = false
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, Export.text(vm.likes))
                            }
                            runCatching { ctx.startActivity(Intent.createChooser(send, "Listeyi paylaş")) }
                        })
                        DropdownMenuItem(text = { Text("Metin dosyası olarak kaydet (.txt)") }, onClick = {
                            menu = false
                            txtSave.launch("yirdas-begeniler.txt")
                        })
                        DropdownMenuItem(text = { Text("Tablo olarak kaydet (.csv)") }, onClick = {
                            menu = false
                            csvSave.launch("yirdas-begeniler.csv")
                        })
                    }
                }
            }
        }
        if (vm.likes.isEmpty()) {
            Hint("Listelerin arasına karışan yeni parçalarda kalbe dokun, burada birikir. Sonra dışa aktar simgesiyle listeyi paylaşabilir ya da dosya olarak kaydedebilirsin.")
        } else {
            Hint("Önizleme 30 saniyedir. Tam hâlini kendi hesabınla indirmek için parçanın menüsünden Deezer, YouTube Music ya da Spotify'da açabilir, listeyi de paylaşabilir ya da .txt / .csv dosyası olarak kaydedebilirsin.")
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            itemsIndexed(vm.likes) { i, s -> SongRow(s, vm) { vm.play(vm.likes, i) } }
        }
    }
}
