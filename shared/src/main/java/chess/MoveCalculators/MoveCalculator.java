package chess.MoveCalculators;

import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.HashSet;

public interface MoveCalculator {

    static boolean isValidSquare(ChessPosition position) {
        return position.getRow() >= 1 && position.getRow() <= 8 &&
                position.getColumn() >= 1 && position.getColumn() <= 8;
    }

    static HashSet<ChessMove> generateMoves(ChessBoard board, ChessPosition currPosition, int[][] directions, boolean repeat) {

        HashSet<ChessMove> moves = new HashSet<>();

        int currX = currPosition.getColumn();
        int currY = currPosition.getRow();

        ChessPiece currentPiece = board.getPiece(currPosition);
        if (currentPiece == null) return moves; // no piece at start square

        var team = currentPiece.getTeamColor();

        for (int[] dir : directions) {
            int step = 1;
            while (true) {
                ChessPosition target = new ChessPosition(currY + dir[1] * step, currX + dir[0] * step);

                if (!isValidSquare(target)) break;

                ChessPiece targetPiece = board.getPiece(target);

                if (targetPiece == null) {
                    // empty square → valid move
                    moves.add(new ChessMove(currPosition, target, null));
                } else {
                    // occupied square
                    if (targetPiece.getTeamColor() != team) {
                        // capture opponent piece
                        moves.add(new ChessMove(currPosition, target, null));
                    }
                    // always stop after hitting a piece
                    break;
                }
                if (!repeat) break; // knights/kings only step once
                step++;
            }
        }
        return moves;
    }
}
