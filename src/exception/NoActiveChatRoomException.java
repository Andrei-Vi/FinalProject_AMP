package exception;

public class NoActiveChatRoomException extends Exception {
    public NoActiveChatRoomException(String message) {
        super(message);
    }
}
