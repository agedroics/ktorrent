package ktorrent.ui.component

import javafx.event.EventHandler
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.input.MouseEvent
import javafx.util.Callback

class BetterTableView<T>() : TableView<T>() {

    var rowContextMenu: ContextMenu? = null
    var onRowDoubleClicked: EventHandler<MouseEvent>? = null

    init {
        placeholder = Label()
        rowFactory = Callback {
            object : TableRow<T>() {

                override fun updateItem(item: T?, empty: Boolean) {
                    super.updateItem(item, empty)
                    when (item) {
                        null -> {
                            setContextMenu(null)
                            setOnMouseClicked(null)
                        }
                        else -> {
                            setContextMenu(rowContextMenu)
                            setOnMouseClicked {
                                if (it.clickCount == 2) {
                                    onRowDoubleClicked?.handle(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
