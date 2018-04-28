package ktorrent.protocol

class MappingException : RuntimeException {

    constructor(message: String) : super(message)

    constructor(message: String, throwable: Throwable) : super(message, throwable)

    override fun fillInStackTrace() = this
}
