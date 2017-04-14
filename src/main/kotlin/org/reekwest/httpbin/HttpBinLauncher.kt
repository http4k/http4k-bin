package org.reekwest.httpbin

import org.reekwest.http.jetty.startJettyServer

fun main(args: Array<String>) {
    HttpBin().startJettyServer(resolvePort(args))
}

private fun resolvePort(args: Array<String>) = if (args.isNotEmpty()) args[0].toInt() else 8000

