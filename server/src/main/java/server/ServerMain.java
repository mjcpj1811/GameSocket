package server;

import java.net.ServerSocket;
import java.net.Socket;
import common.Config;
public class ServerMain {
    public static void main(String[] args) throws Exception {
        Matchmaker hub = new Matchmaker();
        try (ServerSocket server = new ServerSocket(Config.PORT)) {
            System.out.println("MemoryGame Server listening on port " + Config.PORT);
            while (true) {
                Socket s = server.accept();
                ClientHandler h = new ClientHandler(s, hub);
                new Thread(h, "ClientHandler-" + s.getRemoteSocketAddress()).start();
            }
        }
    }
}
