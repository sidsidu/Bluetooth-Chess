package com.example.bluetoothchess.engine

enum class PieceColor { WHITE, BLACK }
enum class PieceType { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }

data class Piece(val color: PieceColor, val type: PieceType)

data class Position(val row: Int, val col: Int) {
    fun isValid() = row in 0..7 && col in 0..7
}

data class Move(
    val start: Position,
    val end: Position,
    val piece: Piece,
    val capturedPiece: Piece? = null,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false,
    var promotion: PieceType? = null
) {
    fun toAlgebraic(): String {
        val fileStart = ('a' + start.col).toString()
        val rankStart = (8 - start.row).toString()
        val fileEnd = ('a' + end.col).toString()
        val rankEnd = (8 - end.row).toString()
        return "$fileStart$rankStart-$fileEnd$rankEnd" + (promotion?.name?.first()?.lowercase() ?: "")
    }

    companion object {
        fun fromAlgebraic(s: String, piece: Piece, captured: Piece?): Move {
            val startCol = s[0] - 'a'
            val startRow = 8 - (s[1] - '0')
            val endCol = s[3] - 'a'
            val endRow = 8 - (s[4] - '0')
            var promo: PieceType? = null
            if (s.length > 5) {
                promo = when(s[5]) {
                    'q' -> PieceType.QUEEN
                    'r' -> PieceType.ROOK
                    'b' -> PieceType.BISHOP
                    'n' -> PieceType.KNIGHT
                    else -> null
                }
            }
            return Move(Position(startRow, startCol), Position(endRow, endCol), piece, captured, promotion = promo)
        }
    }
}
