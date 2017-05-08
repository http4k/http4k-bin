package org.http4k.bin

import org.http4k.server.startJettyServer

fun main(args: Array<String>) {
    HttpBin().startJettyServer(resolvePort(args))
}

private fun resolvePort(args: Array<String>) = if (args.isNotEmpty()) args[0].toInt() else 8000

