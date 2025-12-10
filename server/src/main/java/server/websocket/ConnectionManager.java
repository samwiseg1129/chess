package server.websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import websocket.messages.ServerMessage;
import io.javalin.websocket.WsContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ConnectionManager {

    private static class ClientConnection {
        final String authToken;
        final WsContext ctx;
        final ChessGame.TeamColor color;

        ClientConnection(String authToken, WsContext ctx, ChessGame.TeamColor color) {
            this.authToken = authToken;
            this.ctx = ctx;
            this.color = color;
        }
    }

    private final Map<Integer, Map<String, ClientConnection>> games = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    private String key(WsContext ctx) {
        return ctx.session.toString();
    }

    public void add(int gameId, String authToken, WsContext ctx, ChessGame.TeamColor color) {
        games.computeIfAbsent(gameId, id -> new ConcurrentHashMap<>())
                .put(key(ctx), new ClientConnection(authToken, ctx, color)); // CHANGED
    }

    public void remove(WsContext ctx) {
        String k = key(ctx); // CHANGED
        games.values().forEach(map -> map.remove(k));
        games.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    public void remove(int gameId, WsContext ctx) {
        String k = key(ctx); // CHANGED
        var gameMap = games.get(gameId);
        if (gameMap != null) {
            gameMap.remove(k);
            if (gameMap.isEmpty()) {
                games.remove(gameId);
            }
        }
    }

    public void broadcastToGame(int gameId, ServerMessage message) {
        var gameMap = games.get(gameId);
        if (gameMap == null) return;

        String json = gson.toJson(message);
        for (var entry : gameMap.values()) {
            send(entry.ctx, json);
        }
    }

    public void sendToColor(int gameId, ChessGame.TeamColor color, ServerMessage message) {
        var gameMap = games.get(gameId);
        if (gameMap == null) return;

        String json = gson.toJson(message);
        for (var connection : gameMap.values()) {
            if (connection.color == color) {
                send(connection.ctx, json);
            }
        }
    }

    private void send(WsContext ctx, String json) {
        try {
            ctx.send(json);
        } catch (Exception ignored) {
            try {
                ctx.session.close();
            } catch (Exception ignored2) {
            }
            remove(ctx);
        }
    }
    public void broadcastToGameExcept(int gameId, WsContext except, ServerMessage message) {
        var gameMap = games.get(gameId);
        if (gameMap == null) return;

        String json = gson.toJson(message);
        String exceptKey = key(except);
        for (var entry : gameMap.entrySet()) {
            if (!entry.getKey().equals(exceptKey)) {
                send(entry.getValue().ctx, json);
            }
        }
    }
}
