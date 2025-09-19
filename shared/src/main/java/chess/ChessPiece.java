package chess;

import chess.MoveCalculators.BishopMoveCalculator;
import chess.MoveCalculators.KingMoveCalculator;
import chess.MoveCalculators.KnightMoveCalculator;
import chess.MoveCalculators.QueenMoveCalculator;

import java.util.Collection;


/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        return switch (type) {
            case BISHOP -> BishopMoveCalculator.getMoves(board,myPosition);
            case QUEEN -> QueenMoveCalculator.getMoves(board,myPosition);
            case KING -> KingMoveCalculator.getMoves(board,myPosition);
            case KNIGHT -> KnightMoveCalculator.getMoves(board,myPosition);

        };
    }

}
