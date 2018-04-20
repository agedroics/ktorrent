package ktorrent.peer

sealed class PeerProtocolException(message: String) : RuntimeException(message)

class IncompatibleProtocolException(protocol: String) : PeerProtocolException("Incompatible protocol $protocol")

class IncorrectLengthException(messageType: String, val length: Int, expected: Int? = null)
    : PeerProtocolException("Incorrect length for message '$messageType' (${expected?.let { "expected: $it, actual: " } ?: ""}$length)")

class PieceMessageTooLongException(val length: Int, maxLength: Int)
    : PeerProtocolException("Message 'piece' too long (max: $maxLength, actual: $length)")

class UnrecognizedMessageException(id: Int, val length: Int)
    : PeerProtocolException("Unrecognized message with id $id of length $length received")
