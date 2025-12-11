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

    public void onOpen(WsContext ctx) {
        System.out.println("=== WebSocket OPEN: " + ctx.session.getRemoteAddress());
    }

    public void onMessage(WsMessageContext ctx) {
        String message = ctx.message();
        System.out.println("=== WebSocket MESSAGE received: " + message);
        try {
            UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
            handleCommand(ctx, command);
        } catch (Exception ex) {
            System.err.println("=== Exception parsing command: " + ex.getMessage());
            ex.printStackTrace();
            sendError(ctx, "Error: invalid command: " + ex.getMessage());
        }
    }

    public void onClose(WsContext ctx) {
        System.out.println("=== WebSocket CLOSED: " + ctx.session.getRemoteAddress());
        connectionManager.remove(ctx);
    }

    public void onError(WsContext ctx) {
        System.err.println("=== WebSocket onError for session: " + ctx.session);
        try {
            if (ctx.session.isOpen()) {
                sendError(ctx, "Error: websocket failure");
            } else {
                System.err.println("Session already closed; not sending error.");
            }
        } catch (Exception e) {
            System.err.println("Error while handling onError: " + e.getMessage());
        } finally {
            connectionManager.remove(ctx);
        }
    }

    private void handleCommand(WsContext ctx, UserGameCommand command) {
        try {
            System.out.println("=== Handling command: " + command.getCommandType());
            switch (command.getCommandType()) {
                case CONNECT -> handleConnect(ctx, command);
                case MAKE_MOVE -> handleMakeMove(ctx, command);
                case LEAVE -> handleLeave(ctx, command);
                case RESIGN -> handleResign(ctx, command);
                default -> sendError(ctx, "Error: unknown command");
            }
            System.out.println("=== Command completed: " + command.getCommandType());
        } catch (DataAccessException ex) {
            System.err.println("=== DataAccessException in handleCommand: " + ex.getMessage());
            ex.printStackTrace();
            sendError(ctx, ex.getMessage());
        } catch (Exception ex) {
            System.err.println("=== Exception in handleCommand: " + ex.getMessage());
            ex.printStackTrace();
            sendError(ctx, "Error: " + ex.getMessage());
        }
    }

    private void handleConnect(WsContext ctx, UserGameCommand cmd) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(cmd.getAuthToken());
        GameData gameData = dataAccess.getGame(cmd.getGameID());
        System.out.println("=== CONNECT: user=" + auth.username()
                + " gameId=" + cmd.getGameID()
                + " teamTurn=" + gameData.game().getTeamTurn());

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
        System.out.println("=== HANDLING MAKE_MOVE for gameID: " + cmd.getGameID());

        AuthData auth = dataAccess.getAuth(cmd.getAuthToken());
        System.out.println("=== Auth retrieved: " + auth.username());

        GameData gameData = dataAccess.getGame(cmd.getGameID());
        ChessGame game = gameData.game();
        System.out.println("=== Game retrieved: " + gameData.gameName());
        System.out.println("=== Team turn at start of handleMakeMove: " + game.getTeamTurn());

        ChessMove move = cmd.getMove();
        System.out.println("=== Move to process: " + moveToString(move));

        if (game.isGameOver()) {
            System.out.println("=== Game is already over; rejecting move.");
            sendError(ctx, "Error: game is over");
            return;
        }

        ChessGame.TeamColor playerColor = determineColorForUser(gameData, auth.username());
        System.out.println("=== Player color: " + playerColor);

        if (playerColor == null) {
            throw new DataAccessException("Error: observers cannot move");
        }

        if (game.getTeamTurn() != playerColor) {
            System.out.println("=== Not player's turn. teamTurn=" + game.getTeamTurn()
                    + " playerColor=" + playerColor);
            throw new DataAccessException("Error: not your turn");
        }

        try {
            game.makeMove(move);
            System.out.println("=== Move applied. New teamTurn: " + game.getTeamTurn());
        } catch (chess.InvalidMoveException e) {
            System.err.println("=== InvalidMoveException: " + e.getMessage());
            throw new DataAccessException("Error: illegal move", e);
        }

        GameData updated = new GameData(
                gameData.gameID(),
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                game
        );
        System.out.println("=== Updating game in DataAccess with teamTurn=" + game.getTeamTurn());
        dataAccess.updateGame(updated);

        ServerMessage load = new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME);
        load.setGame(game);
        connectionManager.broadcastToGame(cmd.getGameID(), load);

        ServerMessage moveNote = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION);
        moveNote.setMessage(auth.username() + " moved " + moveToString(move));
        connectionManager.broadcastToGameExcept(cmd.getGameID(), ctx, moveNote);

        ChessGame.TeamColor opponentColor =
                (game.getTeamTurn() == ChessGame.TeamColor.WHITE)
                        ? ChessGame.TeamColor.BLACK
                        : ChessGame.TeamColor.WHITE;

        System.out.println("=== After move, teamTurn=" + game.getTeamTurn()
                + " opponentColor=" + opponentColor);

        gameData = dataAccess.getGame(cmd.getGameID());
        System.out.println("=== Reloaded game from DataAccess. teamTurn="
                + gameData.game().getTeamTurn());

        String opponentName = (opponentColor == ChessGame.TeamColor.WHITE)
                ? gameData.whiteUsername()
                : gameData.blackUsername();

        if (game.isInCheckmate(opponentColor)) {
            System.out.println("=== Checkmate detected for " + opponentColor);
            game.setGameOver(true);
            GameData finalGame = new GameData(
                    gameData.gameID(),
                    gameData.whiteUsername(),
                    gameData.blackUsername(),
                    gameData.gameName(),
                    game
            );
            dataAccess.updateGame(finalGame);
            ServerMessage checkmateMsg = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION);
            checkmateMsg.setMessage(opponentName + " is in checkmate! Game over.");
            connectionManager.broadcastToGame(cmd.getGameID(), checkmateMsg);
        } else if (game.isInStalemate(opponentColor)) {
            System.out.println("=== Stalemate detected for " + opponentColor);
            game.setGameOver(true);
            GameData finalGame = new GameData(
                    gameData.gameID(),
                    gameData.whiteUsername(),
                    gameData.blackUsername(),
                    gameData.gameName(),
                    game
            );
            dataAccess.updateGame(finalGame);
            ServerMessage stalemateMsg = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION);
            stalemateMsg.setMessage("Stalemate! The game is a draw.");
            connectionManager.broadcastToGame(cmd.getGameID(), stalemateMsg);
        } else if (game.isInCheck(opponentColor)) {
            System.out.println("=== Check detected for " + opponentColor);
            ServerMessage checkMsg = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION);
            checkMsg.setMessage(opponentName + " is in check!");
            connectionManager.broadcastToGame(cmd.getGameID(), checkMsg);
        }
    }

    private void handleLeave(WsContext ctx, UserGameCommand cmd) throws DataAccessException {
        AuthData auth = dataAccess.getAuth(cmd.getAuthToken());
        GameData gameData = dataAccess.getGame(cmd.getGameID());
        ChessGame.TeamColor color = determineColorForUser(gameData, auth.username());
        System.out.println("=== LEAVE: user=" + auth.username()
                + " color=" + color
                + " gameId=" + cmd.getGameID());

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
            System.out.println("=== Updating game on LEAVE");
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
        System.out.println("=== RESIGN: user=" + auth.username()
                + " gameId=" + cmd.getGameID()
                + " teamTurn=" + game.getTeamTurn());

        if (game.isGameOver()) {
            sendError(ctx, "Error: game is over");
            return;
        }

        ChessGame.TeamColor resignColor = determineColorForUser(gameData, auth.username());
        if (resignColor == null) {
            throw new DataAccessException("Error: observers cannot resign");
        }

        game.setGameOver(true);
        GameData updated = new GameData(
                gameData.gameID(),
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                game
        );
        dataAccess.updateGame(updated);

        ChessGame.TeamColor winner =
                (resignColor == ChessGame.TeamColor.WHITE)
                        ? ChessGame.TeamColor.BLACK
                        : ChessGame.TeamColor.WHITE;

        ServerMessage note = new ServerMessage(ServerMessage.ServerMessageType.NOTIFICATION);
        note.setMessage(auth.username() + " resigned. "
                + (winner == ChessGame.TeamColor.WHITE ? "White" : "Black") + " wins.");
        connectionManager.broadcastToGame(cmd.getGameID(), note);
    }

    private void sendError(WsContext ctx, String errorText) {
        System.err.println("=== SENDING ERROR: " + errorText);
        ServerMessage error = new ServerMessage(ServerMessage.ServerMessageType.ERROR);
        error.setErrorMessage(errorText);
        sendToContext(ctx, error);
    }

    private void sendToContext(WsContext ctx, ServerMessage msg) {
        try {
            String json = gson.toJson(msg);
            ctx.send(json);
            System.out.println("=== Sent message: " + msg.getServerMessageType());
        } catch (Exception e) {
            System.err.println("=== ERROR sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ChessGame.TeamColor determineColorForUser(GameData game, String username) {
        if (username.equals(game.whiteUsername())) return ChessGame.TeamColor.WHITE;
        if (username.equals(game.blackUsername())) return ChessGame.TeamColor.BLACK;
        return null;
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
