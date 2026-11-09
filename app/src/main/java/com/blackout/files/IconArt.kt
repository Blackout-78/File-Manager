package com.blackout.files

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.IconCompat

object IconArt {
    val BG = 0xFF0B0D12.toInt()
    val DEF_BACK = 0xFF5B4FD0.toInt()
    val DEF_FRONT = 0xFF8B7CF6.toInt()
    val DEF_KEY = 0xFFF5B93A.toInt()

    private fun backPath(): Path = Path().apply {
        moveTo(24f, 32f); lineTo(46f, 32f); lineTo(52f, 39f); lineTo(84f, 39f)
        quadTo(86f, 39f, 86f, 41f); lineTo(86f, 76f)
        quadTo(86f, 78f, 84f, 78f); lineTo(26f, 78f)
        quadTo(24f, 78f, 24f, 76f); close()
    }

    private fun frontPath(): Path = Path().apply {
        moveTo(24f, 50f); lineTo(86f, 50f); lineTo(86f, 76f)
        quadTo(86f, 78f, 84f, 78f); lineTo(26f, 78f)
        quadTo(24f, 78f, 24f, 76f); close()
    }

    private fun stemPath(): Path = Path().apply {
        moveTo(52.6f, 63.5f); lineTo(57.4f, 63.5f); lineTo(59f, 72f); lineTo(51f, 72f); close()
    }

    fun render(size: Int, back: Int, front: Int, key: Int, rounded: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val s = size / 108f
        c.scale(s, s)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.FILL
        p.color = BG
        if (rounded) {
            c.drawRoundRect(RectF(0f, 0f, 108f, 108f), 26f, 26f, p)
        } else {
            c.drawRect(0f, 0f, 108f, 108f, p)
        }
        p.color = back
        c.drawPath(backPath(), p)
        p.color = front
        c.drawPath(frontPath(), p)
        p.color = ColorUtils.blendARGB(front, Color.WHITE, 0.35f)
        c.drawRect(24f, 49f, 86f, 51.5f, p)
        p.color = key
        c.drawCircle(55f, 61f, 5f, p)
        c.drawPath(stemPath(), p)
        return bmp
    }

    fun pin(ctx: Context, back: Int, front: Int, key: Int): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)) return false
        val bmp = render(432, back, front, key, false)
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
