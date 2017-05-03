package integration.org.reekwest.httpbin

import contract.org.reekwest.httpbin.HttpBinContract
import org.reekwest.http.apache.ApacheHttpClient
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request

class HttpBinTest : HttpBinContract() {
    val client = ApacheHttpClient()

    override val httpBin: HttpHandler = { request: Request ->
        client(request.withHttpBinHost())
    }

    private fun Request.withHttpBinHost() = copy(uri = uri.copy(scheme = "http", authority = "httpbin.org"))
}
