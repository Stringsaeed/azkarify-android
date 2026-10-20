package sh.saeed.azkarify

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import sh.saeed.azkarify.services.CatalogAuthorization
import sh.saeed.azkarify.services.CountryCatalogClient

class CountryCatalogClientTest {
    @Test
    fun authenticatedCountryPagesUseThePrivateCacheAndRevalidateWithEtag() = runBlocking {
        val requests = mutableListOf<Pair<String, String?>>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/countries.json") { exchange ->
            requests += exchange.requestHeaders.getFirst("Authorization") to
                exchange.requestHeaders.getFirst("If-None-Match")
            if (exchange.requestHeaders.getFirst("If-None-Match") == "\"countries-v1\"") {
                exchange.sendResponseHeaders(304, -1)
            } else {
                val body = """{"data":[{"code":"AE","flag":"🇦🇪","names":{"en":"United Arab Emirates","ar":"الإمارات العربية المتحدة"}}],"meta":{"version":"v1","nextCursor":null}}"""
                val bytes = body.toByteArray()
                exchange.responseHeaders.add("ETag", "\"countries-v1\"")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            exchange.close()
        }
        server.start()
        val cache = Files.createTempDirectory("azkarify-catalog-test").toFile()
        val auth = object : CatalogAuthorization {
            override suspend fun header(method: String, url: String, language: String, etag: String?) =
                "Authorization" to "Bearer local-test-token"
        }
        try {
            val client = CountryCatalogClient(cache, "http://127.0.0.1:${server.address.port}/v1", auth)
            assertEquals("الإمارات العربية المتحدة", client.countries("ar").single().displayName("ar"))
            assertEquals("AE", client.countries("ar").single().code)
            assertEquals(1, requests.size)

            val stored = cache.listFiles()!!.single()
            val stale = JSONObject(stored.readText()).put("storedAt", 0)
            stored.writeText(stale.toString())
            assertEquals("AE", client.countries("ar").single().code)
            assertEquals(listOf("Bearer local-test-token" to null,
                "Bearer local-test-token" to "\"countries-v1\""), requests)
        } finally {
            server.stop(0)
            cache.listFiles()?.forEach { it.delete() }
            cache.delete()
        }
    }
}
