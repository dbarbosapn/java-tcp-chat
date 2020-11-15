import java.io.IOException;
import java.nio.channels.SocketChannel;

public class Protocol {
    public static enum State {
        INIT, INSIDE, OUTSIDE
    };

    private static String[] parseInput(String input) {
        return input.split(" ");
    }

    public static void processInput(String input, SocketChannel channel) throws IOException {
        User user = User.getByChannel(channel);

        switch (user.getCurrentState()) {
            case INIT:
                initState(input, user);
                break;
            case INSIDE:
                insideState(input, user);
                break;
            case OUTSIDE:
                outsideState(input, user);
                break;
        }
    }

    private static void initState(String input, User user) throws IOException {
        String[] tokens = parseInput(input);

        switch (tokens[0]) {
            case "/nick":
                nickCommand(tokens, user);
                break;
            default:
                MessagingUtils.sendError(user);
        }
    }

    private static String parseInsideInput(String input) {
        if (input.charAt(0) == '/' && (input.length() > 1 && input.charAt(1) == '/')) {
            return input.substring(1);
        } else if (input.charAt(0) == '/') {
            return null;
        } else {
            return input;
        }
    }

    private static void insideState(String input, User user) throws IOException {
        String message = parseInsideInput(input);

        if (message == null) {
            String[] tokens = parseInput(input);

            switch (tokens[0]) {
                case "/join":
                    joinCommand(tokens, user);
                    break;
                default:
                    MessagingUtils.sendError(user);
            }
        } else {
            // TODO: Check if input starts with /. If it does, remove it.
            // (input.substring(1))
            user.sendMessage(message);
        }
    }

    private static void outsideState(String input, User user) throws IOException {
        String[] tokens = parseInput(input);

        switch (tokens[0]) {
            case "/nick":
                nickCommand(tokens, user);
                break;
            case "/join":
                joinCommand(tokens, user);
                break;
            default:
                MessagingUtils.sendError(user);
        }
    }

    private static void nickCommand(String[] tokens, User user) throws IOException {
        if (tokens.length != 2 || !user.changeName(tokens[1])) {
            MessagingUtils.sendError(user);
        } else {
            if (user.getCurrentState() == State.INIT)
                user.setState(State.OUTSIDE);
            MessagingUtils.sendOK(user);
        }
    }

    private static void joinCommand(String[] tokens, User user) throws IOException {
        if (tokens.length != 2) {
            MessagingUtils.sendError(user);
        } else {
            Room room = Room.getByName(tokens[1]);
            user.joinRoom(room);
            MessagingUtils.sendOK(user);
        }

        user.setState(State.INSIDE);
    }
}
