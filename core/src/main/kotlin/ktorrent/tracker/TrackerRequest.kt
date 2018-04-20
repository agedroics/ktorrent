package ktorrent.tracker

import ktorrent.utils.urlEncode
import java.io.BufferedInputStream
import java.net.InetAddress
import java.net.URL
import java.net.URLEncoder

class TrackerRequest(val infoHash: ByteArray,
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

    fun send(announce: URL): TrackerResponse {
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
            event.takeIf { it != EventType.NOT_SPECIFIED }?.let { append("&event=").append(it.value) }
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

        return url.openStream().use { TrackerResponse.read(BufferedInputStream(it)) }
    }
}
