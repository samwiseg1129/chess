package server.websocket;

import chess.ChessGame;
import chess.ChessMove;
import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import io.javalin.websocket.WsContext;

import io.javalin.websocket.WsMessageContext;
import model.AuthData;
import model.GameData;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

public class WebSocketHandler {

    private static final Gson gson = new Gson();

    private final DataAccess dataAccess;
    private final ConnectionManager connectionManager = new ConnectionManager();

    public WebSocketHandler(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public void onOpen(WsContext ctx) {}

    public void onMessage(WsMessageContext ctx) {   // CHANGED: WsMessageContext
        String message = ctx.message();            // VALID in Javalin 6

        try {
            UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
            handleCommand(ctx, command);          // handleCommand can take WsContext
        } catch (Exception ex) {
            sendError(ctx, "Error: invalid command: " + ex.getMessage());
        }
    }

    public void onClose(WsContext ctx) {
        connectionManager.remove(ctx);
    }

    public void onError(WsContext ctx) {
        sendError(ctx, "Error: websocket failure");
        connectionManager.remove(ctx);
    }

    private void handleCommand(WsContext ctx, UserGameCommand command) {
        try {
            switch (command.getCommandType()) {
                case CONNECT -> handleConnect(ctx, command);
                case MAKE_MOVE -> handleMakeMove(ctx, command);
                case LEAVE -> handleLeave(ctx, command);
                case RESIGN -> handleResign(ctx, command);
                default -> sendError(ctx, "Error: unknown command");
            }
        } catch (DataAccessException ex) {
            sendError(ctx, ex.getMessage());
        } catch (Exception ex) {
            sendError(ctx, "Error: " + ex.getMessage());
        }
    }

    private void handleConnect(WsContext ctx, UserGameCommand cmd) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(cmd.getAuthToken());
        GameData gameData = dataAccess.getGame(cmd.getGameID());
        ChessGame.TeamColor color = determineColorForUser(gameData, auth.username());

        connectionManager.add(cmd.getGameID(), cmd.getAuthToken(), ctx, color);

        ServerMessage load = new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME);
        load.setGame(gameData.game());
        sendToContext(ctx, load);

        ServerMessage note = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION);
        if (color == ChessGame.TeamColor.WHITE || color == ChessGame.TeamColor.BLACK) {
            note.setMessage(auth.username() + " joined as " + color.name().toLowerCase());
        } else {
            note.setMessage(auth.username() + " joined as an observer");
        }

        connectionManager.broadcastToGameExcept(cmd.getGameID(), ctx, note);
    }



    private void handleMakeMove(WsContext ctx, UserGameCommand cmd) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(cmd.getAuthToken());
        GameData gameData = dataAccess.getGame(cmd.getGameID());
        ChessGame game = gameData.game();
        ChessMove move = cmd.getMove();

        ChessGame.TeamColor playerColor = determineColorForUser(gameData, auth.username());
        if (playerColor == null) {
            throw new DataAccessException("Error: observers cannot move");
        }
        if (game.getTeamTurn() != playerColor) {
            throw new DataAccessException("Error: not your turn");
        }

        try {
            game.makeMove(move);
        } catch (chess.InvalidMoveException e) {
            throw new DataAccessException("Error: illegal move", e);
        }

        GameData updated = new GameData(
                gameData.gameID(),
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                game
        );
        dataAccess.updateGame(updated);

        ServerMessage load = new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME);
        load.setGame(game);
        connectionManager.broadcastToGame(cmd.getGameID(), load);

        ServerMessage moveNote = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION);
        moveNote.setMessage(auth.username() + " moved " + moveToString(move));
        connectionManager.broadcastToGame(cmd.getGameID(), moveNote);
    }

    private void handleLeave(WsContext ctx, UserGameCommand cmd) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(cmd.getAuthToken());
        GameData gameData = dataAccess.getGame(cmd.getGameID());
        ChessGame.TeamColor color = determineColorForUser(gameData, auth.username());

        GameData updated = gameData;
        if (color == ChessGame.TeamColor.WHITE) {
            updated = new GameData(
                    gameData.gameID(),
                    null,
                    gameData.blackUsername(),
                    gameData.gameName(),
                    gameData.game()
            );
        } else if (color == ChessGame.TeamColor.BLACK) {
            updated = new GameData(
                    gameData.gameID(),
                    gameData.whiteUsername(),
                    null,
                    gameData.gameName(),
                    gameData.game()
            );
        }
        if (updated != gameData) {
            dataAccess.updateGame(updated);
        }

        connectionManager.remove(cmd.getGameID(), ctx);

        ServerMessage note = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION);
        note.setMessage(auth.username() + " left the game");
        connectionManager.broadcastToGame(cmd.getGameID(), note);
    }

    private void handleResign(WsContext ctx, UserGameCommand cmd) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(cmd.getAuthToken());
        GameData gameData = dataAccess.getGame(cmd.getGameID());
        ChessGame game = gameData.game();

        ChessGame.TeamColor resignColor = determineColorForUser(gameData, auth.username());
        if (resignColor == null) {
            throw new DataAccessException("Error: observers cannot resign");
        }

        GameData updated = new GameData(
                gameData.gameID(),
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                game
        );
        dataAccess.updateGame(updated);

        ChessGame.TeamColor winner =
                (resignColor == ChessGame.TeamColor.WHITE) ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;

        ServerMessage note = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION);
        note.setMessage(auth.username() + " resigned. " +
                (winner == ChessGame.TeamColor.WHITE ? "White" : "Black") + " wins.");
        connectionManager.broadcastToGame(cmd.getGameID(), note);
    }

    private void sendError(WsContext ctx, String errorText) {
        ServerMessage error = new ServerMessage(ServerMessage.ServerMessageType.ERROR);
        error.setErrorMessage(errorText);
        sendToContext(ctx, error);
    }

    private void sendToContext(WsContext ctx, ServerMessage msg) {
        try {
            ctx.send(gson.toJson(msg));
        } catch (Exception ignored) {
        }
    }

    private ChessGame.TeamColor determineColorForUser(GameData game, String username) {
        if (username.equals(game.whiteUsername())) return ChessGame.TeamColor.WHITE;
        if (username.equals(game.blackUsername())) return ChessGame.TeamColor.BLACK;
        return null; // observer
    }

    private String moveToString(ChessMove move) {
        return squareToString(move.getStartPosition()) +
                squareToString(move.getEndPosition());
    }

    private String squareToString(chess.ChessPosition pos) {
        char file = (char) ('a' + pos.getColumn() - 1);
        char rank = (char) ('1' + pos.getRow() - 1);
        return "" + file + rank;
    }
}
