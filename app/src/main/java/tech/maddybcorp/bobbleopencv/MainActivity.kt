package tech.maddybcorp.bobbleopencv

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import android.util.Log
import android.widget.TextView
import android.widget.Button
import android.widget.ImageView
import android.widget.VideoView
import androidx.core.graphics.PathUtils
import androidx.core.net.toFile

class MainActivity : AppCompatActivity() {
    lateinit var imageView: ImageView
    lateinit var videoView: VideoView
    private lateinit var loadButton: Button
    lateinit var convertButton: Button
    private val pickImage = 2
    private var imageUri: Uri?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = findViewById(R.id.imageView)
        videoView=findViewById(R.id.videoView)
        loadButton = findViewById(R.id.buttonLoadPicture)
        convertButton = findViewById(R.id.convertButton)
        convertButton.isEnabled=false
//        imageView.isEnabled=false
//        videoView.isEnabled=false
        loadButton.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK)
            gallery.type = "video/mp4"
            startActivityForResult(gallery, pickImage)
        }
        convertButton.setOnClickListener{
            var webp:Unit=WebPObject(imageUri?.getFilePath(context=applicationContext)!!);
        }
        // Example of a call to a native method
        findViewById<TextView>(R.id.sample_text).text = stringFromJNI().javaClass.simpleName
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("${this::class.simpleName}", "RESULT: ${resultCode== RESULT_OK}")
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
//            var realPathUtil:RealPathUtil= RealPathUtil;
//            var realPath: String? =realPathUtil.getRealPath(applicationContext,imageUri!!);
            Log.d("${this::class.simpleName}", "PATH: ${imageUri.getFilePath(context = applicationContext)}")
            Log.d("${this::class.simpleName}", "TYPE: ${data?.resolveType(contentResolver)}")
            if(data?.resolveType(contentResolver)=="video/mp4") {
                videoView.isEnabled=true
                videoView.setVideoURI(imageUri)
                videoView.start()
                videoView.setOnCompletionListener { videoView.start() }
                convertButton.isEnabled=true
            }
            else{
                imageView.isEnabled=true
            Log.d("${this::class.simpleName}", "ANDROID VERSION: ${android.os.Build.VERSION.SDK_INT}")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                var source=ImageDecoder.createSource(contentResolver, imageUri!!)
                val drawable = ImageDecoder.decodeDrawable(source)
                imageView.setImageDrawable(drawable)
                if(drawable is AnimatedImageDrawable)
                    drawable.start()
                convertButton.isEnabled=true
            } else {
                TODO("VERSION.SDK_INT < P")
            }
            }
//            imageView.setImageURI(imageUri)
        }
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun WebPObject(str:String):Unit
    external fun stringFromJNI():String

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
            System.loadLibrary("opencv_java4")
            System.loadLibrary("bobble_opencv")
        }
    }
}
fun Uri?.getFilePath(context: Context): String {
    return this?.let { uri -> RealPathUtil.getRealPath(context, uri) ?: "" } ?: ""
}