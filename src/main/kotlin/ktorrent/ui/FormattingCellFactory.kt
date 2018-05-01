package ktorrent.ui

import javafx.scene.control.Label
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.util.Callback

class FormattingCellFactory<O, T>(private val formatter: (T) -> String)

    : Callback<TableColumn<O, T>, TableCell<O, T>> {

    override fun call(param: TableColumn<O, T>?) = object : TableCell<O, T>() {

        override fun updateItem(item: T?, empty: Boolean) {
            super.updateItem(item, empty)
            graphic = item?.let { Label(formatter(it)) }
        }
    }
}
