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
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.*;

public class GameController {
    @FXML private Label lblYou, lblOpp, lblRound, lblSeq, lblTimer, lblInfo;
    @FXML private TextField txtAnswer;
    @FXML private Button btnSubmit, btnQuit;
    @FXML private TableView<Map<String,Object>> tblRound;
    @FXML private TableColumn<Map<String,Object>, Object> colRound, colSeq, colYourAns, colOppAns, colScore;

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
            tblRound.getItems().add(row);
        });
    }

    private void onMatchResult(Message m) {
        String info = (String)m.getPayload().getOrDefault("info","");
        int total = ((Number)m.getPayload().getOrDefault(Protocol.TOTAL_SCORE,0)).intValue();
        double tt = ((Number)m.getPayload().getOrDefault(Protocol.TOTAL_TIME,0.0)).doubleValue();
        Object win = m.getPayload().get("winner");

        String msg = !info.isEmpty()
                ? info
                : "Tổng điểm: " + total + "\nThời gian: " + tt + "s\n" +
                ((String.valueOf(win).equals(Session.get().token)) ? "Bạn Thắng!" : "Bạn Thua.");

        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            a.setHeaderText("Kết thúc trận");
            a.showAndWait();
            service.stop();
            returnToLobby();
        });
    }

    @FXML private void onQuit() {
        try {
            if (service != null) service.stop();
            conn.send(new Message(Protocol.QUIT_MATCH));

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Bạn đã thoát trận");
            alert.setContentText("Hệ thống sẽ xử thua bạn trong trận này.");
            alert.showAndWait();

            returnToLobby();
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
                    Stage stage = null;
                    if (lblYou != null && lblYou.getScene() != null) {
                        stage = (Stage) lblYou.getScene().getWindow();
                    } else {
                        stage = (Stage) Stage.getWindows().stream()
                                .filter(Window::isShowing)
                                .findFirst()
                                .orElse(null);
                    }

                    if (stage != null) {
                        stage.setTitle("MemoryGame - Lobby");
                        stage.setScene(scene);
                    } else {
                        System.err.println("[WARN] No active stage found while returning to lobby");
                    }

                    // 🔹 Khôi phục listener lobby
                    if (Session.get().lobbyListener != null) {
                        Session.get().connection.setMainListener(Session.get().lobbyListener);
                    }

                    // 🔹 Yêu cầu refresh danh sách online sau 300ms
                    new Thread(() -> {
                        try {
                            Thread.sleep(300);
                            Session.get().connection.send(new Message(Protocol.LIST_ONLINE));
                        } catch (Exception ignored) {}
                    }).start();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
