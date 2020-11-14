import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Map;

public class User {
	private static Map<String, User> users = new HashMap<>();

	private String name;
	private SelectionKey userKey;
	private Room currentRoom;

	private User(String name, SelectionKey userKey) {
		this.name = name;
		this.userKey = userKey;

		userKey.attach(name);
	}

	/**
	 * Get an user instance from its name
	 * 
	 * @param name to search
	 * @return the user
	 */
	public static User getByName(String name) {
		return users.get(name);
	}

	/**
	 * Create a new user.
	 * 
	 * @return true if created successfully; false if name is taken
	 */
	public static boolean create(String name, SelectionKey userKey) throws IOException {
		if (!users.containsKey(name)) {
			User u = new User(name, userKey);
			users.put(name, u);
			MessagingUtils.sendOK(u);
			return true;
		}

		return false;
	}

	/**
	 * Gets the user name
	 * 
	 * @return the user name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the user key
	 * 
	 * @return the user key
	 */
	public SelectionKey getKey() {
		return this.userKey;
	}

	/**
	 * Sets the user name
	 * 
	 * @param name to set
	 */
	public void setName(String name) {
		users.remove(this.name);
		this.name = name;
		users.put(this.name, this);
		this.userKey.attach(name);
	}

	/**
	 * Joins a room
	 * 
	 * @param room to join
	 */
	public void joinRoom(Room room) {
		leaveRoom();
		room.addUser(this);
		currentRoom = room;
	}

	public void joinRoom(String room) {
		joinRoom(Room.getByName(room));
	}

	/**
	 * Leaves the current room
	 */
	public void leaveRoom() {
		if (currentRoom == null)
			return;
		currentRoom.removeUser(this);
		currentRoom = null;
	}

	/**
	 * Checks if user is currently in a room
	 * 
	 * @return if is in a room
	 */
	public boolean isInRoom() {
		return currentRoom != null;
	}

	/**
	 * Deletes the user removing him from the room
	 */
	public void delete() {
		leaveRoom();
		users.remove(this.name);
	}

	public void sendMessage(String message) throws IOException {
		currentRoom.sendMessage(message);
	}
}
