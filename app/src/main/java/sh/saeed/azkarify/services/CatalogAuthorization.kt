package sh.saeed.azkarify.services

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex

internal fun catalogRequestHash(
    method: String,
    url: String,
    language: String,
    etag: String? = null,
): String {
    val payload = listOf("countries-api:android:v1", method, url, language, etag ?: "").joinToString("\n")
    val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(StandardCharsets.UTF_8))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
}

internal interface CatalogAuthorization {
    suspend fun header(method: String, url: String, language: String, etag: String?): Pair<String, String>
}

internal class LocalCatalogAuthorization(private val token: String) : CatalogAuthorization {
    override suspend fun header(method: String, url: String, language: String, etag: String?) =
        "Authorization" to "Bearer $token"
}

internal class PlayCatalogAuthorization(context: Context, private val cloudProjectNumber: Long) :
    CatalogAuthorization {
    private val manager = IntegrityManagerFactory.createStandard(context.applicationContext)
    private val mutex = Mutex()
    private var tokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider? = null

    override suspend fun header(method: String, url: String, language: String, etag: String?): Pair<String, String> {
        val hash = catalogRequestHash(method, url, language, etag)
        val provider = provider()
        val request = StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
            .setRequestHash(hash)
            .build()
        return try {
            "X-Play-Integrity-Token" to provider.request(request).await().token()
        } catch (error: Exception) {
            mutex.lock()
            try {
                if (tokenProvider === provider) tokenProvider = null
            } finally {
                mutex.unlock()
            }
            throw error
        }
    }

    private suspend fun provider(): StandardIntegrityManager.StandardIntegrityTokenProvider {
        mutex.lock()
        try {
            tokenProvider?.let { return it }
            val request = StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(cloudProjectNumber)
                .build()
            return manager.prepareIntegrityToken(request).await().also { tokenProvider = it }
        } finally {
            mutex.unlock()
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation: CancellableContinuation<T> ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
}
