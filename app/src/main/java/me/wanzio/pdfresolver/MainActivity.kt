package me.wanzio.pdfresolver

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.github.barteksc.pdfviewer.PDFView
import me.wanzio.pdfresolver.utils.ImageUtil
import me.wanzio.pdfresolver.utils.PdfUtil
import me.wanzio.pdfresolver.utils.PermissionUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import kotlin.concurrent.thread

const val IMAGE_REQUEST_CODE = 10086
private const val TAG = "MainActivity"

fun Activity.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

class MainActivity : AppCompatActivity(), PermissionFragment.OnGrantListener,
    PdfUtil.OnEventListener {
    /**
     * 非UI线程修改是否可见
     */
    override fun onFail() {
        runOnUiThread {
            switchProgress(false)
            Log.d(TAG, "pdf create fail")
        }
    }

    override fun onSuccess(filePath: String) {
        runOnUiThread {
            switchProgress(false)
            Log.d(TAG, "pdf create success")
            loadPdf(filePath)
        }
    }

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
    private lateinit var mBtnCreatePdf: Button
    private lateinit var mVProgress: View
    private lateinit var mTvPageIndecitor: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mBtnSelectPic = findViewById<Button>(R.id.btn_add_img)
        mBtnSelectPic.setOnClickListener {
            it.checkDouleClick {
                mBtnSelectPic.isEnabled = false
                checkPermissionAndSelectPic()
            }
        }

        mBtnCreatePdf = findViewById(R.id.btn_create)
        mBtnCreatePdf.setOnClickListener {
            it.checkDouleClick {
                switchProgress(true)
                mTvPageIndecitor.isVisible = false
                thread {
                    createPdf()
                }
            }
        }
        mPdfViewer = findViewById(R.id.pdfview)
        mVProgress = findViewById(R.id.v_progress)
        mTvPageIndecitor = findViewById(R.id.tv_pager_indicotr)
    }

    private fun createPdf() {
        var bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_senior_watermark)
        bitmap = PdfUtil.extractThumbnail(bitmap, bitmap.width / 18, bitmap.height / 18)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        PdfUtil.createPDF(
            filesDir.path,
            baos.toByteArray(),
            mImageList,
            this
        )
    }


    private fun switchProgress(isShow: Boolean) {
        mBtnCreatePdf.isEnabled = !isShow
        if (isShow && !mVProgress.isVisible) mVProgress.isVisible = true
        else if (!isShow && mVProgress.isVisible) mVProgress.isVisible = false
    }


    private fun checkPermissionAndSelectPic() {
        PermissionUtil.requirePermissions(
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

    private val mImageList = LinkedList<String>()

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
            showToast("add a new image")
            mImageList.add(path)
        }
    }

    private fun loadPdf(filePath: String) {
        mPdfViewer.fromFile(File(filePath))
            .onPageChange({ page, pageCount ->
                mTvPageIndecitor.setText(getString(R.string.cur_page, page+1, pageCount))
            })
            .onLoad {
                mTvPageIndecitor.isVisible = true
            }
            .load()
    }

}

fun View.checkDouleClick(time: Long = 500L, block: () -> Unit) {
    var confirm = true
    tag?.let {
        confirm = it as Long - SystemClock.uptimeMillis() < time
    }
    if (confirm) {
        block()
    }
    tag = SystemClock.uptimeMillis()
}