package dataaccess;

import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;
import chess.ChessGame;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class MySqlDataAccess implements DataAccess {

    private final Gson gson = new Gson();

    public MySqlDataAccess() throws DataAccessException {
        DatabaseManager.createDatabase();     // optional, if provided in starter
        createTablesIfNotExists();
    }

    private void createTablesIfNotExists() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {
            try (var stmt = conn.createStatement()) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        username      VARCHAR(50)  NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        email         VARCHAR(255) UNIQUE,
                        PRIMARY KEY (username)
                    )
                    """);

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS auth (
                        auth_token CHAR(36)     NOT NULL,
                        username   VARCHAR(50)  NOT NULL,
                        created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (auth_token),
                        FOREIGN KEY (username) REFERENCES users(username)
                            ON DELETE CASCADE
                    )
                    """);

                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS games (
                        game_id         INT          NOT NULL AUTO_INCREMENT,
                        game_name       VARCHAR(100) NOT NULL,
                        game_state      TEXT         NOT NULL,
                        white_username  VARCHAR(50),
                        black_username  VARCHAR(50),
                        owner_username  VARCHAR(50),
                        PRIMARY KEY (game_id),
                        FOREIGN KEY (white_username) REFERENCES users(username),
                        FOREIGN KEY (black_username) REFERENCES users(username),
                        FOREIGN KEY (owner_username) REFERENCES users(username)
                    )
                    """);
            }
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    //CLEAR
    @Override
    public void clear() throws DataAccessException {
        clearUsers();
        clearAuth();
        clearGames();
    }

    // USERS

    private void clearUsers() throws DataAccessException {
        var sql = "DELETE FROM users";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }


    @Override
    public void createUser(UserData user) throws DataAccessException {
        var sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.username());
            stmt.setString(2, user.password());   // already bcrypt in your service
            stmt.setString(3, user.email());
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        var sql = "SELECT username, password_hash, email FROM users WHERE username = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserData(
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("email")
                    );
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    // AUTH

    private void clearAuth() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("DELETE FROM auth")) {
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    @Override
    public AuthData createAuth(String username) throws DataAccessException {
        var token = UUID.randomUUID().toString();
        var sql = "INSERT INTO auth (auth_token, username) VALUES (?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.setString(2, username);
            stmt.executeUpdate();
            return new AuthData(token, username);
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        var sql = "SELECT auth_token, username FROM auth WHERE auth_token = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, authToken);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AuthData(
                            rs.getString("auth_token"),
                            rs.getString("username")
                    );
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        var sql = "DELETE FROM auth WHERE auth_token = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, authToken);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    // GAMES

    private void clearGames() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("DELETE FROM games")) {
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    @Override
    public void createGame(GameData game) throws DataAccessException {
        var sql = """
                INSERT INTO games (game_name, game_state, white_username, black_username, owner_username)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, game.gameName());
            stmt.setString(2, gson.toJson(game.game())); // ChessGame → JSON
            stmt.setString(3, game.whiteUsername());
            stmt.setString(4, game.blackUsername());
            // no ownerUsername in record → choose a default, e.g. white player as owner
            stmt.setString(5, game.whiteUsername());

            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new DataAccessException("Failed to get generated game id");
                }
                // int gameId = rs.getInt(1); // optional
            }

        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    @Override
    public GameData getGame(int gameId) throws DataAccessException {
        var sql = """
            SELECT game_id, game_name, game_state, white_username, black_username
            FROM games WHERE game_id = ?
            """;
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, gameId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    var gameJson = rs.getString("game_state");
                    ChessGame chessGame = gson.fromJson(gameJson, ChessGame.class);
                    return new GameData(
                            rs.getInt("game_id"),
                            rs.getString("game_name"),
                            chessGame,
                            rs.getString("white_username"),
                            rs.getString("black_username")
                    );
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }

    @Override
    public java.util.List<GameData> listGames() throws DataAccessException {
        var sql = """
        SELECT game_id, game_name, game_state, white_username, black_username
        FROM games
        """;
        var result = new java.util.ArrayList<GameData>();
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {
                ChessGame chessGame = gson.fromJson(rs.getString("game_state"), ChessGame.class);
                result.add(new GameData(
                        rs.getInt("game_id"),
                        rs.getString("game_name"),
                        chessGame,
                        rs.getString("white_username"),
                        rs.getString("black_username")
                ));
            }
            return result;
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }


    @Override
    public void updateGame(GameData game) throws DataAccessException {
        var sql = """
            UPDATE games
            SET game_name = ?, game_state = ?, white_username = ?, black_username = ?
            WHERE game_id = ?
            """;
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, game.gameName());
            stmt.setString(2, gson.toJson(game.game()));
            stmt.setString(3, game.whiteUsername());
            stmt.setString(4, game.blackUsername());
            stmt.setInt(5, game.gameID());
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException(ex.getMessage(), ex);
        }
    }
}
