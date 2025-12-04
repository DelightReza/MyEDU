package myedu.oshsu.kg

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ReferenceResources(val scriptContent: String, val stampBase64: String?)

class ReferenceJsFetcher(private val context: Context) {
    private val client = OkHttpClient()
    private val baseUrl = "https://myedu.oshsu.kg"

    private fun getStampOrPlaceholder(): String {
        return try {
            val inputStream = context.assets.open("stamp.jpg")
            val bytes = inputStream.readBytes()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64" // Reference PDF needs raw base64 string
        } catch (e: Exception) {
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
        }
    }

    suspend fun fetchResources(logger: (String) -> Unit, language: String, dictionary: Map<String, String>): ReferenceResources = withContext(Dispatchers.IO) {
        try {
            val indexHtml = fetchString("$baseUrl/")
            val mainJsName = findMatch(indexHtml, """src="/assets/(index\.[^"]+\.js)"""") 
                ?: throw Exception(context.getString(R.string.error_main_js_missing))
            val mainJsContent = fetchString("$baseUrl/assets/$mainJsName")
            
            var refJsPath = findMatch(mainJsContent, """["']([^"']*References7\.[^"']+\.js)["']""")
            if (refJsPath == null) {
                val docsJsPath = findMatch(mainJsContent, """["']([^"']*StudentDocuments\.[^"']+\.js)["']""")
                if (docsJsPath != null) {
                    val docsContent = fetchString("$baseUrl/assets/${getName(docsJsPath)}")
                    refJsPath = findMatch(docsContent, """["']([^"']*References7\.[^"']+\.js)["']""")
                }
            }
            if (refJsPath == null) throw Exception(context.getString(R.string.error_ref_script_missing))
            
            val refJsName = getName(refJsPath)
            logger("Fetching $refJsName...")
            val refContent = fetchString("$baseUrl/assets/$refJsName")

            val dependencies = StringBuilder()
            
            suspend fun linkModule(fileKeyword: String, exportChar: String, fallbackName: String, fallbackValue: String) {
                var varName: String? = null
                
                // Regex to find what variable name the main script imports it as
                if (exportChar == "DEFAULT_OR_NAMED") {
                     val regex = Regex("""import\s*\{\s*(\w+)\s*\}\s*from\s*['"][^'"]*$fileKeyword[^'"]*['"]""")
                     varName = findMatch(refContent, regex.pattern)
                } else {
                    val regex = Regex("""import\s*\{\s*$exportChar\s+as\s+(\w+)\s*\}\s*from\s*['"][^'"]*$fileKeyword[^'"]*['"]""")
                    varName = findMatch(refContent, regex.pattern)
                }
                
                if (varName == null) varName = fallbackName

                // Special handling for Signed (Stamp) to use local asset
                if (fileKeyword == "Signed") {
                    // For reference PDF, stamp logic is handled mainly in ReferenceResources
                    // But if the script uses a variable, we define it here too
                    // fallbackValue passed in is usually the raw base64 string
                    dependencies.append("var $varName = $fallbackValue;\n")
                    return
                }

                val fileUrlRegex = Regex("""["']([^"']*$fileKeyword\.[^"']+\.js)["']""")
                val fileNameMatch = fileUrlRegex.find(refContent) ?: fileUrlRegex.find(mainJsContent)
                var success = false
                if (fileNameMatch != null) {
                    val fileName = getName(fileNameMatch.groupValues[1])
                    try {
                        val fileContent = fetchString("$baseUrl/assets/$fileName")
                        val exportRegex = if (exportChar == "DEFAULT_OR_NAMED") 
                             Regex("""export\s*\{\s*(\w+)\s*\}""") 
                        else 
                             Regex("""export\s*\{\s*(\w+)\s+as\s+$exportChar\s*\}""")
                        val internalVarMatch = exportRegex.find(fileContent)
                        if (internalVarMatch != null) {
                            val internalVar = internalVarMatch.groupValues[1]
                            val cleanContent = cleanJsContent(fileContent)
                            dependencies.append("var $varName = (() => {\n$cleanContent\nreturn $internalVar;\n})();\n")
                            success = true
                        }
                    } catch (e: Exception) { logger("Link Warn: $fileName ${e.message}") }
                }
                if (!success) dependencies.append("var $varName = $fallbackValue;\n")
            }

            // Link modules using the new robust logic
            linkModule("PdfStyle", "P", "PdfStyle_Fallback", "{}")          
            
            // Pass the quoted string for the JS variable
            val stampJsValue = "\"${getStampOrPlaceholder()}\"" 
            linkModule("Signed", "S", "Signed_Fallback", stampJsValue)            
            
            linkModule("LicenseYear", "L", "LicenseYear_Fallback", "[]")    
            linkModule("SpecialityLincense", "S", "SpecLic_Fallback", "{}") 
            linkModule("DocumentLink", "DEFAULT_OR_NAMED", "DocLink_Fallback", "{}") 
            linkModule("ru", "r", "Ru_Fallback", "{}")                      

            val varsToMock = mutableSetOf<String>()
            val genericImportRegex = Regex("""import\s*\{(.*?)\}\s*from\s*['"].*?['"];?""")
            genericImportRegex.findAll(refContent).forEach { match ->
                match.groupValues[1].split(",").forEach { item ->
                    val parts = item.trim().split(Regex("""\s+as\s+"""))
                    val name = if (parts.size == 2) parts[1] else parts[0]
                    if (name.isNotBlank()) varsToMock.add(name.trim())
                }
            }
            varsToMock.remove("$") 

            val dummyScript = StringBuilder()
            dummyScript.append("const UniversalDummy = new Proxy(function(){}, { get: () => UniversalDummy, apply: () => UniversalDummy, construct: () => UniversalDummy });\n")
            if (varsToMock.isNotEmpty()) {
                dummyScript.append("var ")
                dummyScript.append(varsToMock.joinToString(",") { "$it = UniversalDummy" })
                dummyScript.append(";\n")
            }

            var cleanRef = cleanJsContent(refContent)
            if (language == "en" && dictionary.isNotEmpty()) {
                logger("Translating Reference Template...")
                dictionary.forEach { (ru, en) -> if (ru.length > 1) cleanRef = cleanRef.replace(ru, en) }
            }

            val generatorRegex = Regex("""const\s+(\w+)\s*=\s*\([a-zA-Z0-9,]*\)\s*=>\s*\{[^}]*pageSize:["']A4["']""")
            val generatorMatch = generatorRegex.find(cleanRef)
            val genFuncName = generatorMatch?.groupValues?.get(1) ?: "at" 
            val exposeCode = "\nwindow.RefDocGenerator = $genFuncName;"
            
            // Pass the RAW base64 (not quoted) for the ReferenceResources stampBase64 field
            val rawStamp = getStampOrPlaceholder()
            
            val finalScript = dummyScript.toString() + dependencies.toString() + "\n(() => {\n" + cleanRef + exposeCode + "\n})();"
            return@withContext ReferenceResources(finalScript, rawStamp)
        } catch (e: Exception) {
            logger("Ref Fetch Error: ${e.message}")
            e.printStackTrace()
            return@withContext ReferenceResources("", null)
        }
    }

    private fun cleanJsContent(content: String): String {
        return content
            .replace(Regex("""import\s*\{[^}]*\}\s*from\s*['"][^'"]+['"];?""", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("""import\s+[\w*]+\s+(?:as\s+\w+\s+)?from\s*['"][^'"]+['"];?"""), "")
            .replace(Regex("""import\s*['"][^'"]+['"];?"""), "")
            .replace(Regex("""export\s*\{[^}]*\}""", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("""export\s+default\s+"""), "")
    }

    private fun getName(path: String) = path.split('/').last()

    private fun fetchString(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception(context.getString(R.string.error_http, response.code))
            return response.body?.string() ?: ""
        }
    }

    private fun findMatch(content: String, regex: String): String? {
        return Regex(regex).find(content)?.groupValues?.get(1)
    }
}


class ReferencePdfGenerator(private val context: Context) {
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun generatePdf(
        studentInfoJson: String, licenseInfoJson: String, univInfoJson: String, linkId: Long, qrUrl: String, resources: ReferenceResources, bearerToken: String, language: String = "ru", dictionary: Map<String, String> = emptyMap(), logCallback: (String) -> Unit
    ): ByteArray = suspendCancellableCoroutine { continuation ->
        Handler(Looper.getMainLooper()).post {
            try {
                val webView = WebView(context)
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setCookie("https://myedu.oshsu.kg", "myedu-jwt-token=$bearerToken; Domain=myedu.oshsu.kg; Path=/")
                cookieManager.flush()

                webView.webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(cm: ConsoleMessage): Boolean { logCallback("[JS] ${cm.message()} (L${cm.lineNumber()})"); return true }
                }
                webView.addJavascriptInterface(object : Any() {
                    @JavascriptInterface fun returnPdf(base64: String) {
                        try {
                            val clean = if (base64.contains(",")) base64.split(",")[1] else base64
                            if (continuation.isActive) continuation.resume(Base64.decode(clean, Base64.DEFAULT))
                        } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
                    }
                    @JavascriptInterface fun returnError(msg: String) { if (continuation.isActive) continuation.resumeWithException(Exception(msg)) }
                    @JavascriptInterface fun log(msg: String) = logCallback(msg)
                }, "AndroidBridge")

                val dictionaryJson = JSONObject(dictionary).toString()
                val dateLocale = if (language == "en") "en-US" else "ru-RU"
                val jsContent = resources.scriptContent
                val generatorRegex = Regex("""const\s+(\w+)\s*=\s*\(\w+,\w+,\w+\)\s*=>\s*\{[\s\S]*?return\s*\{[\s\S]*?pageSize\s*:\s*["']A4["'][\s\S]*?\}\s*\}""")
                val match = generatorRegex.find(jsContent)
                val extractedFunction: String = if (match != null) {
                    match.value.replaceFirst("const ${match.groupValues[1]}", "const generateDocDef")
                } else {
                    val startIndex = jsContent.indexOf("const at=(")
                    val endIndex = jsContent.indexOf("const nt={")
                    if (startIndex != -1 && endIndex != -1) {
                        var code = jsContent.substring(startIndex, endIndex)
                        code = code.replaceFirst("const at", "const generateDocDef")
                        if (code.trim().endsWith(",")) code = code.substring(0, code.lastIndexOf(","))
                        code
                    } else {
                        if (continuation.isActive) continuation.resumeWithException(Exception(context.getString(R.string.error_js_logic, "Extraction failed")))
                        return@post
                    }
                }

                // Fallback here is just a safety measure, resources.stampBase64 should be populated by Assets now
                val stamp = resources.stampBase64 ?: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+P+/HgAFhAJ/wlseKgAAAABJRU5ErkJggg=="
                val defaultAddr = context.getString(R.string.university_addr_default)

                val html = """
                <!DOCTYPE html><html><head><script src="https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.2.7/pdfmake.min.js"></script><script src="https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.2.7/vfs_fonts.js"></script></head><body><script>
                    window.onerror = function(msg, url, line) { AndroidBridge.returnError(msg + " @ " + line); };
                    const studentInfo = $studentInfoJson; const licenseInfo = $licenseInfoJson; const univInfo = $univInfoJson; const qrCodeUrl = "$qrUrl"; const lang = "$language"; const dictionary = $dictionaryJson;
                    const tt = { f10: {fontSize:10}, f11:{fontSize:11}, f12:{fontSize:12}, f13:{fontSize:13}, f14:{fontSize:14}, f16:{fontSize:16}, fb:{bold:true}, textCenter:{alignment:'center'}, textRight:{alignment:'right'} };
                    const et = "$stamp";
                    $extractedFunction
                    function translateString(str) { if (!str || typeof str !== 'string') return str; if (dictionary[str]) return dictionary[str]; let s = str; for (const [key, value] of Object.entries(dictionary)) { if (key.length > 2 && s.includes(key)) { s = s.split(key).join(value); } } return s; }
                    function prepareAndGenerate() {
                        try {
                            if (lang === "en") {
                                 const tr = (obj, key) => { if (obj && obj[key]) obj[key] = translateString(obj[key]); };
                                 tr(studentInfo, "faculty_ru"); tr(studentInfo, "speciality_ru"); tr(studentInfo, "edu_form_ru"); tr(studentInfo, "payment_form_name_ru");
                                 if (studentInfo.lastStudentMovement) { const lsm = studentInfo.lastStudentMovement; if (lsm.speciality && lsm.speciality.direction) tr(lsm.speciality.direction, "name_ru"); if (lsm.edu_form) tr(lsm.edu_form, "name_ru"); if (lsm.payment_form) tr(lsm.payment_form, "name_ru"); }
                                 tr(univInfo, "address_ru");
                            }
                            let courses = ["первого","второго","третьего","четвертого","пятого","шестого","седьмого","восьмого"];
                            if (lang === "en") courses = ["First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth"];
                            const activeSem = studentInfo.active_semester || 1; const totalSem = licenseInfo.total_semester || 8; const e = Math.floor((activeSem - 1) / 2); const i = Math.floor((totalSem - 1) / 2);
                            const suffix = lang === "en" ? " Course" : "-го"; const courseStr = courses[Math.min(e, i)] || (Math.min(e, i) + 1) + suffix;
                            const second = studentInfo.second || "24"; const studId = studentInfo.lastStudentMovement ? studentInfo.lastStudentMovement.id_student : "0"; const payId = studentInfo.payment_form_id || (studentInfo.lastStudentMovement ? studentInfo.lastStudentMovement.id_payment_form : "1");
                            const docIdStr = "№ 7-" + second + "-" + studId + "-" + payId;
                            let address = univInfo.address_ru || "$defaultAddr";
                            if(lang === "en") address = translateString(address);
                            const extraData = { id: docIdStr, edunum: courseStr, date: new Date().toLocaleDateString("$dateLocale"), adress: address };
                            const docDef = generateDocDef(studentInfo, extraData, qrCodeUrl);
                            pdfMake.createPdf(docDef).getBase64(function(b64) { AndroidBridge.returnPdf(b64); });
                        } catch(e) { AndroidBridge.returnError("Logic Error: " + e.message); }
                    }
                    prepareAndGenerate();
                </script></body></html>
                """
                webView.loadDataWithBaseURL("https://myedu.oshsu.kg/", html, "text/html", "UTF-8", null)
            } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
        }
    }
}
