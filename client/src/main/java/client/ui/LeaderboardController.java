package client.ui;

import client.state.Session;
import client.net.Connection;
import common.Message;
import common.Protocol;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.*;

public class LeaderboardController {
    @FXML private TableView<Map<String,Object>> tbl;

    @FXML public void initialize() {
        TableColumn<Map<String, Object>, Object> c1 = new TableColumn<>("Hạng");
        c1.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(tbl.getItems().indexOf(cell.getValue()) + 1));

        TableColumn<Map<String, Object>, Object> c2 = new TableColumn<>("Người chơi");
        c2.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().get("username")));

        TableColumn<Map<String, Object>, Object> c3 = new TableColumn<>("Điểm cao nhất");
        c3.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().get("bestScore")));

        TableColumn<Map<String, Object>, Object> c4 = new TableColumn<>("Thời gian tốt nhất");
        c4.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().get("bestTime")));

        tbl.getColumns().setAll(c1, c2, c3, c4);

        Connection conn = Session.get().connection;

        // nhận 1 lần, không đụng mainListener
        conn.once(Protocol.LEADERBOARD_DATA, msg -> {
            var entries = (List<Map<String,Object>>) msg.getPayload().get(Protocol.ENTRIES);
            Platform.runLater(() -> tbl.getItems().setAll(entries));
        });

        conn.send(new Message(Protocol.GET_LEADERBOARD));
    }
}
