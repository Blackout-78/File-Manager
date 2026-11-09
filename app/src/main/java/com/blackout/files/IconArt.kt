package com.blackout.files

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

object IconArt {
    val DEF_BG = 0xFF1C1C1F.toInt()
    val DEF_FG = 0xFFFFFFFF.toInt()

    val STYLES = listOf("Classic", "Ring", "Solid")

    private fun folder(): Path = Path().apply {
        moveTo(30f, 38f); lineTo(47f, 38f); lineTo(52f, 45f); lineTo(78f, 45f)
        lineTo(78f, 72f); lineTo(30f, 72f); close()
    }

    fun render(size: Int, style: Int, bg: Int, fg: Int, rounded: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val s = size / 108f
        c.scale(s, s)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.strokeJoin = Paint.Join.ROUND
        p.style = Paint.Style.FILL
        p.color = bg
        if (rounded) {
            c.drawRoundRect(RectF(0f, 0f, 108f, 108f), 26f, 26f, p)
        } else {
            c.drawRect(0f, 0f, 108f, 108f, p)
        }
        p.color = fg
        when (style) {
            1 -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2.4f
                c.drawCircle(54f, 54f, 31.5f, p)
                p.strokeWidth = 3.5f
                c.drawPath(folder(), p)
                p.strokeWidth = 3f
                c.drawCircle(54f, 58.5f, 8f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(54f, 58.5f, 2.5f, p)
            }
            2 -> {
                p.style = Paint.Style.FILL_AND_STROKE
                p.strokeWidth = 3f
                c.drawPath(folder(), p)
                p.style = Paint.Style.FILL
                p.color = bg
                c.drawCircle(54f, 58.5f, 4.5f, p)
            }
            else -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 5f
                c.drawPath(folder(), p)
                p.strokeWidth = 4f
                c.drawCircle(54f, 58.5f, 9f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(54f, 58.5f, 3f, p)
            }
        }
        return bmp
    }

    fun pin(ctx: Context, style: Int, bg: Int, fg: Int): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)) return false
        val bmp = render(432, style, bg, fg, false)
        val info = ShortcutInfoCompat.Builder(ctx, "file_manager_custom")
            .setShortLabel("File Manager")
            .setIcon(IconCompat.createWithAdaptiveBitmap(bmp))
            .setIntent(Intent(ctx, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
            .build()
        try {
            ShortcutManagerCompat.updateShortcuts(ctx, listOf(info))
        } catch (e: Exception) {
            // not pinned yet; the pin request below creates it
        }
        return ShortcutManagerCompat.requestPinShortcut(ctx, info, null)
    }
}
