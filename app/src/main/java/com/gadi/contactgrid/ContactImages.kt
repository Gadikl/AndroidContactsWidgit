package com.gadi.contactgrid

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlin.math.max

/** Builds the square, rounded-corner images shown in each grid cell. */
object ContactImages {

    private val PALETTE = intArrayOf(
        0xFF1F6FEB.toInt(), 0xFF8250DF.toInt(), 0xFFD1242F.toInt(),
        0xFF1A7F37.toInt(), 0xFFBC4C00.toInt(), 0xFF0E7490.toInt(),
        0xFFBF3989.toInt(), 0xFF6E7781.toInt(), 0xFF9A6700.toInt(),
    )

    private fun cornerRadius(size: Int) = size * 0.18f

    /** Contact photo (high-res if available), or null if none / no permission. */
    fun photo(ctx: Context, slot: Slot, size: Int): Bitmap? {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) return null
        if (slot.contactId < 0) return null
        return try {
            val uri = slot.lookupKey?.let { ContactsContract.Contacts.getLookupUri(slot.contactId, it) }
                ?: ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, slot.contactId)
            val bytes = ContactsContract.Contacts
                .openContactPhotoInputStream(ctx.contentResolver, uri, true)
                ?.use { it.readBytes() }
                ?: return null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= size && bounds.outHeight / (sample * 2) >= size) {
                sample *= 2
            }
            val src = BitmapFactory.decodeByteArray(
                bytes, 0, bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sample }
            ) ?: return null
            roundedSquare(src, size)
        } catch (e: Exception) {
            null
        }
    }

    /** Center-crops [src] to a square of [size] px with rounded corners. */
    private fun roundedSquare(src: Bitmap, size: Int): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val scale = max(size / src.width.toFloat(), size / src.height.toFloat())
        val m = Matrix().apply {
            setScale(scale, scale)
            postTranslate((size - src.width * scale) / 2f, (size - src.height * scale) / 2f)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(m)
            }
        }
        val r = cornerRadius(size)
        Canvas(out).drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), r, r, paint)
        return out
    }

    /** Colored tile with the contact's initials – used when there's no photo. */
    fun initials(name: String, size: Int): Bitmap {
        val color = PALETTE[Math.floorMod(name.hashCode(), PALETTE.size)]
        val text = name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.substring(0, it.offsetByCodePoints(0, 1)).uppercase() }
            .ifEmpty { "?" }
        return textTile(text, color, Color.WHITE, size, 0.38f)
    }

    /** "+" tile for an empty slot. */
    fun empty(size: Int): Bitmap = textTile("+", 0x33FFFFFF, 0xCCFFFFFF.toInt(), size, 0.5f)

    private fun textTile(text: String, bg: Int, fg: Int, size: Int, textRatio: Float): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val r = cornerRadius(size)
        c.drawRoundRect(
            RectF(0f, 0f, size.toFloat(), size.toFloat()), r, r,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = bg }
        )
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = fg
            textSize = size * textRatio
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val y = size / 2f - (tp.descent() + tp.ascent()) / 2f
        c.drawText(text, size / 2f, y, tp)
        return out
    }
}
