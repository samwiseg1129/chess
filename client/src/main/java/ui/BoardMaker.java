package ui;

public class BoardMaker {

    private static final String LIGHT_SQUARE = " ";
    private static final String DARK_SQUARE = "░";

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
        boolean whitePerspective = !"black".equalsIgnoreCase(color);
        drawInitialBoard(whitePerspective);
    }

    public static void drawInitialBoardForObserver() {
        drawInitialBoard(true);
    }

    private static void drawInitialBoard(boolean whitePerspective) {
        if (whitePerspective) {
            drawBoardWhiteAtBottom();
        } else {
            drawBoardBlackAtBottom();
        }
    }

    private static void drawBoardWhiteAtBottom() {
        System.out.println("    a   b   c   d   e   f   g   h");
        for (int rank = 8; rank >= 1; rank--) {
            System.out.print("  +---+---+---+---+---+---+---+---+\n");
            System.out.print(rank + " |");
            for (int file = 1; file <= 8; file++) {
                char piece = initialPieceAt(rank, file);
                String square = (((rank + file) % 2) == 0) ? DARK_SQUARE : LIGHT_SQUARE;
                System.out.print(" " + (piece == 0 ? square : piece) + " |");
            }
            System.out.print(" " + rank + "\n");
        }
        System.out.println("  +---+---+---+---+---+---+---+---+");
        System.out.println("    a   b   c   d   e   f   g   h");
    }

    private static void drawBoardBlackAtBottom() {
        System.out.println("    h   g   f   e   d   c   b   a");
        for (int rank = 1; rank <= 8; rank++) {
            System.out.print("  +---+---+---+---+---+---+---+---+\n");
            System.out.print(rank + " |");
            for (int file = 8; file >= 1; file--) {
                char piece = initialPieceAt(rank, file);
                String square = (((rank + file) % 2) == 0) ? DARK_SQUARE : LIGHT_SQUARE;
                System.out.print(" " + (piece == 0 ? square : piece) + " |");
            }
            System.out.print(" " + rank + "\n");
        }
        System.out.println("  +---+---+---+---+---+---+---+---+");
        System.out.println("    h   g   f   e   d   c   b   a");
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
}
