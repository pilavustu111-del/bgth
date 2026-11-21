@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.cepcalar

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsGroup(title: String) {
    Text(
        title, color = ACC, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 10.dp)
    )
}

@Composable
fun ThemeOption(selected: Boolean, title: String, sub: String, icon: ImageVector, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clip(shape)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) ACC else EDGE, shape)
            .background(CARD).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(if (selected) ACC else CHIP),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = if (selected) INK else MUTED) }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, color = TXT, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = MUTED, fontSize = 13.sp)
        }
        Box(
            Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (selected) ACC else MUTED, CircleShape),
            contentAlignment = Alignment.Center
        ) { if (selected) Box(Modifier.size(11.dp).clip(CircleShape).background(ACC)) }
    }
}

@Composable
fun GeminiKeySection(vm: MusicVM, onDone: () -> Unit) {
    var keyDraft by rememberSaveable { mutableStateOf("") }
    var modelDraft by rememberSaveable { mutableStateOf(vm.aiModel) }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Anahtarsız da çalışırım: telefonun içindeki asistan internetsiz ve ücretsiz ruh haline göre şarkı seçer, " +
                "dinledikçe zevkini öğrenir. Daha akıllı cevap istersen isteğe bağlı olarak ücretsiz Google Gemini " +
                "anahtarı ekleyebilirsin (aistudio.google.com/apikey).",
            color = TXT, fontSize = 14.sp
        )
        TextField(
            value = keyDraft, onValueChange = { keyDraft = it }, singleLine = true,
            placeholder = { Text(if (vm.apiKey.isBlank()) "AIza..." else "Yeni anahtar") },
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(8.dp), colors = fieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = modelDraft, onValueChange = { modelDraft = it }, singleLine = true,
            label = { Text("Model (varsayılanı bırakabilirsin)") },
            shape = RoundedCornerShape(8.dp), colors = fieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    vm.saveModel(modelDraft)
                    if (keyDraft.isNotBlank()) vm.saveKey(keyDraft)
                    if (keyDraft.isNotBlank() || vm.apiKey.isNotBlank()) { keyDraft = ""; onDone() }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ACC, contentColor = INK)
            ) { Text("Kaydet") }
            if (vm.apiKey.isNotBlank()) {
                Button(
                    onClick = { vm.saveKey(""); keyDraft = ""; onDone() },
                    colors = ButtonDefaults.buttonColors(containerColor = CHIP, contentColor = TXT)
                ) { Text("Anahtarı sil") }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: MusicVM, onClose: () -> Unit) {
    var nameDraft by rememberSaveable { mutableStateOf(vm.userName) }
    val locPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refreshLocation() }
    val pal = LocalPal.current
    Column(
        Modifier.fillMaxSize().background(BG).pointerInput(Unit) {}
            .statusBarsPadding().navigationBarsPadding().imePadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, "Geri", tint = TXT) }
            Text("Ayarlar", color = TXT, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SettingsGroup("GÖRÜNÜM")
            ThemeOption(
                vm.themeMode == THEME_AUTO, "Otomatik", "Telefonun gece / gündüz ayarına uyar",
                Icons.Filled.BrightnessAuto
            ) { vm.setTheme(THEME_AUTO) }
            ThemeOption(
                vm.themeMode == THEME_LIGHT, "Gündüz modu", "Her zaman açık tema",
                Icons.Filled.LightMode
            ) { vm.setTheme(THEME_LIGHT) }
            ThemeOption(
                vm.themeMode == THEME_DARK, "Gece modu", "Her zaman koyu tema",
                Icons.Filled.DarkMode
            ) { vm.setTheme(THEME_DARK) }
            ThemeOption(
                vm.themeMode == THEME_SUN, "Gün doğumu / batımı",
                if (vm.themeMode == THEME_SUN) vm.sunText(System.currentTimeMillis())
                else "Konumuna göre gündüz açık, gece koyu tema",
                Icons.Filled.WbTwilight
            ) {
                vm.setTheme(THEME_SUN)
                if (!vm.hasLocPerm()) locPerm.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }

            SettingsGroup("VURGU RENGİ")
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ACCENTS.forEachIndexed { i, o ->
                    val sel = vm.accent == i
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { vm.setAccent(i) }
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(if (pal.dark) o.dark else o.light)
                                .border(if (sel) 3.dp else 1.dp, if (sel) TXT else EDGE, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (sel) Icon(Icons.Filled.Check, o.name, tint = if (pal.dark) o.inkDark else o.inkLight)
                        }
                        Text(
                            o.name, color = if (sel) TXT else MUTED, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Kapak renginden tema", color = TXT, fontSize = 16.sp)
                    Text("Oynatıcının arka planı çalan parçanın kapağına göre renklenir.", color = MUTED, fontSize = 13.sp)
                }
                Switch(
                    checked = vm.coverTheme, onCheckedChange = { vm.setCoverTheme(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = ACC, checkedThumbColor = INK)
                )
            }

            SettingsGroup("PROFİL")
            TextField(
                value = nameDraft, onValueChange = { nameDraft = it; vm.saveName(it) }, singleLine = true,
                label = { Text("Karşılama adı") },
                shape = RoundedCornerShape(12.dp), colors = fieldColors(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            SettingsGroup("SES")
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Asistan sesli konuşsun", color = TXT, fontSize = 16.sp)
                    Text("Kapalıyken asistan yalnızca yazıyla cevap verir.", color = MUTED, fontSize = 13.sp)
                }
                Switch(
                    checked = !vm.voiceMuted, onCheckedChange = { vm.toggleMute() },
                    colors = SwitchDefaults.colors(checkedTrackColor = ACC, checkedThumbColor = INK)
                )
            }

            SettingsGroup("YAPAY ZEKÂ (İSTEĞE BAĞLI)")
            GeminiKeySection(vm) {}

            Text(
                "Yırdaş 1.1", color = MUTED, fontSize = 12.sp,
                modifier = Modifier.padding(start = 20.dp, top = 28.dp, bottom = 24.dp)
            )
        }
    }
}
