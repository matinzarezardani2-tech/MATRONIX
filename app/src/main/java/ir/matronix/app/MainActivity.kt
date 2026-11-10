package ir.matronix.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import kotlin.concurrent.thread

private val bg = Color(0xFF090817)
private val panel = Color(0xFF19152E)
private val cyan = Color(0xFF35E8FF)
private val purple = Color(0xFFB45CFF)

class MainActivity : ComponentActivity() {
    private var socket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    private fun connect(onResult: (String) -> Unit) {
        thread {
            try {
                val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = manager.adapter ?: error("بلوتوث گوشی در دسترس نیست")
                val device = adapter.bondedDevices.firstOrNull {
                    it.name?.contains("HC-05", true) == true ||
                    it.name?.contains("HC05", true) == true
                } ?: error("HC-05 پیدا نشد؛ ابتدا در تنظیمات گوشی Pair کن.")
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                adapter.cancelDiscovery()
                val s = device.createRfcommSocketToServiceRecord(uuid)
                s.connect()
                socket = s
                runOnUiThread { onResult("متصل به ${device.name}") }
            } catch (e: Exception) {
                try { socket?.close() } catch (_: Exception) {}
                socket = null
                runOnUiThread { onResult("خطا: ${e.message}") }
            }
        }
    }

    private fun send(command: String) {
        thread {
            try { socket?.outputStream?.write((command + "\n").toByteArray()) }
            catch (_: Exception) { runOnUiThread { } }
        }
    }

    private fun disconnect() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MatronixScreen(::connect, ::disconnect, ::send) }
    }
}

@Composable
fun MatronixScreen(
    onConnect: ((String) -> Unit) -> Unit,
    onDisconnect: () -> Unit,
    onCommand: (String) -> Unit
) {
    var status by remember { mutableStateOf("آماده اتصال") }
    var speed by remember { mutableFloatStateOf(150f) }
    var guard by remember { mutableStateOf(false) }
    var auto by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val emotions = listOf(
        "😊 خوشحال" to "E1", "😂 خنده" to "E2", "😢 غمگین" to "E3",
        "😠 عصبانی" to "E4", "😴 خواب‌آلود" to "E5", "😲 متعجب" to "E6",
        "🤔 متفکر" to "E7", "😍 ذوق‌زده" to "E8", "🥺 التماس" to "E9",
        "😎 باحال" to "E10", "😳 خجالتی" to "E11", "🤨 مشکوک" to "E12",
        "🥱 خسته" to "E13", "😇 آرام" to "E14", "🤩 هیجان‌زده" to "E15",
        "😏 شیطون" to "E16", "😭 گریه" to "E17", "😐 بی‌تفاوت" to "E18",
        "🧐 کنجکاو" to "E19", "😤 مغرور" to "E20"
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        status = if (granted) "مجوز داده شد؛ دوباره اتصال را بزن." else "مجوز بلوتوث رد شد."
    }

    fun connectClick() {
        if (Build.VERSION.SDK_INT >= 31 &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            status = "در حال اتصال..."
            onConnect { status = it }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = bg, surface = panel, primary = cyan, secondary = purple
    )) {
        Column(
            Modifier.fillMaxSize().background(bg).padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("MATRONIX", color = cyan, fontSize = 30.sp)
            Text("MAT BOT CONTROL SYSTEM", color = purple, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = panel)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("وضعیت بلوتوث", color = cyan)
                    Text(status, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { connectClick() }) { Text("اتصال") }
                        OutlinedButton(onClick = {
                            onDisconnect(); status = "اتصال قطع شد"
                        }) { Text("قطع اتصال") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("کنترل حرکت", color = cyan, fontSize = 20.sp)
            Button(onClick = { onCommand("F") }) { Text("⬆ جلو") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onCommand("L") }) { Text("⬅ چپ") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onCommand("X") }) { Text("■ توقف") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onCommand("R") }) { Text("راست ➡") }
            }
            Button(onClick = { onCommand("B") }) { Text("⬇ عقب") }
            Text("سرعت موتور: ${speed.toInt()} / 255", color = Color.White)
            Slider(value = speed, onValueChange = {
                speed = it
                onCommand("V:${it.toInt()}")
            }, valueRange = 0f..255f)
            HorizontalDivider(color = purple)
            Spacer(Modifier.height(8.dp))
            Text("احساسات MAT BOT", color = cyan, fontSize = 20.sp)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(emotions) { item ->
                    Button(
                        onClick = { onCommand(item.second) },
                        modifier = Modifier.height(46.dp),
                        contentPadding = PaddingValues(2.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(item.first, fontSize = 11.sp) }
                }
            }
            HorizontalDivider(color = purple)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("شخصیت خودکار", color = Color.White)
                    Switch(auto, onCheckedChange = {
                        auto = it; onCommand(if (it) "A1" else "A0")
                    })
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("حالت نگهبان", color = Color.White)
                    Switch(guard, onCheckedChange = {
                        guard = it; onCommand(if (it) "G1" else "G0")
                    })
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("صدا", color = Color.White)
                    Button(onClick = { onCommand("SOUND1") }) { Text("پخش") }
                }
            }
        }
    }
}
