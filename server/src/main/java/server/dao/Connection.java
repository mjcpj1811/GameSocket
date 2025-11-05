package server.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class Connection {
    private static String url;
    private static String user;
    private static String password;

    static {
        try {
            // Đọc file cấu hình db.properties
            Properties props = new Properties();
            props.load(Connection.class.getClassLoader().getResourceAsStream("db.properties"));

            url = props.getProperty("db.url");
            user = props.getProperty("db.user");
            password = props.getProperty("db.password");

            // Nạp driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            System.out.println("[Database] Kết nối cấu hình thành công!");
        } catch (IOException e) {
            System.err.println("[Database] Không đọc được file db.properties: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("[Database] Không tìm thấy MySQL Driver!");
        }
    }

    public static java.sql.Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
