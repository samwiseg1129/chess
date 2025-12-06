package dataaccess;

import model.UserData;
import model.AuthData;
import model.GameData;
import chess.ChessGame;
import com.google.gson.Gson;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySqlDataAccess implements DataAccess {
    private static final Gson gson = new Gson();

    @Override
    public void clear() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection()) {
            clearAuth(conn);
            clearGames(conn);
            clearUsers(conn);
        } catch (SQLException e) {
            throw new DataAccessException("clear failed", e);
        }
    }

    private void clearUsers(Connection conn) throws SQLException {
        String sql = "DELETE FROM users";
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    private void clearAuth(Connection conn) throws SQLException {
        String sql = "DELETE FROM auth";
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    private void clearGames(Connection conn) throws SQLException {
        String sql = "DELETE FROM games";
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (user.username() == null || user.password() == null) {
            throw new DataAccessException("Error: bad request");
        }

        String sql = "SELECT username FROM users WHERE username = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.username());
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    throw new DataAccessException("Error: already taken");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error checking user", e);
        }

        String insertSql = """
        INSERT INTO users (username, password, email)
        VALUES (?, ?, ?)
        """;
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, user.username());
            stmt.setString(2, user.password());
            stmt.setString(3, user.email());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error creating user", e);
        }
    }


    @Override
    public UserData getUser(String username) throws DataAccessException {
        if (username == null) {
            throw new DataAccessException("Error: bad request");
        }

        String sql = "SELECT username, password, email FROM users WHERE username = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserData(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email")
                    );
                }
                throw new DataAccessException("Error: unauthorized");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error getting user", e);
        }
    }

    @Override
    public AuthData createAuth(String username) throws DataAccessException {
        if (username == null) {
            throw new DataAccessException("Error: bad request");
        }

        String token = java.util.UUID.randomUUID().toString();
        String sql = "INSERT INTO auth (auth_token, username) VALUES (?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.setString(2, username);
            stmt.executeUpdate();
            return new AuthData(token, username);
        } catch (SQLException e) {
            throw new DataAccessException("Error creating auth", e);
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        if (authToken == null) {
            throw new DataAccessException("Error: bad request");
        }

        String sql = "SELECT auth_token, username FROM auth WHERE auth_token = ?";
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
                throw new DataAccessException("Error: unauthorized");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error getting auth", e);
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        String sql = "DELETE FROM auth WHERE auth_token = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, authToken);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error deleting auth", e);
        }
    }

    @Override
    public void createGame(GameData game) throws DataAccessException {
        if (game == null || game.gameName() == null || game.gameName().isBlank()) {
            throw new DataAccessException("Error: bad request");
        }

        String checkSql = "SELECT game_id FROM games WHERE game_id = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(checkSql)) {
            stmt.setInt(1, game.gameID());
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    throw new DataAccessException("Error: already taken");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error checking game existence", e);
        }

        String insertSql = "INSERT INTO games (game_id, game_name, game_state, white_username, black_username) VALUES (?, ?, ?, ?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(insertSql)) {
            stmt.setInt(1, game.gameID());
            stmt.setString(2, game.gameName());
            stmt.setString(3, gson.toJson(game.game()));
            stmt.setString(4, game.whiteUsername());
            stmt.setString(5, game.blackUsername());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error creating game", e);
        }
    }


    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        String sql = """
            SELECT game_id, game_name, game_state, white_username, black_username
            FROM games WHERE game_id = ?
            """;
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, gameID);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ChessGame chessGame = gson.fromJson(rs.getString("game_state"), ChessGame.class);
                    return new GameData(
                            rs.getInt("game_id"),
                            rs.getString("white_username"),
                            rs.getString("black_username"),
                            rs.getString("game_name"),
                            chessGame
                    );
                }
                throw new DataAccessException("Error: bad request");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error getting game", e);
        }
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        List<GameData> games = new ArrayList<>();
        String sql = """
            SELECT game_id, game_name, game_state, white_username, black_username
            FROM games
            """;
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            while (rs.next()) {
                ChessGame chessGame = gson.fromJson(rs.getString("game_state"), ChessGame.class);
                games.add(new GameData(
                        rs.getInt("game_id"),
                        rs.getString("white_username"),
                        rs.getString("black_username"),
                        rs.getString("game_name"),
                        chessGame
                ));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error listing games", e);
        }
        return games;
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        // Check if game exists first
        if (getGame(game.gameID()) == null) {
            throw new DataAccessException("Error: bad request");
        }

        String sql = """
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
        } catch (SQLException e) {
            throw new DataAccessException("Error updating game", e);
        }
    }
}
