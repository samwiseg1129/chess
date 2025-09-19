package chess.MoveCalculators;

import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPosition;

import java.util.HashSet;

public class QueenMoveCalculator implements MoveCalculator {

    public static HashSet<ChessMove> getMoves(ChessBoard board, ChessPosition currPosition) {
        int[][] moveDirections = {
                {-1, 1},  { 1, 1},  { 1,-1},  {-1,-1},  // diagonals
                { 0, 1},  { 1, 0},  { 0,-1},  {-1, 0}   // straight lines
        };

        return MoveCalculator.generateMoves(board, currPosition, moveDirections, true);
    }
}
