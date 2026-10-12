package com.sabin.notes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import java.io.File

internal fun Context.imageFiles() = ImageFiles(File(filesDir, "images"))

/** Copies a picked photo into app-private storage, downscaled to [ImageScale.MAX_EDGE] and JPEG compressed. Returns the new file name or null. */
internal fun importImage(context: Context, uri: Uri, files: ImageFiles): String? = runCatching {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    val opts = BitmapFactory.Options().apply { inSampleSize = ImageScale.sampleSize(bounds.outWidth, bounds.outHeight, ImageScale.MAX_EDGE) }
    var bmp = resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, opts) } ?: return@runCatching null
    val orientation = resolver.openInputStream(uri)!!.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
    val target = ImageScale.fit(bmp.width, bmp.height)
    val m = Matrix().apply {
        if (target.width != bmp.width) postScale(target.width.toFloat() / bmp.width, target.height.toFloat() / bmp.height)
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
        }
    }
    if (!m.isIdentity) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    val name = files.newName("jpg")
    files.file(name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    name
}.getOrNull()

/** Decodes a stored photo no larger than [maxEdge] on its longest side, or null if it is missing or unreadable. */
internal fun loadImage(files: ImageFiles, name: String, maxEdge: Int): Bitmap? = runCatching {
    val f = files.fileOrNull(name)?.takeIf { it.exists() } ?: return@runCatching null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.path, bounds)
    BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = ImageScale.sampleSize(bounds.outWidth, bounds.outHeight, maxEdge) })
}.getOrNull()
