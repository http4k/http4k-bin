package integration.org.reekwest.httpbin

import contract.org.reekwest.httpbin.HttpBinContract
import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.client.RedirectStrategy
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.protocol.HttpContext
import org.apache.http.util.EntityUtils
import org.reekwest.http.core.Body
import org.reekwest.http.core.Headers
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import java.net.URI
import java.nio.ByteBuffer

class HttpBinTest : HttpBinContract() {
    val client = ApacheHttpClient2()

    override val httpBin: HttpHandler = { request: Request ->
        client(request.withHttpBinHost())
    }

    private fun Request.withHttpBinHost() = copy(uri = uri.copy(scheme = "http", authority = "httpbin.org"))
}

object NoRedirection : RedirectStrategy {
    override fun getRedirect(request: HttpRequest?, response: HttpResponse?, context: HttpContext?): HttpUriRequest =
        throw IllegalStateException("should never redirect")

    override fun isRedirected(request: HttpRequest?, response: HttpResponse?, context: HttpContext?): Boolean = false
}

class ApacheHttpClient2(val client: CloseableHttpClient = HttpClients.createDefault()) : HttpHandler {

    override fun invoke(request: Request): Response = client.execute(request.toApacheRequest()).toUtterlyIdleResponse()

    private fun CloseableHttpResponse.toUtterlyIdleResponse(): Response =
        Response(statusLine.toTarget(), allHeaders.toTarget(), entity.toTarget())

    private fun Request.toApacheRequest(): HttpRequestBase {
        return object : HttpEntityEnclosingRequestBase() {
            init {
                val request = this@toApacheRequest
                uri = URI(request.uri.toString())
                entity = ByteArrayEntity(request.body.toString().toByteArray())
                request.headers.filter { it.first != "content-length" }.map { addHeader(it.first, it.second) }
                config = RequestConfig.custom()
                    .setRedirectsEnabled(false)
                    .setCookieSpec(CookieSpecs.IGNORE_COOKIES).build()
            }

            override fun getMethod(): String = this@toApacheRequest.method.name
        }
    }

    private fun StatusLine.toTarget() = Status(statusCode, reasonPhrase)

    private fun HttpEntity.toTarget(): Body = ByteBuffer.wrap(EntityUtils.toByteArray(this))

    private fun Array<Header>.toTarget(): Headers = listOf(*this.map { it.name to it.value }.toTypedArray())
}