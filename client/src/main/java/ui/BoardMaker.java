package ui;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static ui.EscapeSequences.*;

public class BoardMaker {

    private static final int BOARD_SIZE_IN_SQUARES = 8;

    // Visual width of each square (3 chars: symbol centered)
    private static final int SQUARE_WIDTH = 3;

    // Unicode chess pieces: white = outline, black = solid
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

    // -------- Public entry points --------

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

    // -------- Core drawing --------

    private static void drawInitialBoard(PrintStream out, boolean whitePerspective) {
        drawFileHeaders(out, whitePerspective);
        drawBoard(out, whitePerspective);
        drawFileHeaders(out, whitePerspective);
    }

    private static void drawBoard(PrintStream out, boolean whitePerspective) {
        if (whitePerspective) {
            for (int rank = 8; rank >= 1; rank--) {
                drawRank(out, rank, 1, 8, +1);
            }
        } else {
            for (int rank = 1; rank <= 8; rank++) {
                drawRank(out, rank, 8, 1, -1);
            }
        }
    }

    private static void drawRank(PrintStream out, int rank,
                                 int startFile, int endFile, int step) {

        // Left rank label
        resetColors(out);
        out.printf("%d ", rank);

        // Squares (one terminal row per rank)
        for (int file = startFile; file != endFile + step; file += step) {
            boolean dark = ((rank + file) % 2) == 0;
            char piece = initialPieceAt(rank, file);

            if (dark) {
                setDarkSquare(out);
            } else {
                setLightSquare(out);
            }

            if (piece == 0) {
                // Empty square: full width of spaces, background provides color
                out.print(" ".repeat(SQUARE_WIDTH));
            } else {
                // Center the piece in the square
                int leftPadding  = SQUARE_WIDTH / 2;
                int rightPadding = SQUARE_WIDTH - leftPadding - 1;

                out.print(" ".repeat(leftPadding));
                out.print(piece);              // outline or solid, but always black
                out.print(" ".repeat(rightPadding));
            }
        }

        // Right rank label
        resetColors(out);
        out.printf(" %d%n", rank);
    }

    private static void drawFileHeaders(PrintStream out, boolean whitePerspective) {
        resetColors(out);
        // Three spaces: "<digit>" + space + one extra so letters align with centered pieces
        out.print("   ");

        if (whitePerspective) {
            for (char file = 'a'; file <= 'h'; file++) {
                out.print(file);
                out.print(" ".repeat(SQUARE_WIDTH - 1));
            }
        } else {
            for (char file = 'h'; file >= 'a'; file--) {
                out.print(file);
                out.print(" ".repeat(SQUARE_WIDTH - 1));
            }
        }
        out.println();
    }

    // -------- Colors --------

    private static void setLightSquare(PrintStream out) {
        out.print(SET_BG_COLOR_WHITE);
        out.print(SET_TEXT_COLOR_BLACK); // all pieces rendered in black
    }

    private static void setDarkSquare(PrintStream out) {
        out.print(SET_BG_COLOR_BLUE);
        out.print(SET_TEXT_COLOR_BLACK); // all pieces rendered in black
    }

    private static void resetColors(PrintStream out) {
        out.print(SET_BG_COLOR_BLACK);
        out.print(SET_TEXT_COLOR_WHITE);
    }

    // -------- Initial piece placement --------

    private static char initialPieceAt(int rank, int file) {
        // Black back rank
        if (rank == 8) {
            switch (file) {
                case 1: case 8: return BR;
                case 2: case 7: return BN;
                case 3: case 6: return BB;
                case 4: return BQ;
                case 5: return BK;
            }
        }
        // Black pawns
        if (rank == 7) {
            return BP;
        }
        // White pawns
        if (rank == 2) {
            return WP;
        }
        // White back rank
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
}
