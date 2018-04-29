package ktorrent.utils

import java.net.URLEncoder
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.math.ceil
import kotlin.math.min

fun ByteArray.split(chunkSize: Int) = (0 until ceil(size / chunkSize.toFloat()).toInt()).map {
    sliceArray(chunkSize * it until min(chunkSize * (it + 1), size))
}

fun ByteArray.splitArray(chunkSize: Int) = Array(ceil(size / chunkSize.toFloat()).toInt()) {
    sliceArray(chunkSize * it until min(chunkSize * (it + 1), size))
}

fun ByteArray.sha1(): ByteArray = with(MessageDigest.getInstance("SHA-1")) {
    update(this@sha1)
    digest()
}

fun ByteArray.urlEncode(): String = URLEncoder.encode(String(this, Charsets.ISO_8859_1), "ISO-8859-1")

fun ByteArray.toShort(): Short = ByteBuffer.wrap(this).short

fun ByteArray.toInt(): Int = ByteBuffer.wrap(this).int

fun Int.toByteArray(): ByteArray = ByteBuffer.allocate(4).putInt(this).array()

private val hexDigits = "0123456789ABCDEF"

fun ByteArray.toHexString() = CharArray(size * 2) { when (it % 2) {
    0 -> hexDigits[this[it / 2].toInt() ushr 4 and 0xF]
    else -> hexDigits[this[it / 2].toInt() and 0xF]
} }.let { String(it) }

fun String.fromHexString() = ByteArray(length / 2) {
    val fst = this[it * 2].let { if (it in '0'..'9') it - '0' else it - 'A' + 10 }
    val snd = this[it * 2 + 1].let { if (it in '0'..'9') it - '0' else it - 'A' + 10 }
    ((fst shl 4) or snd).toByte()
}
