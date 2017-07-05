@file:Suppress("UNUSED_PARAMETER")

package integration.org.http4k.bin

import contract.org.http4k.bin.HttpBinContract
import org.http4k.bin.HttpBin
import org.http4k.client.ApacheClient
import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.server.Jetty
import org.http4k.server.Netty
import org.http4k.server.ServerConfig
import org.http4k.server.SunHttp
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.junit.runner.RunWith
import org.junit.runners.Parameterized


@RunWith(Parameterized::class)
class Http4kImplementationsTest(name: String, client: HttpHandler, serverPort: Int) : HttpBinContract() {
    override val httpBin = DebuggingFilters.PrintRequestAndResponse()
        .then(ClientFilters.SetHostFrom(Uri.of("http://localhost:$serverPort")))
        .then(client)

    companion object {
        val clients = listOf(
            ClientSpec("okHttp", OkHttp()),
            ClientSpec("apache", ApacheClient()))

        val servers = listOf(
            ServerSpec("sun-http", SunHttp(8003), 8003),
            ServerSpec("jetty", Jetty(8000), 8000),
            ServerSpec("netty", Netty(8001), 8001),
            ServerSpec("undertow", Undertow(8002), 8002)
        )

        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun data(): Collection<Array<Any>> {
            servers.forEach { HttpBin().asServer(it.config).start() }
            return clients
                .flatMap { client -> servers.map { server -> client to server } }
                .map { arrayOf("${it.first.name} -> ${it.second.name}", it.first.handler, it.second.port) }
        }
    }

    data class ClientSpec(val name: String, val handler: HttpHandler)
    data class ServerSpec(val name: String, val config: ServerConfig, val port: Int)

}

