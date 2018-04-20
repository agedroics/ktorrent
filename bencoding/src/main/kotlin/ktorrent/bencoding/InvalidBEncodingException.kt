package ktorrent.bencoding

class InvalidBEncodingException : RuntimeException {

    constructor(message: String) : super(message)

    constructor(throwable: Throwable) : super (throwable)
}
