package com.blackout.files

import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun ColorWheel(color: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val cur by rememberUpdatedState(color)
    val cb by rememberUpdatedState(onChange)
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color, hsv)

    fun pick(pos: Offset, w: Float, h: Float) {
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(cx, cy)
        val dx = pos.x - cx
        val dy = pos.y - cy
        var ang = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (ang < 0f) ang += 360f
        val sat = (sqrt(dx * dx + dy * dy) / r).coerceIn(0f, 1f)
        val v = FloatArray(3)
        AndroidColor.colorToHSV(cur, v)
        cb(AndroidColor.HSVToColor(floatArrayOf(ang, sat, v[2])))
    }

    Canvas(
        modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { pick(it, size.width.toFloat(), size.height.toFloat()) })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pick(it, size.width.toFloat(), size.height.toFloat()) },
                    onDrag = { change, _ -> pick(change.position, size.width.toFloat(), size.height.toFloat()) }
                )
            }
    ) {
        val r = minOf(size.width, size.height) / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            Brush.sweepGradient(
                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                c
            ),
            r,
            c
        )
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.Transparent), c, r), r, c)
        val a = Math.toRadians(hsv[0].toDouble())
        val m = Offset(
            c.x + (cos(a) * hsv[1] * r).toFloat(),
            c.y + (sin(a) * hsv[1] * r).toFloat()
        )
        drawCircle(Color(color), 8.dp.toPx(), m)
        drawCircle(Color.White, 10.dp.toPx(), m, style = Stroke(3.dp.toPx()))
    }
}

@Composable
fun IconColors(st: UiState) {
    val ctx = LocalContext.current
    var target by remember { mutableStateOf(0) }
    val current = when (target) {
        0 -> st.iconFront
        1 -> st.iconBack
        else -> st.iconKey
    }
    val preview = remember(st.iconFront, st.iconBack, st.iconKey) {
        IconArt.render(256, st.iconBack, st.iconFront, st.iconKey, true).asImageBitmap()
    }
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(current, hsv)

    fun apply(c: Int) {
        when (target) {
            0 -> st.iconFront = c
            1 -> st.iconBack = c
            else -> st.iconKey = c
        }
        st.saveIconColors()
    }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionLabel("Custom icon colors")
        Image(bitmap = preview, contentDescription = "Icon preview", modifier = Modifier.size(84.dp))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Front", "Back", "Keyhole").forEachIndexed { i, l ->
                Pill(l, target == i) { target = i }
            }
        }
        Spacer(Modifier.height(12.dp))
        ColorWheel(current, { apply(it) }, Modifier.size(200.dp))
        Slider(
            value = hsv[2].coerceAtLeast(0.15f),
            onValueChange = { v -> apply(AndroidColor.HSVToColor(floatArrayOf(hsv[0], hsv[1], v))) },
            valueRange = 0.15f..1f
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                st.iconFront = IconArt.DEF_FRONT
                st.iconBack = IconArt.DEF_BACK
                st.iconKey = IconArt.DEF_KEY
                st.saveIconColors()
            }) { Text("Reset") }
            Button(onClick = {
                val ok = IconArt.pin(ctx, st.iconBack, st.iconFront, st.iconKey)
                Toast.makeText(
                    ctx,
                    if (ok) "Confirm the prompt to add it to your home screen"
                    else "Your launcher can't add icons from apps",
                    Toast.LENGTH_LONG
                ).show()
            }) { Text("Add to home screen") }
        }
        Text(
            "Android can't recolor the app drawer icon. This adds a custom colored icon to your home screen.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
