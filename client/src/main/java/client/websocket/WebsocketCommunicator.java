package client.websocket;

import com.google.gson.Gson;
import exception.ResponseException;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import jakarta.websocket.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class WebsocketCommunicator extends Endpoint {

    private Session session;
    private final Gson gson = new Gson();
    private final NotificationHandler notificationHandler;

    public WebsocketCommunicator(String baseUrl, NotificationHandler notificationHandler) throws ResponseException {
        try {
            String wsUrl = baseUrl.replace("http", "ws") + "/ws";
            URI socketURI = new URI(wsUrl);
            this.notificationHandler = notificationHandler;

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            this.session = container.connectToServer(this, socketURI);

            this.session.addMessageHandler((MessageHandler.Whole<String>) message -> {
                ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);
                notificationHandler.notify(serverMessage);
            });
        } catch (DeploymentException | IOException | URISyntaxException ex) {
            throw new ResponseException(ResponseException.Code.SERVER_ERROR, ex.getMessage());
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {}

    private void sendCommand(UserGameCommand command) throws ResponseException {
        try {
            String json = gson.toJson(command);
            session.getBasicRemote().sendText(json);
        } catch (IOException ex) {
            throw new ResponseException(ResponseException.Code.SERVER_ERROR, ex.getMessage());
        }
    }

    public void connect(String authToken, int gameId) throws ResponseException {
        var cmd = new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameId);
        sendCommand(cmd);
    }

    public void makeMove(String authToken, int gameId, chess.ChessMove move) throws ResponseException {
        var cmd = new UserGameCommand(UserGameCommand.CommandType.MAKE_MOVE, authToken, gameId, move);
        sendCommand(cmd);
    }

    public void leave(String authToken, int gameId) throws ResponseException {
        var cmd = new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, gameId);
        sendCommand(cmd);
    }

    public void resign(String authToken, int gameId) throws ResponseException {
        var cmd = new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, gameId);
        sendCommand(cmd);
    }
}
