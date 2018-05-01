package ktorrent.utils

private val kibiByte = 1024L
private val mebiByte = kibiByte * 1024
private val gibiByte = mebiByte * 1024
private val tebiByte = gibiByte * 1024

fun Long.toFormattedFileSize(): String {
    val bytes = this.toFloat()
    val (amount, unit) = when {
        bytes < kibiByte -> bytes to "B"
        bytes < mebiByte -> bytes / kibiByte to "KiB"
        bytes < gibiByte -> bytes / mebiByte to "MiB"
        bytes < tebiByte -> bytes / gibiByte to "GiB"
        else -> bytes / tebiByte to "TiB"
    }
    val strAmount = when (amount) {
        in 0 until 10 -> String.format("%.2f", amount)
        in 10 until 100 -> String.format("%.1f", amount)
        else -> String.format("%.0f", amount)
    }
    return "$strAmount $unit"
}
