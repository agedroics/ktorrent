package ktorrent.bencoding

import java.io.OutputStream
import java.util.*

class BDictionary : TreeMap<String, BEncodable>, BEncodable {

    constructor()

    constructor(vararg pairs: Pair<String, BEncodable>) {
        putAll(pairs)
    }

    constructor(map: Map<String, BEncodable>) : super(map)

    override fun write(outputStream: OutputStream) = with(outputStream) {
        write('d'.toInt())
        forEach { k, v -> BByteString(k).write(this); v.write(this) }
        write('e'.toInt())
    }
}
