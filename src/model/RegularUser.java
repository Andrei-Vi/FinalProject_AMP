package model;

import model.enums.UserRole;
import model.enums.UserStatus;

public class RegularUser extends User {
    public RegularUser(int id, String username, String password, UserRole role, UserStatus status) {
        super(id, username, password, role, status);
    }
}
