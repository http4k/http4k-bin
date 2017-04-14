package org.reekwest.httpbin

import org.reekwest.http.core.Request
import org.reekwest.http.core.body.bodyString
import org.reekwest.http.core.ok
import org.reekwest.http.jetty.startJettyServer

fun main(args: Array<String>) {
    { _: Request -> ok().bodyString("Hello World") }.startJettyServer()
}

