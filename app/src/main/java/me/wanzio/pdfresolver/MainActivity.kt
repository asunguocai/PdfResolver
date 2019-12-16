package me.wanzio.pdfresolver

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.github.barteksc.pdfviewer.PDFView
import me.wanzio.pdfresolver.utils.ImageUtil
import me.wanzio.pdfresolver.utils.PdfUtil
import me.wanzio.pdfresolver.utils.PermissionUtil
import java.io.File
import kotlin.concurrent.thread

const val IMAGE_REQUEST_CODE = 10086
private const val TAG = "MainActivity"

fun Activity.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

class MainActivity : AppCompatActivity(), PermissionFragment.OnGrantListener {
    override fun onGranted(permissions: List<String>) {
        permissions.forEach {
            Log.d(TAG, "on Granted: $it")
        }
        startSelectPicView()
        mBtnSelectPic.isEnabled = true
    }

    override fun onGrantFailed(permissions: List<String>) {
        permissions.forEach {
            Log.d(TAG, "on Denied: $it")
        }
        showToast("request permission faild.")
        mBtnSelectPic.isEnabled = true
    }

    private lateinit var mPdfViewer: PDFView
    private lateinit var mBtnSelectPic: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mBtnSelectPic = findViewById<Button>(R.id.btn_select_img)
        mBtnSelectPic.setOnClickListener {
            it.checkDouleClick(500L) {
                mBtnSelectPic.isEnabled = false
                checkPermissionAndSelectPic()
            }
        }

        mPdfViewer = findViewById(R.id.pdfview)
    }


    private fun checkPermissionAndSelectPic() {
        PermissionUtil.requirePermission(
            this,
            setOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            this
        )
    }

    private fun startSelectPicView() {
        val intent = ImageUtil.createSelectImgIntent()
        if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            startActivityForResult(
                ImageUtil.createSelectImgIntent(),
                IMAGE_REQUEST_CODE
            )
        } else {
            showToast("Can not find photo album")
            mBtnSelectPic.isEnabled = false
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 666) {
            grantResults.forEachIndexed { index, it ->
                if (it != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this@MainActivity, "unGranted permission", Toast.LENGTH_LONG)
                        .show()
                    return
                } else {
                    Toast.makeText(this@MainActivity, "${permissions[index]}", Toast.LENGTH_LONG)
                        .show()
                }
            }

            startSelectPicView()
        }
    }

    private lateinit var mPdfPath: String

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectImage = data!!.data!!
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(selectImage, filePathColumn, null, null, null)!!
            cursor.moveToFirst()
            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val path = cursor.getString(columnIndex)
            cursor.close()
            showToast("select success")
            thread {
                mPdfPath = PdfUtil.createPdf(filesDir.path, path)
                runOnUiThread {
                    showToast("pdf created")
                    loadPdf()
                    showToast("pdf loaded")
                }
            }
        }
    }

    private fun loadPdf() {
        mPdfViewer.fromFile(File(mPdfPath)).load()
    }

}

fun View.checkDouleClick(time: Long, block: () -> Unit) {
    var confirm = true
    tag?.let {
        confirm = it as Long - SystemClock.uptimeMillis() < time
    }
    if (confirm) {
        block()
    }
    tag = SystemClock.uptimeMillis()
}