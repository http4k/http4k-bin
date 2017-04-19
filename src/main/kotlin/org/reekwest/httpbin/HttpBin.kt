package org.reekwest.httpbin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.reekwest.http.basicauth.basicAuthProtected
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.body.bodyString
import org.reekwest.http.core.header
import org.reekwest.http.core.headerValues
import org.reekwest.http.core.ok
import org.reekwest.http.routing.by
import org.reekwest.http.routing.path
import org.reekwest.http.routing.routes

fun HttpBin(): HttpHandler = routes(
    Method.GET to "/ip" by { request: Request -> ok().json(request.ipResponse()) },
    Method.GET to "/headers" by { request: Request -> ok().json(request.headerResponse()) },
    Method.GET to "/basic-auth/{user}/{pass}" by { request: Request ->
        val user = request.path("user").orEmpty()
        val pass = request.path("pass").orEmpty()
        protectedResource(user).basicAuthProtected("reekwest-httpbin", user, pass).invoke(request)
    }
)

fun protectedResource(user: String): HttpHandler = { _Request -> ok().json(AuthorizationResponse(user)) }

private fun Request.headerResponse(): HeaderResponse = HeaderResponse(mapOf(*headers.toTypedArray()))

private fun Request.ipResponse() = IpResponse(headerValues("x-forwarded-for").joinToString(", "))

data class IpResponse(val origin: String)

data class HeaderResponse(val headers: Map<String, String?>)

data class AuthorizationResponse(val user: String, val authenticated: Boolean = true)

private fun Response.json(value: Any): Response =
    bodyString(jacksonObjectMapper().writeValueAsString(value))
        .header("content-type", "application/json")
