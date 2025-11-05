package client.state;

import client.net.Connection;

public class Session {
    private static Session I = new Session();
    public static Session get() { return I; }

    public String token;     // userId
    public String username;
    public String opponentId;
    public String opponentName;
    public String matchId;
    public Connection connection;
    public int currentRound = 0;

    public java.util.function.Consumer<common.Message> lobbyListener;
}
