package ktorrent.protocol.tracker

import ktorrent.bencoding.*
import ktorrent.protocol.MappingException
import ktorrent.protocol.split
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.nio.ByteBuffer
import java.util.*

sealed class Response : BEncodable {

    protected abstract fun toDictionary(): BDictionary

    override fun write(outputStream: OutputStream) = toDictionary().write(outputStream)

    companion object {

        fun read(inputStream: InputStream) = (BReader(inputStream).read() as? BDictionary)?.let {
            when {
                it.containsKey("failure reason") -> Failure(
                        (it["failure reason"] as? BByteString)?.string()
                                ?: throw MappingException("Failed to read failure reason")
                )
                else -> Success(
                        warningMessage = (it["waning message"] as? BByteString)?.string(),
                        interval = (it["interval"] as? BInteger)?.value
                                ?: throw MappingException("Failed to read interval"),
                        minInterval = (it["min interval"] as? BInteger)?.value,
                        trackerId = (it["tracker id"] as? BByteString)?.value,
                        seeders = (it["complete"] as? BInteger)?.value
                                ?: throw MappingException("Failed to read seeder count"),
                        leechers = (it["incomplete"] as? BInteger)?.value
                                ?: throw MappingException("Failed to read leecher count"),
                        peers = it["peers"]?.let {
                            when (it) {
                                is BList -> it.map {
                                    (it as? BDictionary)?.let { Peer.read(it) }
                                            ?: throw MappingException("Failed to read peer info")
                                }
                                is BByteString -> it.value.split(6).map {
                                    Peer(
                                            ip = Inet4Address.getByAddress(it.sliceArray(0 until 4)),
                                            port = ByteBuffer.wrap(it.sliceArray(4 until 6)).int
                                    )
                                }
                                else -> throw MappingException("Failed to read peer list")
                            }
                        } ?: throw MappingException("Failed to read peer list")
                )
            }
        } ?: throw MappingException("Failed to read tracker response")
    }
}

class Failure(val reason: String) : Response() {

    override fun toDictionary() = BDictionary("failure reason" to BByteString(reason))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Failure

        if (reason != other.reason) return false

        return true
    }

    override fun hashCode(): Int {
        return reason.hashCode()
    }
}

class Success(val warningMessage: String? = null,
              val interval: Long,
              val minInterval: Long? = null,
              val trackerId: ByteArray? = null,
              val seeders: Long,
              val leechers: Long,
              val peers: List<Peer>) : Response() {

    override fun toDictionary() = BDictionary(
            "interval" to BInteger(interval),
            "complete" to BInteger(seeders),
            "incomplete" to BInteger(leechers),
            "peers" to BList(peers)
    ).apply {
        warningMessage?.let { this["warning message"] = BByteString(it) }
        minInterval?.let { this["min interval"] = BInteger(it) }
        trackerId?.let { this["trackerid"] = BByteString(it) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Success

        if (warningMessage != other.warningMessage) return false
        if (interval != other.interval) return false
        if (minInterval != other.minInterval) return false
        if (!Arrays.equals(trackerId, other.trackerId)) return false
        if (seeders != other.seeders) return false
        if (leechers != other.leechers) return false
        if (peers != other.peers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = warningMessage?.hashCode() ?: 0
        result = 31 * result + interval.hashCode()
        result = 31 * result + (minInterval?.hashCode() ?: 0)
        result = 31 * result + (trackerId?.let { Arrays.hashCode(it) } ?: 0)
        result = 31 * result + seeders.hashCode()
        result = 31 * result + leechers.hashCode()
        result = 31 * result + peers.hashCode()
        return result
    }
}
