package server.dao;

import common.model.User;
import java.sql.*;
import java.util.*;

public class UserDAO {

    // Đăng nhập
    public Optional<User> login(String username, String password) {
        String sql = "SELECT * FROM Users WHERE username = ? AND password = ?";
        try (java.sql.Connection conn = Connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User u = map(rs);
                setStatus(u.getId(), "ONLINE");
                return Optional.of(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // Kiểm tra username đã tồn tại chưa
    public boolean exists(String username) {
        String sql = "SELECT 1 FROM Users WHERE username = ?";
        try (java.sql.Connection conn = Connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void insert(String username, String password) {
        String newId = generateNextId();
        String sql = "INSERT INTO Users (id, username, password, status, createdAt) VALUES (?, ?, ?, ?, NOW())";

        try (java.sql.Connection conn = Connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newId);
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4, "OFFLINE");

            ps.executeUpdate();
            System.out.println("[REGISTER] Tạo tài khoản mới: " + username + " (" + newId + ")");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 🧮 Sinh mã U kế tiếp
    private String generateNextId() {
        String prefix = "U";
        String sql = "SELECT id FROM Users WHERE id LIKE 'U%' ORDER BY id DESC LIMIT 1";

        try (java.sql.Connection conn = Connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                String lastId = rs.getString("id"); // ví dụ: U005
                int num = Integer.parseInt(lastId.substring(1)); // -> 5
                return prefix + String.format("%03d", num + 1); // -> U006
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return prefix + "001"; // nếu chưa có user nào
    }

    // Cập nhật trạng thái (ONLINE / OFFLINE / INGAME)
    public void setStatus(String userId, String status) {
        String sql = "UPDATE Users SET status = ? WHERE id = ?";
        try (java.sql.Connection conn = Connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Ánh xạ từ ResultSet -> User model
    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getString("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setEmail(rs.getString("email"));
        u.setStatus(rs.getString("status"));
        u.setCreatedAt(rs.getTimestamp("createdAt"));
        return u;
    }
}
