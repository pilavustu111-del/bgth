package app.cepcalar

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

data class ChatMsg(val fromUser: Boolean, val text: String)

/** Kitaplıkta açık detay sayfası. kind: 0 sanatçı, 1 albüm, 2 klasör, 3 benzer parçalar (key = parçanın mediaId'si). */
class Browse(val kind: Int, val key: String)

class MusicVM(app: Application) : AndroidViewModel(app) {
    private val ctx = app

    var songs by mutableStateOf(emptyList<Song>())
    var stats by mutableStateOf(Store.loadStats(app))
    var hasPermission by mutableStateOf(false)
    var allVideos by mutableStateOf(Store.getAllVideos(app))
    var filter by mutableStateOf(0) // 0 Tümü, 1 Müzik, 2 Klipler
    var libMode by mutableStateOf(0) // Kitaplık: 0 tüm parçalar, 1 en çok dinlenen

    var controller by mutableStateOf<MediaController?>(null)
    var current by mutableStateOf<Song?>(null)
    var isPlaying by mutableStateOf(false)
    var shuffle by mutableStateOf(false)
    var repeatMode by mutableStateOf(Player.REPEAT_MODE_OFF)
    var position by mutableStateOf(0L)
    var duration by mutableStateOf(0L)
    var openFull by mutableStateOf(false)

    var lyrics by mutableStateOf<Lyrics?>(null)
    var lyricsLoading by mutableStateOf(false)

    var online by mutableStateOf(false)
    var similar by mutableStateOf(emptyList<Artist>())
    var mine by mutableStateOf(emptyList<Artist>())

    // Yapay zekâ asistanı
    var apiKey by mutableStateOf(Store.getKey(app))
    var aiModel by mutableStateOf(Store.getModel(app))
    var chat by mutableStateOf(emptyList<ChatMsg>())
    var aiBusy by mutableStateOf(false)
    private val history = ArrayList<Pair<String, String>>()

    // Öneri parçalar (30 sn önizleme) ve beğenilenler
    var recoTracks by mutableStateOf(emptyList<Song>())
    var likes by mutableStateOf(Store.loadLikes(app))
    private var lastReco = 0L

    // Sesli sohbet
    val voice = Voice(app)
    var voiceOn by mutableStateOf(Store.getVoiceOn(app))
    var voiceMuted by mutableStateOf(Store.getMuted(app))

    // Hız, kaldığın yerden, gezinme, rozetler
    var speed by mutableStateOf(1f)
    var resume by mutableStateOf(Store.loadResume(app))
    var browse by mutableStateOf<Browse?>(null)
    var goTab by mutableStateOf<Int?>(null)
    var listenMs by mutableStateOf(Store.getListenMs(app))
    var streak by mutableStateOf(Badges.streak(Store.getDays(app)))
    var earned by mutableStateOf(Store.getEarned(app) ?: emptySet<String>())
    var badgeBanner by mutableStateOf<String?>(null)
    var previewEnd: Long? = null
    private val speeds = Store.loadSpeeds(app).toMutableMap()
    private var lastTickAt = 0L
    private var lastResumeSave = 0L
    private var listenPending = 0L
    private var lastListenSave = 0L
    private var sessionMs = 0L
    private var markedDay = ""

    // Görünüm
    var accent by mutableStateOf(Store.getAccent(app))
    var coverTheme by mutableStateOf(Store.getCoverTheme(app))
    var loc by mutableStateOf(Store.getLoc(app))
    var themeMode by mutableStateOf(Store.getTheme(app))
    var userName by mutableStateOf(Store.getUserName(app))
    var listening by mutableStateOf(false)
    var speaking by mutableStateOf(false)
    var voiceNote by mutableStateOf<String?>(null)
    private var greeted = false

    init {
        voice.init()
        voice.onHeard = { handleHeard(it) }
        voice.onListening = { listening = it }
        voice.onSpeaking = { speaking = it }
        voice.onNote = { voiceNote = it }
        voice.onFatal = { voiceNote = it; voiceOn = false; Store.setVoiceOn(ctx, false) }
    }

    private var counted = false
    private var activeMood: String? = null
    private var lyricsJob: Job? = null
    private var recoJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            current = mediaItem?.let { songFromItem(it) }
            counted = false
            position = 0
            previewEnd = null
            loadLyrics()
            applySpeedFor(current)
            // Sonraki / önceki ile gelinen uzun içerik kaldığı yerden başlar
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                resumeFor(current)?.let { controller?.seekTo(it) }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            this@MusicVM.isPlaying = isPlaying
            if (!isPlaying) saveResumeNow()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            shuffle = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            this@MusicVM.repeatMode = repeatMode
        }
    }

    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            scan()
        }
    }

    init {
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            c.addListener(listener)
            controller = c
            c.currentMediaItem?.let { current = songFromItem(it) }
            isPlaying = c.isPlaying
            shuffle = c.shuffleModeEnabled
            repeatMode = c.repeatMode
            speed = c.playbackParameters.speed
            loadLyrics()
        }, ContextCompat.getMainExecutor(ctx))

        ctx.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaObserver)
        ctx.contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, mediaObserver)

        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        online = isOnline()
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    online = true
                    refreshReco()
                    if (lyrics == null) loadLyrics()
                }

                override fun onLost(network: Network) {
                    online = isOnline()
                }
            })
        }
        checkPermission()
    }

    private fun isOnline(): Boolean {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun checkPermission(): Boolean {
        val p = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
        hasPermission = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
        return hasPermission
    }

    fun scan() {
        if (!checkPermission()) return
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { Library.scan(ctx, allVideos) }
            songs = list
            checkBadges()
            refreshReco()
        }
    }

    fun setShowAllVideos(v: Boolean) {
        allVideos = v
        Store.setAllVideos(ctx, v)
        scan()
    }

    // ---- Çalma ----
    fun play(list: List<Song>, index: Int) {
        val c = controller ?: return
        if (list.isEmpty()) return
        activeMood = null
        val i = index.coerceIn(0, list.size - 1)
        c.setMediaItems(list.map { it.toItem() }, i, resumeFor(list[i]) ?: 0L)
        c.prepare()
        c.play()
        if (list[index.coerceIn(0, list.size - 1)].isVideo) openFull = true
    }

    fun shuffleAll(list: List<Song>) {
        val c = controller ?: return
        if (list.isEmpty()) return
        val q = mixed(list, 5)
        c.shuffleModeEnabled = true
        play(q, Random.nextInt(q.size))
    }

    /** Verilen listeyi karıştırıp çalar (öneri parçalar araya karışmaz). */
    fun playShuffled(list: List<Song>) {
        val c = controller ?: return
        if (list.isEmpty()) return
        c.shuffleModeEnabled = true
        play(list, Random.nextInt(list.size))
    }

    fun toggle() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNext() }
    fun prev() {
        val c = controller ?: return
        if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPrevious()
    }

    fun seekTo(ms: Long) { controller?.seekTo(ms) }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun addNext(s: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) play(listOf(s), 0)
        else c.addMediaItem(c.currentMediaItemIndex + 1, s.toItem())
    }

    /** UI her ~400 ms çağırır: konumu günceller ve 30 sn dinlenince sayacı artırır. */
    fun tick() {
        val c = controller ?: return
        position = c.currentPosition
        duration = c.duration.let { if (it < 0) 0 else it }
        previewEnd?.let { if (position >= it) { c.pause(); previewEnd = null } }
        val cur = current ?: return
        val now = System.currentTimeMillis()
        val dt = (now - lastTickAt).coerceIn(0L, 1000L)
        lastTickAt = now
        if (c.isPlaying && !cur.remote) {
            // Gerçek dinleme süresi ve gün kaydı (rozetler için)
            listenPending += dt
            sessionMs += dt
            if (now - lastListenSave > 15_000L) {
                lastListenSave = now
                listenMs += listenPending
                listenPending = 0L
                Store.setListenMs(ctx, listenMs)
            }
            val today = LocalDate.now().toString()
            if (sessionMs >= 30_000L && markedDay != today) {
                markedDay = today
                Store.addDay(ctx, today)
                checkBadges()
            }
            if (cur.isLong && now - lastResumeSave > 5000L) {
                lastResumeSave = now
                saveResume(cur, position, duration)
            }
        }
        if (!counted && c.isPlaying && !cur.remote) {
            val need = if (duration > 0) min(30000L, duration / 2) else 30000L
            if (position >= need) {
                counted = true
                stats = Store.inc(ctx, cur.statKey)
                activeMood?.let { Store.bumpMood(ctx, it, cur.statKey) }
                checkBadges()
            }
        }
    }

    // ---- Hız ----
    private fun applySpeedFor(s: Song?) {
        val c = controller ?: return
        // Uzun içerikte (konuşma, sesli kitap) parça başına hatırlanır; müzik her parçada 1x'e döner.
        val v = if (s != null && s.isLong) (speeds[s.statKey] ?: 1f) else 1f
        if (c.playbackParameters.speed != v) c.setPlaybackSpeed(v)
        speed = v
    }

    fun setSpeed(v: Float) {
        val c = controller ?: return
        val x = v.coerceIn(0.75f, 2f)
        c.setPlaybackSpeed(x)
        speed = x
        current?.takeIf { it.isLong }?.let {
            if (x == 1f) speeds.remove(it.statKey) else speeds[it.statKey] = x
            Store.saveSpeeds(ctx, speeds)
        }
    }

    // ---- Kaldığın yerden devam ----
    private fun resumeFor(s: Song?): Long? {
        if (s == null || !s.isLong) return null
        val p = resume[s.statKey]?.first ?: return null
        return if (p > 5000L) p else null
    }

    private fun saveResume(s: Song, pos: Long, dur: Long) {
        val m = resume.toMutableMap()
        if (dur > 0 && pos >= dur - 10_000L) m.remove(s.statKey)
        else if (pos > 5000L) m[s.statKey] = Pair(pos, System.currentTimeMillis())
        else return
        resume = m
        Store.saveResume(ctx, m)
    }

    private fun saveResumeNow() {
        val c = controller ?: return
        val cur = current ?: return
        if (cur.isLong) saveResume(cur, c.currentPosition, c.duration.let { if (it < 0) 0 else it })
    }

    fun previewRange(startMs: Long, endMs: Long) {
        val c = controller ?: return
        c.seekTo(startMs)
        previewEnd = endMs
        c.play()
    }

    // ---- Rozetler ----
    fun totalHours(): Double {
        val est = songs.sumOf { count(it).toLong() * it.durationMs } / 3_600_000.0
        return maxOf(est, listenMs / 3_600_000.0)
    }

    private fun metrics(): Metrics {
        val played = songs.filter { count(it) > 0 }
        return Metrics(
            hours = totalHours(), plays = stats.values.sum(), streak = Badges.streak(Store.getDays(ctx)),
            distinct = stats.count { it.value > 0 },
            artists = played.map { it.artist }.filter { it.isNotBlank() }.distinct().size,
            likes = likes.size,
        )
    }

    fun metricsNow(): Metrics = metrics()

    fun checkBadges() {
        val m = metrics()
        streak = m.streak
        val now = Badges.earned(m)
        val old = Store.getEarned(ctx)
        if (old != null) {
            val fresh = Badges.all.filter { it.id in now && it.id !in old }
            if (fresh.isNotEmpty()) badgeBanner = "Yeni rozet: " + fresh.joinToString(", ") { it.title }
        }
        val union = (old ?: emptySet()) + now
        if (old == null || union != old) Store.setEarned(ctx, union)
        earned = union
    }

    fun toast(msg: String) { Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }

    // ---- Gezinme: sanatçı, albüm, klasör, benzer parçalar ----
    fun openBrowse(kind: Int, key: String) {
        browse = Browse(kind, key)
        goTab = 3
    }

    fun tracksOf(b: Browse): List<Song> = when (b.kind) {
        0 -> songs.filter { it.artist.equals(b.key, true) }
        1 -> songs.filter { it.album == b.key }
            .sortedWith(compareBy<Song>({ it.track == 0 }, { it.track }, { it.title.lowercase() }))
        2 -> songs.filter { it.folder == b.key }
        else -> songs.firstOrNull { it.mediaId == b.key }?.let { similarTo(it) } ?: emptyList()
    }

    /** Çalan parçaya benzer parçaları listeden çıkarır: sanatçı, albüm, klasör, hava (hareketli / hüzünlü / sakin), süre, dinlenme. */
    fun similarTo(s: Song, n: Int = 30): List<Song> {
        val a = s.artist.lowercase()
        val tone = LocalAi.tone(s)
        return songs.asSequence().filter { it.mediaId != s.mediaId }.map { o ->
            var sc = 0.0
            val oa = o.artist.lowercase()
            if (a.isNotEmpty() && oa.isNotEmpty()) {
                if (a == oa) sc += 5.0 else if (a.contains(oa) || oa.contains(a)) sc += 3.0
            }
            if (s.album.isNotEmpty() && o.album == s.album) sc += 4.0
            if (s.folder.isNotEmpty() && o.folder == s.folder) sc += 1.5
            if (tone != null && LocalAi.tone(o) == tone) sc += 2.5
            if (o.isVideo == s.isVideo) sc += 0.5
            if (abs(o.durationMs - s.durationMs) < 30_000L) sc += 0.5
            sc += Math.log(1.0 + count(o)) * 0.3
            o to sc
        }.filter { it.second >= 2.0 }.sortedByDescending { it.second }.take(n).map { it.first }.toList()
    }

    // ---- Sözleri elle düzenleme ----
    fun rawLyrics(): String = current?.let { Net.rawLyrics(ctx, it) } ?: ""

    fun saveLyricsText(text: String) {
        val s = current ?: return
        lyricsJob?.cancel()
        lyricsLoading = false
        lyrics = Net.saveLyrics(ctx, s, text)
    }

    fun refetchLyrics() {
        val s = current ?: return
        Net.deleteLyrics(ctx, s)
        loadLyrics()
    }

    // ---- Paylaşım ----
    fun shareNowPlaying() {
        val s = current ?: return
        val ly = lyrics
        val pos = position
        viewModelScope.launch {
            val f = ShareCard.make(ctx, s, ly, pos)
            if (f == null) toast("Kart oluşturulamadı.")
            else runCatching { ctx.startActivity(ShareCard.intent(ctx, f, s)) }
        }
    }

    // ---- Sözler ----
    private fun loadLyrics() {
        lyricsJob?.cancel()
        lyrics = null
        val s = current ?: return
        if (s.remote) return
        lyricsJob = viewModelScope.launch {
            lyricsLoading = true
            lyrics = withContext(Dispatchers.IO) { Net.lyrics(ctx, s) }
            lyricsLoading = false
        }
    }

    // ---- Öneriler ----
    fun count(s: Song) = stats[s.statKey] ?: 0

    fun topArtistNames(n: Int): List<String> {
        val score = HashMap<String, Int>()
        for (s in songs) {
            if (s.artist.isEmpty()) continue
            score[s.artist] = (score[s.artist] ?: 0) + count(s) * 100 + 1
        }
        return score.entries.sortedByDescending { it.value }.map { it.key }.take(n)
    }

    fun refreshReco(force: Boolean = false) {
        if (!isOnline()) { online = false; return }
        online = true
        if (!force && recoTracks.isNotEmpty() && System.currentTimeMillis() - lastReco < 30 * 60_000L) return
        // Beğendiklerin öne geçer: önce beğendiğin parçaların sanatçıları, sonra en çok dinlediklerin
        val names = (likes.map { it.artist.substringBefore(',').trim() }.filter { it.isNotEmpty() }.distinct().take(3) + topArtistNames(4)).distinct().take(6)
        if (names.isEmpty()) return
        recoJob?.cancel()
        recoJob = viewModelScope.launch {
            val own = ArrayList<Artist>()
            val rel = ArrayList<Artist>()
            withContext(Dispatchers.IO) {
                for (n in names) {
                    val a = Net.searchArtist(n) ?: continue
                    own.add(a)
                    rel.addAll(Net.related(a.id, 6))
                }
            }
            val ownIds = own.map { it.id }.toSet()
            val sim = rel.distinctBy { it.id }.filter { it.id !in ownIds }.take(14)
            mine = own
            similar = sim
            val tracks = withContext(Dispatchers.IO) { sim.take(10).flatMap { Net.topTracks(it.id).take(3) } }
            recoTracks = tracks.filter { !isLiked(it) }.distinctBy { it.mediaId }.shuffled()
            lastReco = System.currentTimeMillis()
        }
    }

    /** Listenin arasına her [every] parçada bir öneri parça serpiştirir. */
    fun mixed(list: List<Song>, every: Int = 4, extra: List<Song> = recoTracks): List<Song> {
        if (extra.isEmpty() || list.isEmpty()) return list
        val have = list.map { it.mediaId }.toSet()
        val pool = extra.filter { it.mediaId !in have }.toMutableList()
        val out = ArrayList<Song>()
        list.forEachIndexed { i, s ->
            out.add(s)
            if ((i + 1) % every == 0 && pool.isNotEmpty()) out.add(pool.removeAt(0))
        }
        return out
    }

    // ---- Beğenilenler ----
    fun isLiked(s: Song) = likes.any { it.mediaId == s.mediaId }

    fun toggleLike(s: Song) {
        likes = if (isLiked(s)) likes.filter { it.mediaId != s.mediaId } else likes + s
        Store.saveLikes(ctx, likes)
        checkBadges()
        lastReco = 0L // sonraki açılışta öneriler beğenilerine göre yenilenir
    }

    // ---- Yapay zekâ asistanı ----
    fun hasMic() = androidx.core.content.ContextCompat.checkSelfPermission(
        ctx, android.Manifest.permission.RECORD_AUDIO
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun speak(text: String, then: () -> Unit = {}) {
        if (voiceMuted) { then(); return }
        controller?.volume = 0.25f
        voice.speak(text) {
            controller?.volume = 1f
            then()
        }
    }

    fun setVoiceOn(on: Boolean) {
        voiceOn = on
        Store.setVoiceOn(ctx, on)
        if (on) { voiceNote = null; voice.startListening() } else voice.stopListening()
    }

    fun setTheme(mode: Int) {
        themeMode = mode
        Store.setTheme(ctx, mode)
        if (mode == THEME_SUN) refreshLocation()
    }

    fun setAccent(i: Int) {
        accent = i
        Store.setAccent(ctx, i)
    }

    fun setCoverTheme(v: Boolean) {
        coverTheme = v
        Store.setCoverTheme(ctx, v)
    }

    fun hasLocPerm(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Son bilinen konumu alır (yalnızca gün doğumu / batımı hesabı için, telefonda kalır). */
    fun refreshLocation() {
        if (!hasLocPerm()) return
        runCatching {
            val lm = ctx.getSystemService(LocationManager::class.java)
            var best: Location? = null
            for (p in lm.getProviders(true)) {
                val l = runCatching { lm.getLastKnownLocation(p) }.getOrNull() ?: continue
                if (best == null || l.time > best!!.time) best = l
            }
            val b = best
            if (b != null) {
                Store.setLoc(ctx, b.latitude, b.longitude)
                loc = Pair(b.latitude, b.longitude)
            } else if (Build.VERSION.SDK_INT >= 30) {
                val prov = when {
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    else -> null
                }
                prov?.let {
                    lm.getCurrentLocation(it, null, ContextCompat.getMainExecutor(ctx)) { l ->
                        if (l != null) {
                            Store.setLoc(ctx, l.latitude, l.longitude)
                            loc = Pair(l.latitude, l.longitude)
                        }
                    }
                }
            }
        }
    }

    fun isNightBySun(nowMs: Long): Boolean {
        val (la, lo) = loc ?: Sun.fallback()
        return Sun.isNight(la, lo, nowMs)
    }

    fun sunText(nowMs: Long): String {
        val (la, lo) = loc ?: Sun.fallback()
        val i = Sun.info(la, lo, nowMs)
        val f = java.text.SimpleDateFormat("HH:mm", java.util.Locale("tr"))
        val base = when (i.polar) {
            1 -> "Bugün güneş batmıyor"
            -1 -> "Bugün güneş doğmuyor"
            else -> "Gün doğumu ${f.format(java.util.Date(i.rise))} · gün batımı ${f.format(java.util.Date(i.set))}"
        }
        return if (loc == null) "$base (yaklaşık, konum alınamadı)" else base
    }

    fun saveName(n: String) {
        userName = n.trim()
        Store.setUserName(ctx, userName)
    }

    fun toggleMute() {
        voiceMuted = !voiceMuted
        Store.setMuted(ctx, voiceMuted)
        if (voiceMuted) voice.stopSpeaking()
    }

    fun onAppVisible() {
        if (themeMode == THEME_SUN) refreshLocation()
        if (!greeted) {
            greeted = true
            val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val hi = when (h) { in 5..11 -> "Günaydın"; in 12..17 -> "Merhaba"; in 18..22 -> "İyi akşamlar"; else -> "İyi geceler" }
            val msg = "$hi, Yırdaş'a hoş geldin. Bugün ruh halin nasıl? Ne dinlemek istersin?"
            chat = chat + ChatMsg(false, msg)
            speak(msg) { if (voiceOn && hasMic()) voice.startListening() }
        } else if (voiceOn && hasMic()) {
            voice.startListening()
        }
    }

    fun onAppHidden() {
        voice.stopListening()
        saveResumeNow()
        if (listenPending > 0) {
            listenMs += listenPending
            listenPending = 0L
            Store.setListenMs(ctx, listenMs)
        }
    }

    private fun handleHeard(raw: String) {
        val wakeRe = Regex("yırdaş|yirdaş|yırdas|yirdas|yırdaşım")
        var t = raw.trim().lowercase()
        val wake = wakeRe.containsMatchIn(t)
        t = wakeRe.replace(t, "").trim(' ', ',', '.', '!', '?')
        if (t.isBlank()) { if (wake) speak("Buyur, dinliyorum."); return }
        if (runCommand(raw, t)) return
        if (isPlaying && !wake) return // müzik çalarken sadece "Yırdaş, ..." diyenleri dinle
        if (t.isNotBlank()) ask(t)
    }

    private fun runCommand(raw: String, t: String): Boolean {
        val reply: String? = when (t) {
            "dur", "duraklat", "müziği durdur", "müziği kapat" -> { controller?.pause(); voice.stopSpeaking(); "" }
            "devam", "devam et", "oynat", "müziği aç" -> { controller?.play(); "" }
            "sonraki", "sonraki şarkı", "ileri", "geç", "şarkıyı geç" -> { next(); "" }
            "önceki", "önceki şarkı", "geri" -> { prev(); "" }
            "sesi kapat", "sessiz ol", "konuşma" -> { voiceMuted = true; Store.setMuted(ctx, true); voice.stopSpeaking(); "Tamam, sessizim." }
            "sesi aç", "konuş", "konuşabilirsin" -> { voiceMuted = false; Store.setMuted(ctx, false); "Tamam, tekrar konuşuyorum." }
            "dinlemeyi bırak", "mikrofonu kapat", "sohbeti kapat" -> { setVoiceOn(false); "Dinlemeyi kapattım. Mikrofon simgesine dokununca tekrar açarım." }
            "ne çalıyor", "hangi şarkı", "bu ne", "bu şarkı ne" ->
                current?.let { "${it.title}, ${it.artist.ifEmpty { "sanatçı bilinmiyor" }}" } ?: "Şu an bir şey çalmıyor."
            else -> null
        }
        if (reply == null) return false
        chat = chat + ChatMsg(true, raw.trim())
        if (reply.isNotEmpty()) {
            chat = chat + ChatMsg(false, reply)
            speak(reply)
        }
        return true
    }

    fun saveModel(m: String) {
        Store.setModel(ctx, m)
        aiModel = Store.getModel(ctx)
    }

    fun saveKey(k: String) {
        Store.setKey(ctx, k)
        apiKey = k.trim()
    }

    fun ask(text: String) {
        val t = text.trim()
        if (t.isEmpty() || aiBusy) return
        chat = chat + ChatMsg(true, t)
        if (apiKey.isBlank()) {
            // Anahtar yok: telefonun içindeki asistan çalışır (internetsiz, ücretsiz)
            val talk = LocalAi.chat(t)
            if (talk != null) {
                chat = chat + ChatMsg(false, talk)
                speak(talk)
                return
            }
            val r = LocalAi.plan(t, songs, { count(it) }, Store.moodMap(ctx))
            val queue = mixed(r.picks, 3)
            chat = chat + ChatMsg(false, r.reply + if (queue.isNotEmpty()) "\n\n▶ ${queue.size} parça çalıyor." else "")
            val spoken = r.reply.substringBefore(" Hangi parçayı")
            if (queue.isNotEmpty()) {
                speak(spoken) {
                    play(queue, 0)
                    activeMood = r.mood
                }
            } else speak(spoken)
            return
        }
        if (!isOnline()) {
            chat = chat + ChatMsg(false, "Yapay zekâ için internet lazım. Bağlanınca tekrar dene.")
            return
        }
        aiBusy = true
        val snap = songs.sortedWith(compareByDescending<Song> { count(it) }.thenBy { it.title.lowercase() }).take(500)
        val lib = snap.mapIndexed { i, s ->
            "$i|${s.title.replace('|', ' ')}|${s.artist.replace('|', ' ')}|${count(s)}"
        }.joinToString("\n")
        val msgs = ArrayList<Pair<String, String>>(history.takeLast(6))
        while (msgs.isNotEmpty() && msgs[0].first != "user") msgs.removeAt(0)
        msgs.add("user" to t)
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { Net.ask(apiKey, aiModel, Ai.system(lib), msgs) }
            val raw = r.text.orEmpty()
            val plan = Ai.parse(raw)
            if (plan == null) {
                chat = chat + ChatMsg(false, r.error ?: "Cevabı anlayamadım, bir daha dener misin?")
                aiBusy = false
                return@launch
            }
            history.add("user" to t)
            history.add("assistant" to raw)
            val local = plan.picks.distinct().mapNotNull { snap.getOrNull(it) }.take(25)
            val found = withContext(Dispatchers.IO) {
                plan.discover.take(4).flatMap { n ->
                    val a = Net.searchArtist(n)
                    if (a == null) emptyList() else Net.topTracks(a.id).take(3)
                }
            }.distinctBy { it.mediaId }
            val queue = if (local.isNotEmpty()) mixed(local, 3, found) else found
            var line = plan.reply
            if (queue.isNotEmpty()) line += "\n\n▶ ${queue.size} parça çalıyor."
            chat = chat + ChatMsg(false, line)
            aiBusy = false
            speak(plan.reply) { if (queue.isNotEmpty()) play(queue, 0) }
        }
    }

    fun playStation(a: Artist) {
        viewModelScope.launch {
            val tracks = withContext(Dispatchers.IO) { Net.topTracks(a.id) }
            if (tracks.isNotEmpty()) play(tracks, 0)
        }
    }

    override fun onCleared() {
        voice.release()
        ctx.contentResolver.unregisterContentObserver(mediaObserver)
        controller?.removeListener(listener)
        controller?.release()
        super.onCleared()
    }
}
