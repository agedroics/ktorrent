package ktorrent.metainfo

import ktorrent.MappingException
import ktorrent.bencoding.*
import java.io.OutputStream

class FileInfo(val length: Long, val path: List<String>, val md5Sum: String?) : BEncodable {

    override fun write(outputStream: OutputStream) {
        val dictionary = BDictionary(
                "length" to BInteger(length),
                "path" to BList(path.map { BByteString(it) })
        )
        md5Sum?.let { dictionary["md5sum"] = BByteString(it) }
        dictionary.write(outputStream)
    }

    companion object {

        internal fun read(dictionary: BDictionary) = FileInfo(
                length = (dictionary["length"] as? BInteger)?.value
                        ?: throw MappingException("Failed to read file length"),
                path = (dictionary["path"] as? BList)?.map {
                    (it as? BByteString)?.string()
                            ?: throw MappingException("Failed to read file path")
                } ?: throw MappingException("Failed to read file path"),
                md5Sum = (dictionary["md5sum"] as? BByteString)?.string()
        )
    }
}
