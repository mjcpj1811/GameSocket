package client.ui;

import client.net.Connection;
import client.state.Session;
import common.Message;
import common.Protocol;
import common.Config;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

/** Màn Login: chỉ cache ONLINE_UPDATE, không set mainListener = null ở đây */
public class LoginController {
    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass;
    @FXML private Button btnLogin;
    @FXML private Label lblStatus;

    private Connection conn;

    @FXML public void initialize() {
        try {
            conn = new Connection(Config.HOST, Config.PORT, this::handle);
        } catch (Exception e) {
            lblStatus.setText("Không kết nối được server!");
            btnLogin.setDisable(true);
        }
    }

    @FXML private void onLogin() {
        String u = txtUser.getText().trim();
        String p = txtPass.getText().trim();
        if (u.isEmpty() || p.isEmpty()) {
            lblStatus.setText("Nhập username/password");
            return;
        }
        conn.send(new Message(Protocol.LOGIN)
                .put(Protocol.USERNAME, u)
                .put(Protocol.PASSWORD, p));
    }

    private void handle(Message m) {
        switch (m.getType()) {
            case Protocol.LOGIN_OK -> {
                Session s = Session.get();
                s.token = (String) m.getPayload().get(Protocol.TOKEN);
                s.username = (String) m.getPayload().get(Protocol.YOU);
                s.connection = conn;

                // Mở lobby ngay, Lobby sẽ set mainListener và dùng cache nếu có
                Platform.runLater(this::goLobby);
            }

            case Protocol.ONLINE_UPDATE -> {
                // Nếu nhận khi đang ở login, chỉ cache lại để Lobby dùng
                Session.get().connection.setLastOnlineUpdate(m);
            }

            case Protocol.LOGIN_FAIL -> Platform.runLater(() ->
                    lblStatus.setText("Sai tài khoản/mật khẩu"));
        }
    }

    private void goLobby() {
        try {
            FXMLLoader fxml = new FXMLLoader(getClass().getResource("/fxml/lobby.fxml"));
            Scene sc = new Scene(fxml.load());
            sc.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage st = (Stage) txtUser.getScene().getWindow();
            st.setTitle("MemoryGame - Lobby");
            st.setScene(sc);
            st.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
