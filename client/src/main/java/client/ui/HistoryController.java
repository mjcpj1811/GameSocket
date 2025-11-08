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

public class HistoryController {
    @FXML private TableView<Map<String,Object>> tbl;

    @FXML public void initialize() {
        TableColumn<Map<String,Object>, Object> c1 = new TableColumn<>("Trận đấu");
        c1.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().get("matchId")));

        TableColumn<Map<String,Object>, Object> c2 = new TableColumn<>("Đối thủ");
        c2.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().get("opponent")));

        TableColumn<Map<String,Object>, Object> c3 = new TableColumn<>("Điểm của bạn");
        c3.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().get("score")));

        TableColumn<Map<String,Object>, Object> c4 = new TableColumn<>("Thời gian");
        c4.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().get("time")));

        TableColumn<Map<String,Object>, Object> c5 = new TableColumn<>("Người thắng");
        c5.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().get("winner")));

        tbl.getColumns().setAll(c1, c2, c3, c4, c5);
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Connection conn = Session.get().connection;

        // nhận 1 lần, không đụng mainListener
        conn.once(Protocol.HISTORY_DATA, msg -> {
            var entries = (List<Map<String,Object>>) msg.getPayload().get(Protocol.HISTORY);
            Platform.runLater(() -> tbl.getItems().setAll(entries));
        });

        conn.send(new Message(Protocol.GET_HISTORY));
    }
}
