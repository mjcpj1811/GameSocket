package common.model;

import java.sql.Timestamp;

public class User {
    private String id;
    private String username;
    private String password;
    private String email;
    private String status;
    private Timestamp createdAt;

    public User() {}

    public User(String id, String username, String password, String email, String status, Timestamp createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters / Setters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setId(String id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) { this.password = password; }
    public void setStatus(String status) { this.status = status; }
}
