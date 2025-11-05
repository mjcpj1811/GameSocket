package client.service;

import client.state.Session;
import common.Message;
import common.Protocol;
import javafx.application.Platform;

public class GameService implements Runnable {
    private volatile boolean running = true;

    @Override
    public void run() {
        while (running) {
            Message msg = Session.get().connection.takeGameMessage();
            if (msg == null) continue;

            switch (msg.getType()) {
                case Protocol.ROUND_START ->
                        Platform.runLater(() -> client.ui.GameController.handleRoundStartExternal(msg));
                case Protocol.ROUND_RESULT ->
                        Platform.runLater(() -> client.ui.GameController.handleRoundResultExternal(msg));
                case Protocol.MATCH_RESULT ->
                        Platform.runLater(() -> client.ui.GameController.handleMatchResultExternal(msg));
            }
        }
    }

    public void stop() {
        running = false;
    }
}
