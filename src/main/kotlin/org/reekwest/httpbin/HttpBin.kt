package org.reekwest.httpbin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Response.Companion.movedTemporarily
import org.reekwest.http.core.Response.Companion.ok
import org.reekwest.http.core.cookie.Cookie
import org.reekwest.http.core.cookie.cookie
import org.reekwest.http.core.cookie.cookies
import org.reekwest.http.core.queries
import org.reekwest.http.core.then
import org.reekwest.http.filters.ServerFilters
import org.reekwest.http.routing.by
import org.reekwest.http.routing.path
import org.reekwest.http.routing.routes

fun HttpBin(): HttpHandler = routes(
    GET to "/ip" by { request: Request -> ok().json(request.ipResponse()) },
    GET to "/headers" by { request: Request -> ok().json(request.headerResponse()) },
    GET to "/basic-auth/{user}/{pass}" by { request: Request ->
        val protectedHandler = ServerFilters.BasicAuth("reekwest-httpbin", request.user(), request.password())
            .then(protectedResource(request.path("user").orEmpty()))
        protectedHandler(request)
    },
    GET to "/cookies/set" by { request ->
        println("setting cookie?")
        request.uri.queries()
            .fold(movedTemporarily(listOf("location" to "/cookies")),
                { response, cookie -> response.cookie(Cookie(cookie.first, cookie.second.orEmpty())) })
    },
    GET to "/cookies/" by { request -> ok().json(request.cookieResponse()) }
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
