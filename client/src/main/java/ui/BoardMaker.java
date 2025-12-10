package ui;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import chess.ChessBoard;
import chess.ChessPiece;
import chess.ChessPosition;

import static ui.EscapeSequences.*;

public class BoardMaker {

    private static final int SQUARE_WIDTH = 3;

    private static final char WK = '\u2654';
    private static final char WQ = '\u2655';
    private static final char WR = '\u2656';
    private static final char WB = '\u2657';
    private static final char WN = '\u2658';
    private static final char WP = '\u2659';
    private static final char BK = '\u265A';
    private static final char BQ = '\u265B';
    private static final char BR = '\u265C';
    private static final char BB = '\u265D';
    private static final char BN = '\u265E';
    private static final char BP = '\u265F';

    public static void drawInitialBoardForColor(String color) {
        var out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        out.print(ERASE_SCREEN);
        boolean whitePerspective = !"black".equalsIgnoreCase(color);
        drawInitialBoard(out, whitePerspective);
        resetColors(out);
    }

    public static void drawInitialBoardForObserver() {
        var out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        out.print(ERASE_SCREEN);
        drawInitialBoard(out, true);
        resetColors(out);
    }

    // Draw arbitrary board (no highlights)
    public static void drawBoard(ChessBoard board, String perspectiveColor) {
        var out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        out.print(ERASE_SCREEN);
        boolean whitePerspective = (perspectiveColor == null) ||
                !"BLACK".equalsIgnoreCase(perspectiveColor);
        drawBoard(out, board, whitePerspective);
        resetColors(out);
    }

    // Draw arbitrary board with highlighted squares
    public static void drawBoard(ChessBoard board, String perspectiveColor,
                                 Set<ChessPosition> highlights) {
        var out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        out.print(ERASE_SCREEN);
        boolean whitePerspective = (perspectiveColor == null) ||
                !"BLACK".equalsIgnoreCase(perspectiveColor);
        drawBoard(out, board, whitePerspective, highlights);
        resetColors(out);
    }

    private static void drawInitialBoard(PrintStream out, boolean whitePerspective) {
        // Use generic path with no highlights, null board means initial layout
        drawBoard(out, (ChessBoard) null, whitePerspective, null);
    }

    // Wrapper: existing calls use this (no highlights)
    private static void drawBoard(PrintStream out, ChessBoard board, boolean whitePerspective) {
        drawBoard(out, board, whitePerspective, null);
    }

    // Core drawBoard with optional highlights; always prints headers top/bottom
    private static void drawBoard(PrintStream out, ChessBoard board,
                                  boolean whitePerspective,
                                  Set<ChessPosition> highlights) {

        // top file letters
        drawFileHeaders(out, whitePerspective);

        if (whitePerspective) {
            for (int rank = 8; rank >= 1; rank--) {
                drawRank(out, board, rank, 1, 8, +1, highlights);
            }
        } else {
            for (int rank = 1; rank <= 8; rank++) {
                drawRank(out, board, rank, 8, 1, -1, highlights);
            }
        }

        // bottom file letters
        drawFileHeaders(out, whitePerspective);
    }

    private static void drawBoard(PrintStream out, boolean whitePerspective) {
        drawBoard(out, null, whitePerspective);
    }

    private static void drawRank(PrintStream out, ChessBoard board,
                                 int rank, int startFile, int endFile, int step,
                                 Set<ChessPosition> highlights) {
        resetColors(out);
        out.printf("%d ", rank);

        for (int file = startFile; file != endFile + step; file += step) {
            boolean dark = ((rank + file) % 2) == 0;
            char piece = (board == null)
                    ? initialPieceAt(rank, file)
                    : pieceAt(board, rank, file);

            boolean isHighlighted = false;
            if (highlights != null && board != null) {
                ChessPosition pos = new ChessPosition(rank, file);
                isHighlighted = highlights.contains(pos);
            }

            if (isHighlighted) {
                out.print(SET_BG_COLOR_GREEN);
                out.print(SET_TEXT_COLOR_BLACK);
            } else if (dark) {
                setDarkSquare(out);
            } else {
                setLightSquare(out);
            }

            if (piece == 0) {
                out.print(" ".repeat(SQUARE_WIDTH));
            } else {
                int leftPadding = SQUARE_WIDTH / 2;
                int rightPadding = SQUARE_WIDTH - leftPadding - 1;
                out.print(" ".repeat(leftPadding));
                out.print(piece);
                out.print(" ".repeat(rightPadding));
            }
        }

        resetColors(out);
        out.printf(" %d%n", rank);
    }

    private static void drawFileHeaders(PrintStream out, boolean whitePerspective) {
        resetColors(out);
        // rank label + space on ranks = 2 chars, so match that here
        out.print("  ");
        if (whitePerspective) {
            for (char file = 'a'; file <= 'h'; file++) {
                out.print(" ");
                out.print(file);
                out.print(" ");
            }
        } else {
            for (char file = 'h'; file >= 'a'; file--) {
                out.print(" ");
                out.print(file);
                out.print(" ");
            }
        }
        out.println();
    }

    private static void setLightSquare(PrintStream out) {
        out.print(SET_BG_COLOR_WHITE);
        out.print(SET_TEXT_COLOR_BLACK);
    }

    private static void setDarkSquare(PrintStream out) {
        out.print(SET_BG_COLOR_BLUE);
        out.print(SET_TEXT_COLOR_BLACK);
    }

    private static void resetColors(PrintStream out) {
        out.print(SET_BG_COLOR_BLACK);
        out.print(SET_TEXT_COLOR_WHITE);
    }

    private static char initialPieceAt(int rank, int file) {
        if (rank == 8) {
            switch (file) {
                case 1: case 8: return BR;
                case 2: case 7: return BN;
                case 3: case 6: return BB;
                case 4: return BQ;
                case 5: return BK;
            }
        }
        if (rank == 7) {
            return BP;
        }
        if (rank == 2) {
            return WP;
        }
        if (rank == 1) {
            switch (file) {
                case 1: case 8: return WR;
                case 2: case 7: return WN;
                case 3: case 6: return WB;
                case 4: return WQ;
                case 5: return WK;
            }
        }
        return 0;
    }

    private static char pieceAt(ChessBoard board, int rank, int file) {
        if (board == null) {
            return initialPieceAt(rank, file);
        }
        ChessPosition pos = new ChessPosition(rank, file);
        ChessPiece piece = board.getPiece(pos);
        if (piece == null) {
            return 0;
        }
        return switch (piece.getTeamColor()) {
            case WHITE -> switch (piece.getPieceType()) {
                case KING -> WK;
                case QUEEN -> WQ;
                case ROOK -> WR;
                case BISHOP -> WB;
                case KNIGHT -> WN;
                case PAWN -> WP;
            };
            case BLACK -> switch (piece.getPieceType()) {
                case KING -> BK;
                case QUEEN -> BQ;
                case ROOK -> BR;
                case BISHOP -> BB;
                case KNIGHT -> BN;
                case PAWN -> BP;
            };
        };
    }
}
