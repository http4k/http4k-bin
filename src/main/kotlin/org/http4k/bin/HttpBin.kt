package org.http4k.bin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Response.Companion.movedTemporarily
import org.http4k.core.Response.Companion.ok
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.http4k.core.cookie.invalidate
import org.http4k.core.cookie.removeCookie
import org.http4k.core.queries
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.by
import org.http4k.routing.path
import org.http4k.routing.routes

fun HttpBin(): HttpHandler = routes(
    GET to "/ip" by { request: Request -> ok().json(request.ipResponse()) },
    GET to "/headers" by { request: Request -> ok().json(request.headerResponse()) },
    GET to "/basic-auth/{user}/{pass}" by { request: Request ->
        val protectedHandler = ServerFilters.BasicAuth("http4k-bin", request.user(), request.password())
            .then(protectedResource(request.path("user").orEmpty()))
        protectedHandler(request)
    },
    GET to "/cookies/set" by { request ->
        request.uri.queries()
            .fold(movedTemporarily(listOf("location" to "/cookies")),
                { response, cookie -> response.cookie(Cookie(cookie.first, cookie.second.orEmpty())) })
    },
    GET to "/cookies/delete" by { request ->
        request.uri.queries()
            .fold(movedTemporarily(listOf("location" to "/cookies")),
                { response, cookie -> response.removeCookie(cookie.first).cookie(Cookie(cookie.first, "").invalidate()) })
    },
    GET to "/cookies" by { request -> ok().json(request.cookieResponse()) }
)

fun protectedResource(user: String): HttpHandler = { ok().json(AuthorizationResponse(user)) }

private fun Request.headerResponse(): HeaderResponse = HeaderResponse(mapOf(*headers.toTypedArray()))

private fun Request.ipResponse() = IpResponse(headerValues("x-forwarded-for").joinToString(", "))

private fun Request.cookieResponse() = CookieResponse(cookies().map { it.name to it.value }.toMap())

private fun Request.user() = path("user").orEmpty()

private fun Request.password() = path("pass").orEmpty()

data class IpResponse(val origin: String)

data class HeaderResponse(val headers: Map<String, String?>)

data class AuthorizationResponse(val user: String, val authenticated: Boolean = true)

data class CookieResponse(val cookies: Map<String, String>)

private fun Response.json(value: Any): Response = body(jacksonObjectMapper().writeValueAsString(value))
        .header("content-type", "application/json")
