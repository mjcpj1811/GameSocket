package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Kiểm tra đường dẫn FXML

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load());

        // Nếu có CSS thì load thêm
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

         stage.setTitle("MemoryGame - Đăng nhập");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
