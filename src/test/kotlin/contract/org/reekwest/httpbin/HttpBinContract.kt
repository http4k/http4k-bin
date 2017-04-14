package contract.org.reekwest.httpbin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.junit.Test
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Response
import org.reekwest.http.core.body.bodyString
import org.reekwest.http.core.get
import org.reekwest.http.core.header
import org.reekwest.httpbin.IpResponse

abstract class HttpBinContract {

    abstract val httpBin: HttpHandler

    @Test
    fun returns_ip_address_using_http_forwarded_for() {
        val response = httpBin(get("/ip").header("x-forwarded-for", "1.2.3.4"))
        val ipResponse = response.bodyObject()
        assertThat(ipResponse.origin, containsSubstring("1.2.3.4"))
    }
}

private fun Response.bodyObject(): IpResponse = jacksonObjectMapper().readValue(bodyString(), IpResponse::class.java)
