package functional.org.reekwest.httpbin

import contract.org.reekwest.httpbin.HttpBinContract
import org.reekwest.httpbin.HttpBin

class ReekwestHttpBinTest : HttpBinContract() {
    override val httpBin = HttpBin()
}