package ktorrent.ui.component

import javafx.scene.control.Alert
import javafx.scene.control.TextArea
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import ktorrent.utils.getRootCause
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

class ErrorDialog(e: Throwable, strings: ResourceBundle) : Alert(AlertType.ERROR) {

    init {
        val textArea = with(StringWriter()) {
            PrintWriter(this).use { e.printStackTrace(it) }
            toString()
        }.let {
            TextArea(it).apply {
                isEditable = false
                isWrapText = false
                maxWidth = Double.MAX_VALUE
                maxHeight = Double.MAX_VALUE
                GridPane.setVgrow(this, Priority.ALWAYS)
                GridPane.setHgrow(this, Priority.ALWAYS)
            }
        }
        dialogPane.expandableContent = GridPane().apply {
            maxWidth = Double.MAX_VALUE
            add(textArea, 0, 0)
        }
        dialogPane.isExpanded = true
        title = strings.getString("error")
        headerText = strings.getString("errorHasOccurred")
        contentText = e.getRootCause().localizedMessage
    }
}
