package contract.org.http4k.bin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.bin.AuthorizationResponse
import org.http4k.bin.HeaderResponse
import org.http4k.bin.IpResponse
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.junit.Test

abstract class HttpBinContract {

    abstract val httpBin: HttpHandler

    @Test
    fun returns_ip_address_using_http_forwarded_for() {
        val response = httpBin(Request(GET, "/ip").header("x-forwarded-for", "1.2.3.4"))
        val ipResponse = response.bodyObject()
        assertThat(ipResponse.origin, containsSubstring("1.2.3.4"))
    }

    @Test
    fun returns_header_list_as_case_insensitive_map() {
        val response = httpBin(Request(GET, "/headers").header("my-header", "my-value"))
        val headerResponse = response.headersResponse()
        assertThat(headerResponse.headers.entries.find { it.key.equals("My-Header", true) }?.value, equalTo("my-value"))
    }

    @Test
    fun supports_basic_auth() {
        val response = ClientFilters.BasicAuth("user", "passwd").then(httpBin)(Request(GET, "/basic-auth/user/passwd"))
        assertThat(response.status, equalTo(Status.OK))
        assertThat(response.authorizationResponse(), equalTo(AuthorizationResponse("user")))
    }

    @Test
    fun supports_cookies() {
        val client = ClientFilters.Cookies().then(httpBin)
        assertThat(client(Request(GET, "/cookies")).cookieResponse(), equalTo(CookieResponse(mapOf())))
    }

    @Test
    fun supports_setting_cookies() {
        val client = ClientFilters.FollowRedirects().then(ClientFilters.Cookies()).then(httpBin)
        val response = client(Request(GET, "/cookies/set").query("foo", "bar"))
        assertThat(response.cookieResponse(), equalTo(CookieResponse(mapOf("foo" to "bar"))))
    }

    @Test
    fun `delete cookies`(){
        val client = ClientFilters.FollowRedirects().then(ClientFilters.Cookies()).then(httpBin)
        client(Request(GET, "/cookies/set").query("foo", "bar"))

        val response = client(Request(GET, "/cookies/delete?foo"))

        assertThat(response.cookieResponse(), equalTo(CookieResponse(mapOf())))
    }
}

private val mapper = jacksonObjectMapper()

private fun Response.okBody(): String = if (status.successful) this.bodyString() else throw RuntimeException("Server returned $status")

private fun Response.bodyObject(): IpResponse = mapper.readValue(okBody(), IpResponse::class.java)

private fun Response.headersResponse(): HeaderResponse = mapper.readValue(okBody(), HeaderResponse::class.java)

private fun Response.authorizationResponse(): AuthorizationResponse = mapper.readValue(okBody(), AuthorizationResponse::class.java)

private fun Response.cookieResponse(): CookieResponse = mapper.readValue(okBody(), CookieResponse::class.java)

data class CookieResponse(val cookies: Map<String, String>)
