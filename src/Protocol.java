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
        input = input.replace("\n", "").replace("\r", "");

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
            case "/bye":
                byeCommand(user);
                break;
            default:
                MessagingUtils.sendError(user);
        }
    }

    private static void insideState(String input, User user) throws IOException {
        /* Checks if its a message without // at the start */
        if (input.matches("^[^/](.|\\s)*"))
            user.sendMessage(input);
        /* Checks if its a message with // at the start */
        else if (input.matches("^(//)(.|\\s)*"))
            user.sendMessage(input.replaceFirst("/", ""));
        else {
            String[] tokens = parseInput(input);

            switch (tokens[0]) {
                case "/join":
                    joinCommand(tokens, user);
                    break;
                case "/nick":
                    nickCommand(tokens, user);
                    break;
                case "/leave":
                    leaveCommand(user);
                    break;
                case "/priv":
                    privCommand(user, input);
                    break;
                case "/bye":
                    byeCommand(user);
                    break;
                default:
                    MessagingUtils.sendError(user);

            }
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
            case "/bye":
                byeCommand(user);
                break;
            case "/priv":
                privCommand(user, input);
                break;
            default:
                MessagingUtils.sendError(user);
        }
    }

    private static void nickCommand(String[] tokens, User user) throws IOException {
        String oldName = user.getName();

        if (tokens.length != 2 || !user.changeName(tokens[1])) {
            MessagingUtils.sendError(user);
        } else {
            if (user.getCurrentState() == State.INIT)
                user.setState(State.OUTSIDE);
            else if (user.getCurrentState() == State.INSIDE)
                user.getCurrentRoom().notifyChangedNickname(oldName, user.getName());
            MessagingUtils.sendOK(user);
        }
    }

    private static void privCommand(User user, String input) throws IOException {
        String[] tokens = input.split(" ", 3);

        if (tokens.length == 3 && user.sendPrivateMessage(tokens[1], tokens[2])) {
            MessagingUtils.sendOK(user);
        } else {
            MessagingUtils.sendError(user);
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

    private static void leaveCommand(User user) throws IOException {
        user.leaveRoom();
        MessagingUtils.sendOK(user);
        user.setState(State.OUTSIDE);
    }

    private static void byeCommand(User user) throws IOException {
        MessagingUtils.sendBye(user);
        user.delete();
    }

}
