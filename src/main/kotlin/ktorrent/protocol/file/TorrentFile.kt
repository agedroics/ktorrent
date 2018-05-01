package ktorrent.protocol.file

import ktorrent.utils.AtomicObservable
import java.nio.file.Path

class TorrentFile(rootDirectory: Path,
                  path: Path,
                  offset: Long,
                  length: Long,
                  val pieces: Int,
                  done: Long,
                  ignored: Boolean = false)

    : PhysicalFile(rootDirectory, path, offset, length) {

    val done = AtomicObservable(done)
    val ignored = AtomicObservable(ignored)
}
