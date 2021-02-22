package tech.maddybcorp.bobbleopencv

import android.content.ContentProvider
import android.content.ContentResolver
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import android.util.Log
import android.widget.TextView
import android.widget.Button
import android.widget.ImageView
import java.io.File

class MainActivity : AppCompatActivity() {
    lateinit var imageView: ImageView
    lateinit var button: Button
    private val pickImage = 2
    private var imageUri: Uri?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = findViewById(R.id.imageView)
        button = findViewById(R.id.buttonLoadPicture)
        button.setOnClickListener {
            val gallery = Intent(Intent.ACTION_GET_CONTENT)
            gallery.type = "image/webp"
            startActivityForResult(gallery, pickImage)
        }

        // Example of a call to a native method
        findViewById<TextView>(R.id.sample_text).text = stringFromJNI()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("${this::class.simpleName}", "RESULT: ${resultCode== RESULT_OK}")
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
//            Log.d("${this::class.simpleName}","IMAGE URI: $imageUri")
//            Log.d("${this::class.simpleName}", "IMAGE URI PATH: ${imageUri?.path}")
//            var file: File = File(imageUri?.path!!)

            Log.d("${this::class.simpleName}", "ANDROID VERSION: ${android.os.Build.VERSION.SDK_INT}")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                var source=ImageDecoder.createSource(contentResolver, imageUri!!)
                val drawable = ImageDecoder.decodeDrawable(source)
                imageView.setImageDrawable(drawable)
                if(drawable is AnimatedImageDrawable)
                    drawable.start()
            } else {
                TODO("VERSION.SDK_INT < P")
            }
//            imageView.setImageURI(imageUri)
        }
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
            System.loadLibrary("opencv_java4");
        }
    }
}