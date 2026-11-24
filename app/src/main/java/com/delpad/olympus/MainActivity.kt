package com.delpad.olympus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.min
import kotlin.random.Random

private val Gold = Color(0xFFFFD45E)
private val PaleGold = Color(0xFFFFF0B2)
private val Wine = Color(0xFF35102F)
private val symbols = (0..5).toList()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(55, 20, 76)
        window.navigationBarColor = android.graphics.Color.rgb(38, 14, 38)
        setContent { MaterialTheme { OlympusGame() } }
    }
}

@Composable
private fun OlympusGame() {
    var grid by remember { mutableStateOf(newGrid()) }
    var spinning by remember { mutableStateOf(false) }
    var balance by rememberSaveable { mutableIntStateOf(1000) }
    var bet by rememberSaveable { mutableIntStateOf(10) }
    var spins by rememberSaveable { mutableIntStateOf(0) }
    var bestWin by rememberSaveable { mutableIntStateOf(0) }
    var lastWin by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("¡QUE LOS DIOSES TE ACOMPAÑEN!") }
    var showPaytable by remember { mutableStateOf(false) }
    var doubleChance by rememberSaveable { mutableStateOf(false) }
    val reelOffsets = remember { List(6) { Animatable(0f) } }
    val scope = rememberCoroutineScope()

    fun spin() {
        if (spinning || balance < bet) return
        spinning = true
        balance -= bet
        lastWin = 0
        message = "LOS RODILLOS DEL OLIMPO ESTÁN GIRANDO"
        scope.launch {
            coroutineScope {
                reelOffsets.forEachIndexed { index, offset ->
                    launch {
                        delay(index * 170L)
                        offset.snapTo(0f)
                        // Long, continuous movement followed by a natural staggered stop.
                        offset.animateTo(17f, tween(3000, easing = LinearEasing))
                        offset.animateTo(22f, tween(1450 + index * 170, easing = FastOutSlowInEasing))
                    }
                }
            }
            // 22 complete symbol steps return each reel to a perfectly aligned position.
            grid = newGrid()
            reelOffsets.forEach { it.snapTo(0f) }
            spins++
            val counts = grid.flatten().groupingBy { it }.eachCount()
            val bestMatch = counts.values.maxOrNull() ?: 0
            val multiplier = when {
                bestMatch >= 15 -> 30
                bestMatch >= 12 -> 12
                bestMatch == 11 -> 8
                bestMatch == 10 -> 5
                bestMatch == 9 -> 3
                bestMatch == 8 -> 2
                else -> 0
            } * if (doubleChance) 2 else 1
            if (multiplier > 0) {
                lastWin = bet * multiplier
                balance += lastWin
                bestWin = maxOf(bestWin, lastWin)
                message = "¡BENDICIÓN DIVINA!  +$lastWin CRÉDITOS"
            } else {
                message = "LA SUERTE ESPERA SU MOMENTO"
            }
            spinning = false
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF542B83), Color(0xFF9C4D83), Color(0xFFFFB16B), Color(0xFF4A204B))))) {
        TempleBackdrop(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            GameHeader()
            Text("GANA HASTA 5.000 X APUESTA", color = PaleGold, fontSize = 15.sp, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center)
            ReelBoard(grid, reelOffsets, spinning, Modifier.weight(1f).fillMaxWidth())
            Text(message, color = if (lastWin > 0) PaleGold else Color.White, fontSize = 12.sp,
                fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldPanel(Modifier.weight(1f).height(76.dp), "COMPRE", "TIRADAS GRATIS", "200 CR") { showPaytable = true }
                GoldPanel(Modifier.weight(1f).height(76.dp), "APUESTA", "$bet CR", if (doubleChance) "DOBLE PREMIO: ON" else "DOBLE PREMIO: OFF") {
                    if (!spinning) doubleChance = !doubleChance
                }
            }
            Row(Modifier.fillMaxWidth().height(88.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                RoundControl("−", Modifier.size(54.dp)) { if (!spinning) bet = (bet - 5).coerceAtLeast(5) }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(90.dp).clip(RoundedCornerShape(50)).background(
                    Brush.radialGradient(listOf(Color(0xFFFFF6D2), Color(0xFFB77A22), Color(0xFF4B1E38)))
                ).clickable(enabled = !spinning && balance >= bet) { spin() }, contentAlignment = Alignment.Center) {
                    Text(if (spinning) "•••" else "⟳", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                RoundControl("+", Modifier.size(54.dp)) { if (!spinning) bet = (bet + 5).coerceAtMost(100) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                BottomControl("ⓘ") { showPaytable = true }
                BottomControl("⟳") { if (!spinning) { balance = 1000; message = "TU TESORO HA SIDO RESTAURADO" } }
                BottomControl("Ⅱ") { if (!spinning) message = "JUEGO EN PAUSA" }
                BottomControl("☰") { showPaytable = true }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Color(0xCC32152E)).padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CRÉDITO  $balance CR", color = PaleGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text("APUESTA  $bet CR", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            Text("SIMULACIÓN · CRÉDITOS VIRTUALES · SIN DINERO REAL", color = Color.White.copy(alpha = .8f), fontSize = 8.sp)
        }
    }

    if (showPaytable) {
        AlertDialog(
            onDismissRequest = { showPaytable = false },
            containerColor = Color(0xFF351532),
            title = { Text("BENDICIONES DEL OLIMPO", color = Gold, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Premios por cantidad del mismo símbolo en el tablero:", color = Color.White, fontSize = 12.sp)
                    listOf("8 símbolos  ·  x2", "9 símbolos  ·  x3", "10 símbolos · x5", "11 símbolos · x8", "12–14 símbolos · x12", "15 o más · x30").forEach {
                        Text(it, color = PaleGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text("La opción de doble premio duplica el multiplicador. Los créditos son virtuales y no tienen valor monetario.", color = Color.LightGray, fontSize = 10.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showPaytable = false }) { Text("CERRAR", color = Gold) } }
        )
    }
}

@Composable
private fun GameHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth(.88f).clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(Color(0xFF4B1B43), Color(0xFF1D1235), Color(0xFF6D3B24))))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("⚡ GATES OF OLYMPUS ⚡", color = Gold, fontSize = 23.sp, fontWeight = FontWeight.Black,
                letterSpacing = 1.sp, textAlign = TextAlign.Center)
        }
        Text("ZEUS · EL REINO DE LOS DIOSES", color = PaleGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
}

@Composable
private fun TempleBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        // Sunset clouds and the silhouette of a classical Greek temple.
        for (i in 0..9) {
            val x = size.width * (i / 9f)
            drawCircle(Color(0xFFFFD58A).copy(alpha = .10f), size.width * .18f, Offset(x, size.height * .83f))
        }
        val baseY = size.height * .91f
        val templeW = size.width * .78f
        val left = (size.width - templeW) / 2f
        drawRect(Brush.verticalGradient(listOf(Color(0xFFFFE0A0).copy(alpha = .28f), Color(0xFF8E4D55).copy(alpha = .55f))),
            Offset(left, baseY - size.height * .20f), Size(templeW, size.height * .20f))
        drawRect(Color(0xFFFFD88A).copy(alpha = .55f), Offset(left - 12f, baseY - size.height * .22f), Size(templeW + 24f, 10f))
        for (i in 0..6) {
            val x = left + i * templeW / 6f
            drawRoundRect(Color(0xFFFFE8BD).copy(alpha = .42f), Offset(x, baseY - size.height * .19f),
                Size(templeW / 13f, size.height * .18f), CornerRadius(8f, 8f))
            drawArc(Color(0xFFFFE8BD).copy(alpha = .42f), 180f, 180f, false,
                Offset(x - 2f, baseY - size.height * .205f), Size(templeW / 13f + 4f, size.height * .07f), style = Stroke(5f))
        }
        drawRect(Color(0xFFFFD36A).copy(alpha = .4f), Offset(left - 22f, baseY - 8f), Size(templeW + 44f, 9f))
    }
}

@Composable
private fun ReelBoard(grid: List<List<Int>>, offsets: List<Animatable<Float, *>>, spinning: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(18.dp)).background(
        Brush.verticalGradient(listOf(Color(0xFF6A2E63), Color(0xFF32113F), Color(0xFF190D2D)))
    )) {
        val cols = 6
        val rows = 5
        val cellW = size.width / cols
        val cellH = size.height / rows
        // Heavy gold outer frame and inner reel dividers.
        drawRoundRect(Brush.linearGradient(listOf(Color(0xFFFFF0A5), Color(0xFFB87918), Color(0xFFFFE17A))),
            style = Stroke(width = 8.dp.toPx()), cornerRadius = CornerRadius(18.dp.toPx()))
        drawRoundRect(Color(0xFF3B153F), Offset(7.dp.toPx(), 7.dp.toPx()),
            Size(size.width - 14.dp.toPx(), size.height - 14.dp.toPx()), CornerRadius(13.dp.toPx()), style = Stroke(2.dp.toPx()))
        for (c in 0 until cols) {
            val travel = offsets[c].value * cellH
            for (r in -1..rows) {
                val sourceRow = (r - floor(travel / cellH).toInt()).mod(rows)
                val y = r * cellH - (travel % cellH)
                val symbol = grid[sourceRow][c]
                drawSymbol(symbol, Offset(c * cellW + cellW / 2f, y + cellH / 2f), min(cellW, cellH) * .34f)
            }
        }
        for (c in 1 until cols) drawLine(Color(0xFFFFD879).copy(alpha = .38f), Offset(c * cellW, 4f), Offset(c * cellW, size.height - 4f), 2.dp.toPx())
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSymbol(id: Int, center: Offset, radius: Float) {
    val colors = when (id) {
        0 -> listOf(Color(0xFFFF9C36), Color(0xFFFFD21F), Color(0xFFB66B08))
        1 -> listOf(Color(0xFF8BE8FF), Color(0xFF00A9F4), Color(0xFF0753B7))
        2 -> listOf(Color(0xFFB7FF7A), Color(0xFF22C65A), Color(0xFF087A3A))
        3 -> listOf(Color(0xFFFFA5FF), Color(0xFF9B2DDA), Color(0xFF4A126F))
        4 -> listOf(Color(0xFFFFE7A5), Color(0xFFD99522), Color(0xFF7E3D12))
        else -> listOf(Color(0xFFFFF2C0), Color(0xFFE3425C), Color(0xFF8E173C))
    }
    val jewel = Path().apply {
        when (id) {
            0, 4 -> { // Gold hexagonal medallion
                for (i in 0 until 6) {
                    val a = Math.PI / 3.0 * i - Math.PI / 6.0
                    val x = center.x + kotlin.math.cos(a).toFloat() * radius
                    val y = center.y + kotlin.math.sin(a).toFloat() * radius
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            1 -> { moveTo(center.x, center.y-radius); lineTo(center.x+radius, center.y); lineTo(center.x, center.y+radius); lineTo(center.x-radius, center.y) }
            2 -> { moveTo(center.x, center.y-radius); lineTo(center.x+radius*.88f, center.y+radius*.75f); lineTo(center.x-radius*.88f, center.y+radius*.75f) }
            3 -> { moveTo(center.x-radius*.8f, center.y-radius*.65f); lineTo(center.x+radius*.8f, center.y-radius*.65f); lineTo(center.x, center.y+radius) }
            else -> { moveTo(center.x, center.y-radius); lineTo(center.x+radius*.8f, center.y-radius*.3f); lineTo(center.x+radius*.6f, center.y+radius*.8f); lineTo(center.x-radius*.6f, center.y+radius*.8f); lineTo(center.x-radius*.8f, center.y-radius*.3f) }
        }
        close()
    }
    if (id == 3) {
        drawCircle(Color(0xFF3B1648), radius * 1.04f, center)
        drawCircle(colors[1], radius * .88f, center, style = Stroke(radius * .12f))
        drawCircle(colors[0], radius * .63f, center, style = Stroke(radius * .09f))
        drawCircle(Color.White.copy(alpha = .8f), radius * .12f, Offset(center.x-radius*.25f, center.y-radius*.3f))
    } else {
        drawPath(jewel, Color(0xFF55223A), style = Stroke(radius * .23f))
        drawPath(jewel, Brush.linearGradient(colors, Offset(center.x-radius, center.y-radius), Offset(center.x+radius, center.y+radius)))
        drawPath(jewel, PaleGold.copy(alpha = .9f), style = Stroke(radius * .07f))
        drawCircle(Color.White.copy(alpha = .75f), radius * .12f, Offset(center.x-radius*.22f, center.y-radius*.3f))
    }
}

@Composable
private fun GoldPanel(modifier: Modifier, title: String, value: String, sub: String, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(
        Brush.verticalGradient(listOf(Color(0xFFFFE7A4), Color(0xFF9E632A), Color(0xFFFFD77B)))
    ).clickable { onClick() }.padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, color = Color(0xFF311A46), fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text(value, color = Color(0xFF40205A), fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text(sub, color = Color(0xFF5B2D49), fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RoundControl(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(50)).background(Brush.verticalGradient(listOf(Color(0xFF9A714E), Color(0xFF29142E))))
        .clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BottomControl(label: String, onClick: () -> Unit) {
    Box(Modifier.size(width = 54.dp, height = 42.dp).clip(RoundedCornerShape(50))
        .background(Color(0xAA351D35)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, color = PaleGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

private fun newGrid(): List<List<Int>> = List(5) { List(6) { Random.nextInt(0, 6) } }
