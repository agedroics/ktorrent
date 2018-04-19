package ktorrent.protocol.utils

import java.net.URLEncoder
import java.nio.ByteBuffer
import java.security.MessageDigest

fun ByteArray.split(chunkSize: Int) = (0 until size / chunkSize).map {
    sliceArray(chunkSize * it until kotlin.math.min(chunkSize * (it + 1), size))
}

fun ByteArray.sha1(): ByteArray = with(MessageDigest.getInstance("SHA-1")) {
    update(this@sha1)
    digest()
}

fun ByteArray.urlEncode(): String = URLEncoder.encode(String(this, Charsets.ISO_8859_1), Charsets.ISO_8859_1)

fun ByteArray.toShort(): Short = ByteBuffer.wrap(this).short

fun ByteArray.toInt(): Int = ByteBuffer.wrap(this).int

fun Short.toByteArray(): ByteArray = ByteBuffer.allocate(2).putShort(this).array()

fun Int.toByteArray(): ByteArray = ByteBuffer.allocate(4).putInt(this).array()
