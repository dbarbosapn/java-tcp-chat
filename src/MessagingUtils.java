import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class MessagingUtils {

    private MessagingUtils() {
    }

    public static void sendOK(SelectionKey key) throws IOException {
        sendMessage(key, MessageType.OK);
    }

    public static void sendOK(User user) throws IOException {
        sendMessage(user.getKey(), MessageType.OK);
    }

    public static void sendError(SelectionKey key) throws IOException {
        sendMessage(key, MessageType.ERROR);
    }

    public static void sendError(User user) throws IOException {
        sendMessage(user.getKey(), MessageType.ERROR);
    }

    private static void sendMessage(SelectionKey key, MessageType type, String... data) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
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
            channel.write(ByteBuffer.wrap(message.getBytes()));
        }
    }

}
