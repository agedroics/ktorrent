package ktorrent.protocol.file

import ktorrent.utils.AtomicObservable
import java.io.RandomAccessFile
import java.nio.file.Path

class TorrentFile(rootDirectory: Path,
                  val path: Path,
                  val length: Long,
                  ignored: Boolean = false) {

    val ignored = AtomicObservable(ignored)

    val completed = AtomicObservable(0L)

    val file by lazy { RandomAccessFile(rootDirectory.resolve(path).toFile(), "rw") }
}
