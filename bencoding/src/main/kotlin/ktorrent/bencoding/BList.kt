package ktorrent.bencoding

import java.io.OutputStream

class BList : ArrayList<BEncodable>, BEncodable {

    constructor() : super()

    constructor(collection: Collection<BEncodable>) : super(collection)

    override fun write(outputStream: OutputStream) {
        outputStream.write('l'.toInt())
        forEach { it.write(outputStream) }
        outputStream.write('e'.toInt())
    }
}
