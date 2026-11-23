package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GooglePeopleApi {
    @GET("v1/people/me/connections")
    suspend fun getConnections(
        @Header("Authorization") bearerToken: String,
        @Query("personFields") personFields: String = "names,phoneNumbers,photos,emailAddresses,memberships",
        @Query("pageSize") pageSize: Int = 500
    ): Response<ResponseBody>

    @GET("v1/contactGroups")
    suspend fun getContactGroups(
        @Header("Authorization") bearerToken: String,
        @Query("pageSize") pageSize: Int = 100
    ): Response<ResponseBody>

    @POST("v1/people:createContact")
    suspend fun createContact(
        @Header("Authorization") bearerToken: String,
        @Body payload: RequestBody
    ): Response<ResponseBody>
}

interface GoogleDriveApi {
    @GET("drive/v3/files")
    suspend fun listAppDataFiles(
        @Header("Authorization") bearerToken: String,
        @Query("spaces") spaces: String = "appDataFolder",
        @Query("q") query: String = "name = 'pulsephone_backup.json' and trashed = false",
        @Query("fields") fields: String = "files(id,name,size,modifiedTime)"
    ): Response<ResponseBody>

    @GET("drive/v3/files/{fileId}")
    suspend fun downloadFile(
        @Header("Authorization") bearerToken: String,
        @Path("fileId") fileId: String,
        @Query("alt") alt: String = "media"
    ): Response<ResponseBody>

    @POST("upload/drive/v3/files?uploadType=multipart")
    suspend fun uploadMultipart(
        @Header("Authorization") bearerToken: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @PATCH("upload/drive/v3/files/{fileId}?uploadType=media")
    suspend fun updateMedia(
        @Header("Authorization") bearerToken: String,
        @Path("fileId") fileId: String,
        @Body body: RequestBody
    ): Response<ResponseBody>
}

data class CustomCallerIdResponse(
    val displayName: String,
    val photoUrl: String?
)

class CustomCallerIdApiClient(private val okHttpClient: OkHttpClient) {

    fun lookupNumber(
        baseUrl: String,
        apiToken: String,
        normalizedNumber: String
    ): CustomCallerIdResponse? {
        val parsed = baseUrl.trim().toHttpUrlOrNull() ?: return null
        // Allow only http or https schemes
        if (parsed.scheme != "https" && parsed.scheme != "http") return null

        val requestUrl = parsed.newBuilder()
            .addQueryParameter("number", normalizedNumber)
            .build()

        val reqBuilder = Request.Builder()
            .url(requestUrl)
            .get()
            .header("Accept", "application/json")

        if (apiToken.isNotBlank()) {
            reqBuilder.header("Authorization", "Bearer ${apiToken.trim()}")
        }

        return runCatching {
            okHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val bodyStr = resp.body?.string().orEmpty()
                if (bodyStr.isBlank()) return@use null
                val json = JSONObject(bodyStr)
                val name = listOf("name", "displayName", "callerName", "full_name")
                    .firstNotNullOfOrNull { key -> json.optString(key).takeIf { it.isNotBlank() } }
                    ?: return@use null
                val photo = listOf("photoUrl", "avatar", "photo", "image_url")
                    .firstNotNullOfOrNull { key -> json.optString(key).takeIf { it.isNotBlank() } }
                CustomCallerIdResponse(displayName = name, photoUrl = photo)
            }
        }.getOrNull()
    }

    companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        fun String.toJsonRequestBody(): RequestBody = toRequestBody(JSON_MEDIA_TYPE)
    }
}
