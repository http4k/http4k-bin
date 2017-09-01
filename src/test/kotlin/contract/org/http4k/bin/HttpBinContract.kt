package contract.org.http4k.bin

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.bin.AuthorisationResponse
import org.http4k.bin.CookieResponse
import org.http4k.bin.Responses
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.junit.Ignore
import org.junit.Test

abstract class HttpBinContract {

    abstract val httpBin: HttpHandler

    @Test
    fun returns_ip_address_using_http_forwarded_for() {
        val response = httpBin(Request(GET, "/ip").header("x-forwarded-for", "1.2.3.4"))

        val ipResponse = Responses.ipResponse.extract(response)
        assertThat(ipResponse.origin, containsSubstring("1.2.3.4"))
    }

    @Test
    fun returns_get_parameters(){
        val response = httpBin(Request(GET, "/get").query("foo", "bar"))

        val args = Responses.getParametersResponse.extract(response)
        assertThat(args.args, equalTo(mapOf("foo" to "bar")))
    }

    @Test
    fun relative_redirects() {
        val paths = mutableListOf<String>()
        val client = ClientFilters.FollowRedirects().then(object : Filter {
            override fun invoke(next: HttpHandler): HttpHandler = {
                request ->
                paths.add(request.uri.path)
                next(request)
            }
        }).then(httpBin)

        val response = client(Request(GET, "/relative-redirect/3"))

        assertThat(response.status, equalTo(OK))
        assertThat(paths, equalTo(listOf("/relative-redirect/3", "/relative-redirect/2", "/relative-redirect/1", "/get")))
    }

    @Test
    fun returns_header_list_as_case_insensitive_map() {
        val response = httpBin(Request(GET, "/headers").header("my-header", "my-value"))

        val headerResponse = Responses.headerResponse.extract(response)
        assertThat(headerResponse.headers.entries.find { it.key.equals("My-Header", true) }?.value, equalTo("my-value"))
    }

    @Test
    fun supports_basic_auth() {
        val response = ClientFilters.BasicAuth("user", "passwd").then(httpBin)(Request(GET, "/basic-auth/user/passwd"))

        assertThat(response.status, equalTo(OK))
        assertThat(Responses.authorisationResponse.extract(response), equalTo(AuthorisationResponse("user")))
    }

    @Test
    fun supports_cookies() {
        val client = ClientFilters.Cookies().then(httpBin)

        val response = client(Request(GET, "/cookies"))

        assertThat(Responses.cookieResponse.extract(response), equalTo(CookieResponse(mapOf())))
    }

    @Test
    fun supports_setting_cookies() {
        val client = ClientFilters.FollowRedirects().then(ClientFilters.Cookies()).then(httpBin)

        val response = client(Request(GET, "/cookies/set").query("foo", "bar"))

        val cookies = Responses.cookieResponse.extract(response)
        assertThat(cookies, equalTo(CookieResponse(mapOf("foo" to "bar"))))
    }

    @Test
    fun `delete cookies`(){
        val client = ClientFilters.FollowRedirects().then(ClientFilters.Cookies()).then(httpBin)
        client(Request(GET, "/cookies/set").query("foo", "bar"))

        val response = client(Request(GET, "/cookies/delete?foo"))

        val cookies = Responses.cookieResponse.extract(response)
        assertThat(cookies, equalTo(CookieResponse(mapOf())))
    }
}
