package tech.maddybcorp.bobbleopencv

import android.util.JsonReader
import android.util.Log
import org.json.JSONObject

 class OnFileUploadListenerImpl:ApiRequest.OnFileUploadedListener {
    lateinit var faceImageUrl:String
    override fun onFileUploadingSuccess(response: String){
        val jsonObject = JSONObject(response)
        faceImageUrl=jsonObject.getString("faceImageURL")
        Log.d("API",response)
    }

    override fun onFileUploadingFailed(responseCode: Int){
        Log.w("API","$responseCode")
    }
}