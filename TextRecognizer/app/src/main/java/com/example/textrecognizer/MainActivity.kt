package com.example.textrecognizer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent // 确保 Intent 已导入 (Ensure Intent is imported)
import android.content.pm.PackageManager
import android.graphics.BitmapFactory // 用于从图库图片创建 Bitmap (For creating Bitmap from gallery image)
import android.net.Uri // 用于图库图片 URI (For gallery image URI)
import android.os.Bundle
import android.util.Log
import android.widget.Button // 确保 Button 已导入 (Ensure Button is imported)
import android.widget.Toast // 确保 Toast 已导入 (Ensure Toast is imported)
import androidx.activity.result.ActivityResultLauncher // 用于 Activity Result API (For Activity Result API)
import androidx.activity.result.contract.ActivityResultContracts // 用于 Activity Result API (For Activity Result API)
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    // UI 元素 (UI Elements)
    private lateinit var viewFinder: PreviewView
    private lateinit var captureButton: Button
    private lateinit var selectImageButton: Button // 从图库选择图片按钮 (Select image from gallery button)

    // CameraX 变量 (CameraX Variables)
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    // 文字识别器 (Text Recognizer)
    private lateinit var textRecognizer: TextRecognizer

    // ActivityResultLauncher 用于启动图片选择器 (ActivityResultLauncher for launching image picker)
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 这是一个简单的中文注释 (This is a simple Chinese comment)

        viewFinder = findViewById(R.id.viewFinder) // 获取 PreviewView 引用 (Get PreviewView reference)
        captureButton = findViewById(R.id.capture_button) // 获取拍照 Button 引用 (Get capture Button reference)
        selectImageButton = findViewById(R.id.select_image_button) // 获取图库选择 Button 引用 (Get gallery select Button reference)

        // 初始化文字识别器 (Initialize Text Recognizer)
        // 使用中文文字识别选项 (Use Chinese text recognizer options)
        textRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

        // 初始化图片选择器 (Initialize image picker)
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { imageUri ->
                // 用户选择了一个 URI (User selected a URI)
                try {
                    val inputStream = contentResolver.openInputStream(imageUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close() // 确保关闭输入流 (Ensure input stream is closed)

                    if (bitmap != null) {
                        // 从 Bitmap 创建 InputImage (Create InputImage from Bitmap)
                        // 图库中的图片通常不需要旋转，因此旋转角度设为0 (Images from gallery usually don't need rotation, so rotation is 0)
                        val image = InputImage.fromBitmap(bitmap, 0)
                        processImageForTextRecognition(image) // 调用文本识别方法 (Call text recognition method)
                    } else {
                        Log.e(TAG, "无法从URI解码Bitmap (Failed to decode Bitmap from URI)")
                        Toast.makeText(this, "无法加载图片 (Unable to load image)", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "从图库加载图片失败 (Failed to load image from gallery)", e)
                    Toast.makeText(this, "加载图片失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 请求相机权限 (Request camera permissions)
        requestPermissions() // 注意：图库读取权限将在下一步添加 (Note: Gallery read permission will be added in the next step)

        // 设置拍照按钮点击监听 (Set capture button click listener)
        captureButton.setOnClickListener {
            takePhoto() // 调用拍照方法 (Call take photo method)
        }

        // 设置图库选择按钮的点击监听器 (Set click listener for gallery select button)
        selectImageButton.setOnClickListener {
            // 启动图片选择器，MIME类型为 "image/*" (Launch image picker with MIME type "image/*")
            pickImageLauncher.launch("image/*")
        }

        cameraExecutor = Executors.newSingleThreadExecutor() // 初始化相机执行器 (Initialize camera executor)
    }

    // 拍照方法 (Method to take photo)
    private fun takePhoto() {
        // 获取一个稳定的 ImageCapture 用例引用 (Get a stable reference of the modifiable image capture use case)
        val imageCapture = imageCapture ?: return // 如果 imageCapture 为 null，则直接返回 (If imageCapture is null, return)

        // 使用主线程执行器，因为这是一个用户操作，UI 反馈可能需要 (Use main executor as this is a user action and UI feedback might be needed)
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                @SuppressLint("UnsafeOptInUsageError") // 需要处理 mediaImage (Need to handle mediaImage)
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    // 当照片成功捕获时调用 (Called when a photo has been successfully captured)
                    val mediaImage = imageProxy.image // 获取 MediaImage 对象 (Get MediaImage object)
                    if (mediaImage != null) {
                        // 从 MediaImage 创建 ML Kit 的 InputImage (Create ML Kit's InputImage from MediaImage)
                        // 需要提供图像的旋转角度 (Need to provide the rotation degrees of the image)
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        
                        // !!! 重要: 在 InputImage 创建后立即关闭 imageProxy !!!
                        // !!! IMPORTANT: Close the imageProxy immediately after creating InputImage !!!
                        imageProxy.close() // 释放 ImageProxy 资源 (Release ImageProxy resource)

                        // 调用文本识别 (Call text recognition)
                        processImageForTextRecognition(image)
                    } else {
                        // 如果 mediaImage 为 null，也需要关闭 imageProxy (If mediaImage is null, also close imageProxy)
                        Log.e(TAG, "MediaImage is null, closing imageProxy.")
                        imageProxy.close() // 确保资源被释放 (Ensure resource is released)
                        Toast.makeText(baseContext, "拍照失败: 无法获取图像 (Capture failed: Unable to get image)", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exception: ImageCapture.OutputFileResults) {
                    // 当照片捕获失败时调用 (Called when an error occurs during photo capture)
                    Log.e(TAG, "拍照失败 (Photo capture failed): ${exception.error?.message}", exception.error)
                    Toast.makeText(baseContext, "拍照失败 (Photo capture failed): ${exception.error?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    // 处理图像以进行文本识别 (Process image for text recognition)
    private fun processImageForTextRecognition(image: InputImage) {
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                // 文本识别成功 (Text recognition successful)
                Log.d(TAG, "Successfully recognized text. Full text: ${visionText.text}") // 保留日志记录 (Keep logging)

                // 启动 DisplayResultActivity 来显示结果 (Start DisplayResultActivity to show results)
                val intent = Intent(this@MainActivity, DisplayResultActivity::class.java)
                intent.putExtra("recognized_text", visionText.text) // 传递完整文本 (Pass the full text)
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                // 文本识别失败 (Text recognition failed)
                Log.e(TAG, "文本识别失败 (Text recognition failed): ${e.localizedMessage}", e)
                Toast.makeText(baseContext, "文本识别失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }


    // 请求权限的方法 (Method to request permissions)
    private fun requestPermissions() {
        if (allPermissionsGranted()) {
            startCamera() // 如果权限已授予，则启动相机 (If permissions are granted, start camera)
        } else {
            // 请求相机权限，图库权限将在 Manifest 中声明 (Request camera permission, gallery permission will be declared in Manifest)
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS_CAMERA_ONLY, REQUEST_CODE_PERMISSIONS // 仅请求相机权限 (Request only camera permission for now)
            )
        }
    }

    // 检查是否所有必需的相机权限都已授予 (Check if all required camera permissions are granted)
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS_CAMERA_ONLY.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 启动相机的方法 (Method to start the camera)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // 用于将相机的生命周期绑定到生命周期所有者 (Used to bind the lifecycle of cameras to the lifecycle owner)
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // ImageCapture use case
            imageCapture = ImageCapture.Builder().build()

            // 选择后置摄像头作为默认摄像头 (Select back camera as a default)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // 在重新绑定之前取消绑定用例 (Unbind use cases before rebinding)
                cameraProvider.unbindAll()

                // 将用例绑定到相机 (Bind use cases to camera)
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                Log.d(TAG, "相机已成功启动 (Camera started successfully)")

            } catch (exc: Exception) {
                Log.e(TAG, "用例绑定失败 (Use case binding failed)", exc)
                Toast.makeText(this, "相机启动失败 (Camera start failed): ${exc.message}", Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // 处理权限请求结果的回调 (Callback for the result from requesting permissions)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera() // 如果权限已授予，则启动相机 (If permissions are granted, start camera)
            } else {
                // 如果权限未授予，则通知用户 (If permissions are not granted, notify the user)
                Toast.makeText(
                    this,
                    "相机权限未授予，无法使用相机功能。(Camera permissions not granted, unable to use camera feature.)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown() // 关闭相机执行器 (Shutdown camera executor)
        textRecognizer.close() // 关闭文本识别器，释放资源 (Close text recognizer, release resources)
        Log.d(TAG, "资源已释放 (Resources released)")
    }

    companion object {
        private const val TAG = "MainActivity" // 日志标签 (Log tag)
        private const val REQUEST_CODE_PERMISSIONS = 10
        // 仅包含相机权限，图库权限通过<uses-permission>声明，通常不需要在运行时动态请求（对于ACTION_GET_CONTENT）
        // Only includes camera permission; gallery permission is declared via <uses-permission> and usually doesn't need runtime request for ACTION_GET_CONTENT
        private val REQUIRED_PERMISSIONS_CAMERA_ONLY = arrayOf(Manifest.permission.CAMERA)
    }
}
