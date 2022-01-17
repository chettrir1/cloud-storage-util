package com.evolveasia.cloudstorageutil.utils

import android.graphics.*
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.InputStream

internal fun streamToByteArray(stream: InputStream): ByteArray {
    val buffer = ByteArray(1024)
    val os = ByteArrayOutputStream()

    var line = 0
    // read bytes from stream, and store them in buffer
    while (line != -1) {
        // Writes bytes from byte array (buffer) into output stream.
        os.write(buffer, 0, line)
        line = stream.read(buffer)
    }
    stream.close()
    os.flush()
    os.close()
    return os.toByteArray()
}

internal fun decodeSampledBitmapFromResource(
    data: ByteArray,
    reqWidth: Int,
    reqHeight: Int,
    waterMarkInfo: AwsMetaInfo.WaterMarkInfo?
): Bitmap {
    // First decode with inJustDecodeBounds=true to check dimensions
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeByteArray(data, 0, data.size, options)

    // Calculate inSampleSize
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

    // Decode bitmap with inSampleSize set
    options.inJustDecodeBounds = false
    return BitmapFactory.decodeByteArray(data, 0, data.size, options)
    /*  return waterMarkInfo?.let {
          addAwsWaterMark(waterMarkInfo, data, 0, data.size, options)
      } ?: BitmapFactory.decodeByteArray(data, 0, data.size, options)
  */
}

internal fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    // Raw height and width of image
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

internal fun addAwsWaterMark(
    awsMetaInfo: AwsMetaInfo,
    bitmap: Bitmap,
): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val result = Bitmap.createBitmap(w, h, bitmap.config)
    val canvas = Canvas(result)
    val blurMaskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.NORMAL)
    val backgroundPaint = Paint()
    backgroundPaint.color = Color.BLACK
    backgroundPaint.alpha = 50
    val paint = Paint()
    paint.color = Color.RED
    paint.textSize = 30f
    paint.isAntiAlias = true
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    backgroundPaint.maskFilter = blurMaskFilter
    val fontMetrics = Paint.FontMetrics()
    var yAxisPosition = h - 16
    paint.getFontMetrics(fontMetrics)
    paint.color = Color.WHITE
    awsMetaInfo.imageMetaInfo.waterMarkInfo?.waterMarkInfoList?.asReversed()?.forEach {
        val value = it.second
        canvas.drawRect(
            0f,
            yAxisPosition + fontMetrics.top,
            paint.measureText(value),
            yAxisPosition + fontMetrics.bottom,
            backgroundPaint
        )
        canvas.drawText(
            value,
            16f,
            yAxisPosition.toFloat(),
            paint
        )
        yAxisPosition -= 35
    }
    val out = FileOutputStream(awsMetaInfo.imageMetaInfo.imagePath)
    result.compress(Bitmap.CompressFormat.JPEG, 80, out)
    out.flush()
    out.close()
    bitmap.recycle()
    return result
}