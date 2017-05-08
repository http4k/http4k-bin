package integration.org.http4k.bin

import contract.org.http4k.bin.HttpBinContract
import org.http4k.client.ApacheHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Request

class HttpBinTest : HttpBinContract() {
    val client = ApacheHttpClient()

    override val httpBin: HttpHandler = { request: Request ->
        client(request.withHttpBinHost())
    }

    private fun Request.withHttpBinHost() = uri(uri.copy(scheme = "http", authority = "httpbin.org"))
}
