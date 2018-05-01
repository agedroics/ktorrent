package ktorrent.protocol.file

enum class TorrentState {

    STOPPED,
    DOWNLOADING,
    SEEDING,
    CHECKING,
    ERROR
}
