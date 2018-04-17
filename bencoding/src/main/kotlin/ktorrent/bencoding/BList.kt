package ktorrent.bencoding

import java.io.OutputStream

class BList : ArrayList<BEncodable>, BEncodable {

    constructor()

    constructor(vararg items: BEncodable) : super(items.asList())

    constructor(collection: Collection<BEncodable>) : super(collection)

    override fun write(outputStream: OutputStream) = outputStream.run {
        write('l'.toInt())
        forEach { it.write(this) }
        write('e'.toInt())
    }
}
