package integration.org.reekwest.httpbin

import contract.org.reekwest.httpbin.HttpBinContract

class HttpBinTest : HttpBinContract() {
    val client = ApacheHttpClient()

    override val httpBin: HttpHandler = { request: Request ->
        client(request.withHttpBinHost())
    }

    private fun Request.withHttpBinHost() = copy(uri = uri.copy(scheme = "http", authority = "httpbin.org"))
}
