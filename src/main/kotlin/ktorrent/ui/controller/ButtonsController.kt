package ktorrent.ui.controller

import javafx.stage.FileChooser
import ktorrent.ui.Main

class ButtonsController {

    fun onAdd() = with(FileChooser()) {
        extensionFilters += FileChooser.ExtensionFilter("Torrents", "*.torrent")
        showOpenDialog(Main.stage)?.also {
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
