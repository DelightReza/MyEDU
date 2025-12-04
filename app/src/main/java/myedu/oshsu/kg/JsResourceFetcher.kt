package myedu.oshsu.kg

import android.content.Context
import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PdfResources(val combinedScript: String)

class JsResourceFetcher(private val context: Context) {

    private val client = OkHttpClient()
    private val baseUrl = "https://myedu.oshsu.kg"

    /**
     * Loads stamp.jpg from Assets.
     * Returns a string wrapped in quotes.
     * Returns a transparent 1x1 pixel if loading fails.
     */
    private fun getStampOrPlaceholder(): String {
        return try {
            val inputStream = context.assets.open("stamp.jpg")
            val bytes = inputStream.readBytes()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "\"data:image/jpeg;base64,$base64\""
        } catch (e: Exception) {
            "\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=\""
        }
    }

    suspend fun fetchResources(logger: (String) -> Unit, language: String, dictionary: Map<String, String> = emptyMap()): PdfResources = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch Main Index
            logger("Fetching index...")
            val indexHtml = fetchString("$baseUrl/")
            val mainJsName = findMatch(indexHtml, """src="/assets/(index\.[^"]+\.js)"""") 
                ?: throw Exception(context.getString(R.string.error_main_js_missing))
            
            val mainJsContent = fetchString("$baseUrl/assets/$mainJsName")
            
            // 2. Fetch Transcript Script
            val transcriptJsName = findMatch(mainJsContent, """["']\./(Transcript\.[^"']+\.js)["']""") 
                ?: throw Exception(context.getString(R.string.error_transcript_js_missing))
            
            logger("Fetching $transcriptJsName...")
            val transcriptContent = fetchString("$baseUrl/assets/$transcriptJsName")

            val dependencies = StringBuilder()
            val linkedVariables = mutableSetOf<String>()

            // ==================================================================================
            // DYNAMIC LINKER: Scans imports and links them (Supports '$' and default exports)
            // ==================================================================================
            suspend fun linkDynamicModule(
                fileKeyword: String,        
                fallbackValue: String? = null
            ) {
                try {
                    // Pattern 1: Named Import -> import { h as $ } from "./moment.123.js"
                    val namedImportRegex = Regex("""import\s*\{\s*([\w$]+)\s+as\s+([\w$]+)\s*\}\s*from\s*["']\./($fileKeyword\.[^"']+\.js)["']""")
                    
                    // Pattern 2: Default Import -> import $ from "./moment.123.js"
                    val defaultImportRegex = Regex("""import\s+([\w$]+)\s+from\s*["']\./($fileKeyword\.[^"']+\.js)["']""")

                    var match = namedImportRegex.find(transcriptContent)
                    var isDefault = false

                    if (match == null) {
                        match = defaultImportRegex.find(transcriptContent)
                        isDefault = true
                    }

                    if (match != null) {
                        val fileName = if (isDefault) match.groupValues[2] else match.groupValues[3]
                        val localName = if (isDefault) match.groupValues[1] else match.groupValues[2]
                        val exportName = if (isDefault) "default" else match.groupValues[1]

                        // For Signed/Stamp, we ALWAYS want to use our local asset if available,
                        // overriding whatever is on the server to avoid the "" error.
                        if (fileKeyword == "Signed") {
                            val stamp = fallbackValue ?: "\"\""
                            dependencies.append("var $localName = $stamp;\n")
                            linkedVariables.add(localName)
                            return // Skip fetching the actual file
                        }

                        var fileContent = fetchString("$baseUrl/assets/$fileName")
                        
                        if (language == "en") {
                            fileContent = applyDictionary(fileContent, dictionary)
                        }

                        // Find the internal variable inside the file
                        val internalVar = if (isDefault) {
                             findExportedDefaultName(fileContent)
                        } else {
                             findExportedInternalName(fileContent, exportName)
                        } ?: "{}"

                        val cleanContent = cleanJsContent(fileContent)

                        dependencies.append("var $localName = (() => {\n$cleanContent\nreturn $internalVar;\n})();\n")
                        linkedVariables.add(localName)

                    } else {
                        // FALLBACK LOGIC if file regex not found in Transcript.js
                        val expectedNameRegex = Regex("""import\s*\{\s*[\w$]+\s+as\s+([\w$]+)\s*\}\s*from.*$fileKeyword""")
                        val nameMatch = expectedNameRegex.find(transcriptContent)
                        val varName = nameMatch?.groupValues?.get(1) ?: if (fileKeyword == "moment") "$" else null

                        if (varName != null) {
                            val safeFallback = if (varName == "$") {
                                // Fallback for Moment.js
                                """
                                function(d) { 
                                    return { 
                                        format: function(f) { 
                                            try {
                                                var date = d ? new Date(d) : new Date();
                                                var day = ("0" + date.getDate()).slice(-2);
                                                var month = ("0" + (date.getMonth() + 1)).slice(-2);
                                                var year = date.getFullYear();
                                                return day + "." + month + "." + year; 
                                            } catch(e) { return ""; }
                                        },
                                        locale: function() {}
                                    }; 
                                }
                                """.trimIndent()
                            } else {
                                fallbackValue ?: "{}"
                            }
                            dependencies.append("var $varName = $safeFallback;\n")
                            linkedVariables.add(varName)
                        }
                    }
                } catch (e: Exception) {
                    logger("Link Error ($fileKeyword): ${e.message}")
                }
            }

            // 3. Link All Dependencies
            linkDynamicModule("moment")
            linkDynamicModule("PdfStyle")
            linkDynamicModule("PdfFooter4", "()=>({})")
            linkDynamicModule("KeysValue")
            linkDynamicModule("Signed", getStampOrPlaceholder()) // Pass Local Stamp
            linkDynamicModule("helpers")
            linkDynamicModule("ru")

            // 4. Robust Mocking for leftovers
            val dummyScript = StringBuilder()
            dummyScript.append("""
                const UniversalDummy = new Proxy(function(){}, {
                    get: function(target, prop) {
                        if(prop === Symbol.toPrimitive) return (hint) => hint === 'number' ? 0 : "";
                        if(prop === 'toString') return () => "";
                        if(prop === 'valueOf') return () => 0;
                        return UniversalDummy;
                    },
                    apply: () => UniversalDummy,
                    construct: () => UniversalDummy
                });
            """.trimIndent())

            val allImports = Regex("""import\s*\{(.*?)\}\s*from""").findAll(transcriptContent)
            allImports.forEach { m ->
                m.groupValues[1].split(",").forEach { 
                    val parts = it.trim().split(Regex("""\s+as\s+"""))
                    if (parts.size == 2) {
                        val varName = parts[1].trim()
                        if (!linkedVariables.contains(varName)) {
                            dummyScript.append("\nvar $varName = UniversalDummy;")
                        }
                    }
                }
            }

            // 5. Final Assembly
            var cleanTranscript = cleanJsContent(transcriptContent)
            if (language == "en") {
                cleanTranscript = applyDictionary(cleanTranscript, dictionary)
            }

            val funcNameMatch = findMatch(cleanTranscript, """(\w+)\s*=\s*\(C,a,c,d\)""")
            val exposeCode = if (funcNameMatch != null) "\nwindow.PDFGenerator = $funcNameMatch;" else ""

            val finalScript = dummyScript.toString() + "\n" + dependencies.toString() + "\n" + cleanTranscript + exposeCode
            return@withContext PdfResources(finalScript)

        } catch (e: Exception) {
            logger("Fetch Error: ${e.message}")
            e.printStackTrace()
            return@withContext PdfResources("")
        }
    }

    private fun findExportedInternalName(content: String, exportName: String): String? {
        val regexAs = Regex("""export\s*\{\s*([\w$]+)\s+as\s+$exportName\s*\}""")
        regexAs.find(content)?.let { return it.groupValues[1] }
        val regexDirect = Regex("""export\s*\{[^}]*\b$exportName\b[^}]*\}""")
        if (regexDirect.containsMatchIn(content)) return exportName
        return null
    }
    
    private fun findExportedDefaultName(content: String): String? {
        val regexDefault = Regex("""export\s+default\s+([\w$]+)""")
        regexDefault.find(content)?.let { return it.groupValues[1] }
        return null
    }

    private fun cleanJsContent(content: String): String {
        return content
            .replace(Regex("""import\s*\{.*?\}\s*from\s*['"].*?['"];?""", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("""import\s+[\w$]+\s+from\s*['"].*?['"];?"""), "")
            .replace(Regex("""export\s*\{.*?\}""", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("""export\s+default"""), "")
    }

    private fun applyDictionary(script: String, dictionary: Map<String, String>): String {
        var s = script
        dictionary.forEach { (ru, en) -> if (ru.length > 1) s = s.replace(ru, en) }
        return s
    }

    private fun fetchString(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception(context.getString(R.string.error_http, response.code))
            return response.body?.string() ?: ""
        }
    }

    private fun findMatch(content: String, regex: String): String? {
        return Regex(regex).find(content)?.let { match ->
            if (match.groupValues.size > 1) match.groupValues[1] else match.groupValues[0]
        }
    }
}
