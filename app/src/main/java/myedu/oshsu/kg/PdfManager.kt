package myedu.oshsu.kg

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext

data class Quadruple(val info: String, val transcript: String, val linkId: Long, val url: String)
data class Quintuple(val info: String, val license: String, val univ: String, val linkId: Long, val url: String)

class PdfManager(
    private val jsFetcher: JsResourceFetcher?,
    private val refFetcher: ReferenceJsFetcher?
) {
    private var cachedResourcesRu: PdfResources? = null
    private var cachedResourcesEn: PdfResources? = null
    private var cachedRefResourcesRu: ReferenceResources? = null
    private var cachedRefResourcesEn: ReferenceResources? = null

    suspend fun generateTranscriptPdf(
        context: Context,
        studentId: Long,
        movId: Long,
        lang: String,
        dictionaryMap: Map<String, String>,
        onStatus: suspend (String?) -> Unit
    ): Uri? {
        onStatus(context.getString(R.string.status_preparing_transcript))
        try {
            var resources = if (lang == "en") cachedResourcesEn else cachedResourcesRu
            if (resources == null) {
                onStatus(context.getString(R.string.status_fetching_scripts))
                val fetcher = jsFetcher ?: JsResourceFetcher(context)
                resources = fetcher.fetchResources({ }, lang, dictionaryMap)
                if (lang == "en") cachedResourcesEn = resources else cachedResourcesRu = resources
            }

            coroutineContext.ensureActive()

            val (infoJsonString, transcriptRaw, linkId, rawUrl) = withContext(Dispatchers.IO) {
                val infoRaw = NetworkClient.api.getStudentInfoRaw(studentId).string()
                val infoJson = JSONObject(infoRaw)
                infoJson.put("fullName", "${infoJson.optString("last_name")} ${infoJson.optString("name")} ${infoJson.optString("father_name")}".replace("null", "").trim())
                val transcriptRaw = NetworkClient.api.getTranscriptDataRaw(studentId, movId).string()
                val keyRaw = NetworkClient.api.getTranscriptLink(DocIdRequest(studentId)).string()
                val keyObj = JSONObject(keyRaw)
                Quadruple(infoJson.toString(), transcriptRaw, keyObj.optLong("id"), keyObj.optString("url"))
            }

            val qrUrl = rawUrl.replace("https::/", "https://")
            val infoObj = JSONObject(infoJsonString)
            val lastName = infoObj.optString("last_name", "").replace("null", "").trim()
            val firstName = infoObj.optString("name", "").replace("null", "").trim()

            onStatus(context.getString(R.string.generating_pdf))
            val bytes = WebPdfGenerator(context).generatePdf(infoJsonString, transcriptRaw, linkId, qrUrl, resources!!, lang, dictionaryMap) { }

            onStatus(context.getString(R.string.uploading_pdf))
            try {
                uploadPdfOnly(linkId, studentId, bytes, getFormattedFileName("Transcript", lang, lastName, firstName), true)
            } catch (e: Exception) {
                DebugLogger.log("PDF_UPLOAD", "Failed to upload transcript: ${e.message}")
            }

            return saveToDownloads(context, bytes, getFormattedFileName("Transcript", lang, lastName, firstName), onStatus)
        } catch (e: CancellationException) {
            onStatus(null)
            return null
        } catch (e: Exception) {
            onStatus(context.getString(R.string.error_generic, e.message))
            delay(3000)
            onStatus(null)
            return null
        }
    }

    suspend fun generateReferencePdf(
        context: Context,
        studentId: Long,
        profileData: StudentInfoResponse?,
        authToken: String?,
        lang: String,
        dictionaryMap: Map<String, String>,
        onStatus: suspend (String?) -> Unit
    ): Uri? {
        onStatus(context.getString(R.string.status_preparing_reference))
        try {
            var resources = if (lang == "en") cachedRefResourcesEn else cachedRefResourcesRu
            if (resources == null) {
                onStatus(context.getString(R.string.status_fetching_scripts))
                val fetcher = refFetcher ?: ReferenceJsFetcher(context)
                resources = fetcher.fetchResources({ }, lang, dictionaryMap)
                if (lang == "en") cachedRefResourcesEn = resources else cachedRefResourcesRu = resources
            }

            coroutineContext.ensureActive()

            val (infoJsonString, licenseRaw, univRaw, linkId, rawUrl) = withContext(Dispatchers.IO) {
                val infoRaw = NetworkClient.api.getStudentInfoRaw(studentId).string()
                val infoJson = JSONObject(infoRaw)
                infoJson.put("fullName", "${infoJson.optString("last_name")} ${infoJson.optString("name")} ${infoJson.optString("father_name")}".replace("null", "").trim())
                val specId = infoJson.optJSONObject("speciality")?.optInt("id") ?: infoJson.optJSONObject("lastStudentMovement")?.optJSONObject("speciality")?.optInt("id") ?: 0
                val eduFormId = infoJson.optJSONObject("lastStudentMovement")?.optJSONObject("edu_form")?.optInt("id") ?: infoJson.optJSONObject("edu_form")?.optInt("id") ?: 0
                val licenseRaw = NetworkClient.api.getSpecialityLicense(specId, eduFormId).string()
                val univRaw = NetworkClient.api.getUniversityInfo().string()
                val linkRaw = NetworkClient.api.getReferenceLink(DocIdRequest(studentId)).string()
                val linkObj = JSONObject(linkRaw)
                Quintuple(infoJson.toString(), licenseRaw, univRaw, linkObj.optLong("id"), linkObj.optString("url"))
            }

            val qrUrl = rawUrl.replace("https::/", "https://")
            val infoObj = JSONObject(infoJsonString)
            val lastName = infoObj.optString("last_name", "").replace("null", "").trim()
            val firstName = infoObj.optString("name", "").replace("null", "").trim()

            onStatus(context.getString(R.string.generating_pdf))
            val bytes = ReferencePdfGenerator(context).generatePdf(infoJsonString, licenseRaw, univRaw, linkId, qrUrl, resources!!, authToken ?: "", lang, dictionaryMap) { }

            onStatus(context.getString(R.string.uploading_pdf))
            try {
                uploadPdfOnly(linkId, studentId, bytes, getFormattedFileName("Reference", lang, lastName, firstName), false)
            } catch (e: Exception) {
                DebugLogger.log("PDF_UPLOAD", "Failed to upload reference: ${e.message}")
            }

            return saveToDownloads(context, bytes, getFormattedFileName("Reference", lang, lastName, firstName), onStatus)
        } catch (e: CancellationException) {
            onStatus(null)
            return null
        } catch (e: Exception) {
            onStatus(context.getString(R.string.error_generic, e.message))
            delay(3000)
            onStatus(null)
            return null
        }
    }

    private suspend fun uploadPdfOnly(linkId: Long, studentId: Long, bytes: ByteArray, filename: String, isTranscript: Boolean) {
        val plain = AppConstants.MIME_PLAIN_TEXT.toMediaTypeOrNull()
        val pdfType = AppConstants.MIME_PDF.toMediaTypeOrNull()
        val bodyId = linkId.toString().toRequestBody(plain)
        val bodyStudent = studentId.toString().toRequestBody(plain)
        val filePart = MultipartBody.Part.createFormData("pdf", filename, bytes.toRequestBody(pdfType))
        withContext(Dispatchers.IO) {
            if (isTranscript) NetworkClient.api.uploadPdf(bodyId, bodyStudent, filePart).string()
            else NetworkClient.api.uploadReferencePdf(bodyId, bodyStudent, filePart).string()
        }
    }

    private suspend fun saveToDownloads(context: Context, bytes: ByteArray, filename: String, onStatus: suspend (String?) -> Unit): Uri? {
        try {
            onStatus(context.getString(R.string.status_saving))
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            var file = File(downloadsDir, filename)
            var counter = 1
            while (file.exists()) {
                file = File(downloadsDir, "${filename.substringBeforeLast(".")}($counter).${filename.substringAfterLast(".")}")
                counter++
            }
            withContext(Dispatchers.IO) { FileOutputStream(file).use { it.write(bytes) } }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.status_saved, file.name), Toast.LENGTH_SHORT).show()
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, AppConstants.MIME_PDF)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.error_no_pdf_viewer), Toast.LENGTH_LONG).show()
                }
            }
            return uri
        } catch (e: Exception) {
            onStatus(context.getString(R.string.status_save_failed, e.message))
            delay(2000)
            return null
        }
    }

    companion object {
        fun getFormattedFileName(docType: String, lang: String?, lastName: String?, firstName: String?): String {
            val last = lastName ?: ""
            val first = firstName ?: ""
            val cleanName = "$last $first".trim().replace(" ", "_").replace(".", "")
            val suffix = if (lang != null) "_$lang" else ""
            return "${cleanName}_${docType}${suffix}.pdf"
        }
    }
}
