package client;

import client.websocket.NotificationHandler;
import client.websocket.WebsocketCommunicator;
import model.AuthData;
import model.GameData;
import ui.BoardMaker;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import exception.ResponseException;
import websocket.messages.ServerMessage;
import chess.ChessMove;
import chess.ChessPosition;

public class ChessClient {
    private enum State {
        PRELOGIN, POSTLOGIN
    }

    private final ServerFacade facade;
    private final Scanner scanner = new Scanner(System.in);
    private State state = State.PRELOGIN;
    private AuthData currentAuth;
    private List<GameData> listOfGames = new ArrayList<>();
    private WebsocketCommunicator ws;
    private GameData currentGame;
    private String currentColor;
    private boolean inGame = false;
    private boolean gameOver = false;

    public ChessClient(ServerFacade facade) {
        this.facade = facade;
    }

    public void run() {
        System.out.println("Welcome to Chess!");
        while (true) {
            if (state == State.PRELOGIN) {
                preloginPrompt();
            } else {
                postloginPrompt();
            }
        }
    }

    private void preloginPrompt() {
        System.out.print("[PRELOGIN] Enter command (help/login/register/quit): ");
        String line = scanner.nextLine().trim().toLowerCase();
        switch (line) {
            case "help" -> preloginHelp();
            case "login" -> handleLogin();
            case "register" -> handleRegister();
            case "quit" -> System.exit(0);
            default -> System.out.println("Unknown command. Type 'help'.");
        }
    }

    private void postloginPrompt() {
        System.out.print("[POSTLOGIN] Enter command (help/logout/create game/list games/play game/observe game): ");
        String line = scanner.nextLine().trim().toLowerCase();
        switch (line) {
            case "help" -> postloginHelp();
            case "logout" -> handleLogout();
            case "create game" -> handleCreateGame();
            case "list games" -> handleListGames();
            case "play game" -> handlePlayGame();
            case "observe game" -> handleObserveGame();
            default -> System.out.println("Unknown command. Type 'help'.");
        }
    }

    private void preloginHelp() {
        System.out.println("Available commands:");
        System.out.println("  help     - Display this help message");
        System.out.println("  login    - Log in to your account");
        System.out.println("  register - Create a new account");
        System.out.println("  quit     - Exit the application");
    }

    private void postloginHelp() {
        System.out.println("Available commands:");
        System.out.println("  help         - Display this help message");
        System.out.println("  logout       - Log out of your account");
        System.out.println("  create game  - Create a new chess game");
        System.out.println("  list games   - List all available games");
        System.out.println("  play game    - Join a game as a player");
        System.out.println("  observe game - Watch a game as an observer");
    }

    private void handleLogin() {
        try {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();
            currentAuth = facade.login(username, password);
            state = State.POSTLOGIN;
            System.out.println("Logged in as " + currentAuth.username());
        } catch (Exception e) {
            System.out.println("Login failed. Please try again.");
        }
    }

    private void handleRegister() {
        try {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();
            System.out.print("Email: ");
            String email = scanner.nextLine().trim();
            currentAuth = facade.register(username, password, email);
            state = State.POSTLOGIN;
            System.out.println("Registered and logged in as " + currentAuth.username());
        } catch (Exception e) {
            System.out.println("Registration failed. Please try again.");
        }
    }

    private void handleLogout() {
        try {
            facade.logout(currentAuth.authToken());
            currentAuth = null;
            state = State.PRELOGIN;
            System.out.println("Logged out.");
        } catch (Exception e) {
            System.out.println("Logout failed.");
        }
    }

    private void handleCreateGame() {
        try {
            System.out.print("Game name: ");
            String gameName = scanner.nextLine().trim();
            GameData game = facade.createGame(currentAuth.authToken(), gameName);
            System.out.println("Created game: " + gameName);
        } catch (Exception e) {
            System.out.println("Could not create game.");
        }
    }

    private void handleListGames() {
        try {
            listOfGames = facade.listGames(currentAuth.authToken());
            for (int i = 0; i < listOfGames.size(); i++) {
                GameData g = listOfGames.get(i);
                System.out.printf("%d. %s (white: %s, black: %s)%n",
                        i + 1, g.gameName(), g.whiteUsername(), g.blackUsername());
            }
        } catch (Exception e) {
            System.out.println("Could not list games.");
        }
    }

    /**
     * Create the WebsocketCommunicator once and wire NotificationHandler so server messages
     * trigger board updates and notifications.
     */
    private void ensureWebsocket() throws ResponseException {
        if (ws != null) {
            return;
        }

        NotificationHandler handler = (serverMessage) -> {
            switch (serverMessage.getServerMessageType()) {
                case LOAD_GAME -> {
                    System.out.println("\n=== CLIENT: Received LOAD_GAME message ===");
                    var game = serverMessage.getGame();
                    if (game == null) {
                        System.out.println("LOAD_GAME with no game.");
                        return;
                    }

                    System.out.println("CLIENT: Updating game state...");
                    if (currentGame != null) {
                        currentGame = new GameData(
                                currentGame.gameID(),
                                currentGame.whiteUsername(),
                                currentGame.blackUsername(),
                                currentGame.gameName(),
                                game
                        );
                    }

                    System.out.println("CLIENT: Drawing board...");
                    String perspective = getCurrentColor();
                    if (perspective == null) {
                        perspective = "WHITE";
                    }

                    System.out.println("\n--- BOARD UPDATE ---");
                    BoardMaker.drawBoard(game.getBoard(), perspective);
                    System.out.println("--- END UPDATE ---\n");
                }

                case NOTIFICATION -> {
                    // Print every notification the server sends
                    String msg = serverMessage.getMessage();
                    System.out.println(msg);

                    // Mark game over for resign/checkmate/stalemate notifications
                    String lowerMsg = msg.toLowerCase();
                    if (lowerMsg.contains("resigned")
                            || lowerMsg.contains("checkmate")
                            || lowerMsg.contains("stalemate")) {
                        gameOver = true;
                        System.out.println("Game is over. No more moves can be made.");
                    }
                }

                case ERROR -> {
                    System.out.println("Server error: " + serverMessage.getMessage());
                }

                default -> {
                    System.out.println("Unknown server message: " + serverMessage.getServerMessageType());
                }
            }
        };

        ws = new WebsocketCommunicator(facade.getBaseUrl(), handler);
    }


    public String getCurrentColor() {
        return currentColor;
    }

    public GameData getCurrentGame() {
        return currentGame;
    }

    private void handlePlayGame() {
        if (listOfGames.isEmpty()) {
            System.out.println("No games loaded. Use 'list games' first.");
            return;
        }

        try {
            System.out.print("Enter game number: ");
            int index = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (index < 0 || index >= listOfGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }

            GameData selected = listOfGames.get(index);
            System.out.print("Color (white/black): ");
            String color = scanner.nextLine().trim().toLowerCase();

            facade.joinGame(currentAuth.authToken(), selected.gameID(), color);

            currentGame = selected;
            currentColor = color.equals("white") ? "WHITE" : "BLACK";
            inGame = true;
            gameOver = false;  // NEW: Reset game over flag

            ensureWebsocket();
            ws.connect(currentAuth.authToken(), currentGame.gameID());

            BoardMaker.drawInitialBoardForColor(color);
            System.out.println("Joined game #" + (index + 1) + " as " + color + ".");
            gameLoop();
        } catch (ResponseException e) {
            System.out.println("Websocket error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Could not join game.");
        }
    }

    private void handleObserveGame() {
        if (listOfGames.isEmpty()) {
            System.out.println("No games loaded. Use 'list games' first.");
            return;
        }

        try {
            System.out.print("Enter game number: ");
            int index = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (index < 0 || index >= listOfGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }

            GameData selected = listOfGames.get(index);
            currentGame = selected;
            currentColor = null; // observer
            inGame = true;
            gameOver = false;  // NEW: Reset game over flag

            ensureWebsocket();
            ws.connect(currentAuth.authToken(), currentGame.gameID());

            BoardMaker.drawInitialBoardForObserver();
            System.out.println("Observing game #" + (index + 1) + ".");
            gameLoop();
        } catch (ResponseException e) {
            System.out.println("Websocket error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Could not observe game.");
        }
    }

    private void gameLoop() {
        System.out.println("Entering game. Commands: move, redraw, highlight, leave, resign, help.");
        while (inGame) {
            System.out.print("[GAME] Enter command: ");
            String cmd = scanner.nextLine().trim().toLowerCase();
            switch (cmd) {
                case "move" -> handleMove();
                case "redraw" -> handleRedraw();
                case "highlight" -> handleLegalMoves();
                case "leave" -> handleLeave();
                case "resign" -> handleResign();
                case "help" -> printGameHelp();
                default -> System.out.println("Unknown command.");
            }
        }
    }

    private void printGameHelp() {
        System.out.println("Game commands:");
        System.out.println("  move      - Make a move (format: e2 e4)");
        System.out.println("  redraw    - Redraw the chess board");
        System.out.println("  highlight - Highlight legal moves for a piece");
        System.out.println("  leave     - Leave the game and return to main menu");
        System.out.println("  resign    - Forfeit the game (you stay in the game as observer)");
        System.out.println("  help      - Display this help message");
        System.out.println();
        System.out.println("Move format: Type starting square and ending square (e.g., 'e2 e4')");
        System.out.println("Square format: Use letters a-h for columns and numbers 1-8 for rows");
    }

//    private void handleMove() {
//        if (currentGame == null) {
//            System.out.println("Not in a game.");
//            return;
//        }
//
//        if (gameOver) {
//            System.out.println("Game is over. No more moves can be made.");
//            return;
//        }
//
//        if (currentColor == null) {
//            System.out.println("Observers cannot make moves.");
//            return;
//        }
//
//        try {
//            System.out.print("Enter move (e.g. e2 e4): ");
//            String[] parts = scanner.nextLine().trim().split("\\s+");
//            if (parts.length != 2) {
//                System.out.println("Invalid format. Use: e2 e4");
//                return;
//            }
//
//            ChessPosition from = parseLetters(parts[0]);
//            ChessPosition to = parseLetters(parts[1]);
//            if (from == null || to == null) {
//                System.out.println("Invalid squares. Use a-h and 1-8.");
//                return;
//            }
//
//            ChessMove move = new ChessMove(from, to, null);
//            ws.makeMove(currentAuth.authToken(), currentGame.gameID(), move);
//        } catch (ResponseException e) {
//            System.out.println("Move failed: " + e.getMessage());
//        }
//    }
private void handleMove() {
    if (currentGame == null) {
        System.out.println("Not in a game.");
        return;
    }

    if (gameOver) {
        System.out.println("Game is over. No more moves can be made.");
        return;
    }

    if (currentColor == null) {
        System.out.println("Observers cannot make moves.");
        return;
    }

    try {
        System.out.println("DEBUG: Attempting to make move...");
        System.out.print("Enter move (e.g. e2 e4): ");
        String[] parts = scanner.nextLine().trim().split("\\s+");
        if (parts.length != 2) {
            System.out.println("Invalid format. Use: e2 e4");
            return;
        }

        ChessPosition from = parseLetters(parts[0]);
        ChessPosition to = parseLetters(parts[1]);
        if (from == null || to == null) {
            System.out.println("Invalid squares. Use a-h and 1-8.");
            return;
        }

        System.out.println("DEBUG: Parsed move from " + parts[0] + " to " + parts[1]);

        // Determine promotion type if this is a pawn reaching last rank
        var game = currentGame.game();
        var piece = game != null ? game.getBoard().getPiece(from) : null;
        chess.ChessPiece.PieceType promotionType = null;

        if (piece != null && piece.getPieceType() == chess.ChessPiece.PieceType.PAWN) {
            boolean isWhite = piece.getTeamColor() == chess.ChessGame.TeamColor.WHITE;
            int targetRow = to.getRow();
            boolean isPromotionRank = (isWhite && targetRow == 8) || (!isWhite && targetRow == 1);

            if (isPromotionRank) {
                System.out.print("Promote to (q,r,b,n): ");
                String choice = scanner.nextLine().trim().toLowerCase();
                switch (choice) {
                    case "q" -> promotionType = chess.ChessPiece.PieceType.QUEEN;
                    case "r" -> promotionType = chess.ChessPiece.PieceType.ROOK;
                    case "b" -> promotionType = chess.ChessPiece.PieceType.BISHOP;
                    case "n" -> promotionType = chess.ChessPiece.PieceType.KNIGHT;
                    default -> {
                        System.out.println("Invalid promotion choice. Defaulting to queen.");
                        promotionType = chess.ChessPiece.PieceType.QUEEN;
                    }
                }
            }
        }

        ChessMove move = new ChessMove(from, to, promotionType);
        System.out.println("DEBUG: Sending move via WebSocket...");
        ws.makeMove(currentAuth.authToken(), currentGame.gameID(), move);
        System.out.println("DEBUG: Move sent successfully!");
    } catch (ResponseException e) {
        System.out.println("Move failed: " + e.getMessage());
        System.out.println("DEBUG: ResponseException caught in handleMove");
        e.printStackTrace();
    } catch (Exception e) {
        System.out.println("Unexpected error in handleMove: " + e.getMessage());
        e.printStackTrace();
    }
}



    private ChessPosition parseLetters(String sq) {
        if (sq == null || sq.length() != 2) {
            return null;
        }
        char fileChar = Character.toLowerCase(sq.charAt(0));
        char rankChar = sq.charAt(1);
        if (fileChar < 'a' || fileChar > 'h') {
            return null;
        }
        if (rankChar < '1' || rankChar > '8') {
            return null;
        }
        int file = (fileChar - 'a') + 1;
        int rank = rankChar - '0';
        return new ChessPosition(rank, file);
    }

    private void handleLeave() {
        if (currentGame == null) {
            System.out.println("Not in a game.");
            return;
        }

        try {
            ws.leave(currentAuth.authToken(), currentGame.gameID());
            inGame = false;
            currentGame = null;
            currentColor = null;
            gameOver = false;  // Reset flag
            System.out.println("Left game.");
        } catch (ResponseException e) {
            System.out.println("Could not leave: " + e.getMessage());
        }
    }

    private void handleResign() {
        if (currentGame == null) {
            System.out.println("Not in a game.");
            return;
        }

        if (gameOver) {
            System.out.println("Game is already over.");
            return;
        }

        if (currentColor == null) {
            System.out.println("Observers cannot resign.");
            return;
        }

        System.out.print("Are you sure you want to resign? (yes/no): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();

        if (!confirmation.equals("yes") && !confirmation.equals("y")) {
            System.out.println("Resignation cancelled.");
            return;
        }

        try {
            ws.resign(currentAuth.authToken(), currentGame.gameID());
            gameOver = true;

            currentColor = null;
            System.out.println("You have resigned. You can continue observing the game.");
        } catch (ResponseException e) {
            System.out.println("Could not resign: " + e.getMessage());
        }
    }

    private void handleRedraw() {
        if (currentGame == null) {
            System.out.println("Not in a game.");
            return;
        }

        var game = currentGame.game();
        if (game == null) {
            System.out.println("No game state to draw.");
            return;
        }

        String perspective = getCurrentColor();
        if (perspective == null) {
            BoardMaker.drawBoard(game.getBoard(), "WHITE");
        } else {
            BoardMaker.drawBoard(game.getBoard(), perspective);
        }
    }

    private void handleLegalMoves() {
        if (currentGame == null) {
            System.out.println("Not in a game.");
            return;
        }

        var game = currentGame.game();
        if (game == null) {
            System.out.println("No game state loaded yet.");
            return;
        }

        System.out.print("Square to inspect (e.g. e2): ");
        String sqStr = scanner.nextLine().trim();
        ChessPosition from = parseLetters(sqStr);

        if (from == null) {
            System.out.println("Invalid square. Use a-h and 1-8.");
            return;
        }

        var piece = game.getBoard().getPiece(from);
        if (piece == null) {
            System.out.println("No piece on that square.");
            return;
        }

        java.util.Set<ChessPosition> targets = new java.util.HashSet<>();
        for (var move : game.validMoves(from)) {
            targets.add(move.getEndPosition());
        }

        String perspective = getCurrentColor();
        if (perspective == null) {
            BoardMaker.drawBoard(game.getBoard(), "WHITE", targets);
        } else {
            BoardMaker.drawBoard(game.getBoard(), perspective, targets);
        }
    }
}