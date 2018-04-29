package ktorrent.protocol

import ktorrent.bencoding.BByteString
import ktorrent.bencoding.BDictionary
import ktorrent.bencoding.BList
import ktorrent.bencoding.BReader
import ktorrent.protocol.file.TorrentStorage
import ktorrent.protocol.info.MetaInfo
import ktorrent.utils.BitArray
import ktorrent.utils.MappingException
import ktorrent.utils.fromHexString
import ktorrent.utils.toHexString
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class Torrent(val infoHash: ByteArray,
              val metaInfo: MetaInfo,
              rootDirectory: Path,
              ignoredFiles: Set<Path> = emptySet(),
              pieceMap: BitArray = BitArray(metaInfo.info.pieces.size)) {

    val storage = TorrentStorage(rootDirectory, metaInfo.info, ignoredFiles, pieceMap)

    fun save() {
        val file = Paths.get("data").resolve(infoHash.toHexString()).toFile()
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        BDictionary(
                "metaInfo" to metaInfo,
                "rootDirectory" to storage.rootDirectory.toUri().toString().let { BByteString(it) },
                "ignoredFiles" to storage.files
                        .filter { it.ignored.value }
                        .map { it.path.toUri().toString().let { BByteString(it) } }
                        .let { BList(it) },
                "pieceMap" to storage.pieceMap
        ).write(file.outputStream())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Torrent

        if (!Arrays.equals(infoHash, other.infoHash)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(infoHash)
    }

    companion object {

        fun load(): List<Torrent> {
            return Paths.get("data").toFile().listFiles { _, name -> name.length == 40 }.flatMap {
                try {
                    listOf(load(it.name))
                } catch (e: Exception) {
                    emptyList<Torrent>()
                }
            }
        }

        fun load(infoHash: String): Torrent {
            val dictionary = (BReader(Paths.get("data", infoHash).toFile().inputStream()).read() as? BDictionary)
                    ?: throw MappingException("Failed to read torrent")

            return Torrent(
                    infoHash = infoHash.fromHexString(),
                    metaInfo = (dictionary["metaInfo"] as? BDictionary)?.let { MetaInfo.read(it) }
                            ?: throw MappingException("Failed to read torrent meta info"),
                    rootDirectory = (dictionary["rootDirectory"] as? BByteString)?.string()?.let { Paths.get(URI(it)) }
                            ?: throw MappingException("Failed to read root directory"),
                    ignoredFiles = (dictionary["ignoredFiles"] as? BList)?.map {
                        (it as? BByteString)?.string()?.let { Paths.get(URI(it)) }
                                ?: throw MappingException("Failed to read ignored file path")
                    }?.toSet() ?: throw MappingException("Failed to read ignored files"),
                    pieceMap = (dictionary["pieceMap"] as? BDictionary)?.let { BitArray.read(it) }
                            ?: throw MappingException("Failed to read piece map")
            )
        }
    }
}
