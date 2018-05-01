package ktorrent.ui.controller

import javafx.fxml.FXML
import javafx.scene.control.TableView
import ktorrent.ui.model.TorrentModel
import java.awt.Desktop

class TorrentsController {

    @FXML
    private var table = TableView<TorrentModel>()

    val stringCellFactory = MainController.stringCellFactory
    val sizeCellFactory = MainController.sizeCellFactory

    fun selectedTorrentProperty() = MainController.selectedTorrentProperty
    fun getSelectedTorrent(): TorrentModel? = MainController.selectedTorrentProperty.get()

    fun initialize() {
        table.itemsProperty().bind(MainController.torrentsProperty)
        MainController.selectedTorrentProperty.bind(table.selectionModel.selectedItemProperty())
    }

    fun onRecheck() {
        getSelectedTorrent()?.let {
            val thread = Thread {
                it.torrent.storage.recheck()
            }
            thread.isDaemon = true
            thread.start()
        }
    }

    fun onOpen() {
        getSelectedTorrent()?.let {
            Desktop.getDesktop().open(it.torrent.storage.files.first().absolutePath.toFile())
        }
    }

    fun onOpenContainingFolder() {
        getSelectedTorrent()?.let {
            val directory = it.torrent.storage.rootDirectory.resolve(if (it.multiFile) it.torrent.name else "")
            Desktop.getDesktop().open(directory.toFile())
        }
    }

    fun onRemoveAndDeleteData() {
        getSelectedTorrent()?.apply {
            torrent.storage.files.forEach {
                try {
                    it.absolutePath.toFile().delete()
                } catch (e: Exception) {}
            }
            MainController.removeSelectedTorrent()
        }
    }
}
