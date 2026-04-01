package com.example.bluetoothchess.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.caverock.androidsvg.SVG
import com.example.bluetoothchess.R
import com.example.bluetoothchess.engine.Piece
import com.example.bluetoothchess.engine.PieceColor
import com.example.bluetoothchess.engine.PieceType
import com.example.bluetoothchess.engine.Position

class ChessBoardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintDark = Paint().apply { color = ContextCompat.getColor(context, R.color.board_dark) }
    private val paintLight = Paint().apply { color = ContextCompat.getColor(context, R.color.board_light) }
    private val paintHighlight = Paint().apply { 
        color = ContextCompat.getColor(context, R.color.board_highlight)
        alpha = 150
    }
    private val paintSelected = Paint().apply { color = ContextCompat.getColor(context, R.color.board_selected) }

    var boardState: Array<Array<Piece?>> = Array(8) { arrayOfNulls(8) }
        set(value) {
            field = value
            invalidate()
        }

    var selectedSquare: Position? = null
        set(value) {
            field = value
            invalidate()
        }

    var isBoardInverted: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var validMoves: List<Position> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var onSquareSelectedListener: ((Position) -> Unit)? = null

    private val piecePictures = mutableMapOf<Piece, Picture>()

    init {
        loadPieces()
    }

    private fun loadPieces() {
        val mapping = mapOf(
            Piece(PieceColor.WHITE, PieceType.KING) to R.raw.white_king,
            Piece(PieceColor.WHITE, PieceType.QUEEN) to R.raw.white_queen,
            Piece(PieceColor.WHITE, PieceType.ROOK) to R.raw.white_rook,
            Piece(PieceColor.WHITE, PieceType.BISHOP) to R.raw.white_bishop,
            Piece(PieceColor.WHITE, PieceType.KNIGHT) to R.raw.white_knight,
            Piece(PieceColor.WHITE, PieceType.PAWN) to R.raw.white_pawn,
            Piece(PieceColor.BLACK, PieceType.KING) to R.raw.black_king,
            Piece(PieceColor.BLACK, PieceType.QUEEN) to R.raw.black_queen,
            Piece(PieceColor.BLACK, PieceType.ROOK) to R.raw.black_rook,
            Piece(PieceColor.BLACK, PieceType.BISHOP) to R.raw.black_bishop,
            Piece(PieceColor.BLACK, PieceType.KNIGHT) to R.raw.black_knight,
            Piece(PieceColor.BLACK, PieceType.PAWN) to R.raw.black_pawn
        )

        mapping.forEach { (piece, resId) ->
            try {
                val svg = SVG.getFromResource(context, resId)
                piecePictures[piece] = svg.renderToPicture()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minDim = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(minDim, minDim)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val squareSize = width / 8f

        // Draw Board
        for (row in 0..7) {
            for (col in 0..7) {
                val isLight = (row + col) % 2 == 0
                val paint = if (isLight) paintLight else paintDark
                
                val drawRow = if (isBoardInverted) 7 - row else row
                val drawCol = if (isBoardInverted) 7 - col else col

                val left = drawCol * squareSize
                val top = drawRow * squareSize
                canvas.drawRect(left, top, left + squareSize, top + squareSize, paint)

                if (selectedSquare?.row == row && selectedSquare?.col == col) {
                    canvas.drawRect(left, top, left + squareSize, top + squareSize, paintSelected)
                }

                if (validMoves.contains(Position(row, col))) {
                    canvas.drawCircle(left + squareSize / 2, top + squareSize / 2, squareSize / 4, paintHighlight)
                }

                // Draw Piece
                val piece = boardState[row][col]
                if (piece != null) {
                    piecePictures[piece]?.let { picture ->
                        val padding = squareSize * 0.1f
                        val pieceLeft = left + padding
                        val pieceTop = top + padding
                        val pieceSize = squareSize - (padding * 2)
                        
                        canvas.save()
                        val scaleX = pieceSize / picture.width.toFloat()
                        val scaleY = pieceSize / picture.height.toFloat()
                        val minScale = Math.min(scaleX, scaleY)
                        
                        // Center piece in square
                        val dx = pieceLeft + (pieceSize - picture.width * minScale) / 2
                        val dy = pieceTop + (pieceSize - picture.height * minScale) / 2
                        
                        canvas.translate(dx, dy)
                        canvas.scale(minScale, minScale)
                        canvas.drawPicture(picture)
                        canvas.restore()
                    }
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            var col = (event.x / (width / 8f)).toInt()
            var row = (event.y / (height / 8f)).toInt()
            
            if (isBoardInverted) {
                col = 7 - col
                row = 7 - row
            }
            
            if (row in 0..7 && col in 0..7) {
                onSquareSelectedListener?.invoke(Position(row, col))
            }
            return true
        }
        return super.onTouchEvent(event)
    }
}
