package client;

import client.websocket.NotificationHandler;
import model.AuthData;
import model.GameData;
import ui.BoardMaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import client.websocket.WebsocketCommunicator;
import exception.ResponseException;
import websocket.messages.ServerMessage;

import chess.ChessMove;
import chess.ChessPosition;

public class ChessClient {

    private enum State { PRELOGIN, POSTLOGIN }

    private final ServerFacade facade;
    private final Scanner scanner = new Scanner(System.in);
    private State state = State.PRELOGIN;
    private AuthData currentAuth;
    private List<GameData> ListOfGames = new ArrayList<>();

    private WebsocketCommunicator ws;
    private GameData currentGame;
    private String currentColor;
    private boolean inGame = false;

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
            case "help" -> PreloginHelp();
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
            case "help" -> PostloginHelp();
            case "logout" -> handleLogout();
            case "create game" -> handleCreateGame();
            case "list games" -> handleListGames();
            case "play game" -> handlePlayGame();
            case "observe game" -> handleObserveGame();
            default -> System.out.println("Unknown command. Type 'help'.");
        }
    }

    private void PreloginHelp() {
        System.out.println("Available commands: help, login, register, quit.");
    }

    private void PostloginHelp() {
        System.out.println("Available commands: help, logout, create game, list games, play game, observe game.");
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
            ListOfGames = facade.listGames(currentAuth.authToken());
            for (int i = 0; i < ListOfGames.size(); i++) {
                GameData g = ListOfGames.get(i);
                System.out.printf("%d) %s (white=%s, black=%s)%n",
                        i + 1, g.gameName(), g.whiteUsername(), g.blackUsername());
            }
        } catch (Exception e) {
            System.out.println("Could not list games.");
        }
    }

    private void ensureWebsocket() throws ResponseException {
        if (ws == null) {
            client.websocket.NotificationHandler handler = serverMessage -> {
                switch (serverMessage.getServerMessageType()) {
                    case LOAD_GAME -> {
                        var game = serverMessage.getGame();
                        if (game == null) {
                            System.out.println("LOAD_GAME with no game.");
                            return;
                        }

                        String perspective = getCurrentColor();
                        if (perspective == null) {
                            BoardMaker.drawBoard(game.getBoard(), "WHITE");
                        } else {
                            BoardMaker.drawBoard(game.getBoard(), perspective);
                        }
                    }

                    case NOTIFICATION -> System.out.println(serverMessage.getMessage());

                    case ERROR -> System.out.println("Server error: " + serverMessage.getMessage());

                    default -> System.out.println("Unknown server message: " + serverMessage.getServerMessageType());
                }
            };

            ws = new WebsocketCommunicator(facade.getBaseUrl(), handler);
        }
    }


    public String getCurrentColor() {
        return currentColor;
    }

    public GameData getCurrentGame() {
        return currentGame;
    }

    private void handlePlayGame() {
        if (ListOfGames.isEmpty()) {
            System.out.println("No games loaded. Use 'list games' first.");
            return;
        }

        try {
            System.out.print("Enter game number: ");
            int index = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (index < 0 || index >= ListOfGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }

            GameData selected = ListOfGames.get(index);
            System.out.print("Color (white/black): ");
            String color = scanner.nextLine().trim().toLowerCase();

            facade.joinGame(currentAuth.authToken(), selected.gameID(), color);

            currentGame = selected;
            currentColor = color.equals("white") ? "WHITE" : "BLACK";
            inGame = true;

            ensureWebsocket();
            ws.connect(currentAuth.authToken(), currentGame.gameID());

            BoardMaker.drawInitialBoardForColor(color);
            System.out.println("Joined game " + (index + 1) + " as " + color + ".");

            gameLoop();
        } catch (ResponseException e) {
            System.out.println("Websocket error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Could not join game.");
        }
    }

    private void handleObserveGame() {
        if (ListOfGames.isEmpty()) {
            System.out.println("No games loaded. Use 'list games' first.");
            return;
        }

        try {
            System.out.print("Enter game number: ");
            int index = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (index < 0 || index >= ListOfGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }
            GameData selected = ListOfGames.get(index);

            currentGame = selected;
            currentColor = null;
            inGame = true;

            ensureWebsocket();
            ws.connect(currentAuth.authToken(), currentGame.gameID());

            BoardMaker.drawInitialBoardForObserver();
            System.out.println("Observing game " + (index + 1) + ".");

            gameLoop();
        } catch (ResponseException e) {
            System.out.println("Websocket error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Could not observe game.");
        }
    }

    private void gameLoop() {
        System.out.println("Entering game. Commands: move, leave, resign, help.");
        while (inGame) {
            System.out.print("[GAME] Enter command: ");
            String cmd = scanner.nextLine().trim().toLowerCase();
            switch (cmd) {
                case "move" -> handleMove();
                case "leave" -> handleLeave();
                case "resign" -> handleResign();
                case "help" -> printGameHelp();
                default -> System.out.println("Unknown command.");
            }
        }
    }

    private void printGameHelp() {
        System.out.println("Game commands: move, leave, resign, help.");
        System.out.println("Move format: fromRow fromCol toRow toCol (e.g., e2 e4).");
    }

    private void handleMove() {
        if (currentGame == null) {
            System.out.println("Not in a game.");
            return;
        }
        try {
            System.out.print("Enter move (e.g. e2 e4): ");
            String[] parts = scanner.nextLine().trim().split("\\s+");
            if (parts.length != 2) {
                System.out.println("Invalid format. Use: e2 e4");
                return;
            }

            ChessPosition from = parseLetters(parts[0]);
            ChessPosition to = parseLetters(parts[1]);
            if (from == null || to == null) {
                System.out.println("Invalid square(s). Use a–h and 1–8.");
                return;
            }

            ChessMove move = new ChessMove(from, to, null);
            ws.makeMove(currentAuth.authToken(), currentGame.gameID(), move);
        } catch (ResponseException e) {
            System.out.println("Move failed: " + e.getMessage());
        }
    }

    private ChessPosition parseLetters(String sq) {
        if (sq == null || sq.length() != 2) return null;

        char fileChar = Character.toLowerCase(sq.charAt(0));
        char rankChar = sq.charAt(1);

        if (fileChar < 'a' || fileChar > 'h') return null;
        if (rankChar < '1' || rankChar > '8') return null;

        int file = fileChar - 'a' + 1;
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
        try {
            ws.resign(currentAuth.authToken(), currentGame.gameID());
            inGame = false;
            currentGame = null;
            currentColor = null;
            System.out.println("You resigned.");
        } catch (ResponseException e) {
            System.out.println("Could not resign: " + e.getMessage());
        }
    }
}
