package app;

import service.AuditService;
import service.ChatService;

public class ChatApplicationContext {
    private final ChatService chatService;
    private final AuditService auditService;

    public ChatApplicationContext(ChatService chatService, AuditService auditService) {
        this.chatService = chatService;
        this.auditService = auditService;
    }

    public ChatService getChatService() {
        return chatService;
    }

    public AuditService getAuditService() {
        return auditService;
    }
}
