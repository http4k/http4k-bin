package org.reekwest.httpbin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.reekwest.http.basicauth.BasicAuthServer
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Response.Companion.ok
import org.reekwest.http.core.bodyString
import org.reekwest.http.core.header
import org.reekwest.http.core.then
import org.reekwest.http.routing.by
import org.reekwest.http.routing.path
import org.reekwest.http.routing.routes

fun HttpBin(): HttpHandler = routes(
    Method.GET to "/ip" by { request: Request -> ok().json(request.ipResponse()) },
    Method.GET to "/headers" by { request: Request -> ok().json(request.headerResponse()) },
    Method.GET to "/basic-auth/{user}/{pass}" by { request: Request ->
        val protectedHandler = BasicAuthServer("reekwest-httpbin", request.user(), request.password())
            .then(protectedResource(request.path("user").orEmpty()))
        protectedHandler(request)
    }
)

fun protectedResource(user: String): HttpHandler = { _Request -> ok().json(AuthorizationResponse(user)) }

private fun Request.headerResponse(): HeaderResponse = HeaderResponse(mapOf(*headers.toTypedArray()))

private fun Request.ipResponse() = IpResponse(headerValues("x-forwarded-for").joinToString(", "))

private fun Request.user() = path("user").orEmpty()

private fun Request.password() = path("pass").orEmpty()

data class IpResponse(val origin: String)

data class HeaderResponse(val headers: Map<String, String?>)

data class AuthorizationResponse(val user: String, val authenticated: Boolean = true)

private fun Response.json(value: Any): Response = bodyString(jacksonObjectMapper().writeValueAsString(value))
        .header("content-type", "application/json")
