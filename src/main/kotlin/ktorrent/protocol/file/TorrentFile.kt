package ktorrent.protocol.file

import ktorrent.utils.AtomicObservable
import java.io.RandomAccessFile
import java.nio.file.Path

class TorrentFile(rootDirectory: Path,
                  val path: Path,
                  val offset: Long = 0,
                  val length: Long = rootDirectory.resolve(path).toFile().length(),
                  ignored: Boolean = false) {

    val ignored = AtomicObservable(ignored)

    val completed = AtomicObservable(0L)

    val absolutePath: Path = rootDirectory.resolve(path)

    val file by lazy { RandomAccessFile(absolutePath.toFile(), "rw") }
}
