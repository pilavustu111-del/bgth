package app.cepcalar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Tema modları: 0 Otomatik (telefonun teması), 1 Gündüz, 2 Gece */
const val THEME_AUTO = 0
const val THEME_LIGHT = 1
const val THEME_DARK = 2
/** Konuma göre gün doğumunda açık, gün batımında koyu tema */
const val THEME_SUN = 3

class Pal(
    val dark: Boolean,
    val bg: Color,
    val bg2: Color,
    val card: Color,
    val chip: Color,
    val accent: Color,
    val muted: Color,
    val ink: Color,
    val edge: Color,
    val text: Color,
    /** Her zaman koyu olan oynatıcı üstündeki vurgu rengi */
    val playerAcc: Color = accent,
)

val DarkPal = Pal(
    dark = true,
    bg = Color(0xFF0A0A0C), bg2 = Color(0xFF16141A), card = Color(0xFF161518), chip = Color(0xFF232126),
    accent = Color(0xFFE5484D), muted = Color(0xFFA9A3A0), ink = Color.White,
    edge = Color(0x2EFFFFFF), text = Color(0xFFF3EEEA),
)

val LightPal = Pal(
    dark = false,
    bg = Color(0xFFF4E9DE), bg2 = Color(0xFFFBF3EA), card = Color(0xFFFBF4EC), chip = Color(0xFFEBDDCD),
    accent = Color(0xFFA32E22), muted = Color(0xFF7C6A5C), ink = Color(0xFFFFF8F0),
    edge = Color(0x338B5A3C), text = Color(0xFF2A1A12),
    playerAcc = Color(0xFFE5484D),
)

/** Vurgu rengi seçenekleri: ad, koyu tema rengi, açık tema rengi, üstündeki yazı rengi (koyu / açık tema). */
class AccentOpt(val name: String, val dark: Color, val light: Color, val inkDark: Color, val inkLight: Color)

val ACCENTS = listOf(
    AccentOpt("Kırmızı", Color(0xFFE5484D), Color(0xFFA32E22), Color.White, Color(0xFFFFF8F0)),
    AccentOpt("Altın", Color(0xFFE8B04A), Color(0xFF9A6B00), Color(0xFF1A1208), Color(0xFFFFF8F0)),
    AccentOpt("Mavi", Color(0xFF4C9AFF), Color(0xFF1F5FBF), Color(0xFF07121F), Color(0xFFFFFFFF)),
    AccentOpt("Yeşil", Color(0xFF3DBB7A), Color(0xFF1E7A4A), Color(0xFF04140B), Color(0xFFFFFFFF)),
)

fun palFor(dark: Boolean, accent: Int): Pal {
    val o = ACCENTS.getOrElse(accent) { ACCENTS[0] }
    val b = if (dark) DarkPal else LightPal
    return Pal(
        dark, b.bg, b.bg2, b.card, b.chip,
        accent = if (dark) o.dark else o.light, muted = b.muted,
        ink = if (dark) o.inkDark else o.inkLight, edge = b.edge, text = b.text,
        playerAcc = o.dark,
    )
}

val LocalPal = staticCompositionLocalOf { DarkPal }

// Tema duyarlı renkler (Composable içinde okunur)
val BG: Color @Composable get() = LocalPal.current.bg
val CARD: Color @Composable get() = LocalPal.current.card
val CHIP: Color @Composable get() = LocalPal.current.chip
val ACC: Color @Composable get() = LocalPal.current.accent
val MUTED: Color @Composable get() = LocalPal.current.muted
val INK: Color @Composable get() = LocalPal.current.ink
val EDGE: Color @Composable get() = LocalPal.current.edge
val TXT: Color @Composable get() = LocalPal.current.text

// Her zaman koyu kalan yüzeyler (oynatıcı, resim üstü yazılar)
val DARK_BG = Color(0xFF0A0A0C)
val DARK_INK = Color(0xFF1A1208)
val PLAYER_ACC: Color @Composable get() = LocalPal.current.playerAcc
val IMG_MUTED = Color(0xFFD6CEC6)

@Composable
fun LogoMark(size: Dp, tint: Color = TXT, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_logo), contentDescription = "Yırdaş",
        tint = tint, modifier = modifier.size(size)
    )
}

private fun DrawScope.ridge(w: Float, h: Float, color: Color, pts: List<Pair<Float, Float>>) {
    val p = Path()
    p.moveTo(0f, h)
    for (pt in pts) p.lineTo(pt.first * w, pt.second * h)
    p.lineTo(w, h)
    p.close()
    drawPath(p, color)
}

private fun DrawScope.drawDay(w: Float, h: Float) {
    drawRect(
        Brush.verticalGradient(
            listOf(Color(0xFF86AEE0), Color(0xFFB7C8E8), Color(0xFFF3CBAA), Color(0xFFF7DEC8)), 0f, h
        )
    )
    val sun = Offset(w * 0.42f, h * 0.60f)
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFFFFF1D0), Color(0x00FFE2B0)), center = sun, radius = h * 0.38f),
        radius = h * 0.38f, center = sun
    )
    drawCircle(color = Color(0xFFFFF7E6), radius = h * 0.04f, center = sun)
    ridge(w, h, Color(0xFFC9CCE0), listOf(0f to 0.78f, 0.12f to 0.64f, 0.22f to 0.72f, 0.33f to 0.58f, 0.45f to 0.74f, 0.58f to 0.62f, 0.72f to 0.76f, 1f to 0.68f))
    ridge(w, h, Color(0xFFA7A9C4), listOf(0f to 0.86f, 0.15f to 0.76f, 0.28f to 0.84f, 0.42f to 0.74f, 0.6f to 0.86f, 0.8f to 0.78f, 1f to 0.84f))
    ridge(w, h, Color(0xFF8C8A8A), listOf(0.56f to 0.84f, 0.66f to 0.76f, 0.70f to 0.735f, 0.97f to 0.735f, 1f to 0.77f))
}

private fun DrawScope.drawNight(w: Float, h: Float) {
    drawRect(
        Brush.verticalGradient(
            listOf(Color(0xFF040406), Color(0xFF101218), Color(0xFF1C1E27)), 0f, h
        )
    )
    val stars = listOf(
        0.08f to 0.30f, 0.20f to 0.12f, 0.33f to 0.22f, 0.45f to 0.08f, 0.52f to 0.30f,
        0.14f to 0.46f, 0.38f to 0.42f, 0.90f to 0.10f, 0.60f to 0.14f,
    )
    for (s in stars) drawCircle(Color.White.copy(alpha = 0.55f), radius = 1.4.dp.toPx(), center = Offset(s.first * w, s.second * h))
    val c = Offset(w * 0.70f, h * 0.30f)
    val r = h * 0.15f
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0x33DDE4F2), Color(0x00DDE4F2)), center = c, radius = r * 2.2f),
        radius = r * 2.2f, center = c
    )
    val a = Path().apply { addOval(Rect(c, r)) }
    val b = Path().apply { addOval(Rect(Offset(c.x + r * 0.42f, c.y - r * 0.10f), r * 0.90f)) }
    drawPath(Path.combine(PathOperation.Difference, a, b), Color(0xFFE6EAF2))
    ridge(w, h, Color(0xFF262833), listOf(0f to 0.78f, 0.12f to 0.64f, 0.22f to 0.72f, 0.33f to 0.58f, 0.45f to 0.74f, 0.58f to 0.62f, 0.72f to 0.76f, 1f to 0.68f))
    ridge(w, h, Color(0xFF16171E), listOf(0f to 0.86f, 0.15f to 0.76f, 0.28f to 0.84f, 0.42f to 0.74f, 0.6f to 0.86f, 0.8f to 0.78f, 1f to 0.84f))
    ridge(w, h, Color(0xFF0C0C10), listOf(0.56f to 0.84f, 0.66f to 0.76f, 0.70f to 0.735f, 0.97f to 0.735f, 1f to 0.77f))
}

/** Ana sayfa üstündeki manzara: gündüz gün batımı / gece ay ışığı, kayada uluyan bozkurt. */
@Composable
fun HeroScene(modifier: Modifier = Modifier) {
    val pal = LocalPal.current
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            if (pal.dark) drawNight(size.width, size.height) else drawDay(size.width, size.height)
        }
        Image(
            painter = painterResource(R.drawable.ic_wolf), contentDescription = null,
            colorFilter = ColorFilter.tint(if (pal.dark) Color(0xFF0B0B0F) else Color(0xFF3B2F2B)),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = 108.dp).size(150.dp)
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(0.70f to pal.bg.copy(alpha = 0f), 1f to pal.bg)
            )
        )
    }
}

@Composable
fun Waveform(active: Boolean, color: Color, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "wave")
    val t by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "w"
    )
    Canvas(modifier) {
        val n = 26
        val gap = size.width / n
        for (i in 0 until n) {
            val env = 1f - abs(i - n / 2f) / (n / 2f)
            val base = 0.15f + 0.85f * env * abs(sin(i * 0.9f))
            val k = if (active) 0.5f + 0.5f * sin(2f * PI.toFloat() * t + i * 0.7f) else 1f
            val hh = size.height * base * (0.35f + 0.65f * k)
            val x = i * gap + gap / 2f
            drawLine(
                color, Offset(x, size.height / 2f - hh / 2f), Offset(x, size.height / 2f + hh / 2f),
                strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
            )
        }
    }
}
