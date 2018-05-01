package ktorrent.ui.model

import javafx.beans.property.SimpleBooleanProperty
import ktorrent.protocol.file.TorrentFile
import ktorrent.ui.UIObservable

class FileModel(val file: TorrentFile) {

    val path = file.path.toString()
    val size = file.length

    private val doneProperty = UIObservable(file.done)
    fun doneProperty() = doneProperty
    fun getDone() = doneProperty.value

    val pieces = file.pieces

    private val skipProperty = SimpleBooleanProperty(file.ignored.value).apply {
        file.ignored.listeners += { _, value -> this.value = value }
    }
    fun skipProperty() = skipProperty
    fun getSkip() = skipProperty.value
}
