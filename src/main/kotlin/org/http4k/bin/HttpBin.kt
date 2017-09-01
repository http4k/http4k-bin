package org.http4k.bin

import org.http4k.bin.Responses.authorisationResponse
import org.http4k.bin.Responses.cookieResponse
import org.http4k.bin.Responses.getParametersResponse
import org.http4k.bin.Responses.headerResponse
import org.http4k.bin.Responses.ipResponse
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.http4k.core.cookie.invalidateCookie
import org.http4k.core.queries
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson.auto
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.io.PipedInputStream
import java.io.PipedOutputStream

object Responses {
    val getParametersResponse = Body.auto<GetParametersResponse>().toLens()
    val ipResponse = Body.auto<IpResponse>().toLens()
    val headerResponse = Body.auto<HeaderResponse>().toLens()
    var cookieResponse = Body.auto<CookieResponse>().toLens()
    val authorisationResponse = Body.auto<AuthorisationResponse>().toLens()
}

object HttpBin {
    operator fun invoke() = routes(
        "/ip" bind GET to HttpBin::resolveIp,
        "/get" bind GET to HttpBin::getParameters,
        "/headers" bind GET to HttpBin::headers,
        "/basic-auth/{user}/{pass}" bind GET to HttpBin::protectedResource,
        "/cookies/set" bind GET to HttpBin::setCookies,
        "/cookies/delete" bind GET to HttpBin::removeCookies,
        "/cookies" bind GET to HttpBin::listCookies,
        "/relative-redirect/{times:\\d+}" bind GET to HttpBin::redirectionCountdown,
        "/stream/{times:\\d+}" bind GET to HttpBin::stream
    )

    private fun stream(request: Request): Response {
        val times = request.path("times")?.toInt() ?: 2
        val stream = PipedInputStream()
        val out = PipedOutputStream(stream)
        Thread(Runnable {
            (0 until times).forEach {
                out.write("""{"id": $it} """.toByteArray())
                out.flush()
            }
            out.close()
        }).start()
        return Response(OK).body(Body(stream))
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val stream = PipedInputStream()
        val out = PipedOutputStream(stream)
        Thread(Runnable {
            out.write("abc".toByteArray())
            out.flush()
            out.close()
        }).start()
        println("String() = ${String(stream.readBytes())}")
    }

    private fun resolveIp(request: Request) =
        okWith(ipResponse of IpResponse(request.headerValues("x-forwarded-for").joinToString(", ")))

    private fun getParameters(request: Request) =
        okWith(getParametersResponse of GetParametersResponse(request.uri.queries().map { it.first to it.second.orEmpty() }.toMap()))

    private fun headers(request: Request) = okWith(headerResponse of HeaderResponse(mapOf(*request.headers.toTypedArray())))

    private fun redirectionCountdown(request: Request): Response {
        val counter = request.path("times")?.toInt() ?: 5
        return redirectTo(if (counter > 1) "/relative-redirect/${counter - 1}" else "/get")
    }

    private fun removeCookies(request: Request) = redirectTo("/cookies").with({
        request.uri.queries().fold(it, { response, cookie -> response.invalidateCookie(cookie.first) })
    })

    private fun setCookies(request: Request) = redirectTo("/cookies").with({
        request.uri.queries().fold(it, { response, cookie -> response.cookie(Cookie(cookie.first, cookie.second.orEmpty())) })
    })

    private fun listCookies(request: Request) =
        okWith(cookieResponse of CookieResponse(request.cookies().map { it.name to it.value }.toMap()))

    private fun okWith(injection: (Response) -> Response) = Response(OK).with(injection)

    private fun redirectTo(target: String) = Response(TEMPORARY_REDIRECT).header("location", target)

    private fun protectedResource(request: Request): Response {
        val user = request.path("user").orEmpty()
        val password = request.path("pass").orEmpty()
        val resource = ServerFilters.BasicAuth("http4k-bin", user, password)
            .then({ okWith(authorisationResponse of AuthorisationResponse(user)) })
        return resource.invoke(request)
    }
}

data class StreamResponse(val id: Int)

data class IpResponse(val origin: String)

data class GetParametersResponse(val args: Map<String, String>)

data class HeaderResponse(val headers: Map<String, String?>)

data class AuthorisationResponse(val user: String, val authenticated: Boolean = true)

data class CookieResponse(val cookies: Map<String, String>)