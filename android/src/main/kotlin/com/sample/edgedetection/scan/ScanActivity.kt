package com.sample.edgedetection.scan

//import android.media.ExifInterface
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.Display
import android.view.MenuItem
import android.view.SurfaceView
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.sample.edgedetection.R
import com.sample.edgedetection.REQUEST_CODE
import com.sample.edgedetection.SCANNED_RESULT
import com.sample.edgedetection.base.BaseActivity
import com.sample.edgedetection.view.PaperRectangle
import kotlinx.android.synthetic.main.activity_scan.*
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*


class ScanActivity : BaseActivity(), IScanView.Proxy {

    private val REQUEST_CAMERA_PERMISSION = 0

    private lateinit var mPresenter: ScanPresenter


    override fun provideContentViewId(): Int = R.layout.activity_scan

    override fun initPresenter() {
        mPresenter = ScanPresenter(this, this)
    }

    override fun prepare() {
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "loading opencv error, exit")
            finish()
        }
        if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                            android.Manifest.permission.CAMERA,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_CAMERA_PERMISSION
            )
        } else if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
            )
        } else if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CAMERA_PERMISSION
            )
        }

        shut.setOnClickListener {
            if (mPresenter.canShut) {
                mPresenter.shut()
            }
        }

        //갤러리에서 이미지 선택 후 결과 받는 함수
        val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result: ActivityResult -> if ( result.resultCode == Activity.RESULT_OK) {
                    val intent = result.data // Handle the Intent //do stuff here
                    val uri: Uri = intent!!.data!!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val mimeTypeMap= MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri))
                if(mimeTypeMap.equals("jpg") || mimeTypeMap.equals("jpeg")){
                    onImageSelected(uri)
                }else{
                    //jpg가 아닐 경우 파일 포맷 변환
                    try {
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                            val source = ImageDecoder.createSource(this.contentResolver, uri)
                            val bitmap = ImageDecoder.decodeBitmap(source)
                            val bitmapUri = getImageUri(this,bitmap)
                                onImageSelected(bitmapUri!!)
                        }else{
                            val source =  MediaStore.Images.Media.getBitmap(contentResolver,uri)
                            val bitmapUri = getImageUri(this,source)
                            onImageSelected(bitmapUri!!)
                        }
                    }catch (e:Exception){
                        Log.e(TAG, e.toString())
                    }
                }
            }
        } }

        gallery.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK)
            gallery.data=MediaStore.Images.Media.INTERNAL_CONTENT_URI
            gallery.type="image/*"
            startForResult.launch(gallery)
            }
    }

    private fun getImageUri(inContext: Context?, inImage: Bitmap?): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage?.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext?.contentResolver, inImage, "Title" + " - " + Calendar.getInstance().getTime(), null)
        return Uri.parse(path)
    }



    override fun onStart() {
        super.onStart()
        mPresenter.start()
    }

    override fun onStop() {
        super.onStop()
        mPresenter.stop()
    }

    override fun exit() {
        finish()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {

        var allGranted = false
        var indexPermission = -1

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.count() == 1) {
                if (permissions.indexOf(android.Manifest.permission.CAMERA) >= 0) {
                    indexPermission = permissions.indexOf(android.Manifest.permission.CAMERA)
                }
                if (permissions.indexOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) >= 0) {
                    indexPermission =
                            permissions.indexOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                if (indexPermission >= 0 && grantResults[indexPermission] == PackageManager.PERMISSION_GRANTED) {
                    allGranted = true
                }
            }

            if (grantResults.count() == 2 && (
                            grantResults[permissions.indexOf(android.Manifest.permission.CAMERA)] == PackageManager.PERMISSION_GRANTED
                                    && grantResults[permissions.indexOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)] == PackageManager.PERMISSION_GRANTED)
            ) {
                allGranted = true
            }
        }

        if (allGranted) {
            showMessage(R.string.camera_grant)
            mPresenter.initCamera()
            mPresenter.updateCamera()
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

    override fun getCurrentDisplay(): Display? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            this.display
        } else {
            this.windowManager.defaultDisplay
        }
    }

    override fun getSurfaceView(): SurfaceView = surface

    override fun getPaperRect(): PaperRectangle = paper_rect

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (null != data && null != data.extras) {
                    val path = data.extras!!.getString(SCANNED_RESULT)
                    setResult(Activity.RESULT_OK, Intent().putExtra(SCANNED_RESULT, path))
                    finish()
                }
            }
        }

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data!!.data!!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val mimeTypeMap= MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri))
                if(mimeTypeMap.equals("jpg") || mimeTypeMap.equals("jpeg")){
                    onImageSelected(uri)
                }else{
                    // 다이얼로그를 생성하기 위해 Builder 클래스 생성자를 이용해 줍니다.
                    val builder = AlertDialog.Builder(this, R.style.AppCompatAlertDialog)
                    builder.setTitle("사용 불가 이미지 파일")
                        .setMessage("jpg 또는 jpeg 파일을 선택해주세요.")
                        .setPositiveButton("확인",
                            DialogInterface.OnClickListener { dialog, id ->
                            })
                    // 다이얼로그를 띄워주기
                    builder.show()
                }

            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun onImageSelected(imageUri: Uri) {
        val iStream: InputStream = contentResolver.openInputStream(imageUri)!!
        try{
             val exif = ExifInterface(iStream)
            Log.i(TAG, "onImageSelected: 3 :: "+iStream.read())
            var rotation = -1
            val orientation: Int = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotation = Core.ROTATE_90_CLOCKWISE
                ExifInterface.ORIENTATION_ROTATE_180 -> rotation = Core.ROTATE_180
                ExifInterface.ORIENTATION_ROTATE_270 -> rotation = Core.ROTATE_90_COUNTERCLOCKWISE
            }
            Log.i(TAG, "rotation:" + rotation)

            var imageWidth = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0).toDouble()
            var imageHeight = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0).toDouble()
            if (rotation == Core.ROTATE_90_CLOCKWISE || rotation == Core.ROTATE_90_COUNTERCLOCKWISE) {
                imageWidth = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0).toDouble()
                imageHeight = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0).toDouble()
            }
            Log.i(TAG, "width:" + imageWidth)
            Log.i(TAG, "height:" + imageHeight)

            val inputData: ByteArray? = getBytes(contentResolver.openInputStream(imageUri)!!)
            val mat = Mat(Size(imageWidth, imageHeight), CvType.CV_8U)
            mat.put(0, 0, inputData)
            val pic = Imgcodecs.imdecode(mat, Imgcodecs.CV_LOAD_IMAGE_UNCHANGED)
            if (rotation > -1) Core.rotate(pic, pic, rotation)
            mat.release()
            mPresenter.detectEdge(pic)
        }catch(e: Throwable){
            Log.d(TAG, "ExifInterface:" + e.message)
        }
    }

    @Throws(IOException::class)
    fun getBytes(inputStream: InputStream): ByteArray? {
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var len = 0
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        return byteBuffer.toByteArray()
    }
}