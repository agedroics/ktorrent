package ktorrent.protocol.file

import ktorrent.protocol.torrent.FileInfo
import ktorrent.protocol.torrent.Info
import ktorrent.protocol.torrent.MultiFileInfo
import ktorrent.protocol.torrent.SingleFileInfo
import ktorrent.utils.AtomicObservable
import ktorrent.utils.BitArray
import ktorrent.utils.sha1
import java.io.File
import java.io.IOException
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

    constructor(rootDirectory: Path, info: Info, pieceMap: BitArray? = null, ignoredFiles: Set<FileInfo> = emptySet()) {
        this.info = info
        when (info) {
            is SingleFileInfo -> {
                val torrentFile = TorrentFile(rootDirectory, Paths.get(info.name), info.length)
                torrentFile.completed.observers += { old, new -> completed.update { it + new - old } }
                files[0] = torrentFile
                totalLength = AtomicObservable(torrentFile.length)
            }
            is MultiFileInfo -> {
                var offset = 0L
                val directory = rootDirectory.resolve(info.directoryName)
                totalLength = AtomicObservable(0)
                info.files.forEach {
                    val torrentFile = TorrentFile(directory, it.path, it.length, ignoredFiles.contains(it))
                    torrentFile.completed.observers += { old, new -> completed.update { it + new - old } }
                    if (!torrentFile.ignored.value) {
                        totalLength.update { it + torrentFile.length }
                    }
                    files[offset] = torrentFile
                    offset += torrentFile.length
                }
            }
        }
        when (pieceMap) {
            null -> {
                this.pieceMap = BitArray(info.pieces.size)
                // recheck()
            }
            else -> this.pieceMap = pieceMap
        }
    }

    constructor(file: File, pieceLength: Int, private: Boolean? = null) {
        val path = file.toPath()
        val torrentFile = TorrentFile(path.parent, path.fileName, file.length())
        files[0] = torrentFile
        totalLength = AtomicObservable(torrentFile.length)
        val pieceCount = ceil(totalLength.value / pieceLength.toFloat()).toInt()
        val pieces = Array(pieceCount) {
            val bytesToRead = when (it) {
                pieceCount - 1 -> (totalLength.value - it * pieceLength.toLong()).toInt()
                else -> pieceLength
            }
            read(it, 0, bytesToRead, pieceLength).sha1()
        }
        info = SingleFileInfo(
                pieceLength = pieceLength,
                pieces = pieces,
                private = private,
                name = path.fileName.toString(),
                length = torrentFile.length
        )
        pieceMap = BitArray(pieceCount, true)
    }

    constructor(rootDirectory: Path,
                files: List<Path>,
                pieceLength: Int,
                private: Boolean? = null,
                directoryName: String) {

        var offset = 0L
        totalLength = AtomicObservable(0)
        files.forEach {
            val length = it.toFile().length()
            val torrentFile = TorrentFile(rootDirectory, it, length)
            this.files[offset] = torrentFile
            totalLength.update { it + torrentFile.length }
            offset += torrentFile.length
        }
        val pieceCount = ceil(totalLength.value / pieceLength.toFloat()).toInt()
        val pieces = Array(pieceCount) {
            val bytesToRead = when (it) {
                pieceCount - 1 -> (totalLength.value - it * pieceLength.toLong()).toInt()
                else -> pieceLength
            }
            read(it, 0, bytesToRead, pieceLength).sha1()
        }
        info = MultiFileInfo(
                pieceLength = pieceLength,
                pieces = pieces,
                private = private,
                directoryName = directoryName,
                files = this.files.values.map { FileInfo(it.length, it.path) }
        )
        pieceMap = BitArray(pieceCount, true)
    }

    fun read(pieceIndex: Int, offset: Int, length: Int, pieceLength: Int = info.pieceLength) = ByteArray(length).also {
        var actualOffset = pieceIndex * pieceLength.toLong() + offset
        var bytesRead = 0
        do {
            val (fileStart, torrentFile) = files.floorEntry(actualOffset)
            val readAmount = min(fileStart + torrentFile.length - actualOffset, length.toLong() - bytesRead).toInt()
            torrentFile.file.apply {
                seek(actualOffset - fileStart)
                read(it, bytesRead, readAmount)
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
            torrentFile.file.apply {
                seek(offset - fileStart)
                write(data, bytesWritten, writeAmount)
                torrentFile.completed.update { it + writeAmount }
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
            } catch (e: IOException) {
                continue
            }
        }
    }
}
