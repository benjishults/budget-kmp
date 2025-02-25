package bps.budget

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class TestClient {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                },
            )
        }
    }

     suspend fun getTestResultLocalhost(): String {
        return httpClient
            .get("http://localhost:8080/test")
            .body()
    }
     suspend fun getTestResultInternalIp(): String {
        return httpClient
            .get("http://192.168.7.235:8080/test")
            .body()
    }
     suspend fun getTestResultPublicIp(): String {
        return httpClient
            .get("http://107.2.69.243:8080/test")
            .body()
    }
}
