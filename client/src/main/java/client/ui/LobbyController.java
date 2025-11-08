package client.ui;

import client.net.Connection;
import client.state.Session;
import common.Message;
import common.Protocol;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

/** Lobby là mainListener chính. Luôn nhận ONLINE_UPDATE live từ Connection. */
public class LobbyController {
    @FXML private Label lblUser;
    @FXML private ListView<Map<String,Object>> lvOnline;
    @FXML private Button btnLeaderboard, btnHistory, btnLogout;

    private Connection conn;
    private final ObservableList<Map<String,Object>> online = FXCollections.observableArrayList();

    @FXML public void initialize() {
        lblUser.setText(Session.get().username);
        conn = Session.get().connection;

        // Đặt Lobby làm listener chính
        conn.setMainListener(this::handle);
        Session.get().lobbyListener = this::handle;

        lvOnline.setItems(online);
        lvOnline.setCellFactory(v -> new OnlineCell());

        // Nếu có cache ONLINE_UPDATE thì dùng ngay, đồng thời hỏi lại server cho chắc
        Message cached = conn.getLastOnlineUpdate();
        if (cached != null) handle(cached);
        conn.send(new Message(Protocol.LIST_ONLINE));
    }

    @FXML private void onLeaderboard() { open("/fxml/leaderboard.fxml", "Leaderboard"); }
    @FXML private void onHistory() { open("/fxml/history.fxml", "Lịch sử đấu"); }
    @FXML private void onLogout() {
        conn.send(new Message(Protocol.LOGOUT));
        ((Stage) lblUser.getScene().getWindow()).close();
    }

    private void open(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Scene sc = new Scene(loader.load());
            sc.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage st = new Stage();
            st.setTitle(title);
            st.setScene(sc);
            st.initModality(Modality.WINDOW_MODAL);
            st.initOwner(lblUser.getScene().getWindow());
            st.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handle(Message m) {
        Platform.runLater(() -> {
            switch (m.getType()) {
                case Protocol.ONLINE_UPDATE -> {
                    List<Map<String,Object>> users =
                            (List<Map<String,Object>>) m.getPayload().get(Protocol.USERS);
                    online.setAll(users);
                }

                case Protocol.CHALLENGE_REQ -> {
                    String challengerId = (String) m.getPayload().get(Protocol.YOU);
                    String name = (String) m.getPayload().get("name");
                    Alert a = new Alert(Alert.AlertType.CONFIRMATION, name + " thách đấu!",
                            ButtonType.YES, ButtonType.NO);
                    a.setHeaderText("Bạn có chấp nhận?");
                    a.showAndWait().ifPresent(btn -> {
                        boolean accept = btn == ButtonType.YES;
                        conn.send(new Message(Protocol.CHALLENGE_RESP)
                                .put(Protocol.ACCEPT, accept)
                                .put(Protocol.YOU, challengerId));
                    });
                }

                case Protocol.CHALLENGE_RESP -> {
                    boolean accept = Boolean.TRUE.equals(m.getPayload().get(Protocol.ACCEPT));
                    String opponent = (String) m.getPayload().get("opponent");
                    if (!accept) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setHeaderText(null);
                        alert.setContentText(opponent + " đã từ chối lời mời thách đấu của bạn!");
                        alert.showAndWait();
                    }
                }

                case Protocol.MATCH_START -> {
                    Session.get().matchId = (String) m.getPayload().get(Protocol.MATCH_ID);
                    Session.get().opponentName = (String) m.getPayload().get(Protocol.OPP);
                    goGame();
                }

                case Protocol.ROUND_START -> GameController.handleRoundStartExternal(m);
                case Protocol.ROUND_RESULT -> GameController.handleRoundResultExternal(m);
                case Protocol.MATCH_RESULT -> GameController.handleMatchResultExternal(m);
            }
        });
    }

    private void goGame() {
        try {
            FXMLLoader fxml = new FXMLLoader(getClass().getResource("/fxml/game.fxml"));
            Scene sc = new Scene(fxml.load());
            sc.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage st = (Stage) lblUser.getScene().getWindow();
            st.setScene(sc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Hiển thị mỗi người online kèm trạng thái và nút thách đấu */
    private class OnlineCell extends ListCell<Map<String,Object>> {
        @Override
        protected void updateItem(Map<String,Object> item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            String name = String.valueOf(item.get("username"));
            String status = String.valueOf(item.getOrDefault("status", "UNKNOWN"));

            Label lblName = new Label(name);
            lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Label lblStatus = new Label(status);
            lblStatus.setStyle("-fx-text-fill: " +
                    ("INGAME".equals(status) ? "orange" :
                            "ONLINE".equals(status) ? "green" : "gray") +
                    "; -fx-font-size: 12px;");

            Button btnChallenge = new Button("Thách đấu");
            btnChallenge.getStyleClass().add("primary");
            btnChallenge.setDisable("INGAME".equals(status));

            btnChallenge.setOnAction(ev -> {
                Session.get().opponentId = String.valueOf(item.get("id"));
                conn.send(new Message(Protocol.CHALLENGE)
                        .put(Protocol.OPP, Session.get().opponentId));
            });

            HBox row = new HBox(10, lblName, lblStatus, btnChallenge);
            row.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 4 8;");

            setGraphic(row);
            setText(null);
        }
    }
}
