package org.http4k.bin

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
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson.asJsonString
import org.http4k.routing.by
import org.http4k.routing.path
import org.http4k.routing.routes

fun HttpBin(): HttpHandler = routes(
    GET to "/ip" by { request: Request -> Response(OK).json(request.ipResponse()) },
    GET to "/headers" by { request: Request -> Response(OK).json(request.headerResponse()) },
    GET to "/basic-auth/{user}/{pass}" by { request: Request ->
        val protectedHandler = ServerFilters.BasicAuth("http4k-bin", request.user(), request.password())
            .then(protectedResource(request.path("user").orEmpty()))
        protectedHandler(request)
    },
    GET to "/cookies/set" by { request ->
        request.uri.queries()
            .fold(Response(TEMPORARY_REDIRECT).header("location", "/cookies"),
                { response, cookie -> response.cookie(Cookie(cookie.first, cookie.second.orEmpty())) })
    },
    GET to "/cookies/delete" by { request ->
        request.uri.queries()
            .fold(Response(TEMPORARY_REDIRECT).header("location", "/cookies"),
                { response, cookie -> response.invalidateCookie(cookie.first) })
    },
    GET to "/cookies" by { request -> Response(OK).json(request.cookieResponse()) }
)

fun protectedResource(user: String): HttpHandler = { Response(OK).json(AuthorizationResponse(user)) }

private fun Request.headerResponse(): HeaderResponse = HeaderResponse(mapOf(*headers.toTypedArray()))

private fun Request.ipResponse() = IpResponse(headerValues("x-forwarded-for").joinToString(", "))

private fun Request.cookieResponse() = CookieResponse(cookies().map { it.name to it.value }.toMap())

private fun Request.user() = path("user").orEmpty()

private fun Request.password() = path("pass").orEmpty()

data class IpResponse(val origin: String)

data class HeaderResponse(val headers: Map<String, String?>)

data class AuthorizationResponse(val user: String, val authenticated: Boolean = true)

data class CookieResponse(val cookies: Map<String, String>)

private fun Response.json(value: Any): Response = body(value.asJsonString())
    .header("content-type", "application/json")
