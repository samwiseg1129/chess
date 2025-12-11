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
                .put(key(ctx), new ClientConnection(authToken, ctx, color));
    }

    public void remove(WsContext ctx) {
        String k = key(ctx);
        games.values().forEach(map -> map.remove(k));
        games.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    public void remove(int gameId, WsContext ctx) {
        String k = key(ctx);
        var gameMap = games.get(gameId);
        if (gameMap != null) {
            gameMap.remove(k);
            if (gameMap.isEmpty()) {
                games.remove(gameId);
            }
        }
    }

    public void broadcastToGame(int gameId, ServerMessage message) {
        System.out.println("=== BROADCASTING to game " + gameId);
        var gameMap = games.get(gameId);
        if (gameMap == null) {
            System.err.println("ERROR: No connections found for game " + gameId);
            return;
        }

        System.out.println("=== Found " + gameMap.size() + " connections for game " + gameId);
        String json = gson.toJson(message);

        for (var entry : gameMap.values()) {
            System.out.println("=== Sending to connection...");
            send(entry.ctx, json);
            System.out.println("=== Sent message: " + message.getServerMessageType());
        }
        System.out.println("=== BROADCAST complete");
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
            // Check if session is still open before sending
            if (ctx.session.isOpen()) {
                ctx.send(json);
            } else {
                System.err.println("ERROR: Session is closed, cannot send message");
                remove(ctx);
            }
        } catch (Exception e) {
            System.err.println("ERROR sending message: " + e.getMessage());
            e.printStackTrace();
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
