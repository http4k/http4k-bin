package org.http4k.bin

import org.http4k.server.Jetty
import org.http4k.server.asServer

fun main(args: Array<String>) {
    HttpBin().asServer(Jetty(resolvePort(args))).start().block()
}

private fun resolvePort(args: Array<String>) = if (args.isNotEmpty()) args[0].toInt() else 8000

