package com.farid.opengl3dmodel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import java.util.*

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

    private val RECORD_REQUEST_CODE = 101


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionSetup()

        button.setOnClickListener {
            permissionSetup()
        }
    }

    private fun loadAndOpenCamera() {
        ContentUtils.provideAssets(this)
        launchModelRendererActivity(Uri.parse("assets://com.farid.opengl3dmodel/models/ToyPlane.obj"))
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
        finish()
    }

    private fun permissionSetup() {
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                RECORD_REQUEST_CODE
            )
        } else {
            loadAndOpenCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            RECORD_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    toast(resources.getString(R.string.permission_denied))
                } else {
                    toast(resources.getString(R.string.permission_granted))
                    loadAndOpenCamera()
                }
            }
        }
    }
}

// Extension function to show toast message
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}