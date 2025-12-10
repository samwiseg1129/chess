package client;

import org.junit.jupiter.api.*;
import server.Server;

import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        int port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        String baseUrl = "http://localhost:" + port;
        facade = new ServerFacade(baseUrl);
    }

    @AfterAll
    public static void stopServer() {
        server.stop();
    }

    @BeforeEach
    public void clearDB() throws Exception {
        facade.clear();
    }


    @Test
    public void registerPositive() throws Exception {
        var auth = facade.register("user1", "password", "u1@email.com");
        assertNotNull(auth.authToken());
        assertTrue(auth.authToken().length() > 5);
        assertEquals("user1", auth.username());
    }

    @Test
    public void registerNegative_duplicateUsername() throws Exception {
        facade.register("user1", "password", "u1@email.com");
        assertThrows(Exception.class, () ->
                facade.register("user1", "password", "other@email.com"));
    }


    @Test
    public void loginPositive() throws Exception {
        facade.register("user1", "password", "u1@email.com");
        var auth = facade.login("user1", "password");
        assertNotNull(auth.authToken());
    }

    @Test
    public void loginNegative_badPassword() throws Exception {
        facade.register("user1", "password", "u1@email.com");
        assertThrows(Exception.class, () ->
                facade.login("user1", "wrong"));
    }


    @Test
    public void logoutPositive() throws Exception {
        var auth = facade.register("user1", "password", "u1@email.com");
        facade.logout(auth.authToken());
        assertThrows(Exception.class, () ->
                facade.listGames(auth.authToken()));
    }

    @Test
    public void logoutNegative_badToken() {
        assertDoesNotThrow(() -> facade.logout("not-a-token"));
    }


    @Test
    public void createGamePositive() throws Exception {
        var auth = facade.register("user1", "password", "u1@email.com");
        var game = facade.createGame(auth.authToken(), "My Game");
        assertNotNull(game);
        assertEquals("My Game", game.gameName());
        assertTrue(game.gameID() > 0);
    }

    @Test
    public void createGameNegative_noAuth() {
        assertThrows(Exception.class, () ->
                facade.createGame(null, "My Game"));
    }


    @Test
    public void listGamesPositive() throws Exception {
        var auth = facade.register("user1", "password", "u1@email.com");
        facade.createGame(auth.authToken(), "Game1");
        facade.createGame(auth.authToken(), "Game2");
        var games = facade.listGames(auth.authToken());
        assertTrue(games.size() >= 2);
    }

    @Test
    public void listGamesNegative_noAuth() {
        assertThrows(Exception.class, () ->
                facade.listGames(null));
    }


    @Test
    public void joinGamePositiveWhite() throws Exception {
        var auth = facade.register("user1", "password", "u1@email.com");
        var game = facade.createGame(auth.authToken(), "Game1");
        var joined = facade.joinGame(auth.authToken(), game.gameID(), "WHITE");
        assertEquals(game.gameID(), joined.gameID());
    }

    @Test
    public void joinGameNegative_badGameId() throws Exception {
        var auth = facade.register("user1", "password", "u1@email.com");
        assertThrows(Exception.class, () ->
                facade.joinGame(auth.authToken(), 999999, "WHITE"));
    }


    @Test
    public void clearPositive() throws Exception {
        var auth = facade.register("user1", "password", "u1@email.com");
        facade.createGame(auth.authToken(), "Game1");
        facade.clear();
        // after clear, listing games should require a new user
        var auth2 = facade.register("user2", "password", "u2@email.com");
        var games = facade.listGames(auth2.authToken());
        assertEquals(0, games.size());
    }

    @Test
    public void clearNegative_doubleClear() throws Exception {
        facade.clear();
        // Calling clear again should not crash; just ensure no exception
        assertDoesNotThrow(() -> facade.clear());
    }
}
