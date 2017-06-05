package org.http4k.bin

import org.http4k.bin.Responses.authorisationResponse
import org.http4k.bin.Responses.cookieResponse
import org.http4k.bin.Responses.getParameters
import org.http4k.bin.Responses.headerResponse
import org.http4k.bin.Responses.ip
import org.http4k.core.Body
import org.http4k.core.HttpHandler
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
import org.http4k.routing.by
import org.http4k.routing.path
import org.http4k.routing.routes

object Responses {
    val getParameters = Body.auto<GetParametersResponse>().toLens()
    val ip = Body.auto<IpResponse>().toLens()
    val headerResponse = Body.auto<HeaderResponse>().toLens()
    var cookieResponse = Body.auto<CookieResponse>().toLens()
    val authorisationResponse = Body.auto<AuthorisationResponse>().toLens()
}

object HttpBin {
    operator fun invoke() = routes(
        "/ip" to GET by { okWith(ip of it.ipResponse()) },
        "/get" to GET by { okWith(getParameters of it.getParametersResponse()) },
        "/headers" to GET by { okWith(headerResponse of it.headerResponse()) },
        "/basic-auth/{user}/{pass}" to GET by { createProtectedResource(it.user(), it.password()).invoke(it) },
        "/cookies/set" to GET by { redirectTo("/cookies").with(storeCookies(it)) },
        "/cookies/delete" to GET by { redirectTo("/cookies").with(deleteCookies(it)) },
        "/cookies" to GET by { okWith(cookieResponse of it.cookieResponse()) },
        "/relative-redirect/{times:\\d+}" to GET by HttpBin::redirectionCountdown
    )

    private fun redirectionCountdown(request: Request): Response {
        val counter = request.path("times")?.toInt() ?: 5
        return redirectTo(if (counter > 1) "/relative-redirect/${counter - 1}" else "/get")
    }

    private fun deleteCookies(request: Request): (Response) -> Response = {
        request.uri.queries().fold(it, { response, cookie -> response.invalidateCookie(cookie.first) })
    }

    private fun storeCookies(request: Request): (Response) -> Response = {
        request.uri.queries().fold(it, { response, cookie -> response.cookie(Cookie(cookie.first, cookie.second.orEmpty())) })
    }

    private fun okWith(injection: (Response) -> Response) = Response(OK).with(injection)

    private fun redirectTo(target: String) = Response(TEMPORARY_REDIRECT).header("location", target)

    private fun createProtectedResource(user: String, password: String) =
        ServerFilters.BasicAuth("http4k-bin", user, password).then(protectedResource(user))

    private fun protectedResource(user: String): HttpHandler = { okWith(authorisationResponse of AuthorisationResponse(user)) }

    private fun Request.headerResponse(): HeaderResponse = HeaderResponse(mapOf(*headers.toTypedArray()))

    private fun Request.ipResponse() = IpResponse(headerValues("x-forwarded-for").joinToString(", "))

    private fun Request.cookieResponse() = CookieResponse(cookies().map { it.name to it.value }.toMap())

    private fun Request.getParametersResponse() = GetParametersResponse(uri.queries().map { it.first to it.second.orEmpty() }.toMap())

    private fun Request.user() = path("user").orEmpty()

    private fun Request.password() = path("pass").orEmpty()
}

data class IpResponse(val origin: String)

data class GetParametersResponse(val args: Map<String, String>)

data class HeaderResponse(val headers: Map<String, String?>)

data class AuthorisationResponse(val user: String, val authenticated: Boolean = true)

data class CookieResponse(val cookies: Map<String, String>)