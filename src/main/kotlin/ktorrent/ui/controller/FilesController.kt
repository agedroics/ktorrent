package ktorrent.ui.controller

import javafx.fxml.FXML
import javafx.scene.control.TableView
import ktorrent.ui.FormattingCellFactory
import ktorrent.ui.Main
import ktorrent.ui.model.FileModel
import ktorrent.ui.model.TorrentModel
import java.awt.Desktop

class FilesController {

    @FXML
    private var table = TableView<FileModel>()

    val stringCellFactory = MainController.stringCellFactory
    val sizeCellFactory = MainController.sizeCellFactory
    val booleanCellFactory = FormattingCellFactory<FileModel, Boolean> {
        Main.strings.getString(if (it) "yes" else "no")
    }

    fun selectedTorrentProperty() = MainController.selectedTorrentProperty
    fun getSelectedTorrent(): TorrentModel? = MainController.selectedTorrentProperty.get()

    fun selectedFileProperty() = table.selectionModel.selectedItemProperty()
    fun getSelectedFile(): FileModel? = selectedFileProperty().value

    fun onOpen() {
        getSelectedFile()?.let {
            Desktop.getDesktop().open(it.file.absolutePath.toFile())
        }
    }

    fun onOpenContainingFolder() {
        getSelectedFile()?.let {
            Desktop.getDesktop().open(it.file.absolutePath.parent.toFile())
        }
    }

    fun onSkip() {
        getSelectedFile()?.let {
            it.file.ignored.update { !it }
        }
    }
}
