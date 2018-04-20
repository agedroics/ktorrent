package ktorrent.metainfo

import ktorrent.MappingException
import ktorrent.bencoding.*
import ktorrent.utils.sha1
import java.io.InputStream
import java.io.OutputStream
import java.net.MalformedURLException
import java.net.URL
import java.time.Instant

class MetaInfo(val info: Info,
               val announce: URL,
               val announceList: List<List<URL>>? = null,
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

        fun read(inputStream: InputStream) = (BReader(inputStream).read() as? BDictionary)?.let {
            val infoDictionary = (it["info"] as? BDictionary) ?: throw MappingException("Failed to read torrent info")
            MetaInfo(
                    info = Info.read(infoDictionary),
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
                    encoding = (it["encoding"] as? BByteString)?.string()
            ) to infoDictionary.encode().sha1()
        } ?: throw MappingException("Failed to read torrent file")
    }
}
