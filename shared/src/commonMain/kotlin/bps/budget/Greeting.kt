package bps.budget

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.seconds

class Greeting {
    private val platform = getPlatform()
    private val httpClient : TestClient = TestClient()

    fun greet(): Flow<String> = flow {
        emit("Hello, ${platform.name}!")
        delay(1.seconds)
        emit("localhost: ${httpClient.getTestResultLocalhost()}")
        delay(1.seconds)
        emit("internal ip: ${httpClient.getTestResultInternalIp()}")
        delay(1.seconds)
        emit("public ip: ${httpClient.getTestResultPublicIp()}")
    }
}
