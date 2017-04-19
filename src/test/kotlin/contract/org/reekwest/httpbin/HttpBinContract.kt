package contract.org.reekwest.httpbin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.basicauth.basicAuth
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import org.reekwest.http.core.body.bodyString
import org.reekwest.http.core.get
import org.reekwest.http.core.header
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
        val response = httpBin.basicAuth("user", "passwd")(get("/basic-auth/user/passwd"))
        assertThat(response.status, equalTo(Status.OK))
        assertThat(response.authorizationResponse(), equalTo(AuthorizationResponse("user")))
    }
}

private val mapper = jacksonObjectMapper()

private fun Response.bodyObject(): IpResponse = mapper.readValue(bodyString(), IpResponse::class.java)

private fun Response.headersResponse(): HeaderResponse = mapper.readValue(bodyString(), HeaderResponse::class.java)

private fun Response.authorizationResponse(): AuthorizationResponse = mapper.readValue(bodyString(), AuthorizationResponse::class.java)

