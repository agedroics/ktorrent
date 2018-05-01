package ktorrent.protocol.file

import ktorrent.protocol.info.FileInfo
import ktorrent.protocol.info.Info
import ktorrent.protocol.info.MultiFileInfo
import ktorrent.protocol.info.SingleFileInfo
import ktorrent.utils.AtomicObservable
import ktorrent.utils.EndOfStreamException
import ktorrent.utils.sha1
import java.io.FileNotFoundException
import java.nio.file.Path
import java.util.*
import kotlin.math.ceil
import kotlin.math.min

class VirtualFile(val offsetMap: TreeMap<Long, out PhysicalFile>) {

    val length = offsetMap.values.fold(0L) { length, file ->
        length + file.length
    }

    constructor(rootDirectory: Path, files: List<Path>) : this(TreeMap<Long, PhysicalFile>().apply {
        var offset = 0L
        files.forEach {
            val physicalFile = PhysicalFile(rootDirectory, it, offset, rootDirectory.resolve(it).toFile().length())
            this[offset] = physicalFile
            offset += physicalFile.length
        }
    })

    fun read(offset: Long, length: Int): ByteArray {
        val data = ByteArray(length)
        var bytesRead = 0
        do {
            val currentOffset = offset + bytesRead
            val (fileStart, physicalFile) = offsetMap.floorEntry(currentOffset)
            val readAmount = min(fileStart + physicalFile.length - currentOffset, length.toLong() - bytesRead).toInt()
            physicalFile.file.apply {
                seek(currentOffset - fileStart)
                if (read(data, bytesRead, readAmount) < readAmount) {
                    throw EndOfStreamException
                }
            }
            bytesRead += readAmount
        } while (bytesRead < length)
        return data
    }

    inline fun write(offset: Long,
                     data: ByteArray,
                     fileCallback: (file: PhysicalFile, bytesWritten: Int) -> Unit = { _, _ -> }) {

        var bytesWritten = 0
        do {
            val currentOffset = offset + bytesWritten
            val (fileStart, physicalFile) = offsetMap.floorEntry(currentOffset)
            val writeAmount = min(fileStart + physicalFile.length - currentOffset, data.size.toLong() - bytesWritten).toInt()
            try {
                physicalFile.file
            } catch (e: FileNotFoundException) {
                physicalFile.absolutePath.parent.toFile().mkdirs()
                physicalFile.file
            }.apply {
                seek(currentOffset - fileStart)
                write(data, bytesWritten, writeAmount)
            }
            fileCallback(physicalFile, writeAmount)
            bytesWritten += writeAmount
        } while (bytesWritten < data.size)
    }

    fun generateInfo(pieceLength: Int,
                     directoryName: String,
                     private: Boolean? = null,
                     progress: AtomicObservable<Double>): Info {

        val pieceCount = ceil(length / pieceLength.toFloat()).toInt()
        val pieces = Array(pieceCount) { pieceIndex ->
            val offset = pieceIndex.toLong() * pieceLength
            val bytesToRead = when (pieceIndex) {
                pieceCount - 1 -> (length - offset).toInt()
                else -> pieceLength
            }
            read(offset, bytesToRead).sha1().also {
                progress.update { (pieceIndex.toDouble() + 1) / pieceCount }
            }
        }
        return when (offsetMap.values.size) {
            1 -> SingleFileInfo(
                    pieceLength = pieceLength,
                    pieces = pieces,
                    private = private,
                    name = (offsetMap[0] as PhysicalFile).path.fileName.toString(),
                    length = length
            )
            else -> MultiFileInfo(
                    pieceLength = pieceLength,
                    pieces = pieces,
                    private = private,
                    directoryName = directoryName,
                    files = offsetMap.values.map {
                        FileInfo(it.length, it.path)
                    }
            )
        }
    }
}
