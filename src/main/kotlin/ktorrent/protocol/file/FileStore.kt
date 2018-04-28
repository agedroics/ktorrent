package ktorrent.protocol.file

import ktorrent.protocol.torrent.FileInfo
import ktorrent.protocol.torrent.Info
import ktorrent.protocol.torrent.MultiFileInfo
import ktorrent.protocol.torrent.SingleFileInfo
import ktorrent.utils.AtomicObservable
import ktorrent.utils.BitArray
import ktorrent.utils.EndOfStreamException
import ktorrent.utils.sha1
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class FileStore {

    private val files: TreeMap<Long, TorrentFile> = TreeMap()

    val info: Info
    val pieceMap: BitArray
    val totalLength: AtomicObservable<Long>
    val completed = AtomicObservable(0L)

    constructor(rootDirectory: Path,
                info: Info,
                pieceMap: BitArray? = null,
                ignoredFiles: Set<FileInfo> = emptySet(),
                vararg progressObservers: (oldValue: Long, newValue: Long) -> Unit) {

        this.info = info
        when (info) {
            is SingleFileInfo -> {
                val torrentFile = TorrentFile(rootDirectory, Paths.get(info.name), 0, info.length)
                torrentFile.completed.observers += { old, new -> completed.update { it + new - old } }
                files[0] = torrentFile
                totalLength = AtomicObservable(torrentFile.length)
            }
            is MultiFileInfo -> {
                var offset = 0L
                val directory = rootDirectory.resolve(info.directoryName)
                totalLength = AtomicObservable(0)
                info.files.forEach {
                    val torrentFile = TorrentFile(directory, it.path, offset, it.length, ignoredFiles.contains(it))
                    torrentFile.completed.observers += { old, new -> completed.update { it + new - old } }
                    if (!torrentFile.ignored.value) {
                        totalLength.update { it + torrentFile.length }
                    }
                    files[offset] = torrentFile
                    offset += torrentFile.length
                }
            }
        }
        completed.observers += progressObservers
        when (pieceMap) {
            null -> {
                this.pieceMap = BitArray(info.pieces.size)
                recheck()
            }
            else -> this.pieceMap = pieceMap
        }
    }

    constructor(file: File,
                pieceLength: Int,
                private: Boolean? = null,
                vararg progressObservers: (oldValue: Long, newValue: Long) -> Unit) {

        val path = file.toPath()
        val torrentFile = TorrentFile(path.parent, path.fileName)
        totalLength = AtomicObservable(torrentFile.length)
        torrentFile.completed.update { torrentFile.length }
        torrentFile.completed.observers += { old, new -> completed.update { it + new - old } }
        files[0] = torrentFile
        completed.observers += progressObservers
        info = SingleFileInfo(
                pieceLength = pieceLength,
                pieces = calculatePieces(pieceLength),
                private = private,
                name = path.fileName.toString(),
                length = torrentFile.length
        )
        pieceMap = BitArray(info.pieces.size, true)
    }

    constructor(rootDirectory: Path,
                files: List<Path>,
                pieceLength: Int,
                private: Boolean? = null,
                directoryName: String,
                vararg progressObservers: (oldValue: Long, newValue: Long) -> Unit) {

        var offset = 0L
        totalLength = AtomicObservable(0)
        files.forEach {
            val torrentFile = TorrentFile(rootDirectory, it, offset)
            totalLength.update { it + torrentFile.length }
            torrentFile.completed.update { torrentFile.length }
            torrentFile.completed.observers += { old, new -> completed.update { it + new - old } }
            this.files[offset] = torrentFile
            offset += torrentFile.length
        }
        completed.observers += progressObservers
        info = MultiFileInfo(
                pieceLength = pieceLength,
                pieces = calculatePieces(pieceLength),
                private = private,
                directoryName = directoryName,
                files = this.files.values.map { FileInfo(it.length, it.path) }
        )
        pieceMap = BitArray(info.pieces.size, true)
    }

    private fun calculatePieces(pieceLength: Int): Array<ByteArray> {
        val pieceCount = ceil(totalLength.value / pieceLength.toFloat()).toInt()
        return Array(pieceCount) {
            val bytesToRead = when (it) {
                pieceCount - 1 -> (totalLength.value - it * pieceLength.toLong()).toInt()
                else -> pieceLength
            }
            val piece = read(it, 0, bytesToRead, pieceLength).sha1()
            completed.update { it + bytesToRead }
            piece
        }
    }

    fun read(pieceIndex: Int, offset: Int, length: Int, pieceLength: Int = info.pieceLength) = ByteArray(length).also {
        var actualOffset = pieceIndex * pieceLength.toLong() + offset
        var bytesRead = 0
        do {
            val (fileStart, torrentFile) = files.floorEntry(actualOffset)
            val readAmount = min(fileStart + torrentFile.length - actualOffset, length.toLong() - bytesRead).toInt()
            torrentFile.file.apply {
                seek(actualOffset - fileStart)
                if (read(it, bytesRead, readAmount) == -1) {
                    throw EndOfStreamException
                }
            }
            bytesRead += readAmount
            actualOffset += readAmount
        } while (bytesRead < length)
    }
    
    fun write(pieceIndex: Int, data: ByteArray) {
        if (pieceMap[pieceIndex]) {
            return
        }
        if (!Arrays.equals(info.pieces[pieceIndex], data.sha1())) {
            throw HashMismatchException(pieceIndex)
        }
        var offset = pieceIndex * info.pieceLength.toLong()
        var bytesWritten = 0
        do {
            val (fileStart, torrentFile) = files.floorEntry(offset)
            val writeAmount = min(fileStart + torrentFile.length - offset, data.size.toLong() - bytesWritten).toInt()
            try {
                torrentFile.file
            } catch (e: FileNotFoundException) {
                torrentFile.absolutePath.parent.toFile().mkdirs()
                torrentFile.file
            }.apply {
                seek(offset - fileStart)
                write(data, bytesWritten, writeAmount)
            }
            torrentFile.completed.update { it + writeAmount }
            if (torrentFile.completed.value == torrentFile.length) {
                torrentFile.file.fd.sync()
            }
            bytesWritten += writeAmount
            offset += writeAmount
        } while (bytesWritten < data.size)
        pieceMap[pieceIndex] = true
    }

    fun recheck() {
        pieceMap.fillWith(false)
        files.values.forEach { it.completed.update { 0 } }
        for ((fileStart, torrentFile) in files.entries) {
            if (torrentFile.ignored.value || !torrentFile.absolutePath.toFile().exists()) {
                continue
            }
            try {
                val fileEnd = fileStart + torrentFile.length
                ((fileStart / info.pieceLength).toInt() until ceil(fileEnd / info.pieceLength.toFloat()).toInt()).forEach {
                    val pieceOffset = it * info.pieceLength.toLong()
                    val pieceLength = when (it) {
                        info.pieces.size - 1 -> files.lastEntry().let { it.key + it.value.length - pieceOffset }.toInt()
                        else -> info.pieceLength
                    }
                    when {
                        pieceMap[it] || Arrays.equals(info.pieces[it], read(it, 0, pieceLength).sha1()) -> {
                            pieceMap[it] = true
                            val lengthCoverage = (min(fileEnd, pieceOffset + pieceLength) - max(fileStart, pieceOffset)).toInt()
                            torrentFile.completed.update { it + lengthCoverage }
                        }
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
