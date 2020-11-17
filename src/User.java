import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class User {
	private static Map<SocketChannel, User> users = new HashMap<>();
	private static Set<String> names = new HashSet<>();

	private String name;
	private SocketChannel userChannel;
	private Room currentRoom;
	private Protocol.State currentState;

	private User(SocketChannel userChannel) {
		this.userChannel = userChannel;
		this.currentState = Protocol.State.INIT;
		this.name = null;
	}

	/**
	 * Get an user instance from its channel
	 * 
	 * @param channel to search
	 * @return the user
	 */
	public static User getByChannel(SocketChannel channel) {
		return users.get(channel);
	}

	/**
	 * Create a new user.
	 */
	public static void create(SocketChannel userChannel) throws IOException {
		User u = new User(userChannel);
		users.put(userChannel, u);
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
	 * Gets the current room
	 * 
	 * @return the room
	 */
	public Room getCurrentRoom() {
		return this.currentRoom;
	}

	/**
	 * Gets the user channel
	 * 
	 * @return the user channel
	 */
	public SocketChannel getChannel() {
		return this.userChannel;
	}

	/**
	 * Gets the user current state
	 * 
	 * @return the user state
	 */
	public Protocol.State getCurrentState() {
		return this.currentState;
	}

	/**
	 * Sets a new state
	 * 
	 * @param newState to set
	 */
	public void setState(Protocol.State newState) {
		this.currentState = newState;
	}

	/**
	 * Changes the user name
	 * 
	 * @param name to set
	 * @return if was successful
	 */
	public boolean changeName(String name) {
		name = name.replace("\n", "").replace("\r", "");

		if (names.contains(name))
			return false;

		names.add(name);
		names.remove(this.name);
		this.name = name;

		return true;
	}

	/**
	 * Joins a room
	 * 
	 * @param room to join
	 */
	public void joinRoom(Room room) throws IOException {
		leaveRoom();
		room.addUser(this);
		currentRoom = room;
	}

	public void joinRoom(String room) throws IOException {
		joinRoom(Room.getByName(room));
	}

	/**
	 * Leaves the current room
	 */
	public void leaveRoom() throws IOException {
		if (currentRoom == null)
			return;
		currentRoom.removeUser(this);
		currentRoom = null;
	}

	/**
	 * Changes user room
	 * 
	 * @param room to change to
	 */
	public void changeRoom(Room room) throws IOException {
		leaveRoom();
		room.addUser(this);
		currentRoom = room;
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
	public void delete() throws IOException {
		leaveRoom();
		users.remove(this.userChannel);
		if (currentState != Protocol.State.INIT) {
			names.remove(this.name);
		}
		this.userChannel.close();
	}

	public void sendMessage(String message) throws IOException {
		currentRoom.sendMessage(this.name, message);
	}
}
