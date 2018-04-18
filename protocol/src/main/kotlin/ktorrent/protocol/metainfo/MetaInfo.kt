package ktorrent.protocol.metainfo

import ktorrent.bencoding.*
import ktorrent.protocol.MappingException
import java.io.InputStream
import java.io.OutputStream
import java.net.MalformedURLException
import java.net.URL
import java.time.Instant

data class MetaInfo(val info: Info,
                    val announce: URL,
                    val announceList: List<List<URL>>? = null,
                    val creationDate: Instant? = null,
                    val comment: String? = null,
                    val createdBy: String? = null,
                    val encoding: String? = null,
                    val original: BDictionary = BDictionary()) : BEncodable {

    override fun write(outputStream: OutputStream) = with(BDictionary(original)) {
        this += mapOf(
            "info" to info,
            "announce" to BByteString(announce.toString())
        )
        announceList?.map { BList(it.map { BByteString(it.toString()) }) }?.let { this["announce-list"] = BList(it) }
        creationDate?.let { this["creation date"] = BInteger(it.epochSecond) }
        comment?.let { this["comment"] = BByteString(it) }
        createdBy?.let { this["created by"] = BByteString(it) }
        encoding?.let { this["encoding"] = BByteString(it) }
        write(outputStream)
    }


    companion object {

        fun read(inputStream: InputStream) = (BReader(inputStream).read() as? BDictionary)?.let {
            MetaInfo(
                    info = (it["info"] as? BDictionary)?.let { Info.read(it) }
                            ?: throw MappingException("Failed to read torrent info"),
                    announce = (it["announce"] as? BByteString)?.string()?.let {
                        try {
                            URL(it)
                        } catch (e: MalformedURLException) {
                            throw MappingException("Invalid announce URL")
                        }
                    } ?: throw MappingException("Failed to read announce URL"),
                    announceList = (it["announce-list"] as? BList)?.map {
                        (it as? BList)?.map {
                            (it as? BByteString)?.string()?.let {
                                try {
                                    URL(it)
                                } catch (e: MalformedURLException) {
                                    throw MappingException("Invalid URL in announce-list")
                                }
                            } ?: throw MappingException("Malformed announce-list")
                        } ?: throw MappingException("Malformed announce-list")
                    },
                    creationDate = (it["creation date"] as? BInteger)?.value?.let { Instant.ofEpochSecond(it) },
                    comment = (it["comment"] as? BByteString)?.string(),
                    createdBy = (it["created by"] as? BByteString)?.string(),
                    encoding = (it["encoding"] as? BByteString)?.string(),
                    original = it
            )
        } ?: throw MappingException("Failed to read torrent meta info")
    }
}
