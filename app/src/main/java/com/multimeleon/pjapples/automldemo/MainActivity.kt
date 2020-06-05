package com.multimeleon.pjapples.automldemo

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.automl.AutoMLImageLabelerLocalModel
import com.google.mlkit.vision.label.automl.AutoMLImageLabelerOptions
import com.google.mlkit.vision.label.automl.AutoMLImageLabelerRemoteModel
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private val GALLERY = 1
    private var AutoMLEnabled = true
    private var labeler: ImageLabeler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        callAutoMLModelLocally()
        uploadImageButton.setOnClickListener {
            showPictureDialog()
        }
    }

    private fun choosePhotoFromGallary() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )

        startActivityForResult(galleryIntent, GALLERY)
    }

    private fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf("Select photo from gallery", "Capture photo from camera")
        pictureDialog.setItems(
            pictureDialogItems
        ) { dialog, which ->
            when (which) {
                0 -> choosePhotoFromGallary()
            }
        }
        pictureDialog.show()
    }

    private fun callAutoMLModelLocally() {

        val localModel = AutoMLImageLabelerLocalModel.Builder()
            .setAssetFilePath("manifest.json")
            .build()

        val remoteModel = callAutoMLModelRemotely()
        RemoteModelManager.getInstance().isModelDownloaded(remoteModel)
            .addOnSuccessListener { isDownloaded ->
                val optionsBuilder =
                    if (isDownloaded) {
                        AutoMLImageLabelerOptions.Builder(remoteModel)
                    } else {
                        AutoMLImageLabelerOptions.Builder(localModel)
                    }
                val options = optionsBuilder.setConfidenceThreshold(0.0f).build()
                labeler = ImageLabeling.getClient(options)
            }
    }


    fun imageFromArray(byteArray: ByteArray) {
        val image = InputImage.fromByteArray(
            byteArray,
            480,
            360,
            0,
            InputImage.IMAGE_FORMAT_NV21
        )

        labeler?.process(image)
            ?.addOnSuccessListener { labels ->
                labels.forEach {
                    val text = it.text
                    val confidence = it.confidence
                    result_textview.text = " ${result_textview.text} $text $confidence \n"
                }
            }
            ?.addOnFailureListener { e ->
            }


    }

    private fun downloadRemoteModel(
        remoteModel: AutoMLImageLabelerRemoteModel,
        conditions: DownloadConditions
    ) {
        RemoteModelManager.getInstance().download(remoteModel, conditions)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Download remote AutoML model success.",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
    }


    private fun callAutoMLModelRemotely(): AutoMLImageLabelerRemoteModel {
        val remoteModel =
            AutoMLImageLabelerRemoteModel.Builder("Pavonia_Leafshapes").build()
        val downloadConditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        downloadRemoteModel(remoteModel = remoteModel, conditions = downloadConditions)
        return remoteModel
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY) {
            if (data != null) {
                val contentURI = data.data
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)

                    Toast.makeText(this@MainActivity, "Image Saved!", Toast.LENGTH_SHORT).show()
                    imageView.setImageBitmap(bitmap)


                    val imgString = Base64.encodeToString(
                        getBytesFromBitmap(bitmap),
                        Base64.NO_WRAP
                    )

                    if (AutoMLEnabled) {
                        imageFromArray(getBytesFromBitmap(bitmap))
                    } else {
                        val requestBody = ModelRequestBody(PayloadRequest(ModelImage(imgString)))
                        Network("https://automl.googleapis.com/v1beta1/", true)
                            .getRetrofitClient()
                            .create(Endpoint::class.java)
                            .classifyImage(requestBody)
                            .enqueue(object : Callback<PayloadResult> {

                                override fun onResponse(
                                    call: Call<PayloadResult>?,
                                    response: Response<PayloadResult>?
                                ) {
                                    if (response!!.isSuccessful) {
                                        Log.d("Hello", response.body().toString())
                                        result_textview.text =
                                            "${response.body()?.items?.first()?.displayName} Score: ${(response.body()?.items?.first()?.classification?.let { it.score * 100 })}"
                                    }
                                }

                                override fun onFailure(call: Call<PayloadResult>, t: Throwable) {
                                    print(t.message)
                                }
                            })
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, "Failed!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun getBytesFromBitmap(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, 70, stream)
        return stream.toByteArray()
    }
}