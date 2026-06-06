package model;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatRoom {
    int id;
    String name;
    Set<User> members;
    List<Message> messages;

    public ChatRoom(int id, String name) {
        this.id = id;
        this.name = name;
        this.members = new HashSet<>();
        this.messages = new ArrayList<>();
    }

    public void addMember(User user) {
        members.add(user);
    }
    public void removeMember(User user) {
        members.remove(user);
    }
    public void addMessage(Message message) {
        messages.add(message);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<User> getMembers() {
        return members;
    }

    public void setMembers(Set<User> members) {
        this.members = members;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
