package ktorrent.protocol.torrent

import ktorrent.utils.toHexString
import kotlin.test.Test
import kotlin.test.assertEquals

class TorrentTest {

    @Test
    fun `Reads single-file torrent`() {
        val (_, hash) = Torrent.read(javaClass.classLoader.getResourceAsStream("ubuntu-16.04.4-desktop-amd64.iso.torrent"))
        assertEquals("778CE280B595E57780FF083F2EB6F897DFA4A4EE", hash.toHexString())
    }

    @Test
    fun `Reads multi-file torrent`() {
        val (_, hash) = Torrent.read(javaClass.classLoader.getResourceAsStream("The Onion.torrent"))
        assertEquals("37F308912F8C8A2C70B60303FFB1712B00B195B8", hash.toHexString())
    }
}
