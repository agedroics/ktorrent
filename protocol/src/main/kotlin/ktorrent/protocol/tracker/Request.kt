package ktorrent.protocol.tracker

import java.io.BufferedInputStream
import java.net.*
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
                   val trackerId: String? = null) {

    fun send(announce: URL): Response {
        val query = StringBuilder()
        if (!announce.query.isEmpty()) {
            query.append(announce.query).append("&")
        }
        query.append("info_hash=").append(URLEncoder.encode(String(infoHash, Charsets.ISO_8859_1), Charsets.ISO_8859_1))
                .append("&peer_id=").append(URLEncoder.encode(String(peerId, Charsets.ISO_8859_1), Charsets.ISO_8859_1))
                .append("&port=").append(port)
                .append("&uploaded=").append(uploaded)
                .append("&downloaded=").append(downloaded)
                .append("&left=").append(left)
                .append("&compact=").append(if (compact) 1 else 0)
                .append("&no_peer_id=").append(if (noPeerId) 1 else 0)
                .append("&event=").append(event.value)
        ip?.let { query.append("&ip=").append(URLEncoder.encode(it.hostAddress, Charsets.ISO_8859_1)) }
        numWant?.let { query.append("&numwant=").append(it) }
        key?.let { query.append("&key=").append(URLEncoder.encode(String(it, Charsets.ISO_8859_1), Charsets.ISO_8859_1)) }
        trackerId?.let { query.append("&trackerid=").append(URLEncoder.encode(it, Charsets.ISO_8859_1)) }

        val url = URI(
                announce.protocol,
                null,
                announce.host,
                announce.port,
                announce.path,
                query.toString(),
                null
        ).toURL()

        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        try {
            return Response.read(BufferedInputStream(conn.inputStream))
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
        if (trackerId != other.trackerId) return false

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
        result = 31 * result + (trackerId?.hashCode() ?: 0)
        return result
    }
}
