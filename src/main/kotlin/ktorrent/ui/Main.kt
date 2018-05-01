package ktorrent.ui

import javafx.application.Application
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.text.Font
import javafx.stage.Stage
import ktorrent.bencoding.InvalidBEncodingException
import ktorrent.ui.component.ErrorDialog
import ktorrent.ui.controller.MainController
import ktorrent.utils.MappingException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

class Main : Application() {

    override fun start(stage: Stage) {
        Main.stage = stage
        Font.loadFont(javaClass.getResourceAsStream("/fonts/fa-solid-900.woff"), 16.0)
        val version = javaClass.getResourceAsStream("/version.txt").reader().use { it.readText() }

        stage.apply {
            onHiding = EventHandler { MainController.torrentsProperty.get().forEach { it.torrent.save() } }
            scene = createScene()
            title = "KTorrent $version"
            show()
        }

        Executors.newSingleThreadScheduledExecutor {
            Thread(it).apply {
                isDaemon = true
            }
        }.scheduleWithFixedDelay(UIUpdater, 1, 1, TimeUnit.SECONDS)
    }

    private fun createScene(): Scene {
        strings = ResourceBundle.getBundle("strings", Locale("en"))
        Thread.setDefaultUncaughtExceptionHandler { _, e -> ErrorDialog(e, strings).showAndWait() }

        val loader = FXMLLoader()
        loader.resources = strings

        loader.setController(MainController)
        val scene = Scene(loader.load(Main::class.java.getResourceAsStream("/views/main.fxml")))
        scene.stylesheets += javaClass.getResource("/css/style.css").toExternalForm()

        scene.onDragOver = EventHandler {
            with(it.dragboard) {
                if (hasFiles() && files.size == 1 && files[0].extension == "torrent") {
                    it.acceptTransferModes(javafx.scene.input.TransferMode.COPY)
                } else {
                    it.consume()
                }
            }
        }

        scene.onDragDropped = EventHandler {
            with(it.dragboard) {
                it.isDropCompleted = if (hasFiles() && files.size == 1 && files[0].extension == "torrent") {
                    try {
                        MainController.processTorrentFile(files[0])
                        true
                    } catch (e: MappingException) {
                        false
                    } catch (e: InvalidBEncodingException) {
                        false
                    }
                } else false
                it.consume()
            }
        }

        return scene
    }

    companion object {

        var stage by Delegates.notNull<Stage>()
        var strings by Delegates.notNull<ResourceBundle>()

        @JvmStatic
        fun main(vararg args: String) {
            Application.launch(Main::class.java, *args)
        }
    }
}
