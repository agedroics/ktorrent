package ktorrent.protocol.file

enum class TorrentState {

    INACTIVE,
    LEECHING,
    SEEDING,
    CHECKING,
    ERROR
}
