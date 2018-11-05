package com.multimeleon.pjapples.automldemo


import android.support.annotation.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

class Network(private val baseUrl: String, enableLog: Boolean) {


    private val sGson: Gson = GsonBuilder()
        .setLenient()
        .create()


    @VisibleForTesting
    internal val okHttpClient: OkHttpClient

    private val gsonConverterFactory = GsonConverterFactory.create(sGson)

    init {
        val httpClientBuilder = OkHttpClient.Builder()
            .readTimeout(NetworkConfig.READ_TIMEOUT, TimeUnit.MINUTES)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT, TimeUnit.MINUTES)
            .connectTimeout(NetworkConfig.CONNECTION_TIMEOUT, TimeUnit.MINUTES)

        //Add debug interceptors
        if (enableLog) {
            httpClientBuilder.addInterceptor(
                HttpLoggingInterceptor()
                .apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }

        val headerAuthorizationInterceptor = Interceptor() {
            var request = it.request()
            val headers = request.headers().newBuilder().add("Authorization","Bearer Enter Service Account Token here").build()
            request = request.newBuilder().headers(headers).build()
            return@Interceptor it.proceed(request)
        }

        httpClientBuilder.addInterceptor(headerAuthorizationInterceptor)

        okHttpClient = httpClientBuilder.build()
    }

    fun getRetrofitClient(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .addConverterFactory(gsonConverterFactory)
            .build()
    }

}

internal object NetworkConfig {


    internal const val READ_TIMEOUT = 1L

    internal const val WRITE_TIMEOUT = 1L

    internal const val CONNECTION_TIMEOUT = 1L
}

data class ModelRequestBody (@field:SerializedName("payload")val image: PayloadRequest)
data class PayloadRequest (@field:SerializedName("image")val image: ModelImage)
data class ModelImage (@field:SerializedName("imageBytes") val imageBytes : String)


internal interface Endpoint {

    @Headers("Content-Type: application/json")
    @POST("projects/{ProjectName}/locations/us-central1/models/{AutoML_Model_ID}:predict")
    fun classifyImage(@Body body: ModelRequestBody): Call<PayloadResult>
}

data class PayloadResult(

    @field:SerializedName("payload")
    val items: List<PayLoad>
)

data class Classification (@field:SerializedName("score")
                           val score: Double)

data class PayLoad(@field:SerializedName("classification")
                   val classification: Classification,

                   @field:SerializedName("displayName")
                   val displayName: String)