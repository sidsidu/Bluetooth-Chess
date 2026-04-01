package com.example.bluetoothchess.engine

import kotlin.math.abs

class ChessEngine {
    var board: Array<Array<Piece?>> = Array(8) { arrayOfNulls(8) }
    var turn: PieceColor = PieceColor.WHITE
    
    // Castling rights
    var whiteKingMoved = false
    var whiteRookQueensideMoved = false
    var whiteRookKingsideMoved = false
    var blackKingMoved = false
    var blackRookQueensideMoved = false
    var blackRookKingsideMoved = false

    var lastMove: Move? = null
    val moveHistory = mutableListOf<Move>()
    val capturedPieces = mutableListOf<Piece>()

    init {
        resetBoard()
    }

    private fun resetBoard() {
        for (i in 0..7) {
            board[1][i] = Piece(PieceColor.BLACK, PieceType.PAWN)
            board[6][i] = Piece(PieceColor.WHITE, PieceType.PAWN)
        }
        val backRow = arrayOf(PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN, PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK)
        for (i in 0..7) {
            board[0][i] = Piece(PieceColor.BLACK, backRow[i])
            board[7][i] = Piece(PieceColor.WHITE, backRow[i])
        }
        for (r in 2..5) {
            for (c in 0..7) {
                board[r][c] = null
            }
        }
        turn = PieceColor.WHITE
        whiteKingMoved = false
        whiteRookQueensideMoved = false
        whiteRookKingsideMoved = false
        blackKingMoved = false
        blackRookQueensideMoved = false
        blackRookKingsideMoved = false
        lastMove = null
        moveHistory.clear()
        capturedPieces.clear()
    }

    fun pieceAt(pos: Position): Piece? = if (pos.isValid()) board[pos.row][pos.col] else null

    fun getValidMoves(pos: Position): List<Move> {
        val piece = pieceAt(pos) ?: return emptyList()
        if (piece.color != turn) return emptyList()

        val rawMoves = generatePseudoLegalMoves(pos, piece)
        return rawMoves.filter { move ->
            !wouldBeInCheckAfter(move)
        }
    }

    private fun generatePseudoLegalMoves(pos: Position, piece: Piece): List<Move> {
        val moves = mutableListOf<Move>()
        val directions = mutableListOf<Pair<Int, Int>>()
        
        when (piece.type) {
            PieceType.PAWN -> {
                val dir = if (piece.color == PieceColor.WHITE) -1 else 1
                val startRow = if (piece.color == PieceColor.WHITE) 6 else 1
                
                // One step forward
                var nextPos = Position(pos.row + dir, pos.col)
                if (nextPos.isValid() && pieceAt(nextPos) == null) {
                    moves.add(Move(pos, nextPos, piece))
                    // Two steps forward
                    if (pos.row == startRow) {
                        val doublePos = Position(pos.row + 2 * dir, pos.col)
                        if (pieceAt(doublePos) == null) {
                            moves.add(Move(pos, doublePos, piece))
                        }
                    }
                }
                
                // Captures
                for (cDir in listOf(-1, 1)) {
                    val capPos = Position(pos.row + dir, pos.col + cDir)
                    if (capPos.isValid()) {
                        val target = pieceAt(capPos)
                        if (target != null && target.color != piece.color) {
                            moves.add(Move(pos, capPos, piece, target))
                        } else {
                            // En Passant
                            lastMove?.let { lm ->
                                if (lm.piece.type == PieceType.PAWN && 
                                    abs(lm.start.row - lm.end.row) == 2 && 
                                    lm.end.row == pos.row && lm.end.col == capPos.col) {
                                    moves.add(Move(pos, capPos, piece, lm.piece, isEnPassant = true))
                                }
                            }
                        }
                    }
                }
            }
            PieceType.KNIGHT -> {
                val knightJumps = listOf(
                    -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                    1 to -2, 1 to 2, 2 to -1, 2 to 1
                )
                for (jmp in knightJumps) {
                    val next = Position(pos.row + jmp.first, pos.col + jmp.second)
                    if (next.isValid()) {
                        val target = pieceAt(next)
                        if (target == null || target.color != piece.color) {
                            moves.add(Move(pos, next, piece, target))
                        }
                    }
                }
            }
            PieceType.BISHOP -> {
                directions.addAll(listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1))
            }
            PieceType.ROOK -> {
                directions.addAll(listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1))
            }
            PieceType.QUEEN -> {
                directions.addAll(listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1, -1 to 0, 1 to 0, 0 to -1, 0 to 1))
            }
            PieceType.KING -> {
                val kingDirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1, -1 to 0, 1 to 0, 0 to -1, 0 to 1)
                for (dir in kingDirs) {
                    val next = Position(pos.row + dir.first, pos.col + dir.second)
                    if (next.isValid()) {
                        val target = pieceAt(next)
                        if (target == null || target.color != piece.color) {
                            moves.add(Move(pos, next, piece, target))
                        }
                    }
                }
                
                // Castling
                if (piece.color == PieceColor.WHITE && !whiteKingMoved) {
                    if (!whiteRookKingsideMoved && pieceAt(Position(7, 5)) == null && pieceAt(Position(7, 6)) == null) {
                        moves.add(Move(pos, Position(7, 6), piece, isCastling = true))
                    }
                    if (!whiteRookQueensideMoved && pieceAt(Position(7, 1)) == null && pieceAt(Position(7, 2)) == null && pieceAt(Position(7, 3)) == null) {
                        moves.add(Move(pos, Position(7, 2), piece, isCastling = true))
                    }
                } else if (piece.color == PieceColor.BLACK && !blackKingMoved) {
                    if (!blackRookKingsideMoved && pieceAt(Position(0, 5)) == null && pieceAt(Position(0, 6)) == null) {
                        moves.add(Move(pos, Position(0, 6), piece, isCastling = true))
                    }
                    if (!blackRookQueensideMoved && pieceAt(Position(0, 1)) == null && pieceAt(Position(0, 2)) == null && pieceAt(Position(0, 3)) == null) {
                        moves.add(Move(pos, Position(0, 2), piece, isCastling = true))
                    }
                }
            }
        }

        // Sliding pieces (Bishop, Rook, Queen)
        for (dir in directions) {
            var curr = Position(pos.row + dir.first, pos.col + dir.second)
            while (curr.isValid()) {
                val target = pieceAt(curr)
                if (target == null) {
                    moves.add(Move(pos, curr, piece))
                } else {
                    if (target.color != piece.color) {
                        moves.add(Move(pos, curr, piece, target))
                    }
                    break
                }
                curr = Position(curr.row + dir.first, curr.col + dir.second)
            }
        }

        return moves
    }

    private fun wouldBeInCheckAfter(move: Move): Boolean {
        // Temporarily apply move
        val targetPiece = board[move.end.row][move.end.col]
        board[move.end.row][move.end.col] = move.piece
        board[move.start.row][move.start.col] = null
        
        var epCaptured: Piece? = null
        if (move.isEnPassant) {
            epCaptured = board[move.start.row][move.end.col]
            board[move.start.row][move.end.col] = null
        }
        
        val inCheck = isInCheck(move.piece.color)
        
        // Revert move
        board[move.start.row][move.start.col] = move.piece
        board[move.end.row][move.end.col] = targetPiece
        if (move.isEnPassant) {
            board[move.start.row][move.end.col] = epCaptured
        }
        
        // Castling check validation
        if (move.isCastling && !inCheck) {
            val dir = if (move.end.col > move.start.col) 1 else -1
            val stepPath = Position(move.start.row, move.start.col + dir)
            board[stepPath.row][stepPath.col] = move.piece
            board[move.start.row][move.start.col] = null
            val pathCheck = isInCheck(move.piece.color)
            board[move.start.row][move.start.col] = move.piece
            board[stepPath.row][stepPath.col] = null
            if (pathCheck || isInCheck(move.piece.color)) { // cannot castle out of or through check
                return true
            }
        }
        return inCheck
    }

    private fun isInCheck(color: PieceColor): Boolean {
        // Find king
        var kingPos: Position? = null
        for (r in 0..7) {
            for (c in 0..7) {
                val p = pieceAt(Position(r, c))
                if (p != null && p.type == PieceType.KING && p.color == color) {
                    kingPos = Position(r, c)
                    break
                }
            }
        }
        if (kingPos == null) return false

        // Check if any opponent piece can attack kingPos
        val oppColor = if (color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        for (r in 0..7) {
            for (c in 0..7) {
                val pos = Position(r, c)
                val p = pieceAt(pos)
                if (p != null && p.color == oppColor) {
                    val moves = generatePseudoLegalMoves(pos, p)
                    if (moves.any { it.end == kingPos }) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun makeMove(move: Move) {
        // En Passant capture
        if (move.isEnPassant) {
            board[move.start.row][move.end.col] = null
            move.capturedPiece?.let { capturedPieces.add(it) }
        } else if (move.capturedPiece != null) {
            capturedPieces.add(move.capturedPiece)
        }

        // Apply move
        board[move.end.row][move.end.col] = if (move.promotion != null) Piece(move.piece.color, move.promotion!!) else move.piece
        board[move.start.row][move.start.col] = null

        // Castling logic (Move Rook)
        if (move.isCastling) {
            if (move.end.col == 6) { // Kingside
                board[move.end.row][5] = board[move.end.row][7]
                board[move.end.row][7] = null
            } else if (move.end.col == 2) { // Queenside
                board[move.end.row][3] = board[move.end.row][0]
                board[move.end.row][0] = null
            }
        }

        // Update rights
        if (move.piece.type == PieceType.KING) {
            if (move.piece.color == PieceColor.WHITE) whiteKingMoved = true
            else blackKingMoved = true
        }
        if (move.piece.type == PieceType.ROOK) {
            if (move.piece.color == PieceColor.WHITE) {
                if (move.start.col == 0) whiteRookQueensideMoved = true
                if (move.start.col == 7) whiteRookKingsideMoved = true
            } else {
                if (move.start.col == 0) blackRookQueensideMoved = true
                if (move.start.col == 7) blackRookKingsideMoved = true
            }
        }

        lastMove = move
        moveHistory.add(move)
        turn = if (turn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
    }
    
    fun getGameState(): GameState {
        val hasValidMoves = hasAnyValidMove(turn)
        val inCheck = isInCheck(turn)
        return when {
            inCheck && !hasValidMoves -> GameState.CHECKMATE
            !inCheck && !hasValidMoves -> GameState.STALEMATE
            else -> GameState.ONGOING
        }
    }
    
    private fun hasAnyValidMove(color: PieceColor): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                val pos = Position(r, c)
                val p = pieceAt(pos)
                if (p != null && p.color == color) {
                    if (getValidMoves(pos).isNotEmpty()) return true
                }
            }
        }
        return false
    }
}

enum class GameState { ONGOING, CHECKMATE, STALEMATE }
