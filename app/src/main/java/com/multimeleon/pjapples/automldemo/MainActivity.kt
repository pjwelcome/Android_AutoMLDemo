package com.multimeleon.pjapples.automldemo

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private val GALLERY = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        uploadImageButton.setOnClickListener {
            showPictureDialog()
        }
    }

    private fun choosePhotoFromGallary() {
        val galleryIntent = Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        startActivityForResult(galleryIntent, GALLERY)
    }

    private fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf("Select photo from gallery", "Capture photo from camera")
        pictureDialog.setItems(pictureDialogItems
        ) { dialog, which ->
            when (which) {
                0 -> choosePhotoFromGallary()

            }
        }
        pictureDialog.show()
    }

    public override fun onActivityResult(requestCode:Int, resultCode:Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY)
        {
            if (data != null)
            {
                val contentURI = data?.data
                try
                {
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)

                    Toast.makeText(this@MainActivity, "Image Saved!", Toast.LENGTH_SHORT).show()
                    imageView.setImageBitmap(bitmap)

                    val imgString = Base64.encodeToString(
                        getBytesFromBitmap(bitmap),
                        Base64.NO_WRAP
                    )


                    val requestBody = ModelRequestBody(PayloadRequest(ModelImage(imgString)))
                    Network("https://automl.googleapis.com/v1beta1/",true).getRetrofitClient().create(Endpoint::class.java).classifyImage(requestBody).enqueue(object : Callback<PayloadResult> {

                        override fun onResponse(call: Call<PayloadResult>?, response: Response<PayloadResult>?) {
                            if (response!!.isSuccessful) {
                                Log.d("Hello", response?.body().toString())
                                result_textview.text =  "${response?.body()?.items?.first()?.displayName} Score: ${(response?.body()?.items?.first()?.classification?.let { it.score * 100 })}"
                            }
                        }

                        override fun onFailure(call: Call<PayloadResult>, t: Throwable) {
                            print(t!!.message)
                        }
                    }
                    )
                }
                catch (e: IOException) {
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
