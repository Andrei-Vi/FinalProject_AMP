package exception;

public class UserAlreadyBannedException extends Exception {
    public UserAlreadyBannedException(String message) {
        super(message);
    }
}
