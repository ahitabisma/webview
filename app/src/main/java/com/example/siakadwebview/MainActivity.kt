package com.example.siakadwebview

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.onesignal.OneSignal
import java.io.File
import java.io.IOException


// OneSignal App ID
const val ONESIGNAL_APP_ID = "0ee2b640-33f0-4849-91ed-f06e3fee211b"

class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {
    // Variable Webview
    private lateinit var webView : WebView
    private lateinit var refreshLayout: SwipeRefreshLayout
    private val url = "http://app.softwaresekolah.my.id/"

    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    companion object {
        private const val FILE_CHOOSER_RESULT_CODE = 1
        private const val CAMERA_PERMISSION_REQUEST_CODE = 2
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Variable
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
//
//        fun create_image(): File {
//            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//            val imageFile = File.createTempFile(
//                "image_",  /* prefix */
//                ".jpg",    /* suffix */
//                storageDir /* directory */
//            )
//            return imageFile
//        }
//
//        myARL = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                val data: Intent? = result.data
//                data?.data?.let { uri ->
//                    val uris = arrayOf(uri)
//                    fileUploadCallback?.onReceiveValue(uris)
//                    fileUploadCallback = null
//                }
//            }
//        }

        webView.webChromeClient = object : WebChromeClient() {
            // For Android 4.1+
            fun openFileChooser(uploadMsg: ValueCallback<Array<Uri>>, acceptType: String) {
                fileUploadCallback = uploadMsg
                requestCameraPermission()
            }

            // For Android 5.0+
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileUploadCallback = filePathCallback
                requestCameraPermission()
                return true
            }
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

//    private fun selectFile() {
//        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
//        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
//        contentSelectionIntent.type = "*/*"
//        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
//        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
//        myARL.launch(chooserIntent)
//        fileUploadCallback = null
//    }

    // Handle camera permission request
    private fun requestCameraPermission() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted, show the camera dialog
            showCameraDialog()
        } else {
            // Permission has not been granted, request it
            requestPermissions(
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Handle the result of permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Camera permission granted, show the camera dialog
                    showCameraDialog()
                } else {
                    // Camera permission denied, handle accordingly (e.g., show a message)
                }
            }
        }
    }


    // Show the camera dialog
    private fun showCameraDialog() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, FILE_CHOOSER_RESULT_CODE)
        }
    }

    // Handle the result of camera capture
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (fileUploadCallback != null) {
                val results = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                fileUploadCallback?.onReceiveValue(results)
                fileUploadCallback = null
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}