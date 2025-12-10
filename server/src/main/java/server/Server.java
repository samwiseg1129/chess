package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;
import dataaccess.MemoryDataAccess;
import dataaccess.MySqlDataAccess;
import io.javalin.Javalin;
import service.*;
import service.requests.*;

public class Server {
    private final Gson gson = new Gson();
    private final Javalin javalin;
    private final DataAccess dao;
    private final UserService userService;
    private final GameService gameService;
    private final ClearService clearService;

    public Server() {
        try {
            DatabaseManager.loadPropertiesFromResources();
            DatabaseManager.createDatabase();

            try (var conn = DatabaseManager.getConnection()) {
                var stmt = conn.createStatement();
                stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(50) PRIMARY KEY,
                    password VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL
                )""");
                stmt.execute("""
                CREATE TABLE IF NOT EXISTS auth (
                    auth_token VARCHAR(50) PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    FOREIGN KEY (username) REFERENCES users(username)
                )""");
                stmt.execute("""
                CREATE TABLE IF NOT EXISTS games (
                    game_id INT PRIMARY KEY,
                    game_name VARCHAR(100) NOT NULL,
                    game_state JSON NOT NULL,
                    white_username VARCHAR(50),
                    black_username VARCHAR(50),
                    FOREIGN KEY (white_username) REFERENCES users(username),
                    FOREIGN KEY (black_username) REFERENCES users(username)
                )""");
            }
        } catch (Exception e) {
            throw new RuntimeException("Database initialization failed", e);
        }

        dao = new MySqlDataAccess();
        userService = new UserService(dao);
        gameService = new GameService(dao);
        clearService = new ClearService(dao);

        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        // clear
        javalin.delete("/db", ctx -> {
            try {
                clearService.clear();
                ctx.status(200).result("{}");
            } catch (DataAccessException e) {
                setError(ctx, e);
            }
        });

        // register
        javalin.post("/user", ctx -> {
            try {
                var req = gson.fromJson(ctx.body(), RegisterRequest.class);
                var result = userService.register(req);
                ctx.status(200).json(gson.toJson(result));
            } catch (DataAccessException e) {
                setError(ctx, e);
            }
        });

        // login
        javalin.post("/session", ctx -> {
            try {
                var req = gson.fromJson(ctx.body(), LoginRequest.class);
                var result = userService.login(req);
                ctx.status(200).json(gson.toJson(result));
            } catch (DataAccessException e) {
                setError(ctx, e);
            }
        });

        // logout
        javalin.delete("/session", ctx -> {
            try {
                String token = ctx.header("authorization");
                var req = new LogoutRequest(token);
                userService.logout(req);
                ctx.status(200).result("{}");
            } catch (DataAccessException e) {
                setError(ctx, e);
            }
        });

        // list games
        javalin.get("/game", ctx -> {
            try {
                String token = ctx.header("authorization");
                var result = gameService.listGames(token);
                ctx.status(200).json(gson.toJson(result));
            } catch (DataAccessException e) {
                setError(ctx, e);
            }
        });

        // create game
        javalin.post("/game", ctx -> {
            try {
                String token = ctx.header("authorization");
                var req = gson.fromJson(ctx.body(), CreateGameRequest.class);
                var result = gameService.createGame(token, req);
                ctx.status(200).json(gson.toJson(result));
            } catch (DataAccessException e) {
                setError(ctx, e);
            }
        });

        // join game
        javalin.put("/game", ctx -> {
            try {
                String token = ctx.header("authorization");
                var req = gson.fromJson(ctx.body(), JoinGameRequest.class);
                if (ctx.body().length() == 1) {
                    gameService.joinObserver(token, req);
                } else {
                    gameService.joinGame(token, req);
                }
                var updated = dao.getGame(req.gameID());
                ctx.status(200).json(gson.toJson(updated));
            } catch (DataAccessException e) {
                setError(ctx, e);
            }
        });
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    private void setError(io.javalin.http.Context ctx, DataAccessException e) {
        String msg = e.getMessage();
        int status = switch (msg) {
            case "Error: bad request" -> 400;
            case "Error: unauthorized" -> 401;
            case "Error: already taken" -> 403;
            case "Error: Unauthorized" -> 401;
            case "Error: Already taken" -> 403;
            default -> 500;
        };
        // there are some dataAccessExceptions that get raised where I should be including ServiceExceptions instead. Found in game and user services respectively.
        ctx.status(status).json(gson.toJson(new ErrorResult(msg)));
    }

    public record ErrorResult(String message) {
    }

    public void stop() {
        javalin.stop();
    }
}
