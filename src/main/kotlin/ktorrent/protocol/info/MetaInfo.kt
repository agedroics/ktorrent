package ktorrent.protocol.info

import ktorrent.bencoding.*
import ktorrent.utils.MappingException
import ktorrent.utils.sha1
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.net.URISyntaxException
import java.time.Instant

class MetaInfo(val info: Info,
               val announce: URI,
               val announceList: List<List<URI>>? = null,
               val creationDate: Instant? = null,
               val comment: String? = null,
               val createdBy: String? = null,
               val encoding: String? = null) : BEncodable {

    override fun write(outputStream: OutputStream) {
        val dictionary = BDictionary(
                "info" to info,
                "announce" to BByteString(announce.toString())
        )
        announceList?.map { BList(it.map { BByteString(it.toString()) }) }?.let { dictionary["announce-list"] = BList(it) }
        creationDate?.let { dictionary["creation date"] = BInteger(it.epochSecond) }
        comment?.let { dictionary["comment"] = BByteString(it) }
        createdBy?.let { dictionary["created by"] = BByteString(it) }
        encoding?.let { dictionary["encoding"] = BByteString(it) }
        dictionary.write(outputStream)
    }

    companion object {

        fun read(inputStream: InputStream): Pair<ByteArray, MetaInfo> {
            val dictionary = BReader(inputStream).read() as? BDictionary ?: throw MappingException("Failed to read torrent meta info")
            val infoDictionary = (dictionary["info"] as? BDictionary) ?: throw MappingException("Failed to read torrent info")
            val metaInfo = read(dictionary, infoDictionary)
            return infoDictionary.encode().sha1() to metaInfo
        }

        fun read(dictionary: BDictionary,
                 infoDictionary: BDictionary = (dictionary["info"] as? BDictionary)
                         ?: throw MappingException("Failed to read torrent info")) =

                MetaInfo(
                        info = Info.read(infoDictionary),
                        announce = (dictionary["announce"] as? BByteString)?.string()?.let {
                            try {
                                URI(it)
                            } catch (e: URISyntaxException) {
                                throw MappingException("Invalid announce URI", e)
                            }
                        } ?: throw MappingException("Failed to read announce URI"),
                        announceList = (dictionary["announce-list"] as? BList)?.map {
                            (it as? BList)?.map {
                                (it as? BByteString)?.string()?.let {
                                    try {
                                        URI(it)
                                    } catch (e: URISyntaxException) {
                                        throw MappingException("Invalid URI in announce-list", e)
                                    }
                                } ?: throw MappingException("Malformed announce-list")
                            } ?: throw MappingException("Malformed announce-list")
                        },
                        creationDate = (dictionary["creation date"] as? BInteger)?.value?.let { Instant.ofEpochSecond(it) },
                        comment = (dictionary["comment"] as? BByteString)?.string(),
                        createdBy = (dictionary["created by"] as? BByteString)?.string(),
                        encoding = (dictionary["encoding"] as? BByteString)?.string()
                )
    }
}
