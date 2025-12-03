package dataaccess;


import model.*;
import java.util.*;

public class MemoryDataAccess implements DataAccess {
    private final Map<String, UserData> users = new HashMap<>();
    private final Map<Integer, GameData> games = new HashMap<>();
    private final Map<String, AuthData> auths = new HashMap<>();

    @Override
    public void clear() {
        users.clear();
        games.clear();
        auths.clear();
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (users.containsKey(user.username())) {
            throw new DataAccessException("Error: already taken");
        }
        if (user.username() == null || user.password() == null) {
            throw new DataAccessException("Error: bad request");
        }
        users.put(user.username(), user);
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        if (!users.containsKey(username)) {
            throw new DataAccessException("Error: unauthorized");
        }
        return users.get(username);
    }

    @Override
    public void createGame(GameData game) throws DataAccessException {
        if (game == null || game.gameName() == null || game.gameName().isBlank()) {
            throw new DataAccessException("Error: bad request");
        }
        if (games.containsKey(game.gameID())) {
            throw new DataAccessException("Error: already taken");
        }
        games.put(game.gameID(), game);
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        GameData game = games.get(gameID);
        if (game == null) throw new DataAccessException("Error: bad request");
        return game;
    }

    @Override
    public List<GameData> listGames() {
        return new ArrayList<>(games.values());
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        if (!games.containsKey(game.gameID())) {
            throw new DataAccessException("Error: bad request");
        }
        games.put(game.gameID(), game);
    }

    @Override
    public AuthData createAuth(String username) throws DataAccessException {
        if (username == null) throw new DataAccessException("Error: bad request");
        var token = UUID.randomUUID().toString();
        var auth = new AuthData(token, username);
        auths.put(token, auth);
        return auth;
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        AuthData auth = auths.get(authToken);
        if (auth == null) throw new DataAccessException("Error: unauthorized");
        return auth;
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        if (auths.remove(authToken) == null) {
            throw new DataAccessException("Error: unauthorized");
        }
    }
}
