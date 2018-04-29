package ktorrent.protocol.file

import ktorrent.utils.AtomicObservable
import java.nio.file.Path

class TorrentFile(rootDirectory: Path,
                  path: Path,
                  offset: Long,
                  length: Long,
                  completed: Long,
                  ignored: Boolean = false)

    : PhysicalFile(rootDirectory, path, offset, length) {

    val completed = AtomicObservable(completed)
    val ignored = AtomicObservable(ignored)
}
