//package server;
//
//import com.google.gson.Gson;
//import dataaccess.DataAccess;
//import dataaccess.MemoryDataAccess;
//import dataaccess.DataAccessException;
//import io.javalin.Javalin;
//import service.*;
//import service.requests.*;
//
//public class Server {
//
//    private final Gson gson = new Gson();
//
//    public Javalin run(int port) {
//        DataAccess dao = new MemoryDataAccess();
//        UserService userService = new UserService(dao);
//        GameService gameService = new GameService(dao);
//        ClearService clearService = new ClearService(dao);
//
//        var app = Javalin.create(config -> {
//            config.staticFiles.add(staticFiles -> {
//                staticFiles.hostedPath = "/";
//                staticFiles.directory = "/web";
//                staticFiles.location = io.javalin.http.staticfiles.Location.CLASSPATH;
//            });
//        });
//
//        // clear
//        app.delete("/db", ctx -> {
//            try {
//                clearService.clear();
//                ctx.status(200).result("{}");
//            } catch (DataAccessException e) {
//                setError(ctx, e);
//            }
//        });
//
//        // register
//        app.post("/user", ctx -> {
//            try {
//                var req = gson.fromJson(ctx.body(), RegisterRequest.class);
//                var result = userService.register(req);
//                ctx.status(200).json(gson.toJson(result));
//            } catch (DataAccessException e) {
//                setError(ctx, e);
//            }
//        });
//
//        // login
//        app.post("/session", ctx -> {
//            try {
//                var req = gson.fromJson(ctx.body(), LoginRequest.class);
//                var result = userService.login(req);
//                ctx.status(200).json(gson.toJson(result));
//            } catch (DataAccessException e) {
//                setError(ctx, e);
//            }
//        });
//
//        // logout
//        app.delete("/session", ctx -> {
//            try {
//                String token = ctx.header("authorization");
//                var req = new LogoutRequest(token);
//                userService.logout(req);
//                ctx.status(200).result("{}");
//            } catch (DataAccessException e) {
//                setError(ctx, e);
//            }
//        });
//
//        // list games
//        app.get("/game", ctx -> {
//            try {
//                String token = ctx.header("authorization");
//                var result = gameService.listGames(token);
//                ctx.status(200).json(gson.toJson(result));
//            } catch (DataAccessException e) {
//                setError(ctx, e);
//            }
//        });
//
//        // create game
//        app.post("/game", ctx -> {
//            try {
//                String token = ctx.header("authorization");
//                var req = gson.fromJson(ctx.body(), CreateGameRequest.class);
//                var result = gameService.createGame(token, req);
//                ctx.status(200).json(gson.toJson(result));
//            } catch (DataAccessException e) {
//                setError(ctx, e);
//            }
//        });
//
//        // join game
//        app.put("/game", ctx -> {
//            try {
//                String token = ctx.header("authorization");
//                var req = gson.fromJson(ctx.body(), JoinGameRequest.class);
//                gameService.joinGame(token, req);
//                ctx.status(200).result(gson.toJson("{}"));
//            } catch (DataAccessException e) {
//                setError(ctx, e);
//            }
//        });
//
//        app.start(port);
//        return app;
//    }
//
//    private void setError(io.javalin.http.Context ctx, DataAccessException e) {
//        String msg = e.getMessage();
//        int status = switch (msg) {
//            case "Error: bad request" -> 400;
//            case "Error: unauthorized" -> 401;
//            case "Auth not found" -> 401;
//            case "Error: already taken" -> 403;
//            default -> 500;
//        };
//        ctx.status(status).json(gson.toJson(new ErrorResult(msg)));
//    }
//
//    public record ErrorResult(String message) { }
//
//    public void stop() {
//        javalin.stop();
//    }
//}

package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import dataaccess.DataAccessException;
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
        dao = new MemoryDataAccess();
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
            case "Error: Unauthorize" -> 401;
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
