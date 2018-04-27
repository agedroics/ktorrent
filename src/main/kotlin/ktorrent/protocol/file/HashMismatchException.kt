package ktorrent.protocol.file

class HashMismatchException(pieceIndex: Int) : RuntimeException("Piece $pieceIndex hash mismatch")
