package ktorrent.protocol

import ktorrent.bencoding.*
import ktorrent.protocol.file.TorrentState
import ktorrent.protocol.file.TorrentStorage
import ktorrent.protocol.info.MetaInfo
import ktorrent.protocol.info.MultiFileInfo
import ktorrent.protocol.info.SingleFileInfo
import ktorrent.utils.BitArray
import ktorrent.utils.MappingException
import ktorrent.utils.fromHexString
import ktorrent.utils.toHexString
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class Torrent(val infoHash: ByteArray,
              val metaInfo: MetaInfo,
              rootDirectory: Path,
              ignoredFiles: Set<Path> = emptySet(),
              pieceMap: BitArray = BitArray(metaInfo.info.pieces.size),
              state: TorrentState = TorrentState.STOPPED) {

    val name = metaInfo.info.let { when (it) {
        is SingleFileInfo -> it.name
        is MultiFileInfo -> it.directoryName
    } }

    val storage = TorrentStorage(rootDirectory, metaInfo.info, ignoredFiles, pieceMap, state)

    fun save() {
        val file = Paths.get("data").resolve(infoHash.toHexString()).toFile()
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        val state = when (storage.state.value) {
            TorrentState.CHECKING -> TorrentState.ERROR
            else -> storage.state.value
        }
        BDictionary(
                "metaInfo" to metaInfo,
                "rootDirectory" to storage.rootDirectory.toUri().toString().let { BByteString(it) },
                "ignoredFiles" to storage.files
                        .filter { it.ignored.value }
                        .map { it.path.toUri().toString().let { BByteString(it) } }
                        .let { BList(it) },
                "pieceMap" to storage.pieceMap,
                "state" to BInteger(state.ordinal.toLong())
        ).write(file.outputStream())
    }

    fun delete() {
        with(Paths.get("data").resolve(infoHash.toHexString()).toFile()) {
            if (exists()) {
                delete()
            }
        }
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
            return Paths.get("data").toFile()
                    .listFiles { _, name -> name.matches(Regex("[0-9A-F]{40}")) }
                    ?.flatMap {
                        try {
                            listOf(load(it))
                        } catch (e: MappingException) {
                            emptyList<Torrent>()
                        } catch (e: InvalidBEncodingException) {
                            emptyList<Torrent>()
                        }
                    } ?: emptyList()
        }

        private fun load(file: File): Torrent {
            val dictionary = (file.inputStream().use { BReader(it).read() } as? BDictionary)
                    ?: throw MappingException("Failed to read torrent")

            return Torrent(
                    infoHash = file.name.fromHexString(),
                    metaInfo = (dictionary["metaInfo"] as? BDictionary)?.let { MetaInfo.read(it) }
                            ?: throw MappingException("Failed to read torrent meta info"),
                    rootDirectory = (dictionary["rootDirectory"] as? BByteString)?.string()?.let { Paths.get(URI(it)) }
                            ?: throw MappingException("Failed to read root directory"),
                    ignoredFiles = (dictionary["ignoredFiles"] as? BList)?.map {
                        (it as? BByteString)?.string()?.let {
                            Paths.get("").toAbsolutePath().relativize(Paths.get(URI(it)))
                        } ?: throw MappingException("Failed to read skip file path")
                    }?.toSet() ?: throw MappingException("Failed to read skip files"),
                    pieceMap = (dictionary["pieceMap"] as? BDictionary)?.let { BitArray.read(it) }
                            ?: throw MappingException("Failed to read piece map"),
                    state = (dictionary["state"] as? BInteger)?.let { TorrentState.values()[it.value.toInt()] }
                            ?: throw MappingException("Failed to read torrent state")
            )
        }
    }
}
