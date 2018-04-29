package ktorrent.protocol.file

import java.io.RandomAccessFile
import java.nio.file.Path

open class PhysicalFile(rootDirectory: Path, val path: Path, val offset: Long, val length: Long) {

    val absolutePath: Path = rootDirectory.resolve(path)

    val file by lazy { RandomAccessFile(absolutePath.toFile(), "rw") }
}
