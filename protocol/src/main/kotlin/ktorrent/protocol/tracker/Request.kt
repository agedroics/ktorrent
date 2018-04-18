package ktorrent.protocol.tracker

import ktorrent.protocol.urlEncode
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.URLEncoder
import java.util.*

data class Request(val infoHash: ByteArray,
                   val peerId: ByteArray,
                   val port: Int,
                   val uploaded: Long,
                   val downloaded: Long,
                   val left: Long,
                   val compact: Boolean = true,
                   val noPeerId: Boolean = false,
                   val event: EventType = EventType.STARTED,
                   val ip: InetAddress? = null,
                   val numWant: Long? = null,
                   val key: ByteArray? = null,
                   val trackerId: ByteArray? = null) {

    fun send(announce: URL): Response {
        val query = StringBuilder().apply {
            announce.query?.takeIf { it.isNotBlank() }?.let { append(it).append('&') }
            append("info_hash=").append(infoHash.urlEncode())
            append("&peer_id=").append(peerId.urlEncode())
            append("&port=").append(port)
            append("&uploaded=").append(uploaded)
            append("&downloaded=").append(downloaded)
            append("&left=").append(left)
            append("&compact=").append(if (compact) 1 else 0)
            append("&no_peer_id=").append(if (noPeerId) 1 else 0)
            append("&event=").append(event.value)
            ip?.let { append("&ip=").append(URLEncoder.encode(it.hostAddress, Charsets.UTF_8)) }
            numWant?.let { append("&numwant=").append(it) }
            key?.let { append("&key=").append(it.urlEncode()) }
            trackerId?.let { append("&trackerid=").append(it.urlEncode()) }
        }

        val url = StringBuilder().apply {
            append(announce.protocol).append(':')
            announce.authority?.let { append("//").append(it) }
            append(announce.path)
            append('?').append(query)
            announce.ref?.let { append('#').append(it) }
        }.let { URL(it.toString()) }

        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        return try {
            Response.read(BufferedInputStream(conn.inputStream))
        } finally {
            conn.disconnect()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Request

        if (!Arrays.equals(infoHash, other.infoHash)) return false
        if (!Arrays.equals(peerId, other.peerId)) return false
        if (port != other.port) return false
        if (uploaded != other.uploaded) return false
        if (downloaded != other.downloaded) return false
        if (left != other.left) return false
        if (compact != other.compact) return false
        if (noPeerId != other.noPeerId) return false
        if (event != other.event) return false
        if (ip != other.ip) return false
        if (numWant != other.numWant) return false
        if (!Arrays.equals(key, other.key)) return false
        if (!Arrays.equals(trackerId, other.trackerId)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(infoHash)
        result = 31 * result + Arrays.hashCode(peerId)
        result = 31 * result + port
        result = 31 * result + uploaded.hashCode()
        result = 31 * result + downloaded.hashCode()
        result = 31 * result + left.hashCode()
        result = 31 * result + compact.hashCode()
        result = 31 * result + noPeerId.hashCode()
        result = 31 * result + event.hashCode()
        result = 31 * result + (ip?.hashCode() ?: 0)
        result = 31 * result + (numWant?.hashCode() ?: 0)
        result = 31 * result + (key?.let { Arrays.hashCode(it) } ?: 0)
        result = 31 * result + (trackerId?.let { Arrays.hashCode(it) } ?: 0)
        return result
    }
}
