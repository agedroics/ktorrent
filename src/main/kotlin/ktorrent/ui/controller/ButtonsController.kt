package ktorrent.ui.controller

import javafx.stage.FileChooser
import ktorrent.ui.Main

class ButtonsController {

    fun onAdd() {
        val fileChooser = FileChooser()
        fileChooser.extensionFilters += FileChooser.ExtensionFilter("Torrents", "*.torrent")
        fileChooser.showOpenDialog(Main.stage)?.also {
            MainController.processTorrentFile(it)
        }
    }

    fun onCreate() {

    }

    fun onRemove() = MainController.removeSelectedTorrent()

    fun onStart() {

    }

    fun onStop() {

    }
}
