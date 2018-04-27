package ktorrent.protocol.file

import ktorrent.protocol.torrent.FileInfo
import ktorrent.protocol.torrent.Info
import ktorrent.protocol.torrent.MultiFileInfo
import ktorrent.protocol.torrent.SingleFileInfo
import ktorrent.utils.BitArray
import ktorrent.utils.sha1
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Path
import java.util.*
import kotlin.math.ceil
import kotlin.math.min

class FileStore : AutoCloseable {

    private val files: TreeMap<Long, RandomAccessFile> = TreeMap()

    val info: Info
    val totalSize: Long
    val bitMap: BitArray

    constructor(rootDirectory: Path, info: Info, bitMap: BitArray? = null) {
        this.info = info
        when (info) {
            is SingleFileInfo -> {
                files[0] = RandomAccessFile(rootDirectory.resolve(info.name).toFile(), "rw")
                totalSize = info.length
            }
            is MultiFileInfo -> {
                var offset = 0L
                val directory = rootDirectory.resolve(info.directoryName)
                info.files.forEach {
                    files[offset] = RandomAccessFile(directory.resolve(it.path).toFile(), "rw")
                    offset += it.length
                }
                totalSize = offset
            }
        }
        when (bitMap) {
            null -> {
                this.bitMap = BitArray(info.pieces.size)
                check()
            }
            else -> this.bitMap = bitMap
        }
    }

    constructor(rootDirectory: Path,
                files: List<File>,
                pieceLength: Int,
                private: Boolean? = null,
                directoryName: String = rootDirectory.fileName.toString()) {

        var offset = 0L
        files.forEach {
            val file = RandomAccessFile(it, "rw")
            this.files[offset] = file
            offset += file.length()
        }
        totalSize = offset
        val pieceCount = ceil(totalSize / pieceLength.toFloat()).toInt()
        bitMap = BitArray(pieceCount)
        (0 until bitMap.size).forEach { bitMap[it] = true }
        val pieces = Array(pieceCount) {
            val bytesToRead = when (it) {
                pieceCount - 1 -> (totalSize - it * pieceLength.toLong()).toInt()
                else -> pieceLength
            }
            read(it, 0, bytesToRead, pieceLength).sha1()
        }
        info = when (files.size) {
            1 -> SingleFileInfo(
                    pieceLength = pieceLength,
                    pieces = pieces,
                    private = private,
                    name = files[0].name,
                    length = totalSize
            )
            else -> MultiFileInfo(
                    pieceLength = pieceLength,
                    pieces = pieces,
                    private = private,
                    directoryName = directoryName,
                    files = files.map { FileInfo(it.length(), rootDirectory.relativize(it.toPath())) }
            )
        }
    }

    fun read(pieceIndex: Int, offset: Int, length: Int, pieceLength: Int = info.pieceLength) = ByteArray(length).also {
        var actualOffset = pieceIndex * pieceLength.toLong() + offset
        var file = files.floorEntry(actualOffset)
        var nextFile = files.higherEntry(actualOffset)
        var bytesRead = 0
        while (bytesRead < length) {
            val readAmount = nextFile?.key?.let {
                min((it - actualOffset).toInt(), length)
            } ?: min((totalSize - actualOffset).toInt(), length)
            file.value.apply {
                seek(actualOffset - file.key)
                read(it, bytesRead, readAmount)
            }
            bytesRead += readAmount
            actualOffset += readAmount
            file = nextFile
            nextFile = nextFile?.key?.let { files.higherEntry(it) }
        }
    }
    
    fun write(pieceIndex: Int, data: ByteArray) {
        if (!Arrays.equals(info.pieces[pieceIndex], data.sha1())) {
            throw HashMismatchException(pieceIndex)
        }
        var offset = pieceIndex * info.pieceLength.toLong()
        val bytesToWrite = min(totalSize - offset, info.pieceLength.toLong()).toInt()
        var file = files.floorEntry(offset)
        var nextFile = files.higherEntry(offset)
        var bytesWritten = 0
        while (bytesWritten < bytesToWrite) {
            val writeAmount = nextFile?.key?.let { min((it - offset).toInt(), bytesToWrite) } ?: bytesToWrite - bytesWritten
            file.value.apply {
                seek(offset - file.key)
                write(data, bytesWritten, writeAmount)
            }
            bytesWritten += writeAmount
            offset += writeAmount
            file = nextFile
            nextFile = nextFile?.key?.let { files.higherEntry(it) }
        }
        bitMap[pieceIndex] = true
    }

    fun check(range: IntRange = 0 until info.pieces.size) = range.forEach {
        bitMap[it] = try {
            val bytesToRead = when (it) {
                info.pieces.size - 1 -> (totalSize - it * info.pieceLength.toLong()).toInt()
                else -> info.pieceLength
            }
            val data = read(it, 0, bytesToRead)
            Arrays.equals(info.pieces[it], data.sha1())
        } catch (e: IOException) {
            false
        }
    }

    override fun close() {
        files.forEach { _, v -> v.close() }
    }
}
