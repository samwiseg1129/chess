package service;

import static org.junit.jupiter.api.Assertions.*;
import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.Test;
import service.requests.*;
import service.results.CreateGameResult;
import service.results.GameListResult;
import service.results.LoginResult;
import service.results.RegisterResult;
import java.util.UUID;



public class ServiceTests {
    private MemoryDataAccess dao;

//GAME SERVICE TESTS
    @Test
    public void clearTest() {
        dao = new MemoryDataAccess();
        var clearService = new ClearService(dao);
        try {
            dao.createUser(new UserData("sam", "code", "christensen@gmail.com"));
            clearService.clear();
        }
        catch(DataAccessException e){
            throw new AssertionError(e.getMessage());
        }
        assertThrows(DataAccessException.class,() -> dao.getUser("sam"));
    }


    @Test
    public void createGamePositive() throws DataAccessException {
        DataAccess dao = new MemoryDataAccess();
        dao.clear();

        AuthData auth = dao.createAuth("testuser");
        String token = auth.authToken();  // Get the actual token

        CreateGameRequest request = new CreateGameRequest("testgame");
        GameService service = new GameService(dao);
        CreateGameResult result = service.createGame(token, request);

        assertTrue(result.gameID() > 0);  // Also fixed > 1 to > 0 (hashCode can be 1)
    }

    @Test
    public void createGameNegative() {
        DataAccess dao = new MemoryDataAccess();
        CreateGameRequest request = new CreateGameRequest("testgame");
        GameService service = new GameService(dao);

        assertThrows(DataAccessException.class, () -> {
            service.createGame("invalid", request);
        });
    }

    @Test
    public void joinGamePositive() throws DataAccessException {
        DataAccess dao = new MemoryDataAccess();
        dao.clear();

        AuthData auth = dao.createAuth("whiteuser");
        String token = auth.authToken();

        int gameID = Math.abs(UUID.randomUUID().hashCode());
        GameData game = new GameData(gameID, null, null, "testgame", new ChessGame());
        dao.createGame(game);

        JoinGameRequest request = new JoinGameRequest("WHITE", gameID);
        GameService service = new GameService(dao);
        service.joinGame(token, request);

        GameData updated = dao.getGame(gameID);
        assertEquals("whiteuser", updated.whiteUsername());
    }

    @Test
    public void joinGameNegative() throws DataAccessException {
        DataAccess dao = new MemoryDataAccess();
        dao.clear();
        String token1 = UUID.randomUUID().toString();
        dao.createAuth("player1");
        String token2 = UUID.randomUUID().toString();
        dao.createAuth("player2");

        int gameID = Math.abs(UUID.randomUUID().hashCode());
        GameData game = new GameData(gameID, "player1", null, "testgame", new ChessGame());
        dao.createGame(game);

        JoinGameRequest request = new JoinGameRequest("WHITE", gameID);
        GameService service = new GameService(dao);

        assertThrows(DataAccessException.class, () -> {
            service.joinGame(token2, request);
        });
    }
    @Test
    public void listGames_validAuth_returnsGames() throws DataAccessException {
        DataAccess dao = new MemoryDataAccess();
        dao.clear();

        AuthData auth = dao.createAuth("testuser");
        String token = auth.authToken();

        GameService service = new GameService(dao);
        GameListResult result = service.listGames(token);

        assertNotNull(result);
        assertEquals(0, result.games().size());
    }


    @Test
    public void listGames_invalidAuth_throwsException() {
        DataAccess dao = new MemoryDataAccess();
        GameService service = new GameService(dao);

        assertThrows(DataAccessException.class, () -> {
            service.listGames("invalid");
        });
    }



    // USER SERVICE TESTS
    @Test
    public void registerPositive() throws DataAccessException {
        DataAccess dao = new MemoryDataAccess();
        dao.clear();
        RegisterRequest request = new RegisterRequest("testuser", "password", "test@email.com");
        UserService service = new UserService(dao);
        RegisterResult result = service.register(request);

        assertNotNull(result.authToken());
        assertEquals("testuser", result.username());
    }

    @Test
    public void registerNegative() throws DataAccessException {
        DataAccess dao = new MemoryDataAccess();
        dao.clear();
        RegisterRequest request = new RegisterRequest("testuser", "password", "test@email.com");
        UserService service = new UserService(dao);
        service.register(request);

        assertThrows(DataAccessException.class, () -> {
            service.register(request);
        });
    }

    @Test
    public void loginPositive() throws DataAccessException {
        DataAccess dao = new MemoryDataAccess();
        dao.clear();
        RegisterRequest regReq = new RegisterRequest("testuser", "password", "test@email.com");
        UserService service = new UserService(dao);
        service.register(regReq);

        LoginRequest loginReq = new LoginRequest("testuser", "password");
        LoginResult result = service.login(loginReq);

        assertNotNull(result.authToken());
        assertEquals("testuser", result.username());
    }

    @Test
    public void loginNegative() throws DataAccessException {
        DataAccess dao = new MemoryDataAccess();
        dao.clear();
        RegisterRequest regReq = new RegisterRequest("testuser", "password", "test@email.com");
        UserService service = new UserService(dao);
        service.register(regReq);

        LoginRequest loginReq = new LoginRequest("testuser", "wrongpass");

        assertThrows(DataAccessException.class, () -> {
            service.login(loginReq);
        });
    }

    @Test
    public void logoutPositive() throws DataAccessException {
        DataAccess dao = new MemoryDataAccess();
        dao.clear();
        RegisterRequest regReq = new RegisterRequest("testuser", "password", "test@email.com");
        UserService service = new UserService(dao);
        service.register(regReq);

        AuthData auth = dao.createAuth("testuser");
        LogoutRequest logoutReq = new LogoutRequest(auth.authToken());
        service.logout(logoutReq);

        assertThrows(DataAccessException.class, () -> {
            dao.getAuth(auth.authToken());
        });
    }

    @Test
    public void logoutNegative() {
        DataAccess dao = new MemoryDataAccess();
        UserService service = new UserService(dao);
        LogoutRequest logoutReq = new LogoutRequest("invalid");

        assertThrows(DataAccessException.class, () -> {
            service.logout(logoutReq);
        });
    }
}
