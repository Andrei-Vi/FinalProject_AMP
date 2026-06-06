package model;

import model.enums.UserRole;
import model.enums.UserStatus;

public class User {
    int id;
    String username;
    String password;
    UserRole role;
    UserStatus status;

    public User(int id, String username, String password, UserRole role, UserStatus status) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
    public boolean isBanned() {
        return status == UserStatus.BANNED;
    }
}
