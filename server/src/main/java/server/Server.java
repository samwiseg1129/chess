package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
//import dataaccess.MemoryDataAccess;
import dataaccess.DataAccessException;
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
                gameService.joinGame(token, req);
                ctx.status(200).result("{}");
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
