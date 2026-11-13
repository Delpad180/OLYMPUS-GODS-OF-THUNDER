package com.delpad.olympus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

private val Gold = Color(0xFFFFD36A)
private val Deep = Color(0xFF09091B)
private val Purple = Color(0xFF28134D)
private val symbols = listOf("RUBÍ", "ZAFIRO", "ESMERALDA", "AMATISTA", "ORO", "CORONA")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 9, 27)
        window.navigationBarColor = android.graphics.Color.rgb(9, 9, 27)
        setContent { MaterialTheme { OlympusGame() } }
    }
}

@Composable
private fun OlympusGame() {
    var grid by remember { mutableStateOf(newGrid()) }
    var spinning by remember { mutableStateOf(false) }
    var balance by remember { mutableIntStateOf(1000) }
    var bet by remember { mutableIntStateOf(10) }
    var lastWin by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("QUE LOS DIOSES TE ACOMPAÑEN") }
    val offsets = remember { List(6) { Animatable(0f) } }
    val scope = rememberCoroutineScope()

    fun spin() {
        if (spinning || balance < bet) return
        spinning = true
        balance -= bet
        lastWin = 0
        message = "LOS RODILLOS ESTÁN GIRANDO..."
        scope.launch {
            // Every column moves vertically, with a small stagger and a smooth deceleration.
            offsets.forEachIndexed { index, anim ->
                launch {
                    delay(index * 105L)
                    anim.snapTo(0f)
                    anim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 1450 + index * 190, easing = FastOutSlowInEasing)
                    )
                }
            }
            delay(2600)
            grid = newGrid()
            val counts = grid.flatten().groupingBy { it }.eachCount()
            val matches = counts.filter { it.value >= 8 }.keys
            if (matches.isNotEmpty()) {
                lastWin = bet * (2 + matches.size * 2)
                balance += lastWin
                message = "¡VICTORIA! +$lastWin CRÉDITOS"
            } else {
                message = "LA SUERTE ESPERA SU MOMENTO"
            }
            spinning = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Deep) {
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF17102F), Deep, Color(0xFF100B22))))
        ) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("✦  OLYMPUS  ✦", color = Gold, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Text("GODS OF THUNDER", color = Color(0xFFD7C4FF), fontSize = 11.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatPanel("CRÉDITOS", balance.toString())
                    StatPanel("APUESTA", bet.toString())
                    StatPanel("ÚLTIMO PREMIO", lastWin.toString())
                }
                Text("EL TEMPLO DEL OLIMPO", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                ReelBoard(grid = grid, offsets = offsets, spinning = spinning, modifier = Modifier.weight(1f).fillMaxWidth())
                Text(message, color = if (lastWin > 0) Gold else Color(0xFFC9B8E8), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ControlButton("−", Modifier.weight(0.7f)) { if (!spinning) bet = (bet - 5).coerceAtLeast(5) }
                    ControlButton("APUESTA", Modifier.weight(1.2f), enabled = false) {}
                    ControlButton("+", Modifier.weight(0.7f)) { if (!spinning) bet = (bet + 5).coerceAtMost(100) }
                }
                Box(
                    Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(18.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFFD36A), Color(0xFFB87920), Color(0xFFFFE7A0))))
                        .clickable(enabled = !spinning) { spin() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (spinning) "GIRANDO..." else "⚡  GIRAR  ⚡", color = Color(0xFF24102F), fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
                Text("SIMULACIÓN · CRÉDITOS VIRTUALES · SIN DINERO REAL", color = Color(0xFF8C7BAA), fontSize = 8.sp, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun StatPanel(label: String, value: String) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xAA251747)).padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFFB9A5D8), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ControlButton(label: String, modifier: Modifier, enabled: Boolean = true, action: () -> Unit) {
    Box(modifier.height(43.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF38205B)).clickable(enabled = enabled) { action() }, contentAlignment = Alignment.Center) {
        Text(label, color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun ReelBoard(grid: List<List<Int>>, offsets: List<Animatable<Float, *>>, spinning: Boolean, modifier: Modifier = Modifier) {
    // The vertical travel is drawn continuously on Canvas; the frame is clipped to the reel window.
    val phase = remember { Animatable(0f) }
    LaunchedEffect(spinning) {
        if (spinning) {
            phase.snapTo(0f)
            phase.animateTo(1f, tween(2350, easing = FastOutSlowInEasing))
        }
    }
    Canvas(modifier.clip(RoundedCornerShape(22.dp)).background(Brush.verticalGradient(listOf(Color(0xFF39205F), Color(0xFF140D2B), Color(0xFF39205F))))) {
        val cols = 6
        val rows = 5
        val cellW = size.width / cols
        val cellH = size.height / rows
        drawRoundRect(Color(0xFFFFD36A).copy(alpha = .7f), style = Stroke(width = 2.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()))
        for (c in 0 until cols) {
            val localPhase = if (spinning) ((phase.value * 1.35f - c * .045f).coerceIn(0f, 1f)) else 0f
            val travel = (localPhase * cellH * 5.5f) % (cellH * rows)
            for (r in -1..rows) {
                val sourceRow = (r - floor(travel / cellH).toInt()).mod(rows)
                val y = r * cellH + (travel % cellH)
                val symbol = grid[sourceRow][c]
                val center = Offset(c * cellW + cellW / 2, y + cellH / 2)
                val radius = min(cellW, cellH) * .34f
                drawCircle(Color(0xFF080615).copy(alpha = .45f), radius * 1.08f, center + Offset(1.dp.toPx(), 3.dp.toPx()))
                val gemColor = gemColor(symbol)
                val path = Path().apply {
                    moveTo(center.x, center.y - radius)
                    lineTo(center.x + radius * .78f, center.y - radius * .25f)
                    lineTo(center.x + radius * .58f, center.y + radius * .72f)
                    lineTo(center.x, center.y + radius)
                    lineTo(center.x - radius * .58f, center.y + radius * .72f)
                    lineTo(center.x - radius * .78f, center.y - radius * .25f)
                    close()
                }
                drawPath(path, Brush.linearGradient(listOf(Color.White.copy(alpha = .9f), gemColor, gemColor.copy(alpha = .65f)), start = Offset(center.x - radius, center.y - radius), end = Offset(center.x + radius, center.y + radius)))
                drawPath(path, Color.White.copy(alpha = .65f), style = Stroke(width = 1.2.dp.toPx()))
                drawLine(Color.White.copy(alpha = .55f), Offset(center.x - radius * .35f, center.y - radius * .35f), Offset(center.x, center.y - radius * .58f), 2.dp.toPx())
            }
        }
        for (c in 1 until cols) drawLine(Color.White.copy(alpha = .10f), Offset(c * cellW, 0f), Offset(c * cellW, size.height), 1.dp.toPx())
        for (r in 1 until rows) drawLine(Color.White.copy(alpha = .10f), Offset(0f, r * cellH), Offset(size.width, r * cellH), 1.dp.toPx())
    }
}

private fun newGrid(): List<List<Int>> = List(5) { List(6) { Random.nextInt(0, 6) } }
private fun gemColor(id: Int): Color = when (id) {
    0 -> Color(0xFFE94D75)
    1 -> Color(0xFF42B9FF)
    2 -> Color(0xFF35D69A)
    3 -> Color(0xFFB77AFF)
    4 -> Color(0xFFFFC64A)
    else -> Color(0xFFFFF0C2)
}
