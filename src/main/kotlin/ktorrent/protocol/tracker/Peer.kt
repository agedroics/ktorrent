package ktorrent.protocol.tracker

import ktorrent.bencoding.BByteString
import ktorrent.bencoding.BDictionary
import ktorrent.bencoding.BInteger
import ktorrent.protocol.MappingException
import java.net.InetAddress

class Peer(val id: ByteArray? = null, val ip: InetAddress, val port: Short) {

    companion object {

        internal fun read(dictionary: BDictionary) = Peer(
                id = (dictionary["peer id"] as? BByteString)?.value,
                ip = (dictionary["ip"] as? BByteString)?.string()?.let { InetAddress.getByName(it) }
                        ?: throw MappingException("Failed to read peer IP address"),
                port = (dictionary["port"] as? BInteger)?.value?.toShort()
                        ?: throw MappingException("Failed to read peer port")
        )
    }
}
