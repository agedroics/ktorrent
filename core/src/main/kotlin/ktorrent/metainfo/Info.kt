package ktorrent.metainfo

import ktorrent.MappingException
import ktorrent.bencoding.*
import ktorrent.utils.split
import java.io.OutputStream

sealed class Info(val pieceLength: Long,
                  val pieces: List<ByteArray>,
                  val private: Boolean? = null) : BEncodable {

    protected open fun toBDictionary(): BDictionary {
        val pieceArray = ByteArray(pieces.size * 20)
        pieces.forEachIndexed { i, bytes ->
            System.arraycopy(bytes, 0, pieceArray, i * 20, 20)
        }
        val dictionary = BDictionary(
                "piece length" to BInteger(pieceLength),
                "pieces" to BByteString(pieceArray)
        )
        private?.let { dictionary["private"] = BInteger(if (it) 1 else 0) }
        return dictionary
    }

    override fun write(outputStream: OutputStream) = toBDictionary().write(outputStream)

    companion object {

        internal fun read(dictionary: BDictionary): Info {
            val pieceLength = (dictionary["piece length"] as? BInteger)?.value
                    ?: throw MappingException("Failed to read piece length")
            val pieces = (dictionary["pieces"] as? BByteString)?.value?.split(20)
                    ?: throw MappingException("Failed to read piece hashes")
            val private = (dictionary["private"] as? BInteger)?.value?.equals(1L)
            val name = (dictionary["name"] as? BByteString)?.string()

            return when {
                dictionary.containsKey("files") -> MultiFileInfo(
                        pieceLength = pieceLength,
                        pieces = pieces,
                        private = private,
                        directoryName = name ?: throw MappingException("Failed to read directory name"),
                        files = (dictionary["files"] as? BList)?.map {
                            (it as? BDictionary)?.let { FileInfo.read(it) }
                                    ?: throw MappingException("Failed to read file list")
                        } ?: throw MappingException("Failed to read file list")
                )
                else -> SingleFileInfo(
                        pieceLength = pieceLength,
                        pieces = pieces,
                        private = private,
                        name = name ?: throw MappingException("Failed to read file name"),
                        length = (dictionary["length"] as? BInteger)?.value
                                ?: throw MappingException("Failed to read file length"),
                        md5Sum = (dictionary["md5sum"] as? BByteString)?.string()
                )
            }
        }
    }
}

class SingleFileInfo(pieceLength: Long,
                     pieces: List<ByteArray>,
                     private: Boolean? = null,
                     val name: String,
                     val length: Long,
                     val md5Sum: String? = null)

    : Info(pieceLength, pieces, private) {

    override fun toBDictionary() = super.toBDictionary().apply {
        this += mapOf(
                "name" to BByteString(name),
                "length" to BInteger(length)
        )
        md5Sum?.let { this["md5sum"] = BByteString(it) }
    }
}

class MultiFileInfo(pieceLength: Long,
                    pieces: List<ByteArray>,
                    private: Boolean? = null,
                    val directoryName: String,
                    val files: List<FileInfo>)

    : Info(pieceLength, pieces, private) {

    override fun toBDictionary() = super.toBDictionary().apply {
        this += mapOf(
                "name" to BByteString(directoryName),
                "files" to BList(files)
        )
    }
}
