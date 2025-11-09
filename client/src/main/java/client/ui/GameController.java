package client.ui;

import client.net.Connection;
import client.state.Session;
import client.service.GameService;
import common.Message;
import common.Protocol;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;


import java.util.*;

public class GameController {
    @FXML private Label lblYou, lblOpp, lblRound, lblSeq, lblTimer, lblInfo;
    @FXML private TextField txtAnswer;
    @FXML private Button btnSubmit, btnQuit;
    @FXML private TableView<Map<String,Object>> tblRound;
    @FXML private TableColumn<Map<String,Object>, Object> colRound, colSeq, colYourAns, colOppAns, colScore;
    @FXML private ImageView bg;

    private static GameController INSTANCE;
    private Connection conn;
    private GameService service;
    private Thread countdownThread;
    private long startInputAt;

    @FXML
    public void initialize() {
        INSTANCE = this;
        conn = Session.get().connection;
        lblYou.setText(Session.get().username);
        lblOpp.setText(Session.get().opponentName);

        colRound.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().get("round")));
        colSeq.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().get("seq")));
        colYourAns.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().get("yourAns")));
        colOppAns.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().get("oppAns")));
        colScore.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().get("score")));
        bg.setImage(new Image(getClass().getResource("/assets/bg_arcade.png").toExternalForm()));

        service = new GameService();
        new Thread(service, "GameService").start();
    }

    // ===== Static relay từ Connection =====
    public static void handleRoundStartExternal(Message m) { if (INSTANCE != null) INSTANCE.onRoundStart(m); }
    public static void handleRoundResultExternal(Message m) { if (INSTANCE != null) INSTANCE.onRoundResult(m); }
    public static void handleMatchResultExternal(Message m) { if (INSTANCE != null) INSTANCE.onMatchResult(m); }

    // ===== Xử lý bắt đầu vòng =====
    private void onRoundStart(Message m) {
        int round = ((Number) m.getPayload().get(Protocol.ROUND_NO)).intValue();
        Session.get().currentRound = round;
        String seq = (String) m.getPayload().get(Protocol.SEQ);
        int show = ((Number) m.getPayload().get(Protocol.TIME_SHOW)).intValue();
        int input = ((Number) m.getPayload().get(Protocol.TIME_INPUT)).intValue();

        Platform.runLater(() -> {
            lblRound.setText("Round " + round);
            lblSeq.setText(seq);
            lblInfo.setText("Hãy ghi nhớ chuỗi!");
            txtAnswer.setDisable(true);
            btnSubmit.setDisable(true);
        });

        startCountdown(show,
                t -> Platform.runLater(() -> lblTimer.setText("Hiển thị: " + t + "s")),
                () -> {
                    Platform.runLater(() -> {
                        lblSeq.setText("●●●●●");
                        lblInfo.setText("Nhập lại chuỗi!");
                        txtAnswer.setDisable(false);
                        btnSubmit.setDisable(false);
                        txtAnswer.clear();
                        startInputAt = System.nanoTime();
                    });
                    startCountdown(input,
                            t -> Platform.runLater(() -> lblTimer.setText("Nhập: " + t + "s")),
                            this::autoSubmitIfNotYet);
                });
    }

    private void startCountdown(int seconds, java.util.function.IntConsumer onTick, Runnable onFinish) {
        if (countdownThread != null && countdownThread.isAlive()) countdownThread.interrupt();
        countdownThread = new Thread(() -> {
            try {
                for (int i = seconds; i >= 0; i--) {
                    final int t = i;
                    onTick.accept(t);
                    Thread.sleep(1000);
                }
                Platform.runLater(onFinish);
            } catch (InterruptedException ignored) {}
        });
        countdownThread.start();
    }

    @FXML private void onSubmit() { submit(false); }
    private void autoSubmitIfNotYet() { submit(true); }

    private void submit(boolean auto) {
        if (btnSubmit.isDisabled()) return;
        btnSubmit.setDisable(true);
        txtAnswer.setDisable(true);

        double elapsed = (System.nanoTime() - startInputAt) / 1_000_000_000.0;
        String ans = Optional.ofNullable(txtAnswer.getText()).orElse("").trim().toUpperCase();

        conn.send(new Message(Protocol.SUBMIT_ANSWER)
                .put(Protocol.ROUND_NO, Session.get().currentRound)
                .put(Protocol.ANSWER, ans)
                .put(Protocol.ELAPSED, elapsed));
        lblInfo.setText(auto ? "Hết giờ. Đã nộp tự động." : "Đã nộp.");
    }

    private void onRoundResult(Message m) {
        int round = ((Number) m.getPayload().get(Protocol.ROUND_NO)).intValue();
        String seq = (String) m.getPayload().get("seq");
        String yourAns = (String) m.getPayload().get("yourAnswer");
        String oppAns = (String) m.getPayload().get("oppAnswer");
        int sYou = ((Number) m.getPayload().get(Protocol.YOUR_SCORE)).intValue();
        int sOpp = ((Number) m.getPayload().get(Protocol.OPP_SCORE)).intValue();

        Platform.runLater(() -> {
            lblInfo.setText("KQ Round " + round + ": Bạn " + sYou + " - Đối thủ " + sOpp);
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("round", round);
            row.put("seq", seq);
            row.put("yourAns", yourAns);
            row.put("oppAns", oppAns);
            row.put("score", sYou + " - " + sOpp);
            tblRound.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            tblRound.getItems().add(row);
        });
    }

    private void onMatchResult(Message m) {
        String info = (String) m.getPayload().getOrDefault("info", "");
        String oppName = (String) m.getPayload().getOrDefault("oppName", lblOpp.getText());

        int yourScore = ((Number) m.getPayload().getOrDefault("yourScore", 0)).intValue();
        int oppScore = ((Number) m.getPayload().getOrDefault("oppScore", 0)).intValue();
        double yourTime = Double.parseDouble(m.getPayload().getOrDefault("yourTime", "0").toString());
        double oppTime = Double.parseDouble(m.getPayload().getOrDefault("oppTime", "0").toString());
        Object winner = m.getPayload().get("winner");

        String msg;
        if (!info.isEmpty()) {
            msg = info;
        } else {
            String result;
            if (winner == null) result = "Hòa!";
            else if (winner.equals(Session.get().token)) result = "Bạn Thắng!";
            else result = "Bạn Thua!";

            msg = String.format("""
        %s
        ────────────────────
        Bạn: 
        ▸ Điểm: %-3d   ▸ Thời gian: %.3fs
        Đối thủ (%s):
        ▸ Điểm: %-3d   ▸ Thời gian: %.3fs
        ─────────────────────
        """,
                    result,yourScore, yourTime,
                    oppName, oppScore, oppTime
                    );
        }

        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            a.setHeaderText("Kết thúc trận");
            Stage owner = (Stage) lblYou.getScene().getWindow();
            a.initOwner(owner);
            a.initModality(Modality.WINDOW_MODAL);
            a.setOnShown(ev -> {
                Stage stage = (Stage) a.getDialogPane().getScene().getWindow();
                stage.toFront();
            });
            styleAlert(a);
            a.showAndWait();
            service.stop();
            returnToLobby();
        });
    }

    @FXML private void onQuit() {
        try {
            if (service != null) service.stop();
            conn.send(new Message(Protocol.QUIT_MATCH));

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("Bạn đã thoát trận");
                alert.setContentText("Hệ thống sẽ xử thua bạn trong trận này.");

                Stage owner = (Stage) lblYou.getScene().getWindow();
                alert.initOwner(owner);
                alert.initModality(Modality.WINDOW_MODAL);
                alert.setOnShown(ev -> {
                    Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                    stage.toFront();
                });
                styleAlert(alert);
                alert.showAndWait();
                returnToLobby();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void returnToLobby() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/lobby.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Platform.runLater(() -> {
                try {
                    Stage stage = (Stage) lblYou.getScene().getWindow();
                    stage.setTitle("MemoryGame - Lobby");
                    stage.setScene(scene);

                    if (Session.get().lobbyListener != null)
                        Session.get().connection.setMainListener(Session.get().lobbyListener);

                    new Thread(() -> {
                        try { Thread.sleep(300);
                            Session.get().connection.send(new Message(Protocol.LIST_ONLINE));
                        } catch (Exception ignored) {}
                    }).start();

                } catch (Exception ex) { ex.printStackTrace(); }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }
    private void styleAlert(Alert alert) {
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/dialog.css").toExternalForm()
        );
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
    }

}