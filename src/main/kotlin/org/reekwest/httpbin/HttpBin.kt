package org.reekwest.httpbin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.body.bodyString
import org.reekwest.http.core.header
import org.reekwest.http.core.headerValues
import org.reekwest.http.core.ok
import org.reekwest.http.routing.by
import org.reekwest.http.routing.routes

fun HttpBin(): HttpHandler = routes(
    Method.GET to "/ip" by { request: Request -> ok().json(request.ipResponse()) }
)

private fun Response.json(ipResponse: IpResponse): Response =
    bodyString(jacksonObjectMapper().writeValueAsString(ipResponse))
        .header("content-type", "application/json")

private fun Request.ipResponse() = IpResponse(headerValues("x-forwarded-for").joinToString(", "))

data class IpResponse(val origin: String)