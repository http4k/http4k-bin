package contract.org.reekwest.httpbin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.junit.Ignore
import org.junit.Test
import org.reekwest.http.basicauth.BasicAuthClient
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import org.reekwest.http.core.cookie.ClientCookies
import org.reekwest.http.core.header
import org.reekwest.http.core.then
import org.reekwest.http.filters.ClientFilters.FollowRedirects
import org.reekwest.httpbin.AuthorizationResponse
import org.reekwest.httpbin.HeaderResponse
import org.reekwest.httpbin.IpResponse

abstract class HttpBinContract {

    abstract val httpBin: HttpHandler

    @Test
    fun returns_ip_address_using_http_forwarded_for() {
        val response = httpBin(get("/ip").header("x-forwarded-for", "1.2.3.4"))
        val ipResponse = response.bodyObject()
        assertThat(ipResponse.origin, containsSubstring("1.2.3.4"))
    }

    @Test
    fun returns_header_list_as_case_insensitive_map() {
        val response = httpBin(get("/headers").header("my-header", "my-value"))
        val headerResponse = response.headersResponse()
        assertThat(headerResponse.headers.entries.find { it.key.equals("My-Header", true) }?.value, equalTo("my-value"))
    }

    @Test
    fun supports_basic_auth() {
        val response = BasicAuthClient("user", "passwd").then(httpBin)(get("/basic-auth/user/passwd"))
        assertThat(response.status, equalTo(Status.OK))
        assertThat(response.authorizationResponse(), equalTo(AuthorizationResponse("user")))
    }

    @Test
    @Ignore("not implemented in reekwest-httpbin yet")
    fun supports_cookies() {
        val client = ClientCookies().then(httpBin)
        assertThat(client(get("/cookies")).cookieResponse(), equalTo(CookieResponse(mapOf())))
    }

    @Test
    @Ignore("not implemented in reekwest-httpbin yet")
    fun supports_setting_cookies() {
        val client = FollowRedirects().then(ClientCookies()).then(httpBin)
        val response = client(get("/cookies/set").query("foo", "bar"))
        println("response.bodyString() = ${response.bodyString()}")
        assertThat(response.cookieResponse(), equalTo(CookieResponse(mapOf("foo" to "bar"))))
    }
}

private val mapper = jacksonObjectMapper()

private fun Response.bodyObject(): IpResponse = mapper.readValue(bodyString(), IpResponse::class.java)

private fun Response.headersResponse(): HeaderResponse = mapper.readValue(bodyString(), HeaderResponse::class.java)

private fun Response.authorizationResponse(): AuthorizationResponse = mapper.readValue(bodyString(), AuthorizationResponse::class.java)

private fun Response.cookieResponse(): CookieResponse = mapper.readValue(bodyString(), CookieResponse::class.java)

data class CookieResponse(val cookies: Map<String, String>)
