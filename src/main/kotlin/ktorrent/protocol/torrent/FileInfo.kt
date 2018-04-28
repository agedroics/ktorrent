package ktorrent.protocol.torrent

import ktorrent.bencoding.*
import ktorrent.protocol.MappingException
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.Paths

data class FileInfo(val length: Long, val path: Path) : BEncodable {

    override fun write(outputStream: OutputStream) = BDictionary(
            "length" to BInteger(length),
            "path" to BList(path.map { BByteString(it.toString()) })
    ).write(outputStream)

    companion object {

        internal fun read(dictionary: BDictionary) = FileInfo(
                length = (dictionary["length"] as? BInteger)?.value
                        ?: throw MappingException("Failed to read file length"),
                path = (dictionary["path"] as? BList)?.map {
                    (it as? BByteString)?.string()
                            ?: throw MappingException("Failed to read file path")
                }?.toTypedArray()?.let { Paths.get(it[0], *it.sliceArray(1 until it.size)) }
                        ?: throw MappingException("Failed to read file path")
        )
    }
}
