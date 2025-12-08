//import chess.*;
//
//public class Main {
//    public static void main(String[] args) {
//        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
//        System.out.println("♕ 240 Chess Client: " + piece);
//    }
//}


import chess.*;
import client.ChessClient;
import client.ServerFacade;

public class Main {
    public static void main(String[] args) {
        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        System.out.println("♕ 240 Chess Client: " + piece);

        int port = 8080; // or read from args/config to match your Server.run(...)
        String baseUrl = "http://localhost:" + port;

        ServerFacade facade = new ServerFacade(baseUrl);
        ChessClient client = new ChessClient(facade);
        client.run();
    }
}
