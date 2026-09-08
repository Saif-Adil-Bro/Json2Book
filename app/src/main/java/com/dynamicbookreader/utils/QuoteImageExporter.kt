package com.dynamicbookreader.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object QuoteImageExporter {

    /**
     * Captures an Android View (or ComposeView) as a Bitmap.
     */
    fun createBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(
            view.width.coerceAtLeast(1),
            view.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    /**
     * Saves a bitmap to cache directory and returns a shareable content Uri.
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String = "quote_card_${System.currentTimeMillis()}.png"): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "quote_cards")
            if (!cachePath.exists()) {
                cachePath.mkdirs()
            }
            val file = File(cachePath, fileName)
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Launches Android Share sheet for the generated quote image.
     */
    fun shareQuoteImage(context: Context, imageUri: Uri, quoteText: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, "\"$quoteText\"\n\n— Dynamic Book Reader")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "উক্তি কার্ড শেয়ার করুন"))
    }
}
