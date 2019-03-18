package com.farid.opengl3dmodel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.farid.opengl3dmodel.view.ModelActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.andresoviedo.util.android.AndroidURLStreamHandlerFactory
import org.andresoviedo.util.android.AssetUtils
import org.andresoviedo.util.android.ContentUtils
import java.net.URL.setURLStreamHandlerFactory
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory
import java.util.HashMap

class MainActivity : AppCompatActivity() {
    // Custom handler: org/andresoviedo/app/util/url/android/Handler.class
    companion object {
        init {
            System.setProperty("java.protocol.handler.pkgs", "org.andresoviedo.util.android")
            setURLStreamHandlerFactory(AndroidURLStreamHandlerFactory())
//            setURLStreamHandlerFactory(object : AndroidURLStreamHandlerFactory() {
//                override fun createURLStreamHandler(protocol: String?): URLStreamHandler? {
//                    return if ("assets" == protocol) Handler() as URLStreamHandler else null
//                }
//            })
        }
    }

    private val loadModelParameters = HashMap<String, Any>()

    private val PermissionsRequestCode = 123
    private lateinit var managePermissions: ManagePermissions


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            loadModelFromAssets()
        }

        val list = listOf<String>(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        // Initialize a new instance of ManagePermissions class
        managePermissions = ManagePermissions(this, list, PermissionsRequestCode)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            managePermissions.checkPermissions()

    }

    private fun loadModelFromAssets() {
        AssetUtils.createChooserDialog(
            this, "Select file", null, "models", "(?i).*\\.(obj|stl|dae)"
        ) { file: String? ->
            if (file != null) {
                ContentUtils.provideAssets(this)
                launchModelRendererActivity(Uri.parse("assets://$packageName/$file"))
            }
        }
    }

    private fun launchModelRendererActivity(uri: Uri) {
        Log.i("Menu", "Launching renderer for '$uri'")
        val intent = Intent(applicationContext, ModelActivity::class.java)
        intent.putExtra("uri", uri.toString())
        intent.putExtra("immersiveMode", "true")

        // content provider case
        if (!loadModelParameters.isEmpty()) {
            intent.putExtra("type", loadModelParameters.get("type").toString())
            loadModelParameters.clear()
        }

        startActivity(intent)
    }
}

// Extension function to show toast message
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}