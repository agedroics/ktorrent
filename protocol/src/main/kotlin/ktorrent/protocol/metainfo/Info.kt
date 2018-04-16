package ktorrent.protocol.metainfo

import ktorrent.bencoding.*
import ktorrent.protocol.MappingException
import ktorrent.protocol.split
import java.io.OutputStream

sealed class Info(val pieceLength: Long,
                  val pieceHashes: List<ByteArray>,
                  val private: Boolean? = null,
                  val original: BDictionary = BDictionary()) : BEncodable {

    protected open fun toBDictionary(): BDictionary {
        val dictionary = BDictionary(original)
        val pieces = ByteArray(pieceHashes.size * 20)
        pieceHashes.forEachIndexed { i, bytes ->
            System.arraycopy(bytes, 0, pieces, i * 20, 20)
        }
        dictionary += mapOf(
                "piece length" to BInteger(pieceLength),
                "pieces" to BByteString(pieces)
        )
        private?.let { dictionary["private"] = BInteger(if (it) 1 else 0) }
        return dictionary
    }

    override fun write(outputStream: OutputStream) {
        toBDictionary().write(outputStream)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Info

        if (pieceLength != other.pieceLength) return false
        if (pieceHashes != other.pieceHashes) return false
        if (private != other.private) return false
        if (original != other.original) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pieceLength.hashCode()
        result = 31 * result + pieceHashes.hashCode()
        result = 31 * result + (private?.hashCode() ?: 0)
        result = 31 * result + original.hashCode()
        return result
    }

    companion object {

        internal fun read(dictionary: BDictionary): Info {
            val pieceLength = (dictionary["piece length"] as? BInteger)?.value
                    ?: throw MappingException("Failed to read piece length")

            val pieceHashes = (dictionary["pieces"] as? BByteString)?.value?.split(20)
                    ?: throw MappingException("Failed to read piece hashes")

            val private = (dictionary["private"] as? BInteger)?.value?.equals(1L)
            val name = (dictionary["name"] as? BByteString)?.string

            return when {
                dictionary.containsKey("files") -> MultiFileInfo(
                        pieceLength = pieceLength,
                        pieceHashes = pieceHashes,
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
                        pieceHashes = pieceHashes,
                        private = private,
                        name = name ?: throw MappingException("Failed to read file name"),
                        length = (dictionary["length"] as? BInteger)?.value
                                ?: throw MappingException("Failed to read file length"),
                        md5Sum = (dictionary["md5sum"] as? BByteString)?.string,
                        original = dictionary
                )
            }
        }
    }
}

class SingleFileInfo(pieceLength: Long,
                     pieceHashes: List<ByteArray>,
                     private: Boolean? = null,
                     val name: String,
                     val length: Long,
                     val md5Sum: String? = null,
                     original: BDictionary = BDictionary())

    : Info(pieceLength, pieceHashes, private, original) {

    override fun toBDictionary(): BDictionary {
        val dictionary = super.toBDictionary()
        dictionary += mapOf(
                "name" to BByteString(name),
                "length" to BInteger(length)
        )
        md5Sum?.let { dictionary["md5sum"] = BByteString(it) }
        return dictionary
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
                    pieceHashes: List<ByteArray>,
                    private: Boolean? = null,
                    val directoryName: String,
                    val files: List<FileInfo>,
                    original: BDictionary = BDictionary())

    : Info(pieceLength, pieceHashes, private, original) {

    override fun toBDictionary(): BDictionary {
        val dictionary = super.toBDictionary()
        dictionary += mapOf(
                "name" to BByteString(directoryName),
                "files" to BList(files)
        )
        return dictionary
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
