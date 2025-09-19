package chess.MoveCalculators;

import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPosition;

import java.util.HashSet;

public class RookMoveCalculator implements MoveCalculator {

    public static HashSet<ChessMove> getMoves(ChessBoard board, ChessPosition currPosition) {
        int[][] moveDirection = {
                { 0, 1},  { 1, 0},  { 0,-1},  {-1, 0}   // straight lines
        };

        return MoveCalculator.generateMoves(board, currPosition, moveDirection, true);
    }

}
