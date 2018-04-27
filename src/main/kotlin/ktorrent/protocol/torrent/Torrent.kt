package ktorrent.protocol.torrent

import java.io.InputStream
import java.util.*

data class Torrent(val metaInfo: MetaInfo, val infoHash: ByteArray) {

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

        fun read(inputStream: InputStream) = MetaInfo.read(inputStream)
    }
}
