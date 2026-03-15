package myedu.oshsu.kg

import okhttp3.*
import okio.Buffer
import java.io.IOException
import java.nio.charset.Charset

class WindowsInterceptor : Interceptor {
    var authToken: String? = null
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .header("User-Agent", AppConstants.HEADER_USER_AGENT_VALUE)
            .header("Accept", AppConstants.HEADER_ACCEPT_VALUE)
            .header("Referer", AppConstants.PORTAL_BASE_URL_SLASH)
            .header("Origin", AppConstants.PORTAL_BASE_URL) 
        if (authToken != null) builder.header("Authorization", "Bearer $authToken")
        return chain.proceed(builder.build())
    }
}

class DeepSpyInterceptor : Interceptor {
    private val sensitivePathSegments = listOf("login", "auth", "token")

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!BuildConfig.DEBUG) return chain.proceed(chain.request())

        val request = chain.request()
        val url = request.url.toString()
        val isSensitive = sensitivePathSegments.any { request.url.encodedPath.contains(it, ignoreCase = true) }
        var reqLog = "REQ: ${request.method} $url"
        val reqBody = request.body
        if (reqBody != null && !isSensitive) {
            try {
                val buffer = Buffer()
                reqBody.writeTo(buffer)
                val charset = reqBody.contentType()?.charset(Charset.forName("UTF-8")) ?: Charset.forName("UTF-8")
                if (isPlaintext(buffer)) reqLog += "\nBODY: ${buffer.readString(charset)}"
            } catch (e: Exception) {}
        }
        DebugLogger.log("NET_REQ", reqLog)
        val response = chain.proceed(request)
        var resLog = "RES: ${response.code} $url"
        val resBody = response.body
        if (resBody != null && response.code != 204 && !isSensitive) {
            try {
                val source = resBody.source()
                source.request(Long.MAX_VALUE)
                val buffer = source.buffer.clone()
                val contentType = resBody.contentType()?.toString()
                if (isPlaintext(buffer) && (contentType == null || !contentType.contains("pdf"))) {
                    val charset = resBody.contentType()?.charset(Charset.forName("UTF-8")) ?: Charset.forName("UTF-8")
                    val content = buffer.readString(charset)
                    resLog += if (content.length > 3000) "\nBODY: ${content.take(3000)}... (Truncated)" else "\nBODY: $content"
                }
            } catch (e: Exception) {}
        }
        DebugLogger.log("NET_RES", resLog)
        return response
    }
    private fun isPlaintext(buffer: Buffer): Boolean {
        try {
            val prefix = Buffer()
            val byteCount = if (buffer.size < 64) buffer.size else 64
            buffer.copyTo(prefix, 0, byteCount)
            for (i in 0 until 16) {
                if (prefix.exhausted()) break
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) return false
            }
            return true
        } catch (e: Exception) { return false }
    }
}

class FailoverInterceptor : Interceptor {
    @Volatile private var isBackup = false
    
    private val primaryHost = AppConstants.PRIMARY_API_HOST
    private val backupHost = AppConstants.BACKUP_API_HOST

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        request = rewriteUrl(request, isBackup)

        try {
            val response = chain.proceed(request)
            if (response.code >= 500) {
                response.close()
                throw IOException("Server Error ${response.code} on ${request.url}")
            }
            return response
        } catch (e: IOException) {
            DebugLogger.log("FAILOVER", "Failed on ${request.url}: ${e.message}. Switching host.")
            synchronized(this) { isBackup = !isBackup }
            val retryRequest = rewriteUrl(request, isBackup)
            return chain.proceed(retryRequest)
        }
    }

    private fun rewriteUrl(request: Request, useBackup: Boolean): Request {
        val oldUrl = request.url
        val newBuilder = oldUrl.newBuilder()

        if (useBackup) {
            newBuilder.host(backupHost)
            val segments = oldUrl.pathSegments
            if (segments.isEmpty() || segments[0] != "public") {
                newBuilder.encodedPath("/") // FIX: Ensure path starts with /
                newBuilder.addPathSegment("public")
                segments.forEach { newBuilder.addPathSegment(it) }
            }
        } else {
            newBuilder.host(primaryHost)
            val segments = oldUrl.pathSegments
            if (segments.isNotEmpty() && segments[0] == "public") {
                newBuilder.encodedPath("/") // FIX: Ensure path starts with /
                for (i in 1 until segments.size) { newBuilder.addPathSegment(segments[i]) }
            }
        }
        return request.newBuilder().url(newBuilder.build()).build()
    }
}
