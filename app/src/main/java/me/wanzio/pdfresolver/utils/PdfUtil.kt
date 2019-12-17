package me.wanzio.pdfresolver.utils

import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.SystemClock
import android.util.Log
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

private const val TAG = "PdfUtil"

object PdfUtil {

    interface OnEventListener {
        fun onSuccess(filePath: String)
        fun onFail()
    }

//    fun createPdf(outputPath: String, path: String): String {
//        val document = Document()
//        val filePath = "$outputPath${File.separator}我是PDF你知道的_${SystemClock.uptimeMillis()}.pdf"
//        val output =
//            FileOutputStream(File(filePath))
//        PdfWriter.getInstance(document, output)
//        // 设置pdf背景
//        document.open()
//        val img = Image.getInstance(path)
//        img.scaleToFit(PageSize.A4.width, PageSize.A4.height)
//        img.setAbsolutePosition(
//            (PageSize.A4.width - img.scaledWidth) / 2,
//            (PageSize.A4.height - img.scaledHeight) / 2
//        )
//        document.add(img)
//        document.close()
//        return filePath
//    }

    fun createPDF(
        outputFilePath: String,
        icon: ByteArray? = null,
        filePaths: List<String>,
        listener: OnEventListener? = null
    ) {
        if (filePaths.isEmpty()) listener?.onFail()
        val outputDir = File("$outputFilePath")
        if (!outputDir.exists() || !outputDir.isDirectory) {
            Log.d(TAG, "outputFilePath 必须是文件夹")
            listener?.onFail()
        }
        val document = Document()
        val filePath = "$outputFilePath${File.separator}temp_pdf_${SystemClock.uptimeMillis()}.pdf"
        val output = FileOutputStream(File(filePath))
        val writer = PdfWriter.getInstance(document, output)
        document.open()
        try {
            filePaths.forEach { imagePath ->
                val img = File(imagePath)
                if (!img.exists()) {
                    listener?.onFail()
                    return
                }
                if (!addPage(document, imagePath)) {
                    listener?.onFail()
                }
                icon?.let {
                    addIcon(it, writer)
                }
            }
            document.close()
            listener?.onSuccess(filePath)
        } catch (e: Exception) {
            Log.d(TAG, e.message)
            if (document.isOpen) {
                document.close()
            }
            listener?.onFail()
        }
    }

    fun createPdfUsePdfDocument(
        outputFilePath: String,
        icon: ByteArray? = null,
        filePaths: List<String>,
        listener: OnEventListener? = null
    ) {
        if (filePaths.isEmpty()) listener?.onFail()
        val outputDir = File("$outputFilePath")
        if (!outputDir.exists() || !outputDir.isDirectory) {
            Log.d(TAG, "outputFilePath 必须是文件夹")
            listener?.onFail()
        }

        // 最低支持到API 19
        val document = PdfDocument()

        try {
            val paint = Paint()
            filePaths.forEach { imagePath ->
                val img = File(imagePath)
                if (!img.exists()) {
                    listener?.onFail()
                    return
                }
                // 0.创建A4大小的新页
                val pageInfo = PdfDocument.PageInfo.Builder(
                    PageSize.A4.width.toInt(),
                    PageSize.A4.height.toInt(),
                    1
                ).create()
                val page = document.startPage(pageInfo)
                page.canvas.apply {
                    // 1.获取图片宽/高
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(imagePath, options)
                    // 2.计算缩放比例
                    // 获取宽高比
                    val radio = options.outWidth / options.outHeight
                    val a4Radio = PageSize.A4.width / PageSize.A4.height
                    // 将宽高等比例缩放至A4大小
                    var width = PageSize.A4.width
                    var height = options.outHeight * (PageSize.A4.width / options.outWidth)
                    // 如果是宽图
                    if (radio < a4Radio) {
                        height = PageSize.A4.height
                        width = height * radio
                    }
                    // 3.加载绘制(不降低采样率)
                    options.inJustDecodeBounds = false
                    val image = BitmapFactory.decodeFile(imagePath, options)
                    Log.d(
                        TAG,
                        "width:$width,height:$height,A4 width:${PageSize.A4.width},A4 height:${PageSize.A4.height}"
                    )
                    save()
                    translate((PageSize.A4.width - width) / 2, (PageSize.A4.height - height) / 2)
                    scale(width / options.outWidth, height / options.outHeight)
                    drawBitmap(image, 0f, 0f, paint)
                    restore()
                    // 4.画水印
                    icon?.let {
                        val iconBitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                        translate(
                            PageSize.A4.width - iconBitmap.width - iconBitmap.height - 10f,
                            PageSize.A4.height - 10f
                        )
                        drawBitmap(iconBitmap, 0f, 0f, paint)
                    }
                }
                document.finishPage(page)
            }

            val filePath =
                "$outputFilePath${File.separator}temp_pdf_${SystemClock.uptimeMillis()}.pdf"
            val output = FileOutputStream(File(filePath))
            document.writeTo(output)
            listener?.onSuccess(filePath)
        } catch (e: Exception) {
            Log.d(TAG, e.message)
            listener?.onFail()
        } finally {
//            document.close()
        }

    }

    private fun addPage(document: Document, filePath: String): Boolean {
        if (!File(filePath).exists()) {
            return false
        }
        if (!document.newPage()) {
            Log.d(TAG, "不能创建新页，可能是内存不足、图片过大或者库本身原因")
            return false
        }

        document.add(generateImage(filePath))
        return true
    }

    private fun generateImage(filePath: String): Image {
        val image = Image.getInstance(filePath)
        // 获取宽高比
        val radio = image.width / image.height
        val a4Radio = PageSize.A4.width / PageSize.A4.height
        // 将宽高等比例缩放至A4大小
        var width = PageSize.A4.width
        var height = image.height * (PageSize.A4.width / image.width)
        // 如果是宽图
        if (radio < a4Radio) {
            height = PageSize.A4.height
            width = height * radio
        }
        image.scaleAbsolute(width, height)
        image.setAbsolutePosition(
            (PageSize.A4.width - width) / 2,
            (PageSize.A4.height - height) / 2
        )
        image.alignment = Element.ALIGN_CENTER
        return image
    }

    /**
     * 给当前页添加水印
     *
     * @param icon byte array of bitmap
     * @param writer pdf writer
     */
    private fun addIcon(icon: ByteArray, writer: PdfWriter) {
        val icon = Image.getInstance(icon)
        icon.isScaleToFitLineWhenOverflow = true
        icon.setAbsolutePosition(
            PageSize.A4.width - icon.width - 10f,
            10f
        )
        writer.directContent.addImage(icon)
    }

    fun extractThumbnail(
        source: Bitmap, width: Int, height: Int, options: Int = OPTIONS_NONE
    ): Bitmap {
        val scale: Float
        if (source.width < source.height) {
            scale = width / source.width.toFloat()
        } else {
            scale = height / source.height.toFloat()
        }
        val matrix = Matrix()
        matrix.setScale(scale, scale)
        return transform(
            matrix, source, width, height,
            OPTIONS_SCALE_UP or options
        )
    }

    /* Options used internally. */
    private val OPTIONS_NONE = 0x0
    private val OPTIONS_SCALE_UP = 0x1
    /**
     * Constant used to indicate we should recycle the input in
     * [.extractThumbnail] unless the output is the input.
     */
    val OPTIONS_RECYCLE_INPUT = 0x2

    /**
     * Constant used to indicate the dimension of mini thumbnail.
     * @hide Only used by media framework and media provider internally.
     */
    val TARGET_SIZE_MINI_THUMBNAIL = 320

    /**
     * Constant used to indicate the dimension of micro thumbnail.
     * @hide Only used by media framework and media provider internally.
     */
    val TARGET_SIZE_MICRO_THUMBNAIL = 96

    /**
     * Transform source Bitmap to targeted width and height.
     */
    private fun transform(
        scaler: Matrix?,
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        options: Int
    ): Bitmap {
        var scaler = scaler
        val scaleUp = options and OPTIONS_SCALE_UP != 0
        val recycle = options and OPTIONS_RECYCLE_INPUT != 0

        val deltaX = source.width - targetWidth
        val deltaY = source.height - targetHeight
        if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
            /*
            * In this case the bitmap is smaller, at least in one dimension,
            * than the target.  Transform it by placing as much of the image
            * as possible into the target and leaving the top/bottom or
            * left/right (or both) black.
            */
            val b2 = Bitmap.createBitmap(
                targetWidth, targetHeight,
                Bitmap.Config.ARGB_8888
            )
            val c = Canvas(b2)

            val deltaXHalf = Math.max(0, deltaX / 2)
            val deltaYHalf = Math.max(0, deltaY / 2)
            val src = Rect(
                deltaXHalf,
                deltaYHalf,
                deltaXHalf + Math.min(targetWidth, source.width),
                deltaYHalf + Math.min(targetHeight, source.height)
            )
            val dstX = (targetWidth - src.width()) / 2
            val dstY = (targetHeight - src.height()) / 2
            val dst = Rect(
                dstX,
                dstY,
                targetWidth - dstX,
                targetHeight - dstY
            )
            c.drawBitmap(source, src, dst, null)
            if (recycle) {
                source.recycle()
            }
            c.setBitmap(null)
            return b2
        }
        val bitmapWidthF = source.width.toFloat()
        val bitmapHeightF = source.height.toFloat()

        val bitmapAspect = bitmapWidthF / bitmapHeightF
        val viewAspect = targetWidth.toFloat() / targetHeight

        if (bitmapAspect > viewAspect) {
            val scale = targetHeight / bitmapHeightF
            if (scale < .9f || scale > 1f) {
                scaler!!.setScale(scale, scale)
            } else {
                scaler = null
            }
        } else {
            val scale = targetWidth / bitmapWidthF
            if (scale < .9f || scale > 1f) {
                scaler!!.setScale(scale, scale)
            } else {
                scaler = null
            }
        }

        val b1: Bitmap
        if (scaler != null) {
            // this is used for minithumb and crop, so we want to filter here.
            b1 = Bitmap.createBitmap(
                source, 0, 0,
                source.width, source.height, scaler, true
            )
        } else {
            b1 = source
        }

        if (recycle && b1 != source) {
            source.recycle()
        }

        val dx1 = Math.max(0, b1.width - targetWidth)
        val dy1 = Math.max(0, b1.height - targetHeight)

        val b2 = Bitmap.createBitmap(
            b1,
            dx1 / 2,
            dy1 / 2,
            targetWidth,
            targetHeight
        )

        if (b2 != b1) {
            if (recycle || b1 != source) {
                b1.recycle()
            }
        }

        return b2
    }
}

fun File.deleteFiles(deleteSelf: Boolean = false) {
    if (exists() && isDirectory) {
        listFiles()?.forEach {
            if (it.isDirectory) {
                it.deleteFiles()
            } else {
                it.delete()
            }
        }
    }
    if (deleteSelf) {
        delete()
    }
}