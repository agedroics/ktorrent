package ktorrent.protocol.tracker

import ktorrent.bencoding.BByteString
import ktorrent.bencoding.BDictionary
import ktorrent.bencoding.BInteger
import ktorrent.utils.MappingException
import java.net.InetAddress
import java.net.InetSocketAddress

class Peer(val id: ByteArray? = null, val address: InetSocketAddress) {

    companion object {

        fun read(dictionary: BDictionary) = Peer(
                id = (dictionary["peer id"] as? BByteString)?.value,
                address = InetSocketAddress(
                        (dictionary["ip"] as? BByteString)?.string()?.let { InetAddress.getByName(it) }
                                ?: throw MappingException("Failed to read peer IP address"),
                        (dictionary["port"] as? BInteger)?.value?.toInt()
                                ?: throw MappingException("Failed to read peer port")
                )
        )
    }
}
