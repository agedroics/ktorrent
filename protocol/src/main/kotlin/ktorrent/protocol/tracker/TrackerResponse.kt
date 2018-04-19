package ktorrent.protocol.tracker

import ktorrent.bencoding.*
import ktorrent.protocol.MappingException
import ktorrent.protocol.utils.split
import ktorrent.protocol.utils.toShort
import java.io.InputStream
import java.net.Inet4Address

sealed class TrackerResponse {

    companion object {

        fun read(inputStream: InputStream) = (BReader(inputStream).read() as? BDictionary)?.let {
            when {
                it.containsKey("failure reason") -> Failure(
                        reason = (it["failure reason"] as? BByteString)?.string()
                                ?: throw MappingException("Failed to read failure reason")
                )
                else -> Success(
                        warningMessage = (it["waning message"] as? BByteString)?.string(),
                        interval = (it["interval"] as? BInteger)?.value
                                ?: throw MappingException("Failed to read interval"),
                        minInterval = (it["min interval"] as? BInteger)?.value,
                        trackerId = (it["tracker id"] as? BByteString)?.value,
                        seeders = (it["complete"] as? BInteger)?.value,
                        leechers = (it["incomplete"] as? BInteger)?.value,
                        peers = it["peers"]?.let {
                            when (it) {
                                is BList -> it.map {
                                    (it as? BDictionary)?.let { Peer.read(it) }
                                            ?: throw MappingException("Failed to read peer info")
                                }
                                is BByteString -> it.value.split(6).map {
                                    Peer(
                                            ip = Inet4Address.getByAddress(it.sliceArray(0 until 4)),
                                            port = it.sliceArray(4 until 6).toShort()
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

class Failure(val reason: String) : TrackerResponse()

class Success(val warningMessage: String? = null,
              val interval: Long,
              val minInterval: Long? = null,
              val trackerId: ByteArray? = null,
              val seeders: Long? = null,
              val leechers: Long? = null,
              val peers: List<Peer>) : TrackerResponse()
