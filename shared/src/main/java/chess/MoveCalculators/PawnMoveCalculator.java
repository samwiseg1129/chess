package chess.MoveCalculators;

import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import chess.ChessGame;

import java.util.*;

public class PawnMoveCalculator implements MoveCalculator {
    public static HashSet<ChessMove> getMoves(ChessBoard board, ChessPosition currPosition) {
        HashSet<ChessMove> moves = new HashSet<>();
        ChessPiece pawn = board.getPiece(currPosition);
        if (pawn == null) return moves;

        ChessGame.TeamColor team = pawn.getTeamColor();
        int currX = currPosition.getColumn();
        int currY = currPosition.getRow();
        int forward = (team == ChessGame.TeamColor.WHITE) ? 1 : -1;
        int promotionRank = (team == ChessGame.TeamColor.WHITE) ? 8 : 1;
        int startRow = (team == ChessGame.TeamColor.WHITE) ? 2 : 7;

        List<ChessPiece.PieceType> promotionTypes = Arrays.asList(
                ChessPiece.PieceType.QUEEN,
                ChessPiece.PieceType.ROOK,
                ChessPiece.PieceType.BISHOP,
                ChessPiece.PieceType.KNIGHT);

        ChessPosition oneStep = new ChessPosition(currY + forward, currX);
        if (MoveCalculator.isValidSquare(oneStep) && board.getPiece(oneStep) == null) {
            if (oneStep.getRow() == promotionRank) {
                for (ChessPiece.PieceType type : promotionTypes) {
                    moves.add(new ChessMove(currPosition, oneStep, type));
                }
            } else {
                moves.add(new ChessMove(currPosition, oneStep, null));
                // Two-square move (only from start row, not promotion)
                ChessPosition twoStep = new ChessPosition(currY + 2 * forward, currX);
                if (currY == startRow && board.getPiece(twoStep) == null && board.getPiece(oneStep) == null) {
                    moves.add(new ChessMove(currPosition, twoStep, null));
                }
            }
        }

        int[][] captures = { { -1, forward }, { 1, forward } };
        for (int[] cap : captures) {
            ChessPosition target = new ChessPosition(currY + cap[1], currX + cap[0]);
            if (MoveCalculator.isValidSquare(target)) {
                ChessPiece targetPiece = board.getPiece(target);
                if (targetPiece != null && targetPiece.getTeamColor() != team) {
                    if (target.getRow() == promotionRank) {
                        for (ChessPiece.PieceType type : promotionTypes) {
                            moves.add(new ChessMove(currPosition, target, type));
                        }
                    } else {
                        moves.add(new ChessMove(currPosition, target, null));
                    }
                }
            }
        }

        return moves;
    }
}
