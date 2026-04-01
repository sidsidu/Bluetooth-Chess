package com.example.bluetoothchess.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothchess.bluetooth.BluetoothManager
import com.example.bluetoothchess.engine.ChessEngine
import com.example.bluetoothchess.engine.GameState
import com.example.bluetoothchess.engine.Move
import com.example.bluetoothchess.engine.Piece
import com.example.bluetoothchess.engine.PieceColor
import com.example.bluetoothchess.engine.PieceType
import com.example.bluetoothchess.engine.Position
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class GameViewModel : ViewModel() {
    private val btManager = BluetoothManager(BluetoothAdapter.getDefaultAdapter())
    private val engine = ChessEngine()

    private val _uiState = MutableStateFlow(GameUiState(board = engine.board))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _connectionState = MutableStateFlow<BluetoothManager.ConnectionState?>(null)
    val connectionState: StateFlow<BluetoothManager.ConnectionState?> = _connectionState.asStateFlow()

    private var myColor: PieceColor = PieceColor.WHITE

    data class GameUiState(
        val board: Array<Array<Piece?>>,
        val selectedSquare: Position? = null,
        val validMoves: List<Position> = emptyList(),
        val isMyTurn: Boolean = true,
        val myColor: PieceColor = PieceColor.WHITE,
        val opponentName: String = "Opponent",
        val gameState: GameState = GameState.ONGOING,
        val moveHistory: List<String> = emptyList(),
        val capturedWhite: List<PieceType> = emptyList(),
        val capturedBlack: List<PieceType> = emptyList()
    )

    init {
        viewModelScope.launch {
            btManager.connectionState.collect { state ->
                _connectionState.value = state
            }
        }
        viewModelScope.launch {
            btManager.incomingMessages.collect { msg ->
                handleIncomingMessage(msg)
            }
        }
    }

    fun startHost() {
        myColor = PieceColor.WHITE
        updateUiState()
        viewModelScope.launch { btManager.startHost() }
    }

    fun joinGame(device: BluetoothDevice) {
        myColor = PieceColor.BLACK
        updateUiState()
        viewModelScope.launch { btManager.connectToDevice(device) }
    }

    fun onSquareSelected(pos: Position) {
        if (_uiState.value.gameState != GameState.ONGOING) return
        if (engine.turn != myColor) return

        val state = _uiState.value
        if (state.selectedSquare == null) {
            val piece = engine.pieceAt(pos)
            if (piece?.color == myColor) {
                val validMoves = engine.getValidMoves(pos)
                _uiState.value = state.copy(
                    selectedSquare = pos,
                    validMoves = validMoves.map { it.end }
                )
            }
        } else {
            val move = engine.getValidMoves(state.selectedSquare).find { it.end == pos }
            if (move != null) {
                // If promotion needed, here we'd show a dialog. Defaulting to Queen for simplicity unless handled by UI
                val finalMove = move.copy(promotion = if (move.piece.type == PieceType.PAWN && (pos.row == 0 || pos.row == 7)) PieceType.QUEEN else null)
                engine.makeMove(finalMove)
                sendMove(finalMove)
                updateUiState()
            } else {
                val piece = engine.pieceAt(pos)
                if (piece?.color == myColor) {
                    val validMoves = engine.getValidMoves(pos)
                    _uiState.value = state.copy(
                        selectedSquare = pos,
                        validMoves = validMoves.map { it.end }
                    )
                } else {
                    _uiState.value = state.copy(selectedSquare = null, validMoves = emptyList())
                }
            }
        }
    }

    fun promotePawn(to: PieceType) {
        // Implement dialogue interface via UI if needed. Built in auto-queen above for now.
    }

    fun resign() {
        viewModelScope.launch {
            val json = JSONObject().apply { put("type", "resign") }
            btManager.sendMessage(json.toString())
        }
    }

    private fun handleIncomingMessage(msg: String) {
        try {
            val json = JSONObject(msg)
            when (json.getString("type")) {
                "move" -> {
                    val moveStr = json.getString("algebraic")
                    val oppPieceType = PieceType.valueOf(json.getString("pieceType"))
                    val startPos = Position(json.getInt("startRow"), json.getInt("startCol"))
                    val endPos = Position(json.getInt("endRow"), json.getInt("endCol"))
                    val isCastling = json.optBoolean("isCastling")
                    val isEnPassant = json.optBoolean("isEnPassant")
                    val promoStr = json.optString("promotion", "")
                    
                    val piece = Piece(if (myColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE, oppPieceType)
                    var capturedPiece: Piece? = null // Handled implicitly by engine state
                    val oppMove = engine.getValidMoves(startPos).find { it.end == endPos } ?: Move(startPos, endPos, piece, isCastling = isCastling, isEnPassant = isEnPassant)
                    
                    engine.makeMove(oppMove)
                    updateUiState()
                }
                "resign" -> {
                    // Update state to win
                    _uiState.value = _uiState.value.copy(gameState = GameState.CHECKMATE)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendMove(move: Move) {
        val json = JSONObject().apply {
            put("type", "move")
            put("algebraic", move.toAlgebraic())
            put("pieceType", move.piece.type.name)
            put("startRow", move.start.row)
            put("startCol", move.start.col)
            put("endRow", move.end.row)
            put("endCol", move.end.col)
            put("isCastling", move.isCastling)
            put("isEnPassant", move.isEnPassant)
            move.promotion?.let { put("promotion", it.name) }
        }
        viewModelScope.launch {
            btManager.sendMessage(json.toString())
        }
    }

    private fun updateUiState() {
        _uiState.value = _uiState.value.copy(
            board = engine.board.map { it.clone() }.toTypedArray(),
            selectedSquare = null,
            validMoves = emptyList(),
            isMyTurn = engine.turn == myColor,
            myColor = myColor,
            gameState = engine.getGameState(),
            moveHistory = engine.moveHistory.map { it.toAlgebraic() },
            capturedWhite = engine.capturedPieces.filter { it.color == PieceColor.WHITE }.map { it.type },
            capturedBlack = engine.capturedPieces.filter { it.color == PieceColor.BLACK }.map { it.type }
        )
    }

    override fun onCleared() {
        super.onCleared()
        btManager.cleanup()
    }
}
