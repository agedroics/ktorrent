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
                     ignoredFiles: Set<Path>,
                     val pieceMap: BitArray,
                     state: TorrentState) {

    private val pieceLength = info.pieceLength
    private val pieces = info.pieces

    val length = AtomicObservable(0L)
    val done = AtomicObservable(0L)
    val state = AtomicObservable(state)

    private val virtualFile = TreeMap<Long, TorrentFile>().apply {
        when (info) {
            is SingleFileInfo -> {
                val file = TorrentFile(
                        rootDirectory = rootDirectory,
                        path = Paths.get(info.name),
                        offset = 0,
                        length = info.length,
                        pieces = pieces.size,
                        done = calculateCompletion(0, info.length)
                )
                length.update { file.length }
                file.done.listeners += { old, new -> done.update { it + new - old } }
                done.update { file.done.value }
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
                            pieces = with(pieceRange(offset, it.length)) { last - first + 1 },
                            done = calculateCompletion(offset, it.length),
                            ignored = ignoredFiles.contains(it.path)
                    )
                    val doneListener = { old: Long, new: Long -> done.update { it + new - old } }
                    if (!file.ignored.value) {
                        length.update { it + file.length }
                        file.done.listeners += doneListener
                    }
                    done.update { it + file.done.value }
                    this[offset] = file
                    offset += file.length
                    file.ignored.listeners += { _, value ->
                        when (value) {
                            true -> {
                                file.done.listeners -= doneListener
                                length.update { it - file.length }
                                done.update { it - file.done.value }
                            }
                            false -> {
                                file.done.listeners += doneListener
                                length.update { it + file.length }
                                done.update { it + file.done.value }
                            }
                        }
                    }
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

    fun read(pieceIndex: Int, offset: Int = 0, length: Int = pieceLength(pieceIndex) - offset) = when (state.value) {
        TorrentState.DOWNLOADING,
        TorrentState.SEEDING,
        TorrentState.CHECKING -> virtualFile.read(pieceIndex.toLong() * pieceLength + offset, length)
        else -> throw IllegalStateException("Attempted to read data while in state ${state.value}")
    }

    fun write(pieceIndex: Int, data: ByteArray) {
        when (state.value) {
            TorrentState.DOWNLOADING -> {
                if (!pieceMap[pieceIndex]) {
                    return
                }
                if (!Arrays.equals(pieces[pieceIndex], data.sha1())) {
                    throw HashMismatchException(pieceIndex)
                }
                virtualFile.write(pieceIndex.toLong() * pieceLength, data) { file, bytesWritten ->
                    file as TorrentFile
                    file.done.update { it + bytesWritten }
                    if (file.done.value == file.length) {
                        file.file.fd.sync()
                    }
                }
                pieceMap[pieceIndex] = true
            }
            else -> throw IllegalStateException("Attempted to write data while in state ${state.value}")
        }
    }

    fun recheck() {
        when (state.value) {
            TorrentState.STOPPED,
            TorrentState.ERROR -> {
                state.update { TorrentState.CHECKING }
                pieceMap.fillWith(false)
                files.forEach { it.done.update { 0 } }
                for (file in files) {
                    if (file.ignored.value || !file.absolutePath.toFile().exists()) {
                        continue
                    }
                    try {
                        pieceRange(file.offset, file.length).forEach {
                            if (pieceMap[it] || Arrays.equals(pieces[it], read(it).sha1())) {
                                val offset = it.toLong() * pieceLength
                                val lengthCoverage = (min(file.offset + file.length, offset + pieceLength) - max(file.offset, offset)).toInt()
                                file.done.update { it + lengthCoverage }
                                pieceMap[it] = true
                            }
                        }
                    } catch (e: EndOfStreamException) {
                        continue
                    } catch (e: FileNotFoundException) {
                        continue
                    }
                }
                state.update { TorrentState.STOPPED }
            }
            TorrentState.CHECKING -> throw IllegalStateException("Attempted to check torrent that is already being checked")
            else -> throw IllegalStateException("Attempting to check torrent while in state ${state.value}")
        }

    }
}
