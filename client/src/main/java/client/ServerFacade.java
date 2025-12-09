package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.AuthData;
import model.GameData;
import model.GameList;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ServerFacade {
    private final String baseUrl;
    private final Gson gson = new Gson();

    public ServerFacade(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public AuthData register(String username, String password, String email) throws Exception {
        var body = Map.of("username", username, "password", password, "email", email);
        String json = gson.toJson(body);
        String response = sendRequest("POST", "/user", null, json);
        return gson.fromJson(response, AuthData.class);
    }

    public AuthData login(String username, String password) throws Exception {
        var body = Map.of("username", username, "password", password);
        String json = gson.toJson(body);
        String response = sendRequest("POST", "/session", null, json);
        return gson.fromJson(response, AuthData.class);
    }

    public void logout(String authToken) throws Exception {
        sendRequest("DELETE", "/session", authToken, null);
    }

    public GameData createGame(String authToken, String gameName) throws Exception {
        var body = Map.of("gameName", gameName);
        String json = gson.toJson(body);
        String response = sendRequest("POST", "/game", authToken, json);

        return gson.fromJson(response, GameData.class);
    }

    public List<GameData> listGames(String authToken) throws Exception {
        String response = sendRequest("GET", "/game", authToken, null);
        GameList g = gson.fromJson(response, GameList.class);
        return g.games();
    }

    public GameData joinGame(String authToken, int gameId, String colorOrNull) throws Exception {
        var body = Map.of("gameId", gameId, "playerColor", colorOrNull);
        String json = gson.toJson(body);
        String response = sendRequest("PUT", "/game", authToken, json);
        return gson.fromJson(response, GameData.class);
    }

    public void clear() throws Exception {
        sendRequest("DELETE", "/db", null, null);
    }


    private String sendRequest(String method, String path, String authToken, String requestBody) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setDoInput(true);
        connection.setRequestProperty("Accept", "application/json");

        if (authToken != null) {
            connection.setRequestProperty("Authorization", authToken);
        }

        if (requestBody != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input);
            }
        }

        int status = connection.getResponseCode();

        InputStream stream = (status >= 200 && status < 300)
                ? connection.getInputStream() : connection.getErrorStream();

        StringBuilder response = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
        }

        if (status < 200 || status >= 300) {
            throw new RuntimeException("HTTP " + status + ": " + response);
        }

        return response.toString();
    }
}
