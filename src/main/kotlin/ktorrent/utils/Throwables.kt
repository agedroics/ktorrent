package ktorrent.utils

tailrec fun Throwable.getRootCause(encountered: Set<Throwable> = setOf(this)): Throwable = when (cause) {
    in encountered -> this
    null -> this
    else -> {
        (cause as Throwable).getRootCause(encountered + this)
    }
}
