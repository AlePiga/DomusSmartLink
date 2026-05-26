package com.example.myapplication

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.ui.theme.Sensore
import com.example.myapplication.ui.theme.getSensori
import com.example.myapplication.ui.theme.sendLed
import com.example.myapplication.ui.theme.sendVentola
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = dynamicDarkColorScheme(this),
                typography = Typography()
            ) {
                App()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var json by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        getSensori { json = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Domus Smart Link",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            isRefreshing = true
                            getSensori {
                                json = it
                                isRefreshing = false
                                snackbarMessage = "Dati aggiornati"
                                showSnackbar = true
                            }
                        }
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        snackbarHost = {
            if (showSnackbar) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(snackbarMessage)
                }
                LaunchedEffect(Unit) {
                    delay(2000)
                    showSnackbar = false
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E)
                        )
                    )
                )
        ) {
            if (json.isNotEmpty()) {
                val sensori = remember { mutableStateListOf<Sensore>() }

                LaunchedEffect(json) {
                    sensori.clear()
                    sensori.addAll(parseSensori(json))
                }

                // Dividi i sensori in controlli e sensori veri e propri
                val controlli = sensori.filter { it.type == "ON/OFF" || it.type == "Ventilatore" }
                val sensoriData = sensori.filter { it.type != "ON/OFF" && it.type != "Ventilatore" }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sezione Controlli
                    if (controlli.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Controlli", icon = Icons.Default.Settings)
                        }

                        items(controlli) { controllo ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInHorizontally()
                            ) {
                                SensorCard(controllo, sensori, sensori.indexOf(controllo))
                            }
                        }
                    }

                    // Sezione Sensori
                    if (sensoriData.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionHeader(title = "Sensori", icon = Icons.Default.Sensors)
                        }

                        items(sensoriData) { sensore ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInHorizontally()
                            ) {
                                SensorCard(sensore, sensori, sensori.indexOf(sensore))
                            }
                        }
                    }
                }
            } else if (isRefreshing) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Caricamento dispositivi...",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF2196F3),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Divider(
            modifier = Modifier.width(100.dp),
            color = Color.White.copy(alpha = 0.3f),
            thickness = 1.dp
        )
    }
}

@Composable
fun SensorCard(s: Sensore, sensori: SnapshotStateList<Sensore>, index: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A3E).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icona basata sul tipo
            Icon(
                imageVector = when {
                    s.type == "ON/OFF" -> Icons.Default.PowerSettingsNew
                    s.type == "Ventilatore" -> Icons.Default.AcUnit
                    s.nome.equals("Temperatura", true) -> Icons.Default.Thermostat
                    s.nome.equals("Umidità", true) -> Icons.Default.WaterDrop
                    s.nome.equals("Acqua", true) -> Icons.Default.Water
                    else -> Icons.Default.DeviceHub
                },
                contentDescription = null,
                tint = when (s.type) {
                    "ON/OFF" -> if (s.status == 1) Color(0xFF4CAF50) else Color(0xFFF44336)
                    else -> Color(0xFF2196F3)
                },
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = s.nome,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Sottotitolo personalizzato
                Text(
                    text = when {
                        s.type == "ON/OFF" -> "LED"
                        s.type == "Ventilatore" -> "Ventola"
                        s.nome.equals("Temperatura", true) -> "DHT11"
                        s.nome.equals("Umidità", true) -> "DHT11"
                        s.nome.equals("Livello dell'acqua", true) -> "Sensore di profondità liquidi"
                        else -> s.type
                    },
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            when (s.type) {
                "ON/OFF" -> ModernSwitch(s, sensori, index)
                "NUMBER" -> {
                    when {
                        s.nome.equals("Temperatura", true) -> TemperatureDisplay(s)
                        s.nome.equals("Umidità", true) -> HumidityDisplay(s)
                        s.nome.equals("Livello dell'acqua", true) -> WaterLevelDisplay(s)
                        else -> NumberDisplay(s)
                    }
                }
                "Ventilatore" -> ModernVentolaUI(s)
            }
        }
    }
}
@Composable
fun NumberDisplay(s: Sensore) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF2196F3).copy(alpha = 0.2f)
    ) {
        Text(
            text = "${s.status}",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )
    }
}

@Composable
fun TemperatureDisplay(s: Sensore) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF2196F3).copy(alpha = 0.2f)
    ) {
        Text(
            text = "${s.status}°C",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )
    }
}

@Composable
fun HumidityDisplay(s: Sensore) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF2196F3).copy(alpha = 0.2f)
    ) {
        Text(
            text = "${s.status}%",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )
    }
}

@Composable
fun WaterLevelDisplay(s: Sensore) {
    // Converti in cm (es. 5 → 0,05 cm)
    val valueInCm = s.status.toDouble() / 100.0
    // Formatta con 2 decimali e virgola
    val formattedValue = String.format("%.2f", valueInCm).replace('.', ',')

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF2196F3).copy(alpha = 0.2f)
    ) {
        Text(
            text = "$formattedValue cm",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )
    }
}

@Composable
fun ModernSwitch(s: Sensore, sensori: SnapshotStateList<Sensore>, index: Int) {
    var isOn by remember { mutableStateOf(s.status == 1) }

    Switch(
        checked = isOn,
        onCheckedChange = { checked ->
            isOn = checked
            val nuovoStatus = if (checked) 1 else 0
            sensori[index] = s.copy(status = nuovoStatus)
            sendLed(s.id, nuovoStatus)
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Color(0xFF4CAF50),
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFFF44336)
        )
    )
}

@Composable
fun ModernVentolaUI(s: Sensore) {
    var showDialog by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf((s.status / 2.5f).coerceIn(0f, 100f)) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF2196F3).copy(alpha = 0.2f),
        onClick = { showDialog = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${currentSpeed.toInt()}%",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Modifica velocità",
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(16.dp)
            )
        }
    }

    if (showDialog) {
        SpeedControlDialog(
            currentSpeed = currentSpeed,
            onSpeedChange = { newSpeed ->
                currentSpeed = newSpeed
                val valoreFinale = (newSpeed * 2.5f).toInt()
                sendVentola(s.id, valoreFinale)
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun SpeedControlDialog(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentSpeed) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF2A2A3E),
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Imposta la velocità",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Animazione cerchio velocità
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(150.dp)
                ) {
                    CircularProgressIndicator(
                        progress = sliderValue / 100f,
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 8.dp,
                        color = when {
                            sliderValue < 30 -> Color(0xFF4CAF50)
                            sliderValue < 70 -> Color(0xFFFFA000)
                            else -> Color(0xFFF44336)
                        }
                    )
                    Text(
                        text = "${sliderValue.toInt()}%",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF2196F3),
                        activeTrackColor = Color(0xFF2196F3)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        onSpeedChange(sliderValue)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Text("Applica")
                }
            }
        }
    }
}

fun parseSensori(json: String): List<Sensore> {
    val lista = mutableListOf<Sensore>()

    try {
        val items = json.removePrefix("[").removeSuffix("]").split("},")

        for (item in items) {
            val clean = item.replace("{", "").replace("}", "")
            val parts = clean.split(",")

            var nome = ""
            var status = 0
            var id = 0
            var type = ""

            for (p in parts) {
                val kv = p.split(":")
                if (kv.size >= 2) {
                    val key = kv[0].replace("\"", "").trim()
                    val value = kv[1].replace("\"", "").trim()

                    when (key) {
                        "nome" -> nome = value
                        "status" -> status = value.toIntOrNull() ?: 0
                        "id" -> id = value.toIntOrNull() ?: 0
                        "type" -> type = value
                    }
                }
            }
            if (nome.isNotEmpty()) {
                lista.add(Sensore(id, nome, status, type))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return lista
}
