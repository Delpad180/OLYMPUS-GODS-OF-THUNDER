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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val Gold = Color(0xFFFFD34F)
private val LightGold = Color(0xFFFFF0B1)
private val DeepPurple = Color(0xFF321039)
private val symbols = 0..6

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(75, 29, 92)
        window.navigationBarColor = android.graphics.Color.rgb(47, 20, 39)
        setContent { MaterialTheme { OlympusGame() } }
    }
}

@Composable
private fun OlympusGame() {
    var grid by remember { mutableStateOf(newGrid()) }
    var spinning by remember { mutableStateOf(false) }
    var balance by rememberSaveable { mutableIntStateOf(100000) }
    var bet by rememberSaveable { mutableIntStateOf(200) }
    var lastWin by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("¡HAGA SU APUESTA!") }
    var doubleChance by rememberSaveable { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    val offsets = remember { List(6) { Animatable(0f) } }
    val scope = rememberCoroutineScope()

    fun spin() {
        if (spinning || balance < bet) return
        spinning = true
        balance -= bet
        lastWin = 0
        message = "LOS DIOSES ESTÁN DECIDIENDO TU DESTINO..."
        scope.launch {
            coroutineScope {
                offsets.forEachIndexed { i, offset ->
                    launch {
                        delay(i * 190L)
                        offset.snapTo(0f)
                        offset.animateTo(3.5f, tween(420, easing = FastOutSlowInEasing))
                        offset.animateTo(31.5f, tween(3300, easing = LinearEasing))
                        offset.animateTo(36f, tween(1450 + i * 120, easing = FastOutSlowInEasing))
                    }
                }
            }
            grid = newGrid()
            offsets.forEach { it.snapTo(0f) }
            val counts = grid.flatten().groupingBy { it }.eachCount()
            val match = counts.values.maxOrNull() ?: 0
            val multiplier = when {
                match >= 15 -> 30
                match >= 12 -> 12
                match == 11 -> 8
                match == 10 -> 5
                match == 9 -> 3
                match == 8 -> 2
                else -> 0
            } * if (doubleChance) 2 else 1
            if (multiplier > 0) {
                lastWin = bet * multiplier
                balance += lastWin
                message = "¡PREMIO DIVINO!  +$lastWin"
            } else message = "LA SUERTE ESPERA SU MOMENTO"
            spinning = false
        }
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color(0xFF472078), Color(0xFF9A4B91), Color(0xFFE18A72), Color(0xFFFFC66D), Color(0xFF4B234D)
        )))
    ) {
        TempleSky(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Logo plaque and Zeus-style hero area.
            Box(Modifier.fillMaxWidth().weight(.82f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) { drawZeusAndClouds() }
                Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth(.88f).height(57.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) { drawLogoPlaque() }
                        Text("GATES OF OLYMPUS", color = Color(0xFFFFD21F), fontSize = 25.sp,
                            fontWeight = FontWeight.Black, letterSpacing = (-.7).sp)
                    }
                    Text("GANA HASTA 5.000 X APUESTA", color = LightGold, fontSize = 17.sp,
                        fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
            }

            // Six reels, five rows, purple glass and heavy gold Greek frame.
            ReelBoard(grid, offsets, Modifier.fillMaxWidth().weight(2.75f))

            Text(message, color = if (lastWin > 0) LightGold else Color.White,
                fontSize = 17.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                maxLines = 1)

            Row(Modifier.fillMaxWidth().weight(.72f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OrnatePanel(Modifier.weight(1f).fillMaxHeight(), "COMPRE", "TIRADAS GRATIS", "200 €", false) {
                    showInfo = true
                }
                OrnatePanel(Modifier.weight(1f).fillMaxHeight(), "APUESTA  $bet CR", "DOBLAR POSIBILIDAD DE", if (doubleChance) "GANAR  ·  ON" else "GANAR  ·  OFF", true) {
                    if (!spinning) doubleChance = !doubleChance
                }
            }

            Row(Modifier.fillMaxWidth().weight(.9f), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                RoundButton("−", Modifier.size(55.dp)) { if (!spinning) bet = (bet - 50).coerceAtLeast(50) }
                Spacer(Modifier.width(14.dp))
                Box(Modifier.size(100.dp).clip(RoundedCornerShape(50)).background(
                    Brush.radialGradient(listOf(Color(0xFFFFF7DB), Color(0xFFD8A33C), Color(0xFF642D45)))
                ).clickable(enabled = !spinning && balance >= bet) { spin() },
                    contentAlignment = Alignment.Center) {
                    Text(if (spinning) "•••" else "⟳", color = Color.White, fontSize = 68.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(14.dp))
                RoundButton("+", Modifier.size(55.dp)) { if (!spinning) bet = (bet + 50).coerceAtMost(5000) }
            }

            Row(Modifier.fillMaxWidth().weight(.43f), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                SmallControl("ⓘ") { showInfo = true }
                SmallControl("⟳") { if (!spinning) { balance = 100000; message = "CRÉDITOS RESTAURADOS" } }
                SmallControl("Ⅱ") { if (!spinning) message = "¡QUE ZEUS TE ACOMPAÑE!" }
                SmallControl("☰") { showInfo = true }
            }

            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(Color(0xCC3C1D31)).padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("CRÉDITO  ${balance} CR", color = LightGold, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text("APUESTA  ${bet} CR", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
            Text("PRAGMATIC-STYLE THEME  ·  CRÉDITOS VIRTUALES", color = Color.White.copy(.85f), fontSize = 8.sp)
        }
    }

    if (showInfo) AlertDialog(
        onDismissRequest = { showInfo = false },
        containerColor = Color(0xFF351532),
        title = { Text("GATES OF OLYMPUS", color = Gold, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tabla de premios por símbolos iguales:", color = Color.White)
                listOf("8 símbolos  ·  x2", "9 símbolos  ·  x3", "10 símbolos · x5", "11 símbolos · x8", "12–14 símbolos · x12", "15 o más · x30").forEach {
                    Text(it, color = LightGold, fontWeight = FontWeight.Bold)
                }
                Text("Solo se utilizan créditos virtuales sin valor monetario.", color = Color.LightGray, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = { showInfo = false }) { Text("CERRAR", color = Gold) } }
    )
}

@Composable
private fun TempleSky(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // Sunset cloud glow.
        for (i in 0..12) {
            val x = w * i / 12f
            drawCircle(Color(0xFFFFD6A0).copy(alpha = .14f), w * .16f, Offset(x, h * .86f))
        }
        // Distant temple facade.
        val left = w * .10f
        val tw = w * .80f
        val top = h * .76f
        drawRect(Brush.verticalGradient(listOf(Color(0xFFFFE4B2).copy(.68f), Color(0xFFB76D65).copy(.35f)),
            startY = top, endY = h), Offset(left, top), Size(tw, h * .20f))
        drawRect(Color(0xFFFFE7B0).copy(.75f), Offset(left - 12f, top), Size(tw + 24f, 9f))
        for (i in 0..8) {
            val x = left + i * tw / 8f
            drawRoundRect(Color(0xFFFFEAC5).copy(.72f), Offset(x, top + 12f), Size(tw / 15f, h * .17f))
            drawRect(Color(0xFFB97858).copy(.45f), Offset(x + tw / 25f, top + 12f), Size(3f, h * .17f))
        }
        drawRect(Color(0xFFFFD98A).copy(.8f), Offset(left - 25f, h * .94f), Size(tw + 50f, 9f))
        // Temple steps.
        for (i in 0..4) drawRect(Color(0xFFFFE5B1).copy(.72f),
            Offset(w * .28f - i * 9f, h * (.91f + i * .012f)), Size(w * (.44f + i * .035f), 5f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLogoPlaque() {
    val gold = Brush.linearGradient(listOf(Color(0xFFFFF2A4), Color(0xFFB46B13), Color(0xFFFFE36A), Color(0xFF75400E)))
    drawRoundRect(Color(0xFF35112F), Offset(4f, 6f), Size(size.width - 8f, size.height - 12f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f))
    drawRoundRect(gold, Offset(2f, 2f), Size(size.width - 4f, size.height - 8f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f), style = Stroke(5f))
    drawLine(Color(0xFFFFE88A), Offset(22f, size.height / 2), Offset(size.width - 22f, size.height / 2), 1.5f)
    for (x in listOf(12f, size.width - 12f)) {
        drawCircle(Color(0xFFFFE28B), 5f, Offset(x, size.height / 2))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawZeusAndClouds() {
    val cx = size.width * .78f
    val cy = size.height * .62f
    // Stylised Zeus bust: laurel, hair, beard, tunic and lightning glow.
    drawCircle(Color(0xFFFFD76A).copy(.16f), size.height * .34f, Offset(cx, cy))
    drawRoundRect(Color(0xFFC58B52), Offset(cx - 25f, cy + 12f), Size(54f, size.height * .35f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f))
    drawCircle(Color(0xFFE5B58A), size.height * .105f, Offset(cx, cy - size.height * .16f))
    drawCircle(Color(0xFFE9E2D2), size.height * .11f, Offset(cx, cy - size.height * .12f))
    drawOval(Color(0xFFE9E2D2), Offset(cx - 22f, cy - size.height * .12f), Size(44f, size.height * .18f))
    drawCircle(Color(0xFFE9E2D2), 11f, Offset(cx - 18f, cy - size.height * .19f))
    drawCircle(Color(0xFFE9E2D2), 11f, Offset(cx + 18f, cy - size.height * .19f))
    drawLine(Color(0xFFFFE7A0), Offset(cx - 22f, cy - size.height * .26f), Offset(cx + 22f, cy - size.height * .26f), 6f)
    drawCircle(Color(0xFF4EC9FF), 5f, Offset(cx, cy + 2f))
}

@Composable
private fun ReelBoard(grid: List<List<Int>>, offsets: List<Animatable<Float, *>>, modifier: Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(8.dp)).background(
        Brush.verticalGradient(listOf(Color(0xFF702A68), Color(0xFF43113F), Color(0xFF210D2A)))
    )) {
        val cols = 6
        val rows = 5
        val cw = size.width / cols
        val ch = size.height / rows
        // Outer sculpted gold frame and dark inner bevel.
        drawRoundRect(Brush.linearGradient(listOf(Color(0xFFFFF1A0), Color(0xFFB26A13), Color(0xFFFFE77D), Color(0xFF8A4C12))),
            style = Stroke(12.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
        drawRoundRect(Color(0xFF2A102F), Offset(8.dp.toPx(), 8.dp.toPx()),
            Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()), style = Stroke(2.dp.toPx()))
        for (c in 0 until cols) {
            val movement = offsets[c].value
            val whole = floor(movement).toInt()
            val fraction = movement - whole
            for (r in -1..rows) {
                val source = (r - whole).mod(rows)
                val y = r * ch - fraction * ch
                drawOlympusSymbol(grid[source][c], Offset(c * cw + cw / 2f, y + ch / 2f), min(cw, ch) * .39f)
            }
        }
        for (c in 1 until cols) drawLine(Color(0xFFFFD96A).copy(.48f),
            Offset(c * cw, 7f), Offset(c * cw, size.height - 7f), 2.dp.toPx())
        // Gold corner ornaments.
        for (x in listOf(15f, size.width - 15f)) for (y in listOf(15f, size.height - 15f)) {
            drawCircle(Color(0xFFFFE68A), 5f, Offset(x, y))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOlympusSymbol(id: Int, c: Offset, r: Float) {
    val gold = Color(0xFFFFC21E)
    val purple = Color(0xFF6A1597)
    val blue = Color(0xFF00B9F2)
    val green = Color(0xFF35C85A)
    when (id) {
        0 -> { // Hexagonal gold token.
            drawMedallion(c, r, gold, 6)
            drawCircle(Color(0xFFFFEF86), r * .34f, c, style = Stroke(r * .12f))
        }
        1 -> { // Gold ring with a ruby.
            drawCircle(Color(0xFF6D174F), r * .91f, c)
            drawCircle(Color(0xFFFFC42C), r * .76f, c, style = Stroke(r * .23f))
            drawCircle(Color(0xFFB92386), r * .49f, c)
            drawOval(Color(0xFFFFD45C), Offset(c.x-r*.29f, c.y-r*.92f), Size(r*.58f, r*.32f))
            drawCircle(Color(0xFFFFF1B0), r*.12f, Offset(c.x-r*.23f, c.y-r*.65f))
        }
        2 -> { // Blue diamond.
            drawJewel(c, r, blue, 4)
            drawLine(Color.White.copy(.65f), Offset(c.x, c.y-r*.72f), Offset(c.x, c.y+r*.72f), r*.06f)
        }
        3 -> { // Green upright triangle.
            drawJewel(c, r, green, 3)
            drawLine(Color(0xFFB7FF91), Offset(c.x, c.y-r*.72f), Offset(c.x-r*.55f, c.y+r*.48f), r*.07f)
        }
        4 -> { // Purple inverted triangle.
            val p = Path().apply { moveTo(c.x-r, c.y-r*.7f); lineTo(c.x+r, c.y-r*.7f); lineTo(c.x, c.y+r); close() }
            drawPath(p, gold.copy(alpha=.9f))
            drawPath(p, Brush.linearGradient(listOf(Color(0xFFE35BFF), purple, Color(0xFF320B5D)), Offset(c.x-r,c.y-r), Offset(c.x+r,c.y+r)))
            drawPath(p, Color(0xFFFFD978), style = Stroke(r*.09f))
            drawCircle(Color.White.copy(.8f), r*.1f, Offset(c.x-r*.32f,c.y-r*.48f))
        }
        5 -> { // Enchanted green hourglass.
            drawCircle(Color(0xFF087B62), r*.91f, c)
            drawCircle(Color(0xFF36D8A0), r*.78f, c, style = Stroke(r*.11f))
            val p = Path().apply {
                moveTo(c.x-r*.47f,c.y-r*.58f); lineTo(c.x+r*.47f,c.y-r*.58f)
                lineTo(c.x+r*.12f,c.y); lineTo(c.x+r*.47f,c.y+r*.58f)
                lineTo(c.x-r*.47f,c.y+r*.58f); lineTo(c.x-r*.12f,c.y); close()
            }
            drawPath(p, Color(0xFFB8F4B9))
            drawLine(Color(0xFF159D83), Offset(c.x-r*.12f,c.y), Offset(c.x+r*.12f,c.y), r*.12f)
            drawOval(gold, Offset(c.x-r*.5f,c.y-r*.69f), Size(r,r*.22f))
            drawOval(gold, Offset(c.x-r*.5f,c.y+r*.48f), Size(r,r*.22f))
        }
        else -> { // Red ruby / crown-like premium icon.
            drawMedallion(c, r, Color(0xFFC51D35), 5)
            val crown = Path().apply {
                moveTo(c.x-r*.65f,c.y-r*.18f); lineTo(c.x-r*.55f,c.y-r*.78f)
                lineTo(c.x,c.y-r*.35f); lineTo(c.x+r*.55f,c.y-r*.78f)
                lineTo(c.x+r*.65f,c.y-r*.18f); close()
            }
            drawPath(crown, gold)
            drawCircle(Color(0xFF3DE6A1), r*.13f, Offset(c.x,c.y-r*.3f))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMedallion(c: Offset, r: Float, color: Color, sides: Int) {
    val p = Path()
    for (i in 0 until sides) {
        val a = 2.0 * PI * i / sides - PI / 2
        val point = Offset(c.x + cos(a).toFloat()*r, c.y + sin(a).toFloat()*r)
        if (i == 0) p.moveTo(point.x,point.y) else p.lineTo(point.x,point.y)
    }
    p.close()
    drawPath(p, Color(0xFF69321B))
    drawPath(p, Brush.linearGradient(listOf(Color(0xFFFFF0A0), color, Color(0xFF9B4B11)),
        Offset(c.x-r,c.y-r), Offset(c.x+r,c.y+r)))
    drawPath(p, Color(0xFFFFE88C), style = Stroke(r*.08f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawJewel(c: Offset, r: Float, color: Color, sides: Int) {
    drawMedallion(c,r,color,sides)
    drawCircle(Color.White.copy(.8f),r*.1f,Offset(c.x-r*.22f,c.y-r*.3f))
}

@Composable
private fun OrnatePanel(modifier: Modifier, title: String, line: String, value: String, bright: Boolean, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(
        Brush.verticalGradient(if (bright) listOf(Color(0xFFFFF0C0), Color(0xFFD4A35A), Color(0xFFFFE5A2))
            else listOf(Color(0xFF3B1A59), Color(0xFF160F3D), Color(0xFF5C2A67)))
    ).clickable { onClick() }) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(Brush.linearGradient(listOf(Color(0xFFFFF1A4), Color(0xFF9B5A12), Color(0xFFFFD65B))),
                style = Stroke(5.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()))
            for (x in listOf(7f,size.width-7f)) for (y in listOf(7f,size.height-7f))
                drawCircle(Color(0xFFFFE89A),4f,Offset(x,y))
        }
        Column(Modifier.fillMaxSize().padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(title, color = if (bright) Color(0xFF38214B) else Color(0xFF42DFFF), fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(line, color = if (bright) Color(0xFF38214B) else Color(0xFF42DFFF), fontSize = 12.sp, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center)
            Text(value, color = if (bright) Color(0xFF3D2247) else Color(0xFFFFD64C), fontSize = 19.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun RoundButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(50)).background(Brush.verticalGradient(
        listOf(Color(0xFF80604B), Color(0xFF25132D)))).clickable { onClick() },
        contentAlignment = Alignment.Center) {
        Text(label,color=Color.White,fontSize=32.sp,fontWeight=FontWeight.Bold)
    }
}

@Composable
private fun SmallControl(label: String, onClick: () -> Unit) {
    Box(Modifier.size(width=64.dp,height=42.dp).clip(RoundedCornerShape(50))
        .background(Color(0xAA442B3B)).clickable { onClick() },contentAlignment=Alignment.Center) {
        Text(label,color=LightGold,fontSize=23.sp,fontWeight=FontWeight.Black)
    }
}

private fun newGrid(): List<List<Int>> = List(5) { List(6) { Random.nextInt(0,7) } }
