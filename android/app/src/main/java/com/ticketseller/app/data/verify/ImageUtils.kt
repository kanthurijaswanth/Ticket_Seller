package com.ticketseller.app.data.verify

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun loadAndCompress(context: Context, uri: Uri, maxDim: Int = 1024, quality: Int = 60): Bitmap? {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(input) ?: return null
        input.close()
        val ratio = maxOf(original.width, original.height).toFloat() / maxDim
        val target = if (ratio > 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width / ratio).toInt(),
                (original.height / ratio).toInt(),
                true,
            )
        } else original
        return target
    }

    fun bitmapToBase64(bmp: Bitmap, quality: Int = 60): String {
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    fun base64ToBitmap(b64: String): Bitmap? {
        if (b64.isBlank()) return null
        val bytes = Base64.decode(b64, Base64.NO_WRAP)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
