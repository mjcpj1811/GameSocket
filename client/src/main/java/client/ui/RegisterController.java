package client.ui;

import client.net.Connection;
import client.state.Session;
import common.Config;
import common.Message;
import common.Protocol;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class RegisterController {
    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass, txtConfirm;
    @FXML private Label lblStatus;
    @FXML private Button btnRegister;
    @FXML private ImageView bg;

    private Connection conn;

    @FXML
    public void initialize() {
        try {
            conn = new Connection(Config.HOST, Config.PORT, this::handle);
            bg.setImage(new Image(getClass().getResource("/assets/bg_arcade.png").toExternalForm()));
        } catch (Exception e) {
            lblStatus.setText("Không kết nối được server!");
            btnRegister.setDisable(true);
        }
    }

    @FXML
    private void onRegister() {
        String u = txtUser.getText().trim();
        String p = txtPass.getText().trim();
        String c = txtConfirm.getText().trim();

        if (u.isEmpty() || p.isEmpty()) {
            lblStatus.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }
        if (!p.equals(c)) {
            lblStatus.setText("Mật khẩu nhập lại không khớp!");
            return;
        }

        conn.send(new Message(Protocol.REGISTER)
                .put(Protocol.USERNAME, u)
                .put(Protocol.PASSWORD, p));
    }

    private void handle(Message m) {
        switch (m.getType()) {
            case Protocol.REGISTER_OK -> Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Đăng ký thành công!");
                a.setHeaderText("Thành công!");
                styleAlert(a);
                a.showAndWait().ifPresent(button -> {
                    if (button == ButtonType.OK) {
                        goLogin();
                    }
                });
            });
            case Protocol.REGISTER_FAIL -> Platform.runLater(() ->
                    lblStatus.setText((String) m.getPayload().getOrDefault("reason", "Đăng ký thất bại!")));
        }
    }

    @FXML
    private void goLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage st = (Stage) txtUser.getScene().getWindow();
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            st.setScene(scene);
            st.setTitle("MemoryGame - Đăng nhập");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void styleAlert(Alert alert) {
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/dialog.css").toExternalForm()
        );
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
    }
}
