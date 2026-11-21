package app.cepcalar

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.LruCache
import android.util.Size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Gün doğumu / batımı (konuma göre otomatik tema)
// ---------------------------------------------------------------------------

/** NOAA "sunrise equation" ile gün doğumu ve batımı. İnternet gerekmez. */
object Sun {
    /** [polar]: 0 normal gün, 1 gün hiç batmıyor, -1 gün hiç doğmuyor. */
    class Info(val rise: Long, val set: Long, val polar: Int)

    private const val J1970 = 2440587.5

    private fun rad(d: Double) = Math.toRadians(d)

    fun info(lat: Double, lon: Double, nowMs: Long): Info {
        val jNow = nowMs / 86_400_000.0 + J1970
        val n0 = ceil(jNow - 2451545.0 + 0.0008)
        var best: Info? = null
        var bestDist = Double.MAX_VALUE
        // Öğle vakti şu ana en yakın olan günü seç: gece yarısı sınırında yanlış güne düşmeyelim.
        for (dn in -1..1) {
            val js = n0 + dn - lon / 360.0
            val m = (357.5291 + 0.98560028 * js) % 360.0
            val mr = rad(m)
            val c = 1.9148 * sin(mr) + 0.02 * sin(2 * mr) + 0.0003 * sin(3 * mr)
            val lam = (m + c + 180.0 + 102.9372) % 360.0
            val lr = rad(lam)
            val transit = 2451545.0 + js + 0.0053 * sin(mr) - 0.0069 * sin(2 * lr)
            val sinD = sin(lr) * sin(rad(23.4397))
            val cosD = cos(asin(sinD))
            val cosW = (sin(rad(-0.833)) - sin(rad(lat)) * sinD) / (cos(rad(lat)) * cosD)
            val tMs = ((transit - J1970) * 86_400_000.0).toLong()
            val dist = abs(tMs - nowMs).toDouble()
            if (dist < bestDist) {
                bestDist = dist
                best = when {
                    cosW > 1.0 -> Info(tMs, tMs, -1)
                    cosW < -1.0 -> Info(tMs, tMs, 1)
                    else -> {
                        val w = Math.toDegrees(acos(cosW)) / 360.0
                        Info(((transit - w - J1970) * 86_400_000.0).toLong(), ((transit + w - J1970) * 86_400_000.0).toLong(), 0)
                    }
                }
            }
        }
        return best!!
    }

    fun isNight(lat: Double, lon: Double, nowMs: Long): Boolean {
        val i = info(lat, lon, nowMs)
        return when (i.polar) {
            1 -> false
            -1 -> true
            else -> nowMs < i.rise || nowMs > i.set
        }
    }

    /** Konum yoksa saat diliminden yaklaşık bir nokta (Türkiye enlemi). */
    fun fallback(): Pair<Double, Double> {
        val offH = java.util.TimeZone.getDefault().rawOffset / 3_600_000.0
        return Pair(39.0, offH * 15.0)
    }
}

// ---------------------------------------------------------------------------
// Dinleme rozetleri
// ---------------------------------------------------------------------------

class Metrics(
    val hours: Double, val plays: Int, val streak: Int,
    val distinct: Int, val artists: Int, val likes: Int,
)

class Badge(
    val id: String, val title: String, val desc: String, val icon: ImageVector,
    val goal: Int, val unit: String, val value: (Metrics) -> Double,
)

object Badges {
    val all: List<Badge> = listOf(
        Badge("first", "İlk Adım", "İlk parçanı dinledin", Icons.Filled.PlayArrow, 1, "dinleme") { it.plays.toDouble() },
        Badge("h1", "İlk Saat", "1 saat dinledin", Icons.Filled.Headphones, 1, "saat") { it.hours },
        Badge("h10", "10 Saat", "10 saat dinledin", Icons.Filled.Headphones, 10, "saat") { it.hours },
        Badge("h50", "50 Saat", "50 saat dinledin", Icons.Filled.Headphones, 50, "saat") { it.hours },
        Badge("h100", "100 Saat", "100 saat dinledin", Icons.Filled.Headphones, 100, "saat") { it.hours },
        Badge("s3", "3 Gün", "3 gün üst üste dinledin", Icons.Filled.Whatshot, 3, "gün") { it.streak.toDouble() },
        Badge("s7", "7 Gün", "7 gün üst üste dinledin", Icons.Filled.Whatshot, 7, "gün") { it.streak.toDouble() },
        Badge("s30", "30 Gün", "30 gün üst üste dinledin", Icons.Filled.Whatshot, 30, "gün") { it.streak.toDouble() },
        Badge("p100", "100 Dinleme", "100 parça dinledin", Icons.Filled.EmojiEvents, 100, "dinleme") { it.plays.toDouble() },
        Badge("p1000", "Bin Dinleme", "1000 parça dinledin", Icons.Filled.EmojiEvents, 1000, "dinleme") { it.plays.toDouble() },
        Badge("d25", "Meraklı", "25 farklı parça dinledin", Icons.Filled.LibraryMusic, 25, "parça") { it.distinct.toDouble() },
        Badge("d100", "Koleksiyoncu", "100 farklı parça dinledin", Icons.Filled.LibraryMusic, 100, "parça") { it.distinct.toDouble() },
        Badge("a10", "Çok Yönlü", "10 farklı sanatçı dinledin", Icons.Filled.Person, 10, "sanatçı") { it.artists.toDouble() },
        Badge("l10", "Kâşif", "10 öneriyi beğendin", Icons.Filled.Favorite, 10, "beğeni") { it.likes.toDouble() },
    )

    fun earned(m: Metrics): Set<String> = all.filter { it.value(m) >= it.goal }.map { it.id }.toSet()

    /** Bugünden geriye doğru kesintisiz kaç gün dinlendi. Bugün henüz dinlenmediyse dünden sayar. */
    fun streak(days: Set<String>, today: LocalDate = LocalDate.now()): Int {
        var d = if (today.toString() in days) today else today.minusDays(1)
        var n = 0
        while (d.toString() in days) { n++; d = d.minusDays(1) }
        return n
    }
}

// ---------------------------------------------------------------------------
// Kapak: bitmap yükleme, baskın renk
// ---------------------------------------------------------------------------

object Cover {
    private val tints = LruCache<String, Int>(120)

    /** Yerel parçada gömülü kapak, öneri parçada internetten kapak. Yoksa null. */
    suspend fun bitmap(ctx: Context, s: Song, px: Int = 400): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            if (s.artUrl != null) {
                val r = ctx.imageLoader.execute(
                    ImageRequest.Builder(ctx).data(s.artUrl).size(px).allowHardware(false).build()
                )
                (r as? SuccessResult)?.drawable?.toBitmap()
            } else if (!s.remote) {
                ctx.contentResolver.loadThumbnail(s.uri, Size(px, px), null)
            } else null
        }.getOrNull()
    }

    /** Görselin baskın rengi; oynatıcı arka planına uygun koyulukta (ARGB). Renk yoksa gri ton döner. */
    fun dominant(b: Bitmap): Int {
        val n = 32
        val sm = Bitmap.createScaledBitmap(b, n, n, true)
        val px = IntArray(n * n)
        sm.getPixels(px, 0, n, 0, 0, n, n)
        val hsv = FloatArray(3)
        val w = DoubleArray(12)
        val r = DoubleArray(12)
        val g = DoubleArray(12)
        val bl = DoubleArray(12)
        var total = 0.0
        for (p in px) {
            Color.colorToHSV(p, hsv)
            if (hsv[2] < 0.15f || hsv[1] < 0.12f) continue
            val wt = (hsv[1] * hsv[2]).toDouble()
            val bin = (hsv[0] / 30f).toInt().coerceIn(0, 11)
            w[bin] += wt
            r[bin] += Color.red(p) * wt
            g[bin] += Color.green(p) * wt
            bl[bin] += Color.blue(p) * wt
            total += wt
        }
        if (total < 20.0) return Color.HSVToColor(floatArrayOf(0f, 0f, 0.26f))
        var best = 0
        for (i in 1 until 12) if (w[i] > w[best]) best = i
        val c = Color.rgb((r[best] / w[best]).toInt(), (g[best] / w[best]).toInt(), (bl[best] / w[best]).toInt())
        Color.colorToHSV(c, hsv)
        hsv[1] = hsv[1].coerceIn(0.35f, 0.85f)
        hsv[2] = 0.40f
        return Color.HSVToColor(hsv)
    }

    /** Parçanın kapağından tema rengi (ARGB). Kapak bulunamazsa null. */
    suspend fun tint(ctx: Context, s: Song): Int? {
        tints.get(s.mediaId)?.let { return it }
        val bmp = bitmap(ctx, s, 200)
        val c = bmp?.let { withContext(Dispatchers.Default) { dominant(it) } }
        if (c != null) tints.put(s.mediaId, c) // bulunamayanı önbelleğe alma: internet sonradan gelebilir
        return c
    }
}

// ---------------------------------------------------------------------------
// Şimdi çalanı paylaş: kapak + söz içeren görsel kart
// ---------------------------------------------------------------------------

object ShareCard {
    private const val W = 1080
    private const val H = 1350

    private fun pickLines(ly: Lyrics?, posMs: Long): Pair<List<String>, Int> {
        if (ly == null) return Pair(emptyList(), -1)
        val lines = ly.lines
        if (ly.synced) {
            val active = lines.indexOfLast { it.timeMs <= posMs + 200 }.coerceAtLeast(0)
            val out = ArrayList<String>()
            var act = 0
            var i = (active - 1).coerceAtLeast(0)
            while (i < lines.size && out.size < 4) {
                val t = lines[i].text
                if (t.isNotBlank()) {
                    if (i == active) act = out.size
                    out.add(t)
                } else if (i == active && out.isNotEmpty()) {
                    // boş satırdaysa sonraki dolu satırı vurgula
                    act = out.size
                }
                i++
            }
            return Pair(out, act)
        }
        return Pair(lines.map { it.text }.filter { it.isNotBlank() }.take(4), -1)
    }

    private fun layout(text: String, paint: TextPaint, width: Int, maxLines: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END).build()

    suspend fun make(ctx: Context, s: Song, ly: Lyrics?, posMs: Long): File? = withContext(Dispatchers.Default) {
        runCatching {
            val art = Cover.bitmap(ctx, s, 800)
            val tint = art?.let { Cover.dominant(it) }
                ?: Color.HSVToColor(floatArrayOf((abs(s.title.hashCode()) % 360).toFloat(), 0.45f, 0.38f))
            val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
            val cv = Canvas(bmp)
            val bg = Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, H.toFloat(), tint, Color.parseColor("#0A0A0C"), Shader.TileMode.CLAMP)
            }
            cv.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bg)

            // Kapak
            val cs = 640f
            val cx = (W - cs) / 2f
            val cy = 100f
            val rect = RectF(cx, cy, cx + cs, cy + cs)
            cv.drawRoundRect(RectF(cx, cy + 18, cx + cs, cy + cs + 18), 40f, 40f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55000000 })
            val clip = Path().apply { addRoundRect(rect, 40f, 40f, Path.Direction.CW) }
            cv.save()
            cv.clipPath(clip)
            if (art != null) {
                val side = minOf(art.width, art.height)
                val src = Rect((art.width - side) / 2, (art.height - side) / 2, (art.width + side) / 2, (art.height + side) / 2)
                cv.drawBitmap(art, src, rect, Paint(Paint.FILTER_BITMAP_FLAG))
            } else {
                cv.drawRect(rect, Paint().apply { color = Color.argb(255, 255, 255, 255); alpha = 40 })
                val lp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 260f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
                val ch = (s.title.trim().firstOrNull()?.uppercase() ?: "♪")
                cv.drawText(ch, rect.centerX(), rect.centerY() + 90f, lp)
            }
            cv.restore()

            val textW = 900
            val tx = (W - textW) / 2f
            var y = cy + cs + 64f

            // Başlık ve sanatçı
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 56f; typeface = Typeface.DEFAULT_BOLD }
            val tl = layout(s.title, tp, textW, 2)
            cv.save(); cv.translate(tx, y); tl.draw(cv); cv.restore()
            y += tl.height + 10f
            val ap = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 190; textSize = 38f }
            val artist = s.artist.ifEmpty { "Sanatçı bilinmiyor" }
            val al = layout(artist, ap, textW, 1)
            cv.save(); cv.translate(tx, y); al.draw(cv); cv.restore()
            y += al.height + 44f

            // Sözler
            val (lines, act) = pickLines(ly, posMs)
            val limit = 1215f
            for ((i, line) in lines.withIndex()) {
                val strong = act < 0 || i == act
                val lp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE; alpha = if (strong) 255 else 130; textSize = 42f
                    typeface = if (strong) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                }
                val ll = layout(line, lp, textW, 2)
                if (y + ll.height > limit) break
                cv.save(); cv.translate(tx, y); ll.draw(cv); cv.restore()
                y += ll.height + 16f
            }

            // Alt imza: logo + Yırdaş
            val fp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 210; textSize = 38f; typeface = Typeface.DEFAULT_BOLD }
            val name = "Yırdaş"
            val nameW = fp.measureText(name)
            val logoS = 60
            val gx = (W - (logoS + 16 + nameW)) / 2f
            val gy = 1246
            ContextCompat.getDrawable(ctx, R.drawable.ic_logo)?.mutate()?.let { d ->
                d.setTint(Color.WHITE)
                d.setBounds(gx.toInt(), gy, gx.toInt() + logoS, gy + logoS)
                d.draw(cv)
            }
            cv.drawText(name, gx + logoS + 16f, gy + 43f, fp)

            val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val f = File(dir, "yirdas_${System.currentTimeMillis()}.png")
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            f
        }.getOrNull()
    }

    fun intent(ctx: Context, f: File, s: Song): Intent {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "${s.title}${if (s.artist.isNotEmpty()) " - ${s.artist}" else ""} · Yırdaş")
            clipData = android.content.ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Şimdi çalanı paylaş").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
