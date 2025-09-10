package com.example.gencont_app.formulaire

import CoursePersister
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.View.GONE
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.gencont_app.R
import com.example.gencont_app.api.ChatApiClient
import com.example.gencont_app.configDB.sqlite.data.*
import com.example.gencont_app.configDB.sqlite.database.*
import com.example.gencont_app.cours.CoursActivity
import com.example.gencont_app.login.UserSessionManager
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

class FormulaireActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper

    // Step 1 UI elements
    private lateinit var courseTitleInputLayout: TextInputLayout
    private lateinit var courseTitleEditText: EditText
    private lateinit var proficiencyLevelSpinner: Spinner
    private lateinit var languageSpinner: Spinner
    private lateinit var descriptionInputLayout: TextInputLayout
    private lateinit var descriptionEditText: EditText
    private lateinit var nextButton: Button

    // Step 2 UI elements (Camera Capture)
    private lateinit var textureView: TextureView
    private lateinit var captureButton: Button
    private lateinit var retakeButton: Button
    private lateinit var imagePreview: ImageView
    private lateinit var generateButton: Button
    private lateinit var previousButton: Button
    private var cameraId: String? = null
    private lateinit var switchCameraButton: ImageButton
    private lateinit var uploadButton: Button
    private lateinit var secondaryButtonGroup : LinearLayout

    // Camera variables
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private val ORIENTATIONS = SparseIntArray()
    lateinit var etat_visage: String

    private var selectedImageUri: Uri? = null
    private val IMAGE_PICK_CODE = 1000

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 0)
        ORIENTATIONS.append(Surface.ROTATION_90, 90)
        ORIENTATIONS.append(Surface.ROTATION_180, 180)
        ORIENTATIONS.append(Surface.ROTATION_270, 270)
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 200
    }

    // Initialize UI components

    private fun initializeUI() {
        viewFlipper = findViewById(R.id.viewFlipper)

        // Step 1
        courseTitleInputLayout = findViewById(R.id.textInputLayoutCourseTitle)
        courseTitleEditText = findViewById(R.id.editTextCourseTitle)
        proficiencyLevelSpinner = findViewById(R.id.spinnerProficiencyLevel)
        languageSpinner = findViewById(R.id.spinnerLanguage)
        descriptionInputLayout = findViewById(R.id.textInputLayoutDescription)
        descriptionEditText = findViewById(R.id.editTextDescription)
        nextButton = findViewById(R.id.buttonNext)

        // Step 2 (Camera Capture)
        textureView = findViewById(R.id.textureView)
        captureButton = findViewById(R.id.captureButton)
        retakeButton = findViewById(R.id.retakeButton)
        imagePreview = findViewById(R.id.imagePreview)
        generateButton = findViewById(R.id.generateButton)
        previousButton = findViewById(R.id.buttonPrevious)
        switchCameraButton = findViewById(R.id.switchCameraButton)
        uploadButton = findViewById(R.id.uploadButton)

        // Initialize secondaryButtonGroup
        secondaryButtonGroup = findViewById(R.id.secondaryButtonGroup) // Add this line

        // Initial visibility of secondary buttons
        imagePreview.visibility = GONE
        retakeButton.visibility = GONE
        generateButton.visibility = GONE
        secondaryButtonGroup.visibility = GONE // Also initialize its initial visibility
    }

    // Set up button click listeners
    private fun setupClickListeners() {
        nextButton.setOnClickListener {
            if (validateStep1()) {
                viewFlipper.showNext()
                startCamera()
            }
        }

        previousButton.setOnClickListener {
            closeCamera()
            viewFlipper.showPrevious()
        }

        captureButton.setOnClickListener {
            takePicture()
        }

        retakeButton.setOnClickListener {
            // Reset to camera preview state
            secondaryButtonGroup.visibility = GONE
            imagePreview.visibility = GONE
            retakeButton.visibility = GONE
            generateButton.visibility = GONE
            textureView.visibility = View.VISIBLE
            captureButton.visibility = View.VISIBLE
            uploadButton.visibility = View.VISIBLE // Make upload button visible again
            selectedImageUri = null // Clear the selected image
        }

        generateButton.setOnClickListener {
            if (selectedImageUri != null) {
                showLoadingDialog()
                generateButton.isEnabled = false

                // Animation de fondu pour le bouton
                generateButton.animate().alpha(0.5f).duration = 200

                generateContent()
            } else {
                Toast.makeText(this, "Veuillez capturer ou sélectionner une image", Toast.LENGTH_SHORT).show()
            }
        }

        switchCameraButton.setOnClickListener {
            switchCamera()
        }

        uploadButton.setOnClickListener {
            pickImageFromGallery()
        }
    }

    // Validate Step 1 inputs
    private fun validateStep1(): Boolean {
        var isValid = true

        if (courseTitleEditText.text.isNullOrBlank()) {
            courseTitleInputLayout.error = "Please enter a course title"
            isValid = false
        } else {
            courseTitleInputLayout.error = null
        }

        if (descriptionEditText.text.isNullOrBlank()) {
            descriptionInputLayout.error = "Please enter a description"
            isValid = false
        } else {
            descriptionInputLayout.error = null
        }

        return isValid
    }

    // Camera methods
    private fun startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            return
        }

        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraId ?: getBackCameraId() ?: manager.cameraIdList[0]

            val characteristics = manager.getCameraCharacteristics(cameraId!!)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val imageDimension = map?.getOutputSizes(SurfaceTexture::class.java)?.get(0)

            imageReader = ImageReader.newInstance(
                imageDimension?.width ?: 1280,
                imageDimension?.height ?: 720,
                ImageFormat.JPEG, 2
            )

            if (!textureView.isAvailable) {
                textureView.surfaceTextureListener = textureListener
            } else {
                openCamera()
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            openCamera()
            configureTransform(width, height)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    private fun openCamera() {
        try {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            if (manager.cameraIdList.isEmpty()) {
                Toast.makeText(this, "No cameras available", Toast.LENGTH_SHORT).show()
                return
            }

            if (cameraId == null) {
                cameraId = getBackCameraId() ?: manager.cameraIdList[0]
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(cameraId!!, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        createCameraPreviewSession()
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        cameraDevice = null
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close()
                        cameraDevice = null
                        runOnUiThread {
                            Toast.makeText(this@FormulaireActivity,
                                "Camera error: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, backgroundHandler)
            }
        } catch (e: CameraAccessException) {
            Log.e("Camera", "Error opening camera: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val texture = textureView.surfaceTexture
            texture?.setDefaultBufferSize(textureView.width, textureView.height)
            val surface = Surface(texture)

            val previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder?.addTarget(surface)

            val rotation = windowManager.defaultDisplay.rotation
            previewRequestBuilder?.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation))

            cameraDevice?.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return

                        cameraCaptureSession = session
                        try {
                            previewRequestBuilder?.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            previewRequestBuilder?.set(
                                CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                            )

                            val previewRequest = previewRequestBuilder?.build()
                            cameraCaptureSession?.setRepeatingRequest(
                                previewRequest!!,
                                null,
                                backgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            Log.e("CameraPreview", "Error setting up preview: ${e.message}")
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(this@FormulaireActivity, "Failed to configure camera preview", Toast.LENGTH_SHORT).show()
                    }
                },
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e("CameraPreview", "Error creating preview session: ${e.message}")
        }
    }
    private fun takePicture() {
        if (cameraDevice == null || cameraCaptureSession == null) return

        try {
            val width = 1280
            val height = 720

            imageReader?.close()
            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)

            // Corrected line: Use proper String interpolation
            val file = File(externalCacheDir, "${System.currentTimeMillis()}.jpg")
            selectedImageUri = Uri.fromFile(file)

            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)

                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    val rotatedBitmap = rotateBitmap(bitmap, getRotationCompensation(), isFrontCamera())

                    FileOutputStream(file).use { output ->
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                    }

                    runOnUiThread {
                        imagePreview.setImageBitmap(rotatedBitmap)
                        imagePreview.visibility = View.VISIBLE
                        textureView.visibility = GONE
                        captureButton.visibility = GONE
                        uploadButton.visibility = GONE

                        secondaryButtonGroup.visibility = View.VISIBLE
                        retakeButton.visibility = View.VISIBLE
                        generateButton.visibility = View.VISIBLE
                    }
                } finally {
                    image.close()
                }
            }, backgroundHandler)

            val readerSurface = imageReader?.surface ?: return

            val targets = listOf(
                Surface(textureView.surfaceTexture),
                readerSurface
            )

            val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                this?.addTarget(readerSurface)
                this?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                this?.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(windowManager.defaultDisplay.rotation))
            }

            cameraDevice?.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        session.capture(captureBuilder?.build()!!, null, backgroundHandler)
                    } catch (e: CameraAccessException) {
                        Log.e("Camera", "Error capturing image: ${e.message}")
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e("Camera", "Failed to configure capture session")
                }
            }, backgroundHandler)

        } catch (e: CameraAccessException) {
            Log.e("Camera", "Access error: ${e.message}")
        } catch (e: Exception) {
            Log.e("Camera", "Error: ${e.message}")
        }
    }
    private fun closeCamera() {
        try {
            cameraCaptureSession?.close()
            cameraCaptureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            Log.e("Camera", "Error closing camera: ${e.message}")
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread?.start()
        backgroundHandler = backgroundThread?.looper?.let { Handler(it) }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

/*    private fun generateContent() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "No image captured or selected.", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Processing image...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val imageFile = uriToFile(selectedImageUri!!)
                val base64Image = encodeImageToBase64(imageFile)

                if (base64Image.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(this@FormulaireActivity, "Failed to encode image.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                detectEmotion(base64Image) { result ->
                    runOnUiThread {
                        progressDialog.dismiss()
                        Log.d("FormulaireActivity", "Emotion detection response: $result")

                        if (result == null) {
                            Toast.makeText(this@FormulaireActivity, "Error detecting emotion.", Toast.LENGTH_SHORT).show()
                        } else {
                            try {
                                val jsonArray = JSONArray(result)
                                var topEmotion = ""
                                var topScore = 0.0

                                for (i in 0 until jsonArray.length()) {
                                    val item = jsonArray.getJSONObject(i)
                                    val label = item.getString("label")
                                    val score = item.getDouble("score")

                                    if (score > topScore) {
                                        topScore = score
                                        topEmotion = label
                                    }
                                }

                                if (topEmotion.isNotEmpty()) {
                                    Toast.makeText(this@FormulaireActivity, "Detected emotion: $topEmotion", Toast.LENGTH_LONG).show()
                                    etat_visage = topEmotion

                                    ChatApiClient.generateCourseJson(
                                        titre = courseTitleEditText.text.toString(),
                                        niveau = proficiencyLevelSpinner.selectedItem.toString(),
                                        language = languageSpinner.selectedItem.toString(),
                                        description = descriptionEditText.text.toString(),
                                        emotion = etat_visage
                                    ) { jsonCourse ->
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            val repo = CoursePersister(AppDatabase.getInstance(applicationContext))
                                            repo.saveCourse(
                                                jsonCourse, 2, etat_visage, languageSpinner.selectedItem.toString()
                                            )
                                        }
                                    }
                                } else {
                                    Toast.makeText(this@FormulaireActivity, "No dominant emotion detected.", Toast.LENGTH_SHORT).show()
                                }

                                etat_visage = topEmotion;


                                Log.d("etat_visage", "l etat est : $etat_visage")

                                ChatApiClient.generateCourseJson(
                                    titre       = courseTitleEditText.text.toString(),
                                    niveau      = proficiencyLevelSpinner.selectedItem.toString(),
                                    language    = languageSpinner.selectedItem.toString(),
                                    description = descriptionEditText.text.toString(),
                                    emotion     = etat_visage
                                ) { jsonCourse ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val repo = CoursePersister(AppDatabase.getInstance(applicationContext))
                                        repo.saveCourse(jsonCourse, 1, etat_visage, languageSpinner.selectedItem.toString())

                                        val intent = Intent(this@FormulaireActivity, CoursActivity::class.java)
                                        startActivity(intent)
                                    }
                                }

                            } catch (e: Exception) {
                                Toast.makeText(this@FormulaireActivity, "Failed to parse response.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(this@FormulaireActivity, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    } */

    private fun generateContent() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "No image captured or selected.", Toast.LENGTH_SHORT).show()
            return
        }

//        val progressDialog = ProgressDialog(this)
//        progressDialog.setMessage("Processing image...")
//        progressDialog.setCancelable(false)
//        progressDialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val imageFile = uriToFile(selectedImageUri!!)
                val base64Image = encodeImageToBase64(imageFile)

                if (base64Image.isEmpty()) {
                    withContext(Dispatchers.Main) {
//                        progressDialog.dismiss()
                        Toast.makeText(this@FormulaireActivity, "Failed to encode image.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                detectEmotion(base64Image) { result ->
                    runOnUiThread {
                        updateLoadingMessage("Création de votre cours personnalisé...")
//                        progressDialog.dismiss()
                        Log.d("FormulaireActivity", "Emotion detection response: $result")

                        if (result == null) {
                            Toast.makeText(this@FormulaireActivity, "Error detecting emotion.", Toast.LENGTH_SHORT).show()
                        } else {
                            try {
                                // Handle Face++ specific JSON format
                                var topEmotion = ""
                                var topScore = 0.0

                                try {
                                    val jsonObject = JSONObject(result)

                                    // Check if this is a Face++ response with faces array
                                    if (jsonObject.has("faces") && jsonObject.getJSONArray("faces").length() > 0) {
                                        val facesArray = jsonObject.getJSONArray("faces")
                                        val faceObject = facesArray.getJSONObject(0)  // Get first face

                                        // Navigate to the emotion object
                                        if (faceObject.has("attributes") &&
                                            faceObject.getJSONObject("attributes").has("emotion")) {

                                            val emotionObject = faceObject.getJSONObject("attributes")
                                                .getJSONObject("emotion")

                                            // Iterate through emotion values to find the highest
                                            val emotionKeys = emotionObject.keys()
                                            while (emotionKeys.hasNext()) {
                                                val emotion = emotionKeys.next()
                                                val score = emotionObject.getDouble(emotion)

                                                if (score > topScore) {
                                                    topScore = score
                                                    topEmotion = emotion
                                                }
                                            }

                                            Log.d("FormulaireActivity", "Found top emotion: $topEmotion with score: $topScore")
                                        } else {
                                            Log.e("FormulaireActivity", "No emotion attributes found in face data")
                                        }
                                    } else {
                                        // Fallback to previous parsing logic for other formats
                                        try {
                                            // Try to parse as JSONArray
                                            val jsonArray = JSONArray(result)

                                            for (i in 0 until jsonArray.length()) {
                                                val item = jsonArray.getJSONObject(i)
                                                val label = item.getString("label")
                                                val score = item.getDouble("score")

                                                if (score > topScore) {
                                                    topScore = score
                                                    topEmotion = label
                                                }
                                            }
                                        } catch (arrayException: Exception) {
                                            // If not an array, try parsing as simple JSONObject
                                            // Check if the object has direct emotion properties
                                            if (jsonObject.has("label") && jsonObject.has("score")) {
                                                topEmotion = jsonObject.getString("label")
                                                topScore = jsonObject.getDouble("score")
                                            } else {
                                                // Try to get emotions directly from object
                                                val keys = jsonObject.keys()

                                                while (keys.hasNext()) {
                                                    val key = keys.next()
                                                    if (key != "request_id" && key != "time_used" && key != "image_id" && key != "face_num") {
                                                        val value = jsonObject.optDouble(key, 0.0)

                                                        if (value > topScore) {
                                                            topScore = value
                                                            topEmotion = key
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("FormulaireActivity", "Failed to parse JSON: ${e.message}")
                                    e.printStackTrace()
                                    Toast.makeText(this@FormulaireActivity, "Failed to parse JSON response: ${e.message}", Toast.LENGTH_SHORT).show()
                                    return@runOnUiThread
                                }

                                if (topEmotion.isNotEmpty()) {
                                    Toast.makeText(this@FormulaireActivity, "Detected emotion: $topEmotion", Toast.LENGTH_LONG).show()
                                    etat_visage = topEmotion

                                    Log.d("etat_visage", "l etat est : $etat_visage")

                                    // Make API call only once
                                    ChatApiClient.generateCourseJson(
                                        titre = courseTitleEditText.text.toString(),
                                        niveau = proficiencyLevelSpinner.selectedItem.toString(),
                                        language = languageSpinner.selectedItem.toString(),
                                        description = descriptionEditText.text.toString(),
                                        emotion = etat_visage
                                    ) { jsonCourse ->
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            val repo = CoursePersister(AppDatabase.getInstance(applicationContext),applicationContext)

                                            // Save course
                                            val userId = UserSessionManager.getUserId(this@FormulaireActivity)
                                            if (userId != -1L) {
                                            repo.saveCourse(
                                                jsonCourse, userId, etat_visage, languageSpinner.selectedItem.toString()
                                            )
                                            }

                                            // Navigate to CoursActivity
                                            withContext(Dispatchers.Main) {
                                                dismissLoadingDialog()
                                                generateButton.isEnabled = true
                                                generateButton.animate().alpha(1f).duration = 200

                                                // Animation de transition
                                                val intent = Intent(this@FormulaireActivity, CoursActivity::class.java)
                                                startActivity(intent)
                                                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                                            }
                                        }
                                    }
                                } else {
                                    Toast.makeText(this@FormulaireActivity, "No dominant emotion detected.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("FormulaireActivity", "Error processing emotion: ${e.message}")
                                Toast.makeText(this@FormulaireActivity, "Failed to parse response: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
//                    progressDialog.dismiss()
                    Toast.makeText(this@FormulaireActivity, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri) ?: throw IOException("Unable to open input stream")
        val file = File(cacheDir, "temp_image.jpg")
        FileOutputStream(file).use { output ->
            inputStream.copyTo(output)
        }
        return file
    }

    private fun encodeImageToBase64(file: File): String {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return ""
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("FormulaireActivity", "Error encoding image: ${e.message}", e)
            ""
        }
    }

    private fun detectEmotion(base64Image: String, onResult: (String?) -> Unit) {
        Log.d("FormulaireActivity", "Starting Face++ emotion detection")

        if (base64Image.isBlank()) {
            Log.e("FormulaireActivity", "Base64 image is blank or empty")
            onResult(null)
            return
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val requestBody = FormBody.Builder()
            .add("api_key", "Ty36Jno7TYDxwJZmUfRhmD-czWAR1Fmi")
            .add("api_secret", "KRqkGNMuXSRndtBkAL8tc0NWODuHjTCA")
            .add("image_base64", base64Image)
            .add("return_attributes", "emotion")
            .build()

        val request = Request.Builder()
            .url("https://api-us.faceplusplus.com/facepp/v3/detect")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FormulaireActivity", "API call failed: ${e.message}", e)
                onResult(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    Log.e("FormulaireActivity", "API error: ${response.code} - ${response.message}")
                    onResult(null)
                } else {
                    Log.d("FormulaireActivity", "API response: $body")
                    onResult(body)
                }
            }
        })
    }





    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (textureView.isAvailable) {
            openCamera()
        } else {
            textureView.surfaceTextureListener = textureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun switchCamera() {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            if (manager.cameraIdList.size > 1) {
                closeCamera()
                cameraId = if (cameraId == getFrontCameraId()) {
                    getBackCameraId()
                } else {
                    getFrontCameraId()
                }
                startCamera()
            } else {
                Toast.makeText(this, "Only one camera available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: CameraAccessException) {
            Log.e("CameraSwitch", "Error accessing cameras: ${e.message}")
        }
    }

    private fun getBackCameraId(): String? {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        return manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
        }
    }

    private fun getFrontCameraId(): String? {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        return manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT
        }
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        if (textureView.width == 0 || textureView.height == 0 || cameraId == null) return

        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(cameraId!!)
            val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            val isFrontFacing = characteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT

            val matrix = Matrix()
            val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
            val bufferRect = RectF(0f, 0f, textureView.height.toFloat(), textureView.width.toFloat())
            val centerX = viewRect.centerX()
            val centerY = viewRect.centerY()

            if (isFrontFacing) {
                matrix.postScale(-1f, 1f, centerX, centerY)
            }

            val rotation = windowManager.defaultDisplay.rotation
            val deviceOrientation = when (rotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }

            val totalRotation = (sensorOrientation + deviceOrientation + 360) % 360

            when (totalRotation) {
                90 -> {}
                0 -> {
                    matrix.postRotate(90f, centerX, centerY)
                    val scale = max(
                        viewHeight.toFloat() / textureView.width,
                        viewWidth.toFloat() / textureView.height
                    )
                    matrix.postScale(scale, scale, centerX, centerY)
                }
                180 -> {
                    matrix.postRotate(180f, centerX, centerY)
                }
                270 -> {
                    matrix.postRotate(270f, centerX, centerY)
                    val scale = max(
                        viewHeight.toFloat() / textureView.width,
                        viewWidth.toFloat() / textureView.height
                    )
                    matrix.postScale(scale, scale, centerX, centerY)
                }
            }

            textureView.setTransform(matrix)
        } catch (e: CameraAccessException) {
            Log.e("Transform", "Error during transform configuration: ${e.message}")
        }
    }

    @SuppressLint("ServiceCast")
    private fun getRotationCompensation(): Int {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val deviceRotation = windowManager.defaultDisplay.rotation
        val sensorOrientation = getSensorOrientation()
        val isFrontFacing = isFrontCamera()

        val deviceRotationDegrees = when (deviceRotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        return (sensorOrientation - deviceRotationDegrees + 360) % 360
    }

    private fun getSensorOrientation(): Int {
        return try {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            val characteristics = manager.getCameraCharacteristics(cameraId!!)
            characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun isFrontCamera(): Boolean {
        return try {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            val characteristics = manager.getCameraCharacteristics(cameraId!!)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        } catch (e: Exception) {
            false
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int, isFrontFacing: Boolean): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())

        if (isFrontFacing) {
            matrix.postScale(-1f, 1f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun getOrientation(rotation: Int): Int {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val characteristics = manager.getCameraCharacteristics(cameraId!!)
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

        return when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    // Pick image from gallery
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    // Handle result from image picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_CODE) {
                selectedImageUri = data?.data
                imagePreview.setImageURI(selectedImageUri)

                // Update visibility of buttons after image is selected
                textureView.visibility = GONE
                captureButton.visibility = GONE
                uploadButton.visibility = GONE // Hide upload after selection

                secondaryButtonGroup.visibility = View.VISIBLE
                retakeButton.visibility = View.VISIBLE
                generateButton.visibility = View.VISIBLE
                imagePreview.visibility = View.VISIBLE
            }
        }
    }



    private var loadingDialog: AlertDialog? = null

    private fun showLoadingDialog() {
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.custom_loading_dialog, null)

        val messageTextView = dialogView.findViewById<TextView>(R.id.loading_message)
        messageTextView.text = "Analyse de votre photo..."

        loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog?.show()
    }

    private fun updateLoadingMessage(message: String) {
        loadingDialog?.findViewById<TextView>(R.id.loading_message)?.text = message
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulaire)
        initializeUI()
        setupClickListeners()
    }
}