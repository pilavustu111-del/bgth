@file:OptIn(androidx.media3.common.util.UnstableApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.cepcalar

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlin.math.roundToInt

// ---------- Küçük parçalar ----------

@Composable
fun SectionTitle(t: String, onAll: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.GraphicEq, null, tint = ACC, modifier = Modifier.size(22.dp))
        Text(
            t, color = TXT, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).padding(start = 10.dp)
        )
        if (onAll != null) {
            Text("Tümünü Gör →", color = MUTED, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onAll))
        }
    }
}

@Composable
fun NavBar(selected: Int, onSelect: (Int) -> Unit) {
    val items: List<Pair<String, ImageVector>> = listOf(
        "Ana Sayfa" to Icons.Filled.Home,
        "Ara" to Icons.Filled.Search,
        "Asistan" to Icons.Filled.Mic,
        "Kitaplığım" to Icons.Filled.MenuBook,
        "Beğenilen" to Icons.Filled.Favorite,
    )
    val shape = RoundedCornerShape(30.dp)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 6.dp)) {
        Row(
            Modifier.fillMaxWidth().height(62.dp).clip(shape).border(1.dp, EDGE, shape).background(CARD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { i, (label, icon) ->
                if (i == 2) {
                    Spacer(Modifier.weight(1f))
                } else {
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable { onSelect(i) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(icon, label, tint = if (i == selected) ACC else MUTED, modifier = Modifier.size(24.dp))
                        Text(
                            label, color = if (i == selected) ACC else MUTED, fontSize = 11.sp, maxLines = 1,
                            fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = (-22).dp).size(68.dp)
                .clip(CircleShape).border(1.5.dp, if (selected == 2) ACC else EDGE, CircleShape)
                .background(CARD).clickable { onSelect(2) },
            contentAlignment = Alignment.Center
        ) { LogoMark(38.dp, if (selected == 2) ACC else TXT) }
    }
}

// ---------- Ana sayfa üst bölüm ----------

@Composable
fun HomeTop(vm: MusicVM, onGo: (Int) -> Unit, onSettings: () -> Unit) {
    val pal = LocalPal.current
    Box(Modifier.fillMaxWidth().height(360.dp)) {
        HeroScene(Modifier.matchParentSize())
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 6.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LogoMark(46.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Yırdaş", color = TXT, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                if (pal.dark) Text("Müziğin ruhu, seninle…", color = MUTED, fontSize = 12.sp)
            }
            IconButton(onClick = { onGo(1) }) { Icon(Icons.Filled.Search, "Ara", tint = TXT) }
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Ayarlar", tint = TXT) }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 34.dp)) {
            Text("Merhaba,", color = MUTED, fontSize = 18.sp)
            Text(
                vm.userName.ifBlank { "Yırdaş" }, color = TXT, fontSize = 40.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                if (pal.dark) "“Kimi zaman bir şarkı,\ntüm dertlere iyi gelir.”" else "“Bozkırın sesi,\nher zaman seninle.”",
                color = MUTED, fontSize = 14.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 6.dp)
            )
            val pill = RoundedCornerShape(50)
            Row(
                Modifier.padding(top = 14.dp).clip(pill).border(1.dp, EDGE, pill)
                    .background(CARD.copy(alpha = 0.7f)).padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.DarkMode, null, tint = MUTED, modifier = Modifier.size(20.dp))
                Text("Gece Modu", color = MUTED, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 10.dp))
                Switch(
                    checked = pal.dark, onCheckedChange = { vm.setTheme(if (it) THEME_DARK else THEME_LIGHT) },
                    colors = SwitchDefaults.colors(checkedTrackColor = ACC, checkedThumbColor = INK)
                )
            }
        }
    }
}

// ---------- Şimdi çalıyor kartı ----------

@Composable
fun NowPlayingCard(vm: MusicVM, onOpen: () -> Unit) {
    val s = vm.current ?: return
    val frac = if (vm.duration > 0) (vm.position.toFloat() / vm.duration).coerceIn(0f, 1f) else 0f
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            .clip(shape).border(1.dp, EDGE, shape).background(CARD).clickable(onClick = onOpen).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Art(s, Modifier.size(92.dp), 16.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    s.title, color = TXT, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    s.artist.ifEmpty { "Sanatçı bilinmiyor" }, color = MUTED, fontSize = 14.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(EDGE)) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(ACC))
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmt(vm.position), color = MUTED, fontSize = 12.sp)
                    Text(fmt(vm.duration), color = MUTED, fontSize = 12.sp)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.toggleShuffle() }) {
                Icon(Icons.Filled.Shuffle, "Karıştır", tint = if (vm.shuffle) ACC else TXT)
            }
            IconButton(onClick = { vm.prev() }) {
                Icon(Icons.Filled.SkipPrevious, "Önceki", tint = TXT, modifier = Modifier.size(32.dp))
            }
            Box(
                Modifier.size(58.dp).clip(CircleShape).border(1.5.dp, ACC, CircleShape).clickable { vm.toggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (vm.isPlaying) "Duraklat" else "Oynat", tint = TXT, modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = { vm.next() }) {
                Icon(Icons.Filled.SkipNext, "Sonraki", tint = TXT, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = { vm.cycleRepeat() }) {
                Icon(
                    if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    "Tekrarla", tint = if (vm.repeatMode == Player.REPEAT_MODE_OFF) TXT else ACC
                )
            }
        }
    }
}

// ---------- Asistan kartı ----------

@Composable
fun AssistantCard(vm: MusicVM, onOpen: () -> Unit, onMic: () -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(shape).border(1.dp, EDGE, shape).background(CARD).clickable(onClick = onOpen).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).border(1.dp, EDGE, CircleShape)
                .background(Brush.radialGradient(listOf(CHIP, CARD))),
            contentAlignment = Alignment.Center
        ) { LogoMark(36.dp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text("Yırdaş Asistan", color = TXT, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("Nasıl bir ruh halindesin?", color = MUTED, fontSize = 13.sp, maxLines = 2)
        }
        Waveform(vm.listening || vm.speaking, ACC, Modifier.width(64.dp).height(30.dp))
        Box(
            Modifier.padding(start = 10.dp).size(48.dp).clip(CircleShape).border(1.5.dp, ACC, CircleShape)
                .clickable(onClick = onMic),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Mic, "Sesli sohbet", tint = ACC) }
    }
}

// ---------- Dinleme istatistikleri ----------

@Composable
fun StatCell(icon: ImageVector, big: String, small: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 6.dp)) {
        Icon(icon, null, tint = ACC, modifier = Modifier.size(22.dp))
        Text(big, color = TXT, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        Text(small, color = MUTED, fontSize = 12.sp)
    }
}

@Composable
fun StatsCard(hours: Double, plays: Int, top: Song?, topCount: Int, streak: Int = 0) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(shape).border(1.dp, EDGE, shape)
            .background(CARD).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.GraphicEq, null, tint = ACC, modifier = Modifier.size(20.dp))
            Text(
                "Dinleme İstatistiklerin", color = TXT, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.Top) {
            val total = if (hours >= 1.0) "${hours.roundToInt()} saat" else "${(hours * 60).roundToInt()} dk"
            StatCell(Icons.Filled.Headphones, total, "Toplam Dinleme", Modifier.weight(1f))
            StatCell(Icons.Filled.PlayArrow, "$plays kez", "Toplam Parça", Modifier.weight(1f))
            if (top != null) {
                Column(Modifier.weight(1.2f).padding(horizontal = 6.dp)) {
                    Icon(Icons.Filled.Star, null, tint = ACC, modifier = Modifier.size(22.dp))
                    Text("En Çok Dinlediğin", color = MUTED, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(top.title, color = TXT, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("($topCount Kez)", color = MUTED, fontSize = 12.sp)
                }
            }
        }
        if (streak >= 2) {
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Whatshot, null, tint = ACC, modifier = Modifier.size(18.dp))
                Text("$streak gün üst üste dinledin", color = MUTED, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
fun BadgeCard(b: Badge, m: Metrics, earned: Boolean) {
    val shape = RoundedCornerShape(20.dp)
    val v = b.value(m)
    val frac = (v / b.goal).toFloat().coerceIn(0f, 1f)
    Column(
        Modifier.width(140.dp).clip(shape).border(if (earned) 1.5.dp else 1.dp, if (earned) ACC else EDGE, shape)
            .background(CARD).padding(14.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(if (earned) ACC else CHIP),
            contentAlignment = Alignment.Center
        ) { Icon(b.icon, null, tint = if (earned) INK else MUTED) }
        Text(
            b.title, color = if (earned) TXT else MUTED, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, modifier = Modifier.padding(top = 10.dp)
        )
        Text(b.desc, color = MUTED, fontSize = 12.sp, maxLines = 2, minLines = 2)
        Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(EDGE)) {
            Box(Modifier.fillMaxWidth(if (earned) 1f else frac).fillMaxHeight().background(ACC))
        }
        val shown = minOf(v, b.goal.toDouble())
        Text(
            if (earned) "Kazanıldı" else (if (b.unit == "saat") "%.1f".format(shown) else "${shown.toInt()}") + " / ${b.goal} ${b.unit}",
            color = if (earned) ACC else MUTED, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// ---------- Ana sayfa ----------

@Composable
fun Home(vm: MusicVM, onGo: (Int) -> Unit, onSettings: () -> Unit, onOpenPlayer: () -> Unit, onAsk: () -> Unit) {
    val list = when (vm.filter) {
        1 -> vm.songs.filter { !it.isVideo }
        2 -> vm.songs.filter { it.isVideo }
        else -> vm.songs
    }
    val ranked = remember(vm.songs, vm.stats) {
        vm.songs.filter { vm.count(it) > 0 }.sortedByDescending { vm.count(it) }
    }
    val queue = remember(list, vm.stats, vm.recoTracks) { vm.mixed(list.sortedByDescending { vm.count(it) }, 4) }
    val shown = queue.take(14)
    val videos = remember(vm.songs) { vm.songs.filter { it.isVideo } }
    val mixList = remember(vm.songs) { vm.songs.shuffled().take(40) }
    val plays = remember(vm.stats) { vm.stats.values.sum() }
    val hours = remember(vm.songs, vm.stats, vm.listenMs) { vm.totalHours() }
    val met = remember(vm.songs, vm.stats, vm.listenMs, vm.streak, vm.likes) { vm.metricsNow() }
    val badges = remember(vm.earned, met) {
        Badges.all.sortedWith(
            compareByDescending<Badge> { it.id in vm.earned }
                .thenByDescending { (it.value(met) / it.goal).coerceAtMost(1.0) }
        )
    }
    val resumables = remember(vm.songs, vm.resume) {
        vm.songs.filter { it.isLong && (vm.resume[it.statKey]?.first ?: 0L) > 5000L }
            .sortedByDescending { vm.resume[it.statKey]?.second ?: 0L }.take(4)
    }
    val toggleVoice = rememberVoiceToggle(vm)

    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item { HomeTop(vm, onGo, onSettings) }
        if (vm.current != null) item { NowPlayingCard(vm, onOpenPlayer) }
        item {
            AssistantCard(vm, onOpen = { onGo(2) }, onMic = {
                if (!vm.voiceOn) toggleVoice()
                onGo(2)
            })
        }
        if (!vm.hasPermission) {
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Telefondaki müzikleri bulabilmem için izin ver.", color = TXT, fontSize = 16.sp)
                    Button(
                        onClick = onAsk,
                        colors = ButtonDefaults.buttonColors(containerColor = ACC, contentColor = INK)
                    ) { Text("İzin ver") }
                    Text(
                        "İzin penceresi çıkmazsa Ayarlar > Uygulamalar > Yırdaş > İzinler bölümünden Müzik ve ses ile Fotoğraf ve video iznini aç.",
                        color = MUTED, fontSize = 13.sp
                    )
                }
            }
        } else if (vm.songs.isEmpty()) {
            item { Hint("Bu telefonda henüz müzik bulunamadı. Dosyalar eklenince liste kendiliğinden güncellenir.") }
        }

        if (resumables.isNotEmpty()) {
            item { SectionTitle("Kaldığın Yerden") }
            itemsIndexed(resumables) { i, s ->
                val pos = vm.resume[s.statKey]?.first ?: 0L
                SongRow(
                    s, vm,
                    sub = "${fmt(pos)} / ${fmt(s.durationMs)} · " + s.artist.ifEmpty { if (s.isVideo) "Klip" else "Sanatçı bilinmiyor" }
                ) { vm.play(resumables, i) }
            }
        }

        if (vm.songs.isNotEmpty()) {
            item { SectionTitle("Senin İçin Hazırlandı") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        val src = ranked.ifEmpty { vm.songs }.take(30)
                        PlaylistCard("Çok Dinlediklerin", "${src.size} parça", src.firstOrNull()) {
                            vm.play(vm.mixed(src, 4), 0)
                        }
                    }
                    item {
                        PlaylistCard("Karışık Çal", "${mixList.size} parça", mixList.firstOrNull()) {
                            vm.play(vm.mixed(mixList, 4), 0)
                        }
                    }
                    if (videos.isNotEmpty()) {
                        item {
                            PlaylistCard("Klipler", "${videos.size} klip", videos.firstOrNull()) { vm.play(videos, 0) }
                        }
                    }
                    items(vm.similar.take(6)) { a ->
                        StationCard(a) { vm.playStation(a) }
                    }
                }
            }
        }

        if (plays > 0) {
            item { Spacer(Modifier.height(20.dp)) }
            item { StatsCard(hours, plays, ranked.firstOrNull(), ranked.firstOrNull()?.let { vm.count(it) } ?: 0, vm.streak) }
        }

        if (vm.songs.isNotEmpty()) {
            item { SectionTitle("Rozetlerin · ${vm.earned.size}/${Badges.all.size}") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) { items(badges, key = { it.id }) { b -> BadgeCard(b, met, b.id in vm.earned) } }
            }
        }

        if (ranked.isNotEmpty()) {
            item { SectionTitle("En Çok Dinlediklerin") { vm.libMode = 1; onGo(3) } }
            itemsIndexed(ranked.take(5)) { i, s ->
                SongRow(s, vm, rank = i + 1, sub = "${vm.count(s)} kez · " + s.artist.ifEmpty { "Sanatçı bilinmiyor" }) {
                    vm.play(ranked, i)
                }
            }
        }

        item { SectionTitle("Parçaların") { vm.libMode = 0; onGo(3) } }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip("Tümü", vm.filter == 0) { vm.filter = 0 }
                Chip("Müzik", vm.filter == 1) { vm.filter = 1 }
                Chip("Klipler", vm.filter == 2) { vm.filter = 2 }
            }
        }
        items(shown) { s -> SongRow(s, vm) { vm.play(queue, queue.indexOf(s)) } }

        if (vm.mine.isNotEmpty()) {
            item { SectionTitle("Senin Radyoların") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) { items(vm.mine) { a -> StationCard(a) { vm.playStation(a) } } }
            }
        } else if (!vm.online && vm.songs.isNotEmpty()) {
            item { Hint("İnternete bağlanınca en çok dinlediğin sanatçılara göre istasyonlar burada çıkar.", Modifier.padding(top = 16.dp)) }
        }
        if (videos.isNotEmpty() && vm.filter != 1) {
            item { SectionTitle("Klipler") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(videos) { v ->
                        Column(Modifier.width(220.dp).clickable { vm.play(videos, videos.indexOf(v)) }) {
                            Art(v, Modifier.fillMaxWidth().aspectRatio(16f / 9f), 12.dp)
                            Text(
                                v.title, color = TXT, fontWeight = FontWeight.SemiBold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(v.artist.ifEmpty { "Klip" }, color = MUTED, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
