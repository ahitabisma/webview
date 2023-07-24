@file:Suppress("DEPRECATION")

package com.example.siakadwebview

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.MediaStore
import android.provider.MediaStore.ACTION_IMAGE_CAPTURE
import android.provider.MediaStore.ACTION_VIDEO_CAPTURE
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.onesignal.OneSignal
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


// OneSignal App ID
const val ONESIGNAL_APP_ID = "0ee2b640-33f0-4849-91ed-f06e3fee211b"

class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {
    // Declare Variable
    private lateinit var webView : WebView
    private lateinit var refreshLayout: SwipeRefreshLayout
    private val url = "http://app.softwaresekolah.my.id/"
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private val file_type = "*/*"
    private var cam_file_data: String? = null
    private var file_data: ValueCallback<Uri>? = null
    private var file_path: ValueCallback<Array<Uri?>?>? = null
    private val file_req_code = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.SIAKAD)
        webView.webViewClient = MyWebViewClient()
        webView.loadUrl(url)
        val javascriptInterface = MyJavascriptInterface(this)

        // Add JavaScriptInterface to WebView
        webView.addJavascriptInterface(javascriptInterface, "AndroidInterface")

        // Declare Variable WebSetting
        val webSettings = webView.settings

        // Enable Javascript, CSS
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // Enable Camera, Location, Import File
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.setGeolocationEnabled(true)

        webView.webChromeClient = object : WebChromeClient() {
            // Input File
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri?>?>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                return if (file_permission() && SDK_INT >= 21) {
                    file_path = filePathCallback
                    var takePictureIntent: Intent? = null
                    var takeVideoIntent: Intent? = null
                    var includeVideo = false
                    var includePhoto = false

                    paramCheck@ for (acceptTypes in fileChooserParams.acceptTypes) {
                        val splitTypes =
                            acceptTypes.split(", ?+".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        for (acceptType in splitTypes) {
                            when (acceptType) {
                                "*/*" -> {
                                    includePhoto = true
                                    includeVideo = true
                                    break@paramCheck
                                }
                                "image/*" -> includePhoto = true
                                "video/*" -> includeVideo = true
                            }
                        }
                    }
                    if (fileChooserParams.acceptTypes.size == 0) {
                        includePhoto = true
                        includeVideo = true
                    }
                    if (includePhoto) {
                        takePictureIntent = Intent(ACTION_IMAGE_CAPTURE)
                        if (takePictureIntent.resolveActivity(this@MainActivity.packageManager) != null) {
                            var photoFile: File? = null
                            try {
                                photoFile = create_image()
                                takePictureIntent.putExtra("PhotoPath", cam_file_data)
                            } catch (ex: IOException) {
                                Log.e(TAG, "Image file creation failed", ex)
                            }
                            if (photoFile != null) {
                                cam_file_data = "file:" + photoFile.getAbsolutePath()
                                takePictureIntent.putExtra(
                                    MediaStore.EXTRA_OUTPUT,
                                    Uri.fromFile(photoFile)
                                )
                            } else {
                                cam_file_data = null
                                takePictureIntent = null
                            }
                        }
                    }
                    if (includeVideo) {
                        takeVideoIntent = Intent(ACTION_VIDEO_CAPTURE)
                        if (takeVideoIntent.resolveActivity(this@MainActivity.packageManager) != null) {
                            var videoFile: File? = null
                            try {
                                videoFile = create_video()
                            } catch (ex: IOException) {
                                Log.e(TAG, "Video file creation failed", ex)
                            }
                            if (videoFile != null) {
                                cam_file_data = "file:" + videoFile.getAbsolutePath()
                                takeVideoIntent.putExtra(
                                    MediaStore.EXTRA_OUTPUT,
                                    Uri.fromFile(videoFile)
                                )
                            } else {
                                cam_file_data = null
                                takeVideoIntent = null
                            }
                        }
                    }
                    val contentSelectionIntent = Intent(ACTION_GET_CONTENT)
                    contentSelectionIntent.addCategory(CATEGORY_OPENABLE)
                    contentSelectionIntent.type = file_type
                    val intentArray: Array<Intent?>
                    intentArray = if (takePictureIntent != null && takeVideoIntent != null) {
                        arrayOf(takePictureIntent, takeVideoIntent)
                    } else takePictureIntent?.let { arrayOf(it) }
                        ?: (takeVideoIntent?.let { arrayOf(it) } ?: arrayOfNulls(0))
                    val chooserIntent = Intent(ACTION_CHOOSER)
                    chooserIntent.putExtra(EXTRA_INTENT, contentSelectionIntent)
                    chooserIntent.putExtra(EXTRA_TITLE, "File chooser")
                    chooserIntent.putExtra(EXTRA_INITIAL_INTENTS, intentArray)
                    startActivityForResult(chooserIntent, file_req_code)
                    true
                } else {
                    false
                }
            }

            // Location
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback
            ) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                } else {
                    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (location != null) {
                        val latitude: String = java.lang.String.valueOf(location.latitude)
                        val longitude: String = java.lang.String.valueOf(location.longitude)
                        val url = "http://app.softwaresekolah.my.id?lat=$latitude&long=$longitude"
                        webView.loadUrl(url)
                        callback.invoke(origin, true, true)
                    } else {
                        callback.invoke(origin, false, false)
                    }
                }
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                if (request.origin.toString().startsWith("http://")) {
                    request.grant(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                } else {
                    super.onPermissionRequest(request)
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            webView.loadUrl(url)
        }

        // Disable Zoom, Copy Paste
        webSettings.setSupportZoom(false)
        webSettings.builtInZoomControls = false
        webView.setOnLongClickListener { true }

        // Swipe Refresh
        refreshLayout = findViewById(R.id.swipefresh)
        refreshLayout.setOnRefreshListener(this)

        // OneSignal
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE)
        OneSignal.initWithContext(this)
        OneSignal.setAppId(ONESIGNAL_APP_ID)
        OneSignal.promptForPushNotifications()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(webView.canGoBack()){
            webView.goBack()
        }else{
            super.onBackPressed()
        }
    }

    // Swipe Refresh
    override fun onRefresh() {
        webView.reload()
        refreshLayout.isRefreshing = false
    }

    // Pull to Refresh
    private inner class MyWebViewClient : WebViewClient(){
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return super.shouldOverrideUrlLoading(view, request)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    webView.loadUrl(url)
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    // Input File
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    fun file_permission(): Boolean {
        return if (SDK_INT >= 23 && (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                1
            )
            false
        } else {
            true
        }
    }

    @Throws(IOException::class)
    private fun create_image(): File? {
        @SuppressLint("SimpleDateFormat") val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "img_" + timeStamp + "_"
        val storageDir: File? = getExternalFilesDir(DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    @Throws(IOException::class)
    private fun create_video(): File? {
        @SuppressLint("SimpleDateFormat") val file_name =
            SimpleDateFormat("yyyy_mm_ss").format(Date())
        val new_name = "file_" + file_name + "_"
        val sd_directory = getExternalFilesDir(DIRECTORY_PICTURES)
        return File.createTempFile(new_name, ".3gp", sd_directory)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        val fileData = file_data;
        if (SDK_INT >= 21) {
            var results: Array<Uri?>? = null

            if (resultCode == RESULT_CANCELED) {
                file_path!!.onReceiveValue(null)
                return
            }

            if (resultCode == RESULT_OK) {
                if (null == file_path) {
                    return
                }
                var clipData: ClipData?
                var stringData: String?
                try {
                    clipData = intent!!.clipData
                    stringData = intent.dataString
                } catch (e: Exception) {
                    clipData = null
                    stringData = null
                }
                if (false && cam_file_data != null) {
                    results = arrayOf(Uri.parse(cam_file_data))
                } else {
                    if (clipData != null) {
                        val numSelectedFiles = clipData.itemCount
                        results = arrayOfNulls(numSelectedFiles)
                        for (i in 0 until clipData.itemCount) {
                            results[i] = clipData.getItemAt(i).uri
                        }
                    } else {
                        try {
                            val cam_photo = intent!!.extras!!["data"] as Bitmap?
                            val bytes = ByteArrayOutputStream()
                            cam_photo!!.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
                            stringData = MediaStore.Images.Media.insertImage(
                                this.contentResolver,
                                cam_photo,
                                null,
                                null
                            )
                        } catch (ignored: Exception) {
                        }
                        results = arrayOf(Uri.parse(stringData))
                    }
                }
            }
            file_path!!.onReceiveValue(results)
            file_path = null
        } else {
            if (requestCode == file_req_code) {
                if (null == fileData) return
                val result = if (intent == null || resultCode != RESULT_OK) null else intent.data
                fileData.onReceiveValue(result)
                file_data = null
            }
        }
    }



}