package ktorrent.protocol.tracker

import ktorrent.bencoding.BByteString
import ktorrent.bencoding.BDictionary
import ktorrent.bencoding.BEncodable
import ktorrent.bencoding.BInteger
import ktorrent.protocol.MappingException
import java.io.OutputStream
import java.net.InetAddress
import java.util.*

data class Peer(val id: ByteArray? = null,
                val ip: InetAddress,
                val port: Int) : BEncodable {

    override fun write(outputStream: OutputStream) = BDictionary(
            "ip" to BByteString(ip.hostAddress),
            "port" to BInteger(port.toLong())
    ).run {
        id?.let { this["peer id"] = BByteString(it) }
        write(outputStream)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Peer

        if (!Arrays.equals(id, other.id)) return false
        if (ip != other.ip) return false
        if (port != other.port) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.let { Arrays.hashCode(it) } ?: 0
        result = 31 * result + ip.hashCode()
        result = 31 * result + port
        return result
    }

    companion object {

        internal fun read(dictionary: BDictionary) = Peer(
                id = (dictionary["peer id"] as? BByteString)?.value,
                ip = (dictionary["ip"] as? BByteString)?.string()?.let { InetAddress.getByName(it) }
                        ?: throw MappingException("Failed to read peer IP address"),
                port = (dictionary["port"] as? BInteger)?.value?.toInt()
                        ?: throw MappingException("Failed to read peer port")
        )
    }
}
