package me.wanzio.pdfresolver.utils

import android.content.Context
import android.os.SystemClock
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfDocument
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL


object PdfUtil {

    fun createPdf(outputPath: String, path: String):String {
        val document = Document()
        val filePath = "$outputPath${File.separator}我是PDF你知道的_${SystemClock.uptimeMillis()}.pdf"
        val output =
            FileOutputStream(File(filePath))
        PdfWriter.getInstance(document, output)
        // 设置pdf背景
        document.open()
        val img = Image.getInstance(path)
        img.scaleToFit(PageSize.A4.width, PageSize.A4.height)
        img.setAbsolutePosition(
            (PageSize.A4.width - img.scaledWidth) / 2,
            (PageSize.A4.height - img.scaledHeight) / 2
        )
        document.add(img)
        document.close()
        return filePath
    }
}