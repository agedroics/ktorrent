package ktorrent.protocol.file

import ktorrent.protocol.info.Info
import ktorrent.protocol.info.MultiFileInfo
import ktorrent.protocol.info.SingleFileInfo
import ktorrent.utils.AtomicObservable
import ktorrent.utils.BitArray
import ktorrent.utils.EndOfStreamException
import ktorrent.utils.sha1
import java.io.FileNotFoundException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class TorrentStorage(val rootDirectory: Path,
                     info: Info,
                     ignoredFiles: Set<Path> = emptySet(),
                     val pieceMap: BitArray) {

    private val pieceLength = info.pieceLength
    private val pieces = info.pieces

    val length = AtomicObservable(0L)
    val completed = AtomicObservable(0L)

    private val virtualFile = TreeMap<Long, TorrentFile>().apply {
        when (info) {
            is SingleFileInfo -> {
                val file = TorrentFile(
                        rootDirectory = rootDirectory,
                        path = Paths.get(info.name),
                        offset = 0,
                        length = info.length,
                        completed = calculateCompletion(0, info.length)
                )
                length.update { file.length }
                file.completed.observers += { old, new -> completed.update { it + new - old } }
                completed.update { file.completed.value }
                this[0] = file
            }
            is MultiFileInfo -> {
                var offset = 0L
                val directory = rootDirectory.resolve(info.directoryName)
                info.files.forEach {
                    val file = TorrentFile(
                            rootDirectory = directory,
                            path = it.path,
                            offset = offset,
                            length = it.length,
                            completed = calculateCompletion(offset, it.length),
                            ignored = ignoredFiles.contains(it.path)
                    )
                    if (!file.ignored.value) {
                        length.update { it + file.length }
                        file.completed.observers += { old, new -> completed.update { it + new - old } }
                    }
                    completed.update { it + file.completed.value }
                    this[offset] = file
                    offset += file.length
                }
            }
        }
    }.let { VirtualFile(it) }

    @Suppress("UNCHECKED_CAST")
    val files = virtualFile.offsetMap.values as Collection<TorrentFile>

    private fun pieceRange(offset: Long, length: Long)
            = (offset / pieceLength).toInt() until ceil((offset.toFloat() + length) / pieceLength).toInt()

    private fun calculateCompletion(offset: Long, length: Long): Long {
        val pieceRange = pieceRange(offset, length)
        return if (pieceRange.first == pieceRange.last) {
            if (pieceMap[pieceRange.first]) length else 0
        } else {
            pieceRange.filter { pieceMap[it] }.fold(0L) { completed, pieceIndex -> completed + when (pieceIndex) {
                pieceRange.first -> pieceLength - (offset % pieceLength).toInt()
                pieceRange.last -> (offset + length - pieceIndex.toLong() * pieceLength).toInt()
                else -> pieceLength
            } }
        }
    }

    private fun pieceLength(pieceIndex: Int) = when (pieceIndex) {
        pieces.size - 1 -> (virtualFile.length - pieceIndex.toLong() * pieceLength).toInt()
        else -> pieceLength
    }

    fun read(pieceIndex: Int, offset: Int = 0, length: Int = pieceLength(pieceIndex) - offset) =
            virtualFile.read(pieceIndex.toLong() * pieceLength + offset, length)

    fun write(pieceIndex: Int, data: ByteArray) {
        if (pieceMap[pieceIndex]) {
            return
        }
        if (!Arrays.equals(pieces[pieceIndex], data.sha1())) {
            throw HashMismatchException(pieceIndex)
        }
        virtualFile.write(pieceIndex.toLong() * pieceLength, data) { file, bytesWritten ->
            file as TorrentFile
            file.completed.update { it + bytesWritten }
            if (file.completed.value == file.length) {
                file.file.fd.sync()
            }
        }
        pieceMap[pieceIndex] = true
    }

    fun recheck() {
        pieceMap.fillWith(false)
        files.forEach { it.completed.update { 0 } }
        for (file in files) {
            if (file.ignored.value || !file.absolutePath.toFile().exists()) {
                continue
            }
            try {
                pieceRange(file.offset, file.length).forEach {
                    if (pieceMap[it] || Arrays.equals(pieces[it], read(it).sha1())) {
                        val offset = it.toLong() * pieceLength
                        val lengthCoverage = (min(file.offset + file.length, offset + pieceLength) - max(file.offset, offset)).toInt()
                        file.completed.update { it + lengthCoverage }
                        pieceMap[it] = true
                    }
                }
            } catch (e: EndOfStreamException) {
                continue
            } catch (e: FileNotFoundException) {
                continue
            }
        }
    }
}
