package ktorrent.protocol

import java.net.URLEncoder
import java.security.MessageDigest

fun ByteArray.split(chunkSize: Int) = (0 until size / chunkSize).map {
    sliceArray(chunkSize * it until kotlin.math.min(chunkSize * (it + 1), size))
}

fun ByteArray.sha1(): ByteArray {
    val digest = MessageDigest.getInstance("SHA-1")
    digest.update(this)
    return digest.digest()
}

fun ByteArray.urlEncode(): String = URLEncoder.encode(String(this, Charsets.ISO_8859_1), Charsets.ISO_8859_1)
