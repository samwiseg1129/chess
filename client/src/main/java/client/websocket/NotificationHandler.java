package client.websocket;

import ui.BoardMaker;
import websocket.messages.ServerMessage;

public interface NotificationHandler {
    void notify(ServerMessage serverMessage);

}
