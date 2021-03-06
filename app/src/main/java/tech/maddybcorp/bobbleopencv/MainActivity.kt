package tech.maddybcorp.bobbleopencv

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.system.Os
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import kotlinx.coroutines.*
import java.io.File
import java.net.URL


class MainActivity : AppCompatActivity() {
    lateinit var imageView: ImageView
    lateinit var videoView: VideoView
    private lateinit var loadButton: Button
    lateinit var convertButton: Button
    private val pickImage = 2
    private var imageUri: Uri?=null

    fun verifyStoragePermissions(activity: Activity?) {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                200
            )
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        verifyStoragePermissions(this)
        imageView = findViewById(R.id.imageView)
//        videoView=findViewById(R.id.videoView)
        loadButton = findViewById(R.id.buttonLoadPicture)
        convertButton = findViewById(R.id.convertButton)
        convertButton.isEnabled=false
//        imageView.isEnabled=false
//        videoView.isEnabled=false
        loadButton.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK)
            gallery.type = "image/webp"
            startActivityForResult(gallery, pickImage)
        }

        convertButton.setOnClickListener {
            convertButton.isEnabled=false
            loadButton.isEnabled=false
            findViewById<TextView>(R.id.sample_text).text = "Converting"
            var webp:Long=webPObjectInit(
                imageUri?.getFilePath(context = applicationContext)!!,
                cacheDir.absolutePath
            );
            val numFrames=webPCountFrames(webp)-1
            val ioScope = CoroutineScope(Dispatchers.IO + Job())
            val deferred = (0..numFrames).map { n ->
                ioScope.async {
                    val apiRequest:ApiRequest= ApiRequest(URL("https://bobblification-api-old.bobbleapp.asia/api/v2/bobble"))
                    val onUpload:OnFileUploadListenerImpl=OnFileUploadListenerImpl()
                    apiRequest.addFormField("gender", "male")
                    apiRequest.addFilePart(
                        "image",
                        File("${cacheDir.absolutePath}/$n.jpg"),
                        "$n.jpg",
                        "image/jpeg"
                    )

                    Log.i("API Request", "Network Call ID: $n")
                    apiRequest.upload(onUpload)
                    Log.i("API Response $n", onUpload.faceImageUrl)
                    val url = URL(onUpload.faceImageUrl)
                    url.openConnection()
//                        val stream = url.openStream()
                    val image =url.readBytes()
                    Log.i("API Response", "downloaded $n")
                    webPUpdateFrames(image, n.toInt(), webp)
                }
            }
            val allJobs=GlobalScope.async {
                deferred.awaitAll();
            }
            allJobs.invokeOnCompletion {
                var loc:String=webPMergeFrames(
                    Environment.getExternalStorageDirectory().absolutePath + '/' + Environment.DIRECTORY_DOWNLOADS,
                    webp
                );
                Log.d("${this::class.simpleName}", "FINAL WEBP PATH: $loc")
                runOnUiThread {
                    setWebP(File(loc).toUri())
                    findViewById<TextView>(R.id.sample_text).text=loc
                    loadButton.isEnabled=true
                }
            }
        }
        findViewById<TextView>(R.id.sample_text).text = "Ready"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("${this::class.simpleName}", "RESULT: ${resultCode == RESULT_OK}")
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
//            var realPathUtil:RealPathUtil= RealPathUtil;
//            var realPath: String? =realPathUtil.getRealPath(applicationContext,imageUri!!);
            Log.d(
                "${this::class.simpleName}",
                "PATH: ${imageUri.getFilePath(context = applicationContext)}"
            )
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
                Log.d(
                    "${this::class.simpleName}",
                    "ANDROID VERSION: ${android.os.Build.VERSION.SDK_INT}"
                )
                setWebP(imageUri!!)
                convertButton.isEnabled=true

            }
//            imageView.setImageURI(imageUri)
        }
    }

    fun setWebP(uri: Uri){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {

            var source=ImageDecoder.createSource(contentResolver, uri)
            val drawable = ImageDecoder.decodeDrawable(source)
            imageView.setImageDrawable(drawable)
            if(drawable is AnimatedImageDrawable)
                drawable.start()
        } else {
            TODO("VERSION.SDK_INT < P")
        }
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun webPObjectInit(path: String, cachePath: String):Long
    external fun webPUpdateFrames(frame: ByteArray, num: Int, webpManip: Long):Long
    external fun webPMergeFrames(path: String, webpManip: Long):String
    external fun webPCountFrames(webpManip: Long):Long

    companion object {
        init {
            System.loadLibrary("opencv_java4")
            System.loadLibrary("bobble_opencv")
        }
    }
}
fun Uri?.getFilePath(context: Context): String {
    return this?.let { uri -> RealPathUtil.getRealPath(context, uri) ?: "" } ?: ""
}