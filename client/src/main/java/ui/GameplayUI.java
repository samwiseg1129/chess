package ui;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import client.websocket.NotificationHandler;
import client.websocket.WebsocketCommunicator;
import exception.ResponseException;
import websocket.messages.ServerMessage;
import java.util.Collection;
import java.util.Scanner;

public class GameplayUI implements NotificationHandler {

    private final Scanner scanner = new Scanner(System.in);
    private final WebsocketCommunicator ws;
    private final String authToken;
    private final int gameId;
    private ChessGame game;
    private ChessGame.TeamColor playerColor;
    private boolean inGame = true;

    public GameplayUI(String baseUrl, String authToken, int gameId,
                      ChessGame.TeamColor playerColor) throws ResponseException {
        this.authToken = authToken;
        this.gameId = gameId;
        this.playerColor = playerColor;
        this.ws = new WebsocketCommunicator(baseUrl, this);
        ws.connect(authToken, gameId);
    }

    public void run() {
        printHelp();
        while (inGame) {
            System.out.print("[game " + gameId + "] >>> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();

            try {
                switch (cmd) {
                    case "help" -> printHelp();
                    case "redraw" -> drawBoard(false);
                    case "pov" -> {
                        playerColor = (playerColor == ChessGame.TeamColor.WHITE)
                                ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
                        drawBoard(false);
                    }
                    case "leave" -> handleLeave();
                    case "resign" -> handleResign();
                    case "move" -> handleMove(parts);
                    case "highlight" -> handleHighlight(parts);
                    default -> System.out.println("Unknown command. Type 'help'.");
                }
            } catch (ResponseException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    private void printHelp() {
        System.out.println("""
                Commands:
                  help                 - show this help
                  redraw               - redraw the board
                  pov                  - flip board perspective
                  move e2 e4           - make move from <from> to <to>
                  highlight e2         - show legal moves for a square
                  leave                - leave game (stay in lobby)
                  resign               - resign game""");
    }

    private void handleLeave() throws ResponseException {
        ws.leave(authToken, gameId);
        inGame = false;
        System.out.println("Left game.");
    }

    private void handleResign() throws ResponseException {
        System.out.print("Are you sure you want to resign? (y/n): ");
        String ans = scanner.nextLine().trim().toLowerCase();
        if (ans.startsWith("y")) {
            ws.resign(authToken, gameId);
        } else {
            System.out.println("Resign cancelled.");
        }
    }

    private void handleMove(String[] parts) throws ResponseException {
        if (parts.length < 3) {
            System.out.println("Usage: move <from> <to>  e.g. move e2 e4");
            return;
        }
        ChessMove move = parseMove(parts[1], parts[2]);
        if (move == null) {
            System.out.println("Invalid square(s). Use a1..h8.");
            return;
        }
        ws.makeMove(authToken, gameId, move);
        // server will respond with LOAD_GAME or ERROR
    }

    private void handleHighlight(String[] parts) {
        if (game == null) {
            System.out.println("No game loaded yet.");
            return;
        }
        if (parts.length < 2) {
            System.out.println("Usage: highlight <square>  e.g. highlight e2");
            return;
        }
        ChessPosition from = parseSquare(parts[1]);
        if (from == null) {
            System.out.println("Invalid square.");
            return;
        }
        Collection<ChessMove> legal = game.validMoves(from);
        drawBoardWithHighlights(legal);
    }

    private ChessMove parseMove(String fromStr, String toStr) {
        ChessPosition from = parseSquare(fromStr);
        ChessPosition to = parseSquare(toStr);
        if (from == null || to == null) return null;
        return new ChessMove(from, to, null); // handle promotion if needed
    }

    private ChessPosition parseSquare(String s) {
        if (s.length() != 2) return null;
        char file = Character.toLowerCase(s.charAt(0));
        char rank = s.charAt(1);
        if (file < 'a' || file > 'h') return null;
        if (rank < '1' || rank > '8') return null;
        int col = file - 'a' + 1;
        int row = rank - '1' + 1;
        return new ChessPosition(row, col);
    }

    private void drawBoard(boolean highlight) {
        if (game == null) {
            System.out.println("Waiting for game state from server...");
            return;
        }
        ChessBoard board = game.getBoard();
        // TODO: use your existing ASCII board renderer and EscapeSequences here
        // This placeholder just prints piece letters.
        boolean whitePerspective = (playerColor == ChessGame.TeamColor.WHITE);
        int startRow = whitePerspective ? 8 : 1;
        int endRow = whitePerspective ? 1 : 8;
        int stepRow = whitePerspective ? -1 : 1;

        int startCol = whitePerspective ? 1 : 8;
        int endCol = whitePerspective ? 8 : 1;
        int stepCol = whitePerspective ? 1 : -1;

        for (int r = startRow; whitePerspective ? r >= endRow : r <= endRow; r += stepRow) {
            System.out.print(r + " ");
            for (int c = startCol; whitePerspective ? c <= endCol : c >= endCol; c += stepCol) {
                var piece = board.getPiece(new ChessPosition(r, c));
                char ch = '.';
                if (piece != null) {
                    ch = piece.getTeamColor() == ChessGame.TeamColor.WHITE
                            ? Character.toUpperCase(piece.getPieceType().toString().charAt(0))
                            : Character.toLowerCase(piece.getPieceType().toString().charAt(0));
                }
                System.out.print(ch + " ");
            }
            System.out.println();
        }
        System.out.println("  a b c d e f g h");
    }

    private void drawBoardWithHighlights(Collection<ChessMove> moves) {
        // You can enhance drawBoard to color squares in 'moves' using EscapeSequences
        drawBoard(true);
    }

    // NotificationHandler implementation: called when ServerMessage arrives
    @Override
    public void notify(ServerMessage serverMessage) {
        switch (serverMessage.getServerMessageType()) {
            case LOAD_GAME -> {
                this.game = serverMessage.getGame();
                System.out.println("Game state updated.");
                drawBoard(false);
            }
            case NOTIFICATION -> System.out.println(serverMessage.getMessage());
            case ERROR -> System.out.println("Server error: " + serverMessage.getErrorMessage());
        }
    }
}
