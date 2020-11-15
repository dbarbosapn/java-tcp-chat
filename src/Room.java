import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Room {
    private static Map<String, Room> rooms = new HashMap<>();

    private String name;
    private Set<User> users = new HashSet<>();

    public Room(String name) {
        this.name = name;
        rooms.put(name, this);
    }

    /**
     * Get a room instance from its name
     * 
     * @param name to search
     * @return the room
     */
    public static Room getByName(String name) {
        if (!rooms.containsKey(name)) {
            rooms.put(name, new Room(name));
        }
        return rooms.get(name);
    }

    /**
     * Removes an user for the room. If it gets empty, delete the room.
     * 
     * @param user to remove
     */
    public void removeUser(User user) {
        users.remove(user);
        if (users.size() == 0)
            rooms.remove(this.name);
    }

    public void addUser(User user) throws IOException {
        sendJoinedMessage(user.getName());
        users.add(user);
    }

    public void sendMessage(String message) throws IOException {
        broadcast(MessageType.MESSAGE, message);
    }

    private void sendJoinedMessage(String name) throws IOException {
        broadcast(MessageType.JOINED, name);
    }

    private void broadcast(MessageType type, String data) throws IOException {
        for (User user : users) {
            MessagingUtils.sendMessage(user, type, data);
        }
    }
}
