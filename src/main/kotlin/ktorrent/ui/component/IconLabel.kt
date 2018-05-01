package ktorrent.ui.component

import javafx.beans.NamedArg
import javafx.scene.control.Label

class IconLabel(@NamedArg("icon") icon: String, @NamedArg("text") text: String) : Label("$icon $text")
