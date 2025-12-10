package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.GameData;
import model.AuthData;
import chess.ChessGame;
import service.requests.CreateGameRequest;
import service.requests.JoinGameRequest;
import service.results.GameListResult;

import java.util.List;
import java.util.UUID;

public class GameService {
    private final DataAccess dao;

    public GameService(DataAccess dao) {
        this.dao = dao;
    }

    public GameListResult listGames(String authToken) throws DataAccessException {
        AuthData auth = dao.getAuth(authToken);
        if (auth == null) throw new DataAccessException("Error: unauthorized");
        List<GameData> games = dao.listGames();
        return new GameListResult(games);
    }

    public GameData createGame(String authToken, CreateGameRequest req) throws DataAccessException {
        AuthData auth = dao.getAuth(authToken);
        if (auth == null) throw new DataAccessException("Error: unauthorized");

        int gameID = Math.abs(UUID.randomUUID().hashCode());
        GameData game = new GameData(gameID, null, null, req.gameName(), new ChessGame());
        dao.createGame(game);
        return game;
    }

    public void joinGame(String authToken, JoinGameRequest req) throws DataAccessException {
        AuthData auth = dao.getAuth(authToken);
        if (auth == null) throw new DataAccessException("Error: unauthorized");

        GameData game = dao.getGame(req.gameID());
        if (game == null) throw new DataAccessException("Error: bad request");

        String username = auth.username();
        String color = req.playerColor();

        if (color == null) {
            // Observer: no seat needed, just ensure game exists and user is authorized
            // No change to GameData required for public observers
            return;
        }

        if (color.equalsIgnoreCase("WHITE")) {
            if (game.whiteUsername() != null) throw new DataAccessException("Error: already taken");
            game = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
        } else if (color.equalsIgnoreCase("BLACK")) {
            if (game.blackUsername() != null) throw new DataAccessException("Error: already taken");
            game = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
        } else {
            throw new DataAccessException("Error: bad request");
        }

        dao.updateGame(game);
    }

}
