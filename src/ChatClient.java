import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import javax.swing.text.DefaultCaret;

public class ChatClient {

    private static enum Command {
        NICK, JOIN, LEAVE, BYE, MESSAGE, PRIV, OTHER
    }

    // UI Variables (Do not change)
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- End of UI variables

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui
    private SocketChannel clientChannel;

    private ByteBuffer buffer = ByteBuffer.allocate(16384);

    private Queue<Command> sentCommands = new LinkedList<>();

    /*
     * We need this list for the same reason we need sentCommands, we need to know
     * which of the new nick name is valid
     */
    private Queue<String> sentUserName = new LinkedList<>();

    private String userName;

    // Method to add a message to the text box
    // * Do not change *
    public void printMessage(final String message) {
        chatArea.append(message + '\n');
    }

    // Constructor
    public ChatClient(String server, int port) throws IOException {

        // UI initialization --- * Do not change *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocus();
            }
        });

        DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.OUT_BOTTOM);
        // --- End of UI initialization

        /* Creates a new Socket Channel, and makes it NON-BLOCKING */
        clientChannel = SocketChannel.open(new InetSocketAddress(server, port));
        clientChannel.configureBlocking(false);
    }

    private void checkSentCommand(String msg) {
        if (msg.startsWith("/nick")) {
            sentCommands.add(Command.NICK);
            sentUserName.add(msg.split(" ")[1]);

        } else if (msg.startsWith("/join"))
            sentCommands.add(Command.JOIN);
        else if (msg.startsWith("/leave"))
            sentCommands.add(Command.LEAVE);
        else if (msg.startsWith("/bye"))
            sentCommands.add(Command.BYE);
        else if (msg.startsWith("/priv"))
            sentCommands.add(Command.PRIV);
        else
            sentCommands.add(Command.OTHER);
    }

    // Method to send message over the server
    public void newMessage(String msg) throws IOException {
        if (msg.matches("^/.*$"))
            checkSentCommand(msg);
        else
            sentCommands.add(Command.MESSAGE);

        clientChannel.write(ChatServer.charset.encode(msg + "\n"));
    }

    /*
     * Return true if it reads something otherwise false. The message is saved in
     * the buffer
     */
    private boolean readMessage() throws IOException {
        buffer.clear();

        /* Checks if there's data to read */
        boolean hasData = clientChannel.read(buffer) > 0;

        buffer.flip();

        return hasData;
    }

    private String removeNewLines(String original) {
        return original.replace("\n", "").replace("\r", "");
    }

    /* Process received message from server */
    private void processReceivedMessage() throws IOException {
        String msg = ChatServer.decoder.decode(buffer).toString();

        String[] tokens = msg.split(" ");

        switch (removeNewLines(tokens[0])) {
            case "ERROR":
                processError();
                break;
            case "OK":
                processOK();
                break;
            case "JOINED":
                processJoined(removeNewLines(tokens[1]));
                break;
            case "LEFT":
                processLeft(removeNewLines(tokens[1]));
                break;
            case "MESSAGE":
                processMessage(removeNewLines(tokens[1]), removeNewLines(msg.split(" ", 3)[2]));
                break;
            case "NEWNICK":
                processNewNick(removeNewLines(tokens[1]), removeNewLines(tokens[2]));
                break;
            case "PRIVATE":
                processPrivate(removeNewLines(tokens[1]), removeNewLines(msg.split(" ", 3)[2]));
                break;
            case "BYE":
                processBye();
                break;
        }
    }

    private void processError() {
        Command cmd = sentCommands.poll();

        switch (cmd) {
            case NICK:
                printMessage("Nome indisponível.");
                sentUserName.poll();
                break;
            case JOIN:
                printMessage("Deve usar primeiro '/nick <nome>' antes de se juntar a uma sala.");
                break;
            case LEAVE:
                printMessage("Deve estar numa sala antes de usar este comando. Use '/join <sala>'");
                break;
            case MESSAGE:
                printMessage("Deve estar numa sala antes de enviar mensagens. Use '/join <sala>'");
                break;
            case PRIV:
                printMessage("Utilizador inválido.");
                break;
            default:
                printMessage("Comando não suportado.");
                break;
        }
    }

    private void processOK() {
        Command cmd = sentCommands.poll();

        switch (cmd) {
            case NICK:
                printMessage("Nome mudado com sucesso!");
                userName = sentUserName.poll();
                break;
            case JOIN:
                printMessage("Entrou!");
                break;
            case LEAVE:
                printMessage("Saiu.");
                break;
            case PRIV:
                printMessage("Mensagem privada enviada com sucesso!");
                break;
            default:
                // Do nothing
                break;
        }
    }

    private void processJoined(String username) {
        printMessage(username + " juntou-se à sala!");
    }

    private void processLeft(String username) {
        printMessage(username + " saiu da sala!");
    }

    private void processMessage(String username, String message) {
        // Check if it is our message
        if (sentCommands.peek() == Command.MESSAGE && username.contentEquals(userName))
            sentCommands.poll();

        printMessage(username + ": " + message);
    }

    private void processPrivate(String username, String message) {
        printMessage(username + " (Em privado): " + message);
    }

    private void processNewNick(String oldName, String newName) {
        printMessage(oldName + " mudou o nome para " + newName);
    }

    private void processBye() {
        printMessage("Adeus! (Pode fechar a janela)");
    }

    private void listenToServer() throws IOException {
        while (true) {
            /* Active or Inactive check */
            if (readMessage())
                processReceivedMessage();
        }
    }

    /*
     * Note: The main thread is where user UI events will be caught. So when an
     * event is called, it DOES not interfere with the thread where we listen to the
     * server.
     */

    // Run method of the client
    public void run() throws IOException {
        Thread listenToServerThread = new Thread() {
            public void run() {
                try {
                    listenToServer();
                } catch (Exception ignore) { }
            }
        };

        listenToServerThread.start();

        /*
         * Important note: No need to call any other functions since user input is
         * connected to an event listener
         */
    }

    // Initializes the object and runs it
    // * Do not change *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));

        client.run();
    }

}
