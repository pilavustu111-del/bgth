@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.cepcalar

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Parçadan bir bölümü yeniden kodlamadan keser ve Zil sesleri klasörüne kaydeder.
 * MP3 (kare kare kopya), AAC/M4A (MP4 kabı) ve Opus (OGG kabı) desteklenir. Klipte sesi alınır.
 */
object Trim {
    class Out(val uri: Uri?, val error: String?)

    private val UNSUPPORTED =
        "Bu ses biçimi kırpılamıyor. MP3, AAC/M4A ve Opus dosyaları desteklenir."

    fun cut(ctx: Context, s: Song, startMs: Long, endMs: Long): Out {
        if (endMs - startMs < 1000) return Out(null, "Seçilen bölüm en az 1 saniye olmalı.")
        val tmp = File(ctx.cacheDir, "trim_tmp.bin")
        val ex = MediaExtractor()
        try {
            ex.setDataSource(ctx, s.uri, null)
            var idx = -1
            var fmt: MediaFormat? = null
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                if ((f.getString(MediaFormat.KEY_MIME) ?: "").startsWith("audio/")) { idx = i; fmt = f; break }
            }
            if (idx < 0 || fmt == null) return Out(null, "Dosyada ses kanalı bulunamadı.")
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
            val kind: Int
            val ext: String
            val outMime: String
            when (mime) {
                "audio/mpeg" -> { kind = 0; ext = "mp3"; outMime = "audio/mpeg" }
                "audio/mp4a-latm" -> { kind = 1; ext = "m4a"; outMime = "audio/mp4" }
                "audio/opus" -> { kind = 2; ext = "ogg"; outMime = "audio/ogg" }
                else -> return Out(null, UNSUPPORTED)
            }
            ex.selectTrack(idx)
            ex.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val endUs = endMs * 1000
            val maxIn = if (fmt.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) fmt.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 0
            val buf = ByteBuffer.allocate(maxOf(maxIn, 262_144))
            var written = 0

            if (kind == 0) {
                val arr = ByteArray(buf.capacity())
                tmp.outputStream().use { o ->
                    while (true) {
                        val t = ex.sampleTime
                        if (t < 0 || t > endUs) break
                        buf.clear()
                        val n = ex.readSampleData(buf, 0)
                        if (n < 0) break
                        buf.get(arr, 0, n)
                        o.write(arr, 0, n)
                        written++
                        ex.advance()
                    }
                }
            } else {
                val muxer = MediaMuxer(
                    tmp.absolutePath,
                    if (kind == 1) MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 else MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG
                )
                try {
                    val track = muxer.addTrack(fmt)
                    muxer.start()
                    val info = MediaCodec.BufferInfo()
                    var base = -1L
                    while (true) {
                        val t = ex.sampleTime
                        if (t < 0 || t > endUs) break
                        buf.clear()
                        val n = ex.readSampleData(buf, 0)
                        if (n < 0) break
                        if (base < 0) base = t
                        val flags = if (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                        info.set(0, n, t - base, flags)
                        muxer.writeSampleData(track, buf, info)
                        written++
                        ex.advance()
                    }
                    if (written > 0) muxer.stop()
                } finally {
                    runCatching { muxer.release() }
                }
            }
            if (written == 0) return Out(null, "Seçilen aralıkta ses bulunamadı.")

            val safe = s.title.replace(Regex("[\\\\/:*?\"<>|]"), "").trim().ifEmpty { "Zil sesi" }
            val cr = ctx.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "$safe (zil).$ext")
                put(MediaStore.Audio.Media.TITLE, "$safe (zil)")
                put(MediaStore.Audio.Media.MIME_TYPE, outMime)
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
                put(MediaStore.Audio.Media.IS_RINGTONE, 1)
                put(MediaStore.Audio.Media.IS_MUSIC, 0)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = cr.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: return Out(null, "Dosya oluşturulamadı.")
            try {
                cr.openOutputStream(uri)?.use { o -> tmp.inputStream().use { it.copyTo(o) } }
                    ?: throw IllegalStateException("yazılamadı")
                cr.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                runCatching { cr.delete(uri, null, null) }
                return Out(null, "Kaydedilemedi: ${e.message ?: "bilinmeyen hata"}")
            }
            return Out(uri, null)
        } catch (e: Exception) {
            return Out(null, "Kırpılamadı: ${e.message ?: "bilinmeyen hata"}")
        } finally {
            runCatching { ex.release() }
            tmp.delete()
        }
    }

    /** Telefonun zil sesi yapar. İzin yoksa sistemin izin ekranını açar ve false döner. */
    fun setAsRingtone(ctx: Context, uri: Uri): Boolean {
        if (Settings.System.canWrite(ctx)) {
            return runCatching {
                RingtoneManager.setActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_RINGTONE, uri)
            }.isSuccess
        }
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        return false
    }
}

@Composable
fun TrimDialog(vm: MusicVM, s: Song, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val totalMs = (if (vm.duration > 0) vm.duration else s.durationMs).coerceAtLeast(2000L)
    val totalS = totalMs / 1000f
    val startInit = (vm.position / 1000f).coerceIn(0f, (totalS - 5f).coerceAtLeast(0f))
    var range by remember { mutableStateOf(startInit..minOf(totalS, startInit + 30f)) }
    var busy by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<Uri?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }

    val startMs = (range.start * 1000).toLong()
    val endMs = (range.endInclusive * 1000).toLong()

    fun close() {
        vm.previewEnd = null
        onClose()
    }

    AlertDialog(
        onDismissRequest = { if (!busy) close() },
        containerColor = CARD, titleContentColor = TXT, textContentColor = TXT,
        title = { Text(if (saved == null) "Kırp ve zil sesi yap" else "Zil sesi hazır", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.title, color = MUTED, fontSize = 14.sp, maxLines = 1)
                if (saved == null) {
                    RangeSlider(
                        value = range, onValueChange = { range = it }, valueRange = 0f..totalS,
                        colors = SliderDefaults.colors(thumbColor = ACC, activeTrackColor = ACC, inactiveTrackColor = EDGE)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(fmt(startMs), color = TXT, fontSize = 14.sp)
                        Text("${(endMs - startMs) / 1000} sn", color = ACC, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(fmt(endMs), color = TXT, fontSize = 14.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            val p = (vm.position / 1000f).coerceIn(0f, range.endInclusive - 1f)
                            range = p..range.endInclusive
                        }) { Text("Başlangıç = şimdi", color = ACC, fontSize = 13.sp) }
                        TextButton(onClick = {
                            val p = (vm.position / 1000f).coerceIn(range.start + 1f, totalS)
                            range = range.start..p
                        }) { Text("Bitiş = şimdi", color = ACC, fontSize = 13.sp) }
                    }
                    TextButton(onClick = { vm.previewRange(startMs, endMs) }) {
                        Text("▶ Seçili bölümü dinle", color = ACC)
                    }
                } else {
                    Text("Zil sesleri klasörüne kaydedildi. Telefon ayarlarındaki zil sesi listesinde görünür.", color = TXT, fontSize = 14.sp)
                }
                msg?.let { Text(it, color = MUTED, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
                if (busy) Text("Kesiliyor…", color = MUTED, fontSize = 13.sp)
            }
        },
        confirmButton = {
            if (saved == null) {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    msg = null
                    vm.previewEnd = null
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { Trim.cut(ctx, s, startMs, endMs) }
                        busy = false
                        saved = r.uri
                        msg = r.error
                    }
                }) { Text("Kaydet", color = ACC, fontWeight = FontWeight.Bold) }
            } else {
                TextButton(onClick = {
                    val ok = Trim.setAsRingtone(ctx, saved!!)
                    msg = if (ok) "Telefon zili olarak ayarlandı."
                    else "İzin ekranı açıldı. İzni verip geri dönünce tekrar dokun."
                }) { Text("Telefon zili yap", color = ACC, fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = { close() }) { Text(if (saved == null) "Vazgeç" else "Tamam", color = MUTED) }
        },
    )
}
