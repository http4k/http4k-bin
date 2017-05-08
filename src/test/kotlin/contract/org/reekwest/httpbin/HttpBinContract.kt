package contract.org.reekwest.httpbin

import org.http4k.httpbin.AuthorizationResponse
import org.http4k.httpbin.HeaderResponse
import org.http4k.httpbin.IpResponse

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
        val response = BasicAuth("user", "passwd").then(httpBin)(get("/basic-auth/user/passwd"))
        assertThat(response.status, equalTo(Status.OK))
        assertThat(response.authorizationResponse(), equalTo(AuthorizationResponse("user")))
    }

    @Test
    fun supports_cookies() {
        val client = ClientCookies().then(httpBin)
        assertThat(client(get("/cookies")).cookieResponse(), equalTo(CookieResponse(mapOf())))
    }

    @Test
    fun supports_setting_cookies() {
        val client = FollowRedirects().then(ClientCookies()).then(httpBin)
        val response = client(get("/cookies/set").query("foo", "bar"))
        assertThat(response.cookieResponse(), equalTo(CookieResponse(mapOf("foo" to "bar"))))
    }

    @Test
    @Ignore("client does not support cookie invalidation yet")
    fun `delete cookies`(){
        val client = FollowRedirects().then(ClientCookies()).then(httpBin)
        client(get("/cookies/set").query("foo", "bar"))

        val response = client(get("/cookies/delete?foo"))

        assertThat(response.cookieResponse(), equalTo(CookieResponse(mapOf())))
    }
}

private val mapper = jacksonObjectMapper()

private fun Response.bodyObject(): IpResponse = mapper.readValue(bodyString(), IpResponse::class.java)

private fun Response.headersResponse(): HeaderResponse = mapper.readValue(bodyString(), HeaderResponse::class.java)

private fun Response.authorizationResponse(): AuthorizationResponse = mapper.readValue(bodyString(), AuthorizationResponse::class.java)

private fun Response.cookieResponse(): CookieResponse = mapper.readValue(bodyString(), CookieResponse::class.java)

data class CookieResponse(val cookies: Map<String, String>)
