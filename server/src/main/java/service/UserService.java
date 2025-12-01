package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.UserData;
import model.AuthData;
import service.requests.LoginRequest;
import service.requests.LogoutRequest;
import service.requests.RegisterRequest;
import service.results.LoginResult;
import service.results.RegisterResult;


public class UserService {
    private final DataAccess dao;

    public UserService(DataAccess dao) {
        this.dao = dao;
    }

    public RegisterResult register(RegisterRequest req) throws DataAccessException {
        if (req.username() == null || req.username().isEmpty() ||
                req.password() == null || req.password().isEmpty() ||
                req.email() == null || req.email().isEmpty()) {
            throw new DataAccessException("Error: bad request");
        }

        try {
            dao.getUser(req.username());
            throw new DataAccessException("Error: already taken");
        } catch (DataAccessException ex) {
            // user not found
        }

        var user = new UserData(req.username(), req.password(), req.email());
        dao.createUser(user);

        AuthData auth = dao.createAuth(req.username());
        return new RegisterResult(auth.username(), auth.authToken());
    }

    public LoginResult login(LoginRequest req) throws DataAccessException {
        var user = dao.getUser(req.username());
        if (!user.password().equals(req.password())) {
            throw new DataAccessException("Error: unauthorized");
        }

        AuthData auth = dao.createAuth(req.username());
        return new LoginResult(auth.username(), auth.authToken());
    }

    public void logout(LogoutRequest req) throws DataAccessException {
        dao.deleteAuth(req.authToken());
    }

}
