package ktorrent.protocol.metainfo

import ktorrent.bencoding.*
import ktorrent.protocol.MappingException
import ktorrent.protocol.split
import java.io.OutputStream

sealed class Info(val pieceLength: Long,
                  val pieces: List<ByteArray>,
                  val private: Boolean? = null,
                  val original: BDictionary = BDictionary()) : BEncodable {

    protected open fun toBDictionary() = BDictionary(original).apply {
        val pieceArray = ByteArray(pieces.size * 20)
        pieces.forEachIndexed { i, bytes ->
            System.arraycopy(bytes, 0, pieceArray, i * 20, 20)
        }
        this += mapOf(
                "piece length" to BInteger(pieceLength),
                "pieces" to BByteString(pieceArray)
        )
        private?.let { this["private"] = BInteger(if (it) 1 else 0) }
    }

    override fun write(outputStream: OutputStream) = toBDictionary().write(outputStream)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Info

        if (pieceLength != other.pieceLength) return false
        if (pieces != other.pieces) return false
        if (private != other.private) return false
        if (original != other.original) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pieceLength.hashCode()
        result = 31 * result + pieces.hashCode()
        result = 31 * result + (private?.hashCode() ?: 0)
        result = 31 * result + original.hashCode()
        return result
    }

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
                        } ?: throw MappingException("Failed to read file list"),
                        original = dictionary
                )
                else -> SingleFileInfo(
                        pieceLength = pieceLength,
                        pieces = pieces,
                        private = private,
                        name = name ?: throw MappingException("Failed to read file name"),
                        length = (dictionary["length"] as? BInteger)?.value
                                ?: throw MappingException("Failed to read file length"),
                        md5Sum = (dictionary["md5sum"] as? BByteString)?.string(),
                        original = dictionary
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
                     val md5Sum: String? = null,
                     original: BDictionary = BDictionary())

    : Info(pieceLength, pieces, private, original) {

    override fun toBDictionary() = super.toBDictionary().apply {
        this += mapOf(
                "name" to BByteString(name),
                "length" to BInteger(length)
        )
        md5Sum?.let { this["md5sum"] = BByteString(it) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as SingleFileInfo

        if (name != other.name) return false
        if (length != other.length) return false
        if (md5Sum != other.md5Sum) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + length.hashCode()
        result = 31 * result + (md5Sum?.hashCode() ?: 0)
        return result
    }
}

class MultiFileInfo(pieceLength: Long,
                    pieces: List<ByteArray>,
                    private: Boolean? = null,
                    val directoryName: String,
                    val files: List<FileInfo>,
                    original: BDictionary = BDictionary())

    : Info(pieceLength, pieces, private, original) {

    override fun toBDictionary() = super.toBDictionary().apply {
        this += mapOf(
                "name" to BByteString(directoryName),
                "files" to BList(files)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as MultiFileInfo

        if (directoryName != other.directoryName) return false
        if (files != other.files) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + directoryName.hashCode()
        result = 31 * result + files.hashCode()
        return result
    }
}
