package chess.MoveCalculators;

import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import chess.ChessGame;

import java.util.HashSet;

public class PawnMoveCalculator implements MoveCalculator {

    public static HashSet<ChessMove> getMoves(ChessBoard board, ChessPosition currPosition) {
        HashSet<ChessMove> moves = new HashSet<>();

        ChessPiece pawn = board.getPiece(currPosition);
        if (pawn == null) return moves;

        var team = pawn.getTeamColor();
        int currX = currPosition.getColumn();
        int currY = currPosition.getRow();

        // Direction pawns move (White moves up +1, Black moves down -1)
        int forward = (team == ChessGame.TeamColor.WHITE) ? 1 : -1;

        // 1-square forward move
        ChessPosition oneStep = new ChessPosition(currY + forward, currX);
        if (MoveCalculator.isValidSquare(oneStep) && board.getPiece(oneStep) == null) {
            moves.add(new ChessMove(currPosition, oneStep, null));

            // 2-square forward move from starting row
            int startRow = (team == ChessGame.TeamColor.WHITE) ? 2 : 7;
            ChessPosition twoStep = new ChessPosition(currY + 2 * forward, currX);
            if (currY == startRow && board.getPiece(twoStep) == null) {
                moves.add(new ChessMove(currPosition, twoStep, null));
            }
        }

        // Diagonal captures
        int[][] captures = {{-1, forward}, {1, forward}};
        for (int[] cap : captures) {
            ChessPosition target = new ChessPosition(currY + cap[1], currX + cap[0]);
            if (MoveCalculator.isValidSquare(target)) {
                ChessPiece targetPiece = board.getPiece(target);
                if (targetPiece != null && targetPiece.getTeamColor() != team) {
                    moves.add(new ChessMove(currPosition, target, null));
                }
            }
        }

        return moves;
    }
}
