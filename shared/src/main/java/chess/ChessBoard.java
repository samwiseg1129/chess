package chess;

import java.util.Arrays;
import java.util.Objects;

/**
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {
    ChessPiece[][] squares = new ChessPiece[8][8];
    public ChessBoard() {
        
    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        squares[position.getRow()-1][position.getColumn()-1] = piece;
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        return squares[position.getRow() - 1][position.getColumn() - 1];
    }
    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        squares = new ChessPiece[8][8];

        ChessGame.TeamColor white = ChessGame.TeamColor.WHITE;
        ChessGame.TeamColor black = ChessGame.TeamColor.BLACK;

        for (int col = 1; col <= 8; col++) {
            addPiece(new ChessPosition(2, col), new ChessPiece(white, ChessPiece.PieceType.PAWN));
            addPiece(new ChessPosition(7, col), new ChessPiece(black, ChessPiece.PieceType.PAWN));
        }

        addPiece(new ChessPosition(1, 1), new ChessPiece(white, ChessPiece.PieceType.ROOK));
        addPiece(new ChessPosition(1, 8), new ChessPiece(white, ChessPiece.PieceType.ROOK));
        addPiece(new ChessPosition(8, 1), new ChessPiece(black, ChessPiece.PieceType.ROOK));
        addPiece(new ChessPosition(8, 8), new ChessPiece(black, ChessPiece.PieceType.ROOK));

        addPiece(new ChessPosition(1, 2), new ChessPiece(white, ChessPiece.PieceType.KNIGHT));
        addPiece(new ChessPosition(1, 7), new ChessPiece(white, ChessPiece.PieceType.KNIGHT));
        addPiece(new ChessPosition(8, 2), new ChessPiece(black, ChessPiece.PieceType.KNIGHT));
        addPiece(new ChessPosition(8, 7), new ChessPiece(black, ChessPiece.PieceType.KNIGHT));

        addPiece(new ChessPosition(1, 3), new ChessPiece(white, ChessPiece.PieceType.BISHOP));
        addPiece(new ChessPosition(1, 6), new ChessPiece(white, ChessPiece.PieceType.BISHOP));
        addPiece(new ChessPosition(8, 3), new ChessPiece(black, ChessPiece.PieceType.BISHOP));
        addPiece(new ChessPosition(8, 6), new ChessPiece(black, ChessPiece.PieceType.BISHOP));

        addPiece(new ChessPosition(1, 4), new ChessPiece(white, ChessPiece.PieceType.QUEEN));
        addPiece(new ChessPosition(8, 4), new ChessPiece(black, ChessPiece.PieceType.QUEEN));

        addPiece(new ChessPosition(1, 5), new ChessPiece(white, ChessPiece.PieceType.KING));
        addPiece(new ChessPosition(8, 5), new ChessPiece(black, ChessPiece.PieceType.KING));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessBoard that = (ChessBoard) o;
        return Objects.deepEquals(squares, that.squares);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(squares);
    }
}
