package com.example.ml

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.TextAppearanceSpan
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel
import com.google.firebase.ml.vision.text.FirebaseVisionText
import java.io.File
import kotlin.math.abs

/**
 * Created by 11837 on 2018/6/5.
 */

class MainActivity : BaseActivity() {

    private val REQUEST_CODE_GALLERY = 100
    private val REQUEST_CODE_CAMARER = 101
    private val REQUEST_CODE_TAKE_PHOTO = 103
    private var result: TextView? = null
    private val file: File? = null
    private var appBarLayout: AppBarLayout? = null
    private var index: Int? = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
            window.navigationBarColor = ContextCompat.getColor(this, android.R.color.white)
        }
        result = findViewById(R.id.result)
        appBarLayout = findViewById(R.id.appbar_layout)
        val spinner = findViewById<Spinner>(R.id.spinner)
        spinner.post {
            spinner.dropDownVerticalOffset = spinner.height.times(1.1).toInt()
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    index = position
                }

                override fun onNothingSelected(parent: AdapterView<*>) {

                }
            }
        }
        appBarLayout!!.post {
            appBarLayout!!.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, offset ->
                val max = appBarLayout!!.totalScrollRange
                if (offset == 0) {//完全展开
                    setStatusBarLight(true)
                } else if (abs(offset) == max) {//完全折叠
                    setStatusBarLight(false)
                }
            })
            appBarLayout!!.setExpanded(false)
        }
    }

    private fun setStatusBarLight(isLight: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.e("ml", "isLight : $isLight")
            if (isLight) {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
                window.statusBarColor = ContextCompat.getColor(this, R.color.translate_alpha)
            } else {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_VISIBLE)
                window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            window.decorView.systemUiVisibility = (
//                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE//防止系统栏隐藏时内容区域大小发生变化
//                    )
        }
    }

    private fun startML(imgPath: String?) {
        val file = File(imgPath)
        if (file.exists()) {
            val imageView = findViewById<View>(R.id.img) as ImageView
            val uri = Uri.fromFile(file)
            imageView.setImageURI(uri)
            showDialog()
            when (index) {
                0 -> {
                    processImageText(uri)
                }
                1 -> {
                    processImageLabel(uri)
                }
                2 -> {
                    processImageFace(uri)
                }
                3 -> {
                    processImageBarCode(uri)
                }
                else -> {
                    processLanuage()
                }
            }
        }
    }

    /**
     *本地识别图片中的文本
     * 对于中文、日文和韩文文本（仅云端 API 支持）
     */
    private fun processImageText(uri: Uri) {
        val firebaseVisionImage = FirebaseVisionImage.fromFilePath(this, uri)
        val deviceTextRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
        deviceTextRecognizer.processImage(firebaseVisionImage)
            .addOnSuccessListener { firebaseVisionText: FirebaseVisionText? ->
                result!!.text = firebaseVisionText!!.text
            }.addOnFailureListener { exception: Exception ->
                result!!.text = exception.toString()
            }.addOnCompleteListener {
                dismiss()
            }
    }

    /**
     *本地识别图片标签
     */
    private fun processImageLabel(uri: Uri) {
        val firebaseVisionImage = FirebaseVisionImage.fromFilePath(this, uri)
        val deviceImageLabeler = FirebaseVision.getInstance().onDeviceImageLabeler
        deviceImageLabeler.processImage(firebaseVisionImage)
            .addOnSuccessListener { mutableList: MutableList<FirebaseVisionImageLabel>? ->
                val builder = StringBuilder()
                if (mutableList != null && mutableList.isNotEmpty()) {
                    for (imageLabel in mutableList) {
                        if (builder.isNotEmpty()) {
                            builder.append("\n")
                        }
                        builder.append(imageLabel.text)
                            .append(" 可信度 -> ${imageLabel.confidence}")
                    }
                    result!!.text = builder
                }
                if (builder.isEmpty()) {
                    result!!.text = "未检测出图片标签信息"
                }
                appBarLayout!!.setExpanded(false, true)
            }.addOnFailureListener { exception: Exception ->
                result!!.text = exception.toString()
            }.addOnCompleteListener {
                dismiss()
            }
    }

    /**
     *本地识别条形码
     */
    private fun processImageBarCode(uri: Uri) {
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(
                FirebaseVisionBarcode.FORMAT_QR_CODE,
                FirebaseVisionBarcode.FORMAT_AZTEC
            )
            .build()
        val firebaseVisionImage = FirebaseVisionImage.fromFilePath(this, uri)
        val visionBarcodeDetector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)
        visionBarcodeDetector.detectInImage(firebaseVisionImage)
            .addOnSuccessListener { barcodes: MutableList<FirebaseVisionBarcode>? ->
                val builder = StringBuilder()
                if (barcodes != null && barcodes.isNotEmpty()) {
                    for (barcode in barcodes) {
                        if (builder.isNotEmpty()) builder.append("\n")
                        val bounds = barcode.boundingBox
                        val corners = barcode.cornerPoints
                        val rawValue = barcode.rawValue
                        // See API reference for complete list of supported types
                        when (val valueType = barcode.valueType) {
                            FirebaseVisionBarcode.TYPE_WIFI -> {
                                val ssid = barcode.wifi!!.ssid
                                val password = barcode.wifi!!.password
                                val type = barcode.wifi!!.encryptionType
                                builder.append("WiFi信息").append("\n")
                                    .append("ssid -> $ssid").append("\n")
                                    .append("password -> $password").append("\n")
                                    .append("type -> $type").append("\n")
                            }
                            FirebaseVisionBarcode.TYPE_URL -> {
                                val title = barcode.url!!.title
                                val url = barcode.url!!.url
                                builder.append("URL信息").append("\n")
                                    .append("title -> $title").append("\n")
                                    .append("url -> $url").append("\n")
                                    .append("url -> $url").append("\n")
                            }
                            else -> {
                                builder.append("条形码信息").append("\n")
                                    .append("bounds -> $bounds").append("\n")
                                    .append("corners -> $corners").append("\n")
                                    .append("rawValue -> $rawValue").append("\n")
                                    .append("valueType -> $valueType").append("\n")
                            }
                        }
                    }
                }
                if (builder.isEmpty()) {
                    result!!.text = "未检测出条形码信息"
                } else {
                    result!!.text = builder
                }
                appBarLayout!!.setExpanded(false, true)
            }.addOnFailureListener { exception: Exception ->
                result!!.text = exception.toString()
            }.addOnCompleteListener {
                dismiss()
            }
    }

    /**
     *本地语言翻译,需要先下载对应的语言模型，才能完成翻译
     */
    private fun processLanuage() {
        //创建翻译器
        val options = FirebaseTranslatorOptions.Builder()
            .setSourceLanguage(FirebaseTranslateLanguage.EN)
            .setTargetLanguage(FirebaseTranslateLanguage.ZH)
            .build()
        val englishGermanTranslator = FirebaseNaturalLanguage.getInstance().getTranslator(options)
        //下载语言模型
        englishGermanTranslator.downloadModelIfNeeded()
            .addOnSuccessListener {
                //开始翻译
                englishGermanTranslator.translate("Hi,My name is xt.sun...")
                    .addOnSuccessListener { translatedText ->
                        result!!.text = String.format("翻译成功 \n 结果 : %s", translatedText)
                    }
                    .addOnFailureListener { exception ->
                        result!!.text = String.format("翻译失败 \n error : %s", exception.message)
                    }
                    .addOnCompleteListener {
                        dismiss()
                    }
            }
            .addOnFailureListener { exception ->
                result!!.text = String.format("语言模型下载失败 \n error : %s", exception.message)
                dismiss()
            }
    }

    /**
     *本地识人脸识别
     */
    private fun processImageFace(uri: Uri) {
        // High-accuracy landmark detection and face classification
        val highAccuracyOpts = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .build()

        // Real-time contour detection of multiple faces
        val realTimeOpts = FirebaseVisionFaceDetectorOptions.Builder()
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
            .build()

        val firebaseVisionImage = FirebaseVisionImage.fromFilePath(this, uri)
        val deviceFaceDetector = FirebaseVision.getInstance().getVisionFaceDetector(highAccuracyOpts)
        deviceFaceDetector.detectInImage(firebaseVisionImage)
            .addOnSuccessListener { faces: List<FirebaseVisionFace>? ->
                val builder = StringBuilder()
                if (faces != null && faces.isNotEmpty()) {
                    for (face in faces) {
                        val bounds = face.boundingBox
                        val rotY = face.headEulerAngleY // 头部旋转到右旋转度
                        val rotZ = face.headEulerAngleZ // 头部侧向倾斜倾斜度
                        builder
                            .append("边界 -> $bounds").append("\n")
                            .append("头部旋转到右旋转度 -> $rotY").append("\n")
                            .append("头部侧向倾斜倾斜度 -> $rotZ").append("\n")
                        //如果启用了地标检测（嘴巴，耳朵，眼睛，脸颊和鼻子可用）
                        val leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR)
                        leftEar?.let {
                            val leftEarPos = leftEar.position
                            builder.append("左耳位置 -> $leftEarPos").append("\n")
                        }
                        val rightEar = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR)
                        rightEar?.let {
                            val rightEarPos = rightEar.position
                            builder.append("右耳位置 -> $rightEarPos").append("\n")
                        }
                        //如果启用了轮廓检测：
                        val leftEyeContour = face.getContour(FirebaseVisionFaceContour.LEFT_EYE).points
                        val rightEyeContour = face.getContour(FirebaseVisionFaceContour.RIGHT_EYE).points
                        val upperLipTopContour = face.getContour(FirebaseVisionFaceContour.UPPER_LIP_TOP).points
                        val upperLipBottomContour = face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).points
                        builder
                            .append("左眼轮廓 -> $leftEyeContour").append("\n")
                            .append("右眼轮廓 -> $rightEyeContour").append("\n")
                            .append("上唇轮廓 -> $upperLipTopContour").append("\n")
                            .append("下唇轮廓 -> $upperLipBottomContour").append("\n")
                        // 如果启用了分类：
                        if (face.smilingProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                            val smileProb = face.smilingProbability
                            builder.append("微笑概率 -> $smileProb").append("\n")
                        }
                        if (face.rightEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                            val rightEyeOpenProb = face.rightEyeOpenProbability
                            builder.append("右眼睁开概率 -> $rightEyeOpenProb").append("\n")
                        }
                        if (face.leftEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                            val leftEyeOpenProb = face.leftEyeOpenProbability
                            builder.append("左眼睁开概率 -> $leftEyeOpenProb").append("\n")
                        }
                        //如果启用了面部跟踪：
                        if (face.trackingId != FirebaseVisionFace.INVALID_ID) {
                            val id = face.trackingId
                            builder.append("面部跟踪 -> $id").append("\n")
                        }
                        result!!.text = builder
                    }
                }
                if (builder.isEmpty()) {
                    result!!.text = "未检测出人脸信息"
                }
                appBarLayout!!.setExpanded(false, true)
            }.addOnFailureListener { exception: Exception ->
                result!!.text = exception.toString()
            }.addOnCompleteListener {
                dismiss()
            }
    }

    private fun showDialog() {
        runOnUiThread { loading.show() }
    }

    private fun dismiss() {
        runOnUiThread {
            if (loading != null && loading.isShowing) {
                loading.dismiss()
            }
        }
    }

    fun startGallery(view: View) {
        val b = checkPermission(
            REQUEST_CODE_GALLERY,
            Manifest.permission_group.STORAGE,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        )
        if (b) {
            startGalleryApp()
        }
    }

    fun startCamera(view: View) {
        val b =
            checkPermission(REQUEST_CODE_CAMARER, Manifest.permission_group.CAMERA, arrayOf(Manifest.permission.CAMERA))
        if (b) {
            startCameraApp()
        }
    }

    private fun startCameraApp() {
        startActivityForResult(Intent(this, TakePhotoActivity::class.java), REQUEST_CODE_TAKE_PHOTO)

        //        调用系统相机拍照
        //        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //        file = new File(this.getExternalCacheDir() + File.separator + System.currentTimeMillis() + ".png");
        //
        //        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        //        //添加权限
        //        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        //        startActivityForResult(intent, REQUEST_CODE_CAMARER);
    }

    private fun startGalleryApp() {
        var intent = Intent()
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            intent.action = Intent.ACTION_GET_CONTENT
        } else {
            intent.action = Intent.ACTION_OPEN_DOCUMENT
        }

        intent = Intent.createChooser(intent, "选择图片")
        startActivityForResult(intent, REQUEST_CODE_GALLERY)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_GALLERY) {
                if (data != null && data.data != null) {
                    val path = UriUtil.uri2Path(this, data.data)
                    if (path != null) {
                        val file = File(path)
                        if (file.exists()) {
                            startML(path)
                        }
                    }
                }
            } else if (requestCode == REQUEST_CODE_TAKE_PHOTO) {
                if (data != null) {
                    val uri = data.getParcelableExtra<Uri>("CROP_IMG_URI")
                    if (uri != null) {
                        val file = File(uri.path)
                        startML(file.path)
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun onPermissionsAllowed(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onPermissionsAllowed(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_CAMARER -> startCameraApp()
            REQUEST_CODE_GALLERY -> startGalleryApp()
        }
    }

    override fun onPermissionsRefusedNever(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onPermissionsRefusedNever(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_CAMARER -> onPermissionRefuseNever(R.string.permission_request_CAMERA)
            REQUEST_CODE_GALLERY -> onPermissionRefuseNever(R.string.permission_request_READ_EXTERNAL_STORAGE)
        }
    }

    private fun onPermissionRefuseNever(stringRes: Int) {
        val appName = getString(R.string.app_name)
        val message = String.format(getString(stringRes), appName)
        val span = SpannableString(message)
        span.setSpan(
            TextAppearanceSpan(this, R.style.text_color_2_15_style),
            0,
            message.indexOf(appName),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val start = message.indexOf(appName) + appName.length
        span.setSpan(
            TextAppearanceSpan(this, R.style.text_color_1_17_bold_style),
            message.indexOf(appName),
            start,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        span.setSpan(
            TextAppearanceSpan(this, R.style.text_color_2_15_style),
            start,
            message.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}
