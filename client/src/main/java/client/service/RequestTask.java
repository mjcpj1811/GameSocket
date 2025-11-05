package client.service;

import client.state.Session;
import common.Message;
import common.Protocol;
import javafx.application.Platform;

import java.util.function.Consumer;

public class RequestTask implements Runnable {
    private final Message request;
    private final String expectType;
    private final Consumer<Message> callback;

    public RequestTask(Message request, String expectType, Consumer<Message> callback) {
        this.request = request;
        this.expectType = expectType;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            var conn = Session.get().connection; // 🟢 Dùng connection hiện tại
            conn.once(expectType, msg -> Platform.runLater(() -> callback.accept(msg)));
            conn.send(request); // 🟢 Gửi yêu cầu
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
