package client;

import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.GameList;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class ServerFacade {
    private final String baseUrl;
    private final Gson gson = new Gson();
    private final HttpClient client = HttpClient.newHttpClient();

    public ServerFacade(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public AuthData register(String username, String password, String email) throws Exception {
        var body = Map.of("username", username, "password", password, "email", email);
        String json = gson.toJson(body);
        String response = handleResponse(sendRequest("POST", "/user", null, json));
        return gson.fromJson(response, AuthData.class);
    }

    public AuthData login(String username, String password) throws Exception {
        var body = Map.of("username", username, "password", password);
        String json = gson.toJson(body);
        String response = handleResponse(sendRequest("POST", "/session", null, json));
        return gson.fromJson(response, AuthData.class);
    }

    public void logout(String authToken) throws Exception {
        sendRequest("DELETE", "/session", authToken, null);
    }

    public GameData createGame(String authToken, String gameName) throws Exception {
        var body = Map.of("gameName", gameName);
        String json = gson.toJson(body);
        String response = handleResponse(sendRequest("POST", "/game", authToken, json));

        return gson.fromJson(response, GameData.class);
    }

    public List<GameData> listGames(String authToken) throws Exception {
        String response = handleResponse(sendRequest("GET", "/game", authToken, null));
        GameList g = gson.fromJson(response, GameList.class);
        return g.games();
    }

    public GameData joinGame(String authToken, int gameId, String color) throws Exception {
        String json;

        if (color == null) {
            // Observer: no playerColor field
            var body = Map.of("gameID", gameId);
            json = gson.toJson(body);
        } else {
            // Player: include playerColor
            var body = Map.of("gameID", gameId, "playerColor", color);
            json = gson.toJson(body);
        }

        String response = handleResponse(sendRequest("PUT", "/game", authToken, json));
        return gson.fromJson(response, GameData.class);
    }

    public void clear() throws Exception {
        sendRequest("DELETE", "/db", null, null);
    }


    private HttpResponse<String> sendRequest(String method, String path, String authToken, String requestBody) throws Exception {
        HttpRequest.Builder requestBuilder = null;
        if (requestBody == null) {
            requestBuilder = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            requestBuilder = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).method(method, HttpRequest.BodyPublishers.ofString(requestBody));
        }
        if (requestBody != null) {
            requestBuilder.setHeader("Content-Type", "application/json");
        }
        if (authToken != null) {
            requestBuilder.setHeader("authorization", authToken);
        }
        HttpRequest request = requestBuilder.build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());

    }
    private String handleResponse (HttpResponse<String> response) throws ErrorResult {
        var status = response.statusCode();
        if (status == 200) {
            return response.body();
        }
        ErrorResult handleError = gson.fromJson(response.body(), ErrorResult.class);
        throw (handleError);
    }


}
