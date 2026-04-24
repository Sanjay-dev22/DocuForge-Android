package com.sanjay.omnidocviewer.utils

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.sanjay.omnidocviewer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

object PptToPdfConverter {

    private val client = OkHttpClient()

    suspend fun convert(
        resolver: ContentResolver,
        uri: Uri
    ): File = withContext(Dispatchers.IO) {

        // =========================
        // STEP 1 — CREATE JOB
        // =========================
        val jobJson = """
        {
          "tasks": {
            "import-file": { "operation": "import/upload" },
            "convert-file": {
              "operation": "convert",
              "input": "import-file",
              "output_format": "pdf"
            },
            "export-file": {
              "operation": "export/url",
              "input": "convert-file"
            }
          }
        }
        """.trimIndent()

        val createJobReq = Request.Builder()
            .url("https://api.cloudconvert.com/v2/jobs")
            .addHeader("Authorization", "Bearer ${BuildConfig.CLOUD_CONVERT_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .post(jobJson.toRequestBody("application/json".toMediaType()))
            .build()

        val jobResponse = client.newCall(createJobReq).execute()
        val responseString = jobResponse.body?.string() ?: ""

        Log.d("CLOUD_CONVERT", "RAW RESPONSE: $responseString")

        if (!jobResponse.isSuccessful) {
            throw Exception("Job creation failed: $responseString")
        }

        val jobJsonObj = JSONObject(responseString)

        val jobId = jobJsonObj.getJSONObject("data").getString("id")
        val tasks = jobJsonObj.getJSONObject("data").getJSONArray("tasks")

        var uploadUrl: String? = null
        var uploadParams: JSONObject? = null

        for (i in 0 until tasks.length()) {
            val task = tasks.getJSONObject(i)

            if (task.getString("name") == "import-file" && task.has("result")) {
                val form = task.getJSONObject("result").getJSONObject("form")
                uploadUrl = form.getString("url")
                uploadParams = form.getJSONObject("parameters")
            }
        }

        if (uploadUrl.isNullOrEmpty() || uploadParams == null) {
            throw Exception("Upload URL not received")
        }

        Log.d("CLOUD_CONVERT", "UPLOAD URL: $uploadUrl")
        Log.d("CLOUD_CONVERT", "UPLOAD PARAMS: $uploadParams")

        // =========================
        // STEP 2 — PREP FILE
        // =========================
        val tempFile = File.createTempFile("upload", ".pptx")

        resolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { input.copyTo(it) }
        } ?: throw Exception("Cannot read input file")

        // =========================
        // STEP 3 — UPLOAD (CRITICAL FIX)
        // =========================
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)

        uploadParams!!.keys().forEach { key ->
            var value = uploadParams!!.getString(key)

            // 🔥 FIX: replace ${filename}
            if (key == "key") {
                value = value.replace("\${filename}", tempFile.name)
            }

            builder.addFormDataPart(key, value)
        }

        builder.addFormDataPart(
            "file",
            tempFile.name,
            tempFile.asRequestBody("application/octet-stream".toMediaType())
        )

        val uploadReq = Request.Builder()
            .url(uploadUrl!!)
            .post(builder.build())
            .build()

        val uploadRes = client.newCall(uploadReq).execute()

        Log.d("CLOUD_CONVERT", "UPLOAD CODE: ${uploadRes.code}")
        Log.d("CLOUD_CONVERT", "UPLOAD BODY: ${uploadRes.body?.string()}")

        if (uploadRes.code != 201) {
            throw Exception("Upload failed: ${uploadRes.code}")
        }

        // =========================
// STEP 4 — POLL RESULT (FINAL)
// =========================
        var pdfUrl: String? = null

        for (attempt in 1..25) {

            Thread.sleep(2000)

            Log.d("CLOUD_CONVERT", "Polling attempt: $attempt")

            val statusReq = Request.Builder()
                .url("https://api.cloudconvert.com/v2/jobs/$jobId")
                .addHeader("Authorization", "Bearer ${BuildConfig.CLOUD_CONVERT_API_KEY}")
                .build()

            val statusRes = client.newCall(statusReq).execute()
            val statusJsonStr = statusRes.body?.string() ?: ""

            Log.d("CLOUD_CONVERT", "STATUS RESPONSE: $statusJsonStr")

            val statusJson = JSONObject(statusJsonStr)
            val updatedTasks = statusJson.getJSONObject("data").getJSONArray("tasks")

            for (i in 0 until updatedTasks.length()) {

                val task = updatedTasks.getJSONObject(i)
                val name = task.getString("name")
                val status = task.getString("status")

                Log.d("CLOUD_CONVERT", "Task: $name | Status: $status")

                if (name == "export-file" && status == "finished") {

                    pdfUrl = task.getJSONObject("result")
                        .getJSONArray("files")
                        .getJSONObject(0)
                        .getString("url")

                    Log.d("CLOUD_CONVERT", "✅ PDF URL FOUND: $pdfUrl")

                    break
                }
            }

            if (!pdfUrl.isNullOrEmpty()) {
                Log.d("CLOUD_CONVERT", "🛑 Polling stopped early — SUCCESS")
                break // ✅ THIS ACTUALLY STOPS LOOP
            }
        }

        if (pdfUrl.isNullOrEmpty()) {
            throw Exception("Conversion timeout")
        }

        Log.d("CLOUD_CONVERT", "🎯 FINAL PDF URL: $pdfUrl")
        // =========================
        // STEP 5 — DOWNLOAD
        // =========================
        val pdfFile = File.createTempFile("converted", ".pdf")

        val downloadReq = Request.Builder()
            .url(pdfUrl!!)
            .build()

        val downloadRes = client.newCall(downloadReq).execute()

        if (!downloadRes.isSuccessful) {
            throw Exception("Download failed")
        }

        downloadRes.body!!.byteStream().use { input ->
            pdfFile.outputStream().use { input.copyTo(it) }
        }

        pdfFile
    }
}