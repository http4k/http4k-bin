package functional.org.http4k.bin

import contract.org.http4k.bin.HttpBinContract
import org.http4k.bin.HttpBin

class HttpBinTest : HttpBinContract() {
    override val httpBin = HttpBin()
}