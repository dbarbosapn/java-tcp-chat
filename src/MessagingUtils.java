import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class MessagingUtils {

    private MessagingUtils() {
    }

    public static void sendBye(User user) throws IOException {
        sendMessage(user.getChannel(), MessageType.BYE);
    }

    public static void sendOK(SocketChannel channel) throws IOException {
        sendMessage(channel, MessageType.OK);
    }

    public static void sendOK(User user) throws IOException {
        sendMessage(user.getChannel(), MessageType.OK);
    }

    public static void sendError(SocketChannel channel) throws IOException {
        sendMessage(channel, MessageType.ERROR);
    }

    public static void sendError(User user) throws IOException {
        sendMessage(user.getChannel(), MessageType.ERROR);
    }

    public static void sendMessage(User user, MessageType type, String... data) throws IOException {
        sendMessage(user.getChannel(), type, data);
    }

    private static void sendMessage(SocketChannel channel, MessageType type, String... data) throws IOException {
        String message = null;

        switch (type) {
            case ERROR:
                message = "ERROR";
                break;
            case OK:
                message = "OK";
                break;
            case JOINED:
                message = "JOINED " + data[0];
                break;
            case MESSAGE:
                message = "MESSAGE " + data[0] + " " + data[1];
                break;
            case NEWNICK:
                message = "NEWNICK " + data[0] + " " + data[1];
                break;
            case LEFT:
                message = "LEFT " + data[0];
                break;
            case BYE:
                message = "BYE";
                break;
        }

        message += '\n';

        if (message != null) {
	    channel.write(ChatServer.charset.encode(message));
        }
    }

}
