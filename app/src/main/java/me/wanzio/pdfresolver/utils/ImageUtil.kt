package me.wanzio.pdfresolver.utils

import android.content.Intent
import android.provider.MediaStore

object ImageUtil {

    fun createSelectImgIntent(): Intent {
        return Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
    }

}