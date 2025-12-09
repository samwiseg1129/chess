package client;

import model.AuthData;
import model.GameData;
import ui.BoardMaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ChessClient {

    private enum State { PRELOGIN, POSTLOGIN }

    private final ServerFacade facade;
    private final Scanner scanner = new Scanner(System.in);

    private State state = State.PRELOGIN;
    private AuthData currentAuth;
    private List<GameData> lastGames = new ArrayList<>();

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
            lastGames = facade.listGames(currentAuth.authToken());
            for (int i = 0; i < lastGames.size(); i++) {
                GameData g = lastGames.get(i);
                System.out.printf("%d) %s (white=%s, black=%s)%n",
                        i + 1, g.gameName(), g.whiteUsername(), g.blackUsername());
            }
        } catch (Exception e) {
            System.out.println("Could not list games.");
        }
    }

    private void handlePlayGame() {
        if (lastGames.isEmpty()) {
            System.out.println("No games loaded. Use 'list games' first.");
            return;
        }
        try {
            System.out.print("Enter game number: ");
            int index = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (index < 0 || index >= lastGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }
            GameData selected = lastGames.get(index);

            System.out.print("Color (white/black): ");
            String color = scanner.nextLine().trim().toLowerCase();

            GameData joined = facade.joinGame(currentAuth.authToken(), selected.gameID(), color);
            BoardMaker.drawInitialBoardForColor(color);
            System.out.println("Joined game " + joined.gameName() + " as " + color + ".");
        } catch (Exception e) {
            System.out.println("Could not join game.");
        }
    }

    private void handleObserveGame() {
        if (lastGames.isEmpty()) {
            System.out.println("No games loaded. Use 'list games' first.");
            return;
        }
        try {
            System.out.print("Enter game number: ");
            int index = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (index < 0 || index >= lastGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }
            GameData selected = lastGames.get(index);

            GameData joined = facade.joinGame(currentAuth.authToken(), selected.gameID(), null);
            BoardMaker.drawInitialBoardForObserver();
            System.out.println("Observing game " + joined.gameName() + ".");
        } catch (Exception e) {
            System.out.println("Could not observe game.");
        }
    }
}
