package chess;

import java.util.Collection;
import java.util.*;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    private static final int boardSize = 8;

    private ChessBoard board;
    private TeamColor teamTurn;
    private boolean gameOver;

    public ChessGame() {
        this.board = new ChessBoard();
        setTeamTurn(TeamColor.WHITE);
        this.gameOver = false;
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE, BLACK;

        @Override
        public String toString() {
            return this == WHITE ? "white" : "black";
        }
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece currPiece = board.getPiece(startPosition);
        if (currPiece == null) return null;
        Collection<ChessMove> possibleMoves = currPiece.pieceMoves(board, startPosition);
        HashSet<ChessMove> validMoves = new HashSet<>();

        for (ChessMove move : possibleMoves) {
            // Save current state
            ChessPiece originalStartPiece = board.getPiece(startPosition);
            ChessPiece originalEndPiece = board.getPiece(move.getEndPosition());

            // Simulate move (handle promotion)
            ChessPiece movingPiece = (move.getPromotionPiece() != null)
                    ? new ChessPiece(currPiece.getTeamColor(), move.getPromotionPiece())
                    : currPiece;
            board.addPiece(startPosition, null);
            board.addPiece(move.getEndPosition(), movingPiece);

            // Validate move
            if (!isInCheck(currPiece.getTeamColor())) {
                validMoves.add(move);
            }

            // Undo move
            board.addPiece(move.getEndPosition(), originalEndPiece);
            board.addPiece(startPosition, originalStartPiece);
        }
        return validMoves;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
