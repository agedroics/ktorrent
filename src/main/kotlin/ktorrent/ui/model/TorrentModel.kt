package ktorrent.ui.model

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import ktorrent.protocol.Torrent
import ktorrent.protocol.info.MultiFileInfo
import ktorrent.protocol.info.SingleFileInfo
import ktorrent.ui.UIObservable

class TorrentModel(val torrent: Torrent) {

    val name = torrent.metaInfo.info.let {
        when (it) {
            is SingleFileInfo -> it.name
            is MultiFileInfo -> it.directoryName
        }
    }

    private val sizeProperty = UIObservable(torrent.storage.length)
    fun sizeProperty() = sizeProperty
    fun getSize() = sizeProperty.value

    private val doneProperty = UIObservable(torrent.storage.done)
    fun doneProperty() = doneProperty
    fun getDone() = doneProperty.value

    val files: ObservableList<FileModel> = FXCollections.observableList(torrent.storage.files.map { FileModel(it) })

    val multiFile = torrent.metaInfo.info is MultiFileInfo

    private var stateProperty = UIObservable(torrent.storage.state)
}
