package ktorrent.protocol

import java.net.URLEncoder
import java.security.MessageDigest

fun ByteArray.split(chunkSize: Int) = (0 until size / chunkSize).map {
    sliceArray(chunkSize * it until kotlin.math.min(chunkSize * (it + 1), size))
}

fun ByteArray.sha1() = MessageDigest.getInstance("SHA-1").run {
    update(this@sha1)
    digest()
}

fun ByteArray.urlEncode(): String = URLEncoder.encode(String(this, Charsets.ISO_8859_1), Charsets.ISO_8859_1)
