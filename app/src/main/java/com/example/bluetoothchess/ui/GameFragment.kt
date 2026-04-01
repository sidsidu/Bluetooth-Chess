package com.example.bluetoothchess.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bluetoothchess.R
import com.example.bluetoothchess.databinding.FragmentGameBinding
import com.example.bluetoothchess.engine.GameState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class GameFragment : Fragment() {

    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.chessBoard.onSquareSelectedListener = { pos ->
            viewModel.onSquareSelected(pos)
        }

        binding.btnResign.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Concede Match")
                .setMessage("Are you sure you want to resign?")
                .setPositiveButton("Resign") { _, _ -> viewModel.resign() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.chessBoard.boardState = state.board
                        binding.chessBoard.selectedSquare = state.selectedSquare
                        binding.chessBoard.validMoves = state.validMoves
                        binding.chessBoard.isBoardInverted = state.myColor == com.example.bluetoothchess.engine.PieceColor.BLACK
                        
                        binding.textPlayerTurnStatus.text = if (state.isMyTurn) getString(R.string.your_turn) else getString(R.string.waiting)
                        binding.textOpponentName.text = state.opponentName

                        updateMoveHistory(state.moveHistory)

                        when (state.gameState) {
                            GameState.CHECKMATE -> showGameOverDialog(getString(R.string.checkmate), !state.isMyTurn)
                            GameState.STALEMATE -> showGameOverDialog(getString(R.string.stalemate), null)
                            else -> {}
                        }
                    }
                }
                
                launch {
                    viewModel.connectionState.collect { conn ->
                        if (conn == com.example.bluetoothchess.bluetooth.BluetoothManager.ConnectionState.DISCONNECTED || 
                            conn == com.example.bluetoothchess.bluetooth.BluetoothManager.ConnectionState.ERROR) {
                                showDisconnectedDialog()
                        }
                    }
                }
            }
        }
    }

    private fun updateMoveHistory(history: List<String>) {
        binding.layoutMoveHistory.removeAllViews()
        history.forEachIndexed { index, move ->
            val tv = TextView(requireContext()).apply {
                text = "${(index/2) + 1}. $move  "
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            }
            binding.layoutMoveHistory.addView(tv)
        }
        binding.scrollMoveHistory.post {
            binding.scrollMoveHistory.fullScroll(View.FOCUS_RIGHT)
        }
    }

    private fun showGameOverDialog(title: String, iWon: Boolean?) {
        val msg = when(iWon) {
            true -> "You Win!"
            false -> "You Lose!"
            else -> "It's a draw."
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("Return to Menu") { _, _ ->
                requireActivity().supportFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    private fun showDisconnectedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.connection_lost))
            .setMessage("The Bluetooth connection was lost.")
            .setPositiveButton("OK") { _, _ ->
                requireActivity().supportFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
