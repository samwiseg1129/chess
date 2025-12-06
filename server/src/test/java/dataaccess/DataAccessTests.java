package dataaccess;

import model.*;
import chess.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class DataAccessTests {
    private static MySqlDataAccess dao;

    @BeforeAll
    public static void setUpClass() {
        dao = new MySqlDataAccess();
    }

    @BeforeEach
    public void setUp() throws DataAccessException {
        dao.clear();
    }

    @AfterEach
    public void tearDown() throws DataAccessException {
        dao.clear();
    }

    // User Methods

    @Test
    public void createUserPositiveTest() throws DataAccessException {
        UserData user = new UserData("testuser", "testpass", "test@email.com");
        dao.createUser(user);
        UserData retrieved = dao.getUser("testuser");
        assertNotNull(retrieved);
        assertEquals("testuser", retrieved.username());
        assertEquals("testpass", retrieved.password());
        assertEquals("test@email.com", retrieved.email());
    }

    @Test
    public void createUserNegativeTestDuplicate() throws DataAccessException {
        UserData user1 = new UserData("testuser", "testpass", "test@email.com");
        dao.createUser(user1);

        UserData user2 = new UserData("testuser", "differentpass", "different@email.com");
        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            dao.createUser(user2);
        });
        assertEquals("Error: already taken", exception.getMessage());
    }

    @Test
    public void getUserPositiveTest() throws DataAccessException {
        UserData user = new UserData("testuser", "testpass", "test@email.com");
        dao.createUser(user);

        UserData retrieved = dao.getUser("testuser");
        assertNotNull(retrieved);
        assertEquals("testuser", retrieved.username());
        assertEquals("testpass", retrieved.password());
    }

    @Test
    public void getUserNegativeTestNonExistent() {
        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            dao.getUser("nonexistent");
        });
        assertEquals("Error: unauthorized", exception.getMessage());
    }

    // Auth Methods

    @Test
    public void createAuthPositiveTest() throws DataAccessException {
        dao.createUser(new UserData("testuser", "testpass", "test@email.com"));

        AuthData auth = dao.createAuth("testuser");
        assertNotNull(auth);
        assertNotNull(auth.authToken());
        assertEquals("testuser", auth.username());

        AuthData retrieved = dao.getAuth(auth.authToken());
        assertEquals(auth.authToken(), retrieved.authToken());
        assertEquals("testuser", retrieved.username());
    }

    @Test
    public void createAuthNegativeTestNonExistentUser() {
        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            dao.createAuth("nonexistent");
        });
        assertTrue(exception.getMessage().contains("Error creating auth"));
    }

    @Test
    public void getAuthPositiveTest() throws DataAccessException {
        dao.createUser(new UserData("testuser", "testpass", "test@email.com"));
        AuthData auth = dao.createAuth("testuser");

        AuthData retrieved = dao.getAuth(auth.authToken());
        assertNotNull(retrieved);
        assertEquals(auth.authToken(), retrieved.authToken());
        assertEquals("testuser", retrieved.username());
    }

    @Test
    public void getAuthNegativeTestInvalidToken() throws DataAccessException {
        dao.createUser(new UserData("testuser", "testpass", "test@email.com"));
        AuthData auth = dao.createAuth("testuser");

        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            dao.getAuth("invalidtoken");
        });
        assertEquals("Error: unauthorized", exception.getMessage());
    }

    @Test
    public void deleteAuthPositiveTest() throws DataAccessException {
        dao.createUser(new UserData("testuser", "testpass", "test@email.com"));
        AuthData auth = dao.createAuth("testuser");

        assertNotNull(dao.getAuth(auth.authToken()));

        dao.deleteAuth(auth.authToken());

        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            dao.getAuth(auth.authToken());
        });
        assertEquals("Error: unauthorized", exception.getMessage());
    }

    @Test
    public void deleteAuthNegativeTestNonExistent() {
        assertDoesNotThrow(() -> dao.deleteAuth("nonexistenttoken"));
    }

    // Game Methods

    @Test
    public void createGamePositiveTest() throws DataAccessException {
        // Create a minimal valid ChessGame - use default constructor or simplest available
        ChessGame game = new ChessGame(); // Use default constructor
        GameData gameData = new GameData(1, null, null, "Test Game", game);

        dao.createGame(gameData);

        GameData retrieved = dao.getGame(1);
        assertNotNull(retrieved);
        assertEquals("Test Game", retrieved.gameName());
        assertEquals(1, retrieved.gameID());
    }

    @Test
    public void createGameNegativeTestDuplicateID() throws DataAccessException {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(1, null, null, "Test Game", game);

        dao.createGame(gameData);

        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            dao.createGame(gameData);
        });
        assertEquals("Error: already taken", exception.getMessage());
    }

    @Test
    public void getGamePositiveTest() throws DataAccessException {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(1, null, null, "Test Game", game);
        dao.createGame(gameData);

        GameData retrieved = dao.getGame(1);
        assertNotNull(retrieved);
        assertEquals("Test Game", retrieved.gameName());
    }

    @Test
    public void getGameNegativeTestNonExistent() {
        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            dao.getGame(999);
        });
        assertEquals("Error: bad request", exception.getMessage());
    }

    @Test
    public void listGamesPositiveTest() throws DataAccessException {
        ChessGame game1 = new ChessGame();
        ChessGame game2 = new ChessGame();
        dao.createGame(new GameData(1, null, null, "Game 1", game1));
        dao.createGame(new GameData(2, null, null, "Game 2", game2));

        List<GameData> games = dao.listGames();
        assertEquals(2, games.size());
        assertTrue(games.stream().anyMatch(g -> g.gameName().equals("Game 1")));
        assertTrue(games.stream().anyMatch(g -> g.gameName().equals("Game 2")));
    }

    @Test
    public void listGamesNegativeTestEmpty() throws DataAccessException {
        List<GameData> games = dao.listGames();
        assertTrue(games.isEmpty());
    }

    @Test
    public void updateGamePositiveTest() throws DataAccessException {
        // Create users FIRST (required for foreign key constraints)
        dao.createUser(new UserData("player1", "pass1", "player1@email.com"));
        dao.createUser(new UserData("player2", "pass2", "player2@email.com"));

        // Create initial game
        ChessGame initialGame = new ChessGame();
        GameData gameData = new GameData(1, null, null, "Original Name", initialGame);
        dao.createGame(gameData);

        // Update game with valid usernames
        ChessGame updatedGame = new ChessGame();
        GameData updatedData = new GameData(1, "player1", "player2", "Updated Name", updatedGame);
        dao.updateGame(updatedData);

        GameData retrieved = dao.getGame(1);
        assertEquals("Updated Name", retrieved.gameName());
        assertEquals("player1", retrieved.whiteUsername());
        assertEquals("player2", retrieved.blackUsername());
    }


    @Test
    public void updateGameNegativeTestNonExistent() {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(999, null, null, "Test Game", game);

        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            dao.updateGame(gameData);
        });
        assertEquals("Error: bad request", exception.getMessage());
    }

    // Clear Method (Positive test only)

    @Test
    public void clearPositiveTest() throws DataAccessException {
        // Create test data
        dao.createUser(new UserData("testuser", "testpass", "test@email.com"));
        AuthData auth = dao.createAuth("testuser");
        ChessGame game = new ChessGame();
        dao.createGame(new GameData(1, null, null, "Test Game", game));

        // Verify data exists
        assertNotNull(dao.getUser("testuser"));
        assertNotNull(dao.getAuth(auth.authToken()));
        assertNotNull(dao.getGame(1));

        // Clear everything
        dao.clear();

        // Verify everything is gone
        assertThrows(DataAccessException.class, () -> dao.getUser("testuser"));
        assertThrows(DataAccessException.class, () -> dao.getGame(1));
        assertTrue(dao.listGames().isEmpty());
    }
}
