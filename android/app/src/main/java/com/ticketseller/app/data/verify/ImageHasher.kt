package com.ticketseller.app.data.verify

import android.graphics.Bitmap
import android.graphics.Color

object ImageHasher {
    fun pHash(bitmap: Bitmap, size: Int = 16): String {
        val gray = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val pixels = IntArray(size * size)
        gray.getPixels(pixels, 0, size, 0, 0, size, size)
        val lums = IntArray(pixels.size) { i ->
            val p = pixels[i]
            (Color.red(p) + Color.green(p) + Color.blue(p)) / 3
        }
        val avg = lums.average()
        val sb = StringBuilder()
        for (l in lums) sb.append(if (l > avg) '1' else '0')
        return binaryToHex(sb.toString())
    }

    private fun binaryToHex(bin: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < bin.length) {
            val chunk = bin.substring(i, minOf(i + 4, bin.length)).padEnd(4, '0')
            sb.append(chunk.toInt(2).toString(16))
            i += 4
        }
        return sb.toString()
    }
}
