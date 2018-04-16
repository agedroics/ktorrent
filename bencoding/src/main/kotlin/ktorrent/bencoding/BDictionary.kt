package ktorrent.bencoding

import java.io.OutputStream
import java.util.*

class BDictionary : TreeMap<String, BEncodable>, BEncodable {

    constructor() : super()

    constructor(map: Map<String, BEncodable>) : super(map)

    override fun write(outputStream: OutputStream) {
        outputStream.write('d'.toInt())
        forEach { k, v -> BByteString(k).write(outputStream); v.write(outputStream) }
        outputStream.write('e'.toInt())
    }
}
