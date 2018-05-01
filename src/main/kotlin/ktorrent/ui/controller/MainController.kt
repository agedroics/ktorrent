package ktorrent.ui.controller

import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import ktorrent.protocol.Torrent
import ktorrent.protocol.info.MetaInfo
import ktorrent.ui.FormattingCellFactory
import ktorrent.ui.model.FileModel
import ktorrent.ui.model.TorrentModel
import ktorrent.utils.toFormattedFileSize
import java.io.File
import java.net.URI
import java.nio.file.Paths

object MainController {

    val stringCellFactory = FormattingCellFactory<FileModel, Any> { it.toString() }
    val sizeCellFactory = FormattingCellFactory<FileModel, Long> { it.toFormattedFileSize() }

    val torrentsProperty = SimpleObjectProperty(FXCollections.observableArrayList<TorrentModel>())
    val selectedTorrentProperty = SimpleObjectProperty<TorrentModel>()

    fun removeSelectedTorrent() {
        selectedTorrentProperty.get()?.let {
            it.torrent.delete()
            torrentsProperty.get() -= it
        }
    }

    fun processTorrentFile(file: File) {
        val (infoHash, metaInfo) = MetaInfo.read(file.inputStream())
        val torrent = Torrent(infoHash, metaInfo, Paths.get(URI("file:///D:/Downloads")))
        torrentsProperty.get() += TorrentModel(torrent)
    }

    init {
        Torrent.load().forEach {
            torrentsProperty.get() += TorrentModel(it)
        }
    }
}
