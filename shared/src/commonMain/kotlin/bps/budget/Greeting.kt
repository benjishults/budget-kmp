package bps.budget

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.seconds

class Greeting {
    private val platform = getPlatform()
    private val httpClient: TestClient = TestClient()

    fun greet(): Flow<String> = flow {
        emit("Hello, ${platform.name}!")
        delay(1.seconds)
        emit(
            try {
                "localhost: ${httpClient.getTestResultLocalhost()}"
            } catch (e: Exception) {
                "error using localhost: ${e.message}"
            },
        )
        delay(1.seconds)
        emit(
            try {
                "internal ip: ${httpClient.getTestResultInternalIp()}"
            } catch (e: Exception) {
                "error using internal ip: ${e.message}"
            },
        )
        delay(1.seconds)
        emit(
            try {
                "public ip: ${httpClient.getTestResultPublicIp()}"
            } catch (e: Exception) {
                "error using public ip: ${e.message}"
            },
        )
    }
}
