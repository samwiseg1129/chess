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
    private final boolean gameOver;

    public ChessGame() {
        this.board = new ChessBoard();
        this.board.resetBoard();
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
        ChessPiece piece = board.getPiece(move.getStartPosition());
        boolean isTeamsTurn = piece != null && getTeamTurn() == piece.getTeamColor();
        Collection<ChessMove> moves = validMoves(move.getStartPosition());
        if (moves == null) {
            throw new InvalidMoveException("No valid moves available");
        }
        boolean isValidMove = moves.contains(move);

        if (isValidMove && isTeamsTurn) {
            ChessPiece pieceToMove = board.getPiece(move.getStartPosition());
            if (move.getPromotionPiece() != null) {
                pieceToMove = new ChessPiece(pieceToMove.getTeamColor(), move.getPromotionPiece());
            }
            board.addPiece(move.getStartPosition(), null);
            board.addPiece(move.getEndPosition(), pieceToMove);
            setTeamTurn(getTeamTurn() == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE);
        } else {
            throw new InvalidMoveException(String.format("Valid move: %b  Your Turn: %b", isValidMove, isTeamsTurn));
        }
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPos = null;
        for (int y = 1; y <= boardSize && kingPos == null; y++) {
            for (int x = 1; x <= boardSize && kingPos == null; x++) {
                ChessPiece currPiece = board.getPiece(new ChessPosition(y, x));
                if (currPiece != null && currPiece.getTeamColor() == teamColor
                        && currPiece.getPieceType() == ChessPiece.PieceType.KING) {
                    kingPos = new ChessPosition(y, x);
                }
            }
        }
        if (kingPos == null) {
            throw new IllegalStateException("King not found on board for team: " + teamColor);
        }
        // See if any enemy piece can attack the king
        for (int y = 1; y <= boardSize; y++) {
            for (int x = 1; x <= boardSize; x++) {
                ChessPiece currPiece = board.getPiece(new ChessPosition(y, x));
                if (currPiece == null || currPiece.getTeamColor() == teamColor) continue;
                for (ChessMove enemyMove : currPiece.pieceMoves(board, new ChessPosition(y, x))) {
                    if (enemyMove.getEndPosition().equals(kingPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) return false;

        for (int y = 1; y <= boardSize; y++) {
            for (int x = 1; x <= boardSize; x++) {
                ChessPosition currPosition = new ChessPosition(y, x);
                ChessPiece currPiece = board.getPiece(currPosition);
                if (currPiece != null && currPiece.getTeamColor() == teamColor) {
                    Collection<ChessMove> moves = validMoves(currPosition);
                    if (moves != null && !moves.isEmpty()) {
                        return false; // Still have valid moves, so not in checkmate
                    }
                }
            }
        }
        return true;
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)) return false;
        for (int y = 1; y <= boardSize; y++) {
            for (int x = 1; x <= boardSize; x++) {
                ChessPosition currPosition = new ChessPosition(y, x);
                ChessPiece currPiece = board.getPiece(currPosition);
                if (currPiece != null && currPiece.getTeamColor() == teamColor) {
                    Collection<ChessMove> moves = validMoves(currPosition);
                    if (moves != null && !moves.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }

    public boolean getGameOver() {
        return gameOver;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessGame chessGame = (ChessGame) o;
        return gameOver == chessGame.gameOver && Objects.equals(board, chessGame.board) && teamTurn == chessGame.teamTurn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, teamTurn, gameOver);
    }
}
