import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class ChatClient {

    // UI Variables (Do not change)
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- End of UI variables

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui
    private SocketChannel clientChannel;

    static private ByteBuffer buffer = ByteBuffer.allocate(6969);

    // Method to add a message to the text box
    // * Do not change *
    public void printMessage(final String message) {
        chatArea.append(message);
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
        // --- End of UI initialization

        /* Creates a new Socket Channel, and makes it NON-BLOCKING */
        clientChannel = SocketChannel.open(new InetSocketAddress(server, port));
        clientChannel.configureBlocking(false);
    }

    // Method to send message over the server
    public void newMessage(String msg) throws IOException {
        clientChannel.write(ChatServer.charset.encode(msg));
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

    /* Process received message from server */
    private void processReceivedMessage() throws IOException {
        String msg = ChatServer.decoder.decode(buffer).toString();

        printMessage(msg);
    }

    // Run method of the client
    public void run() throws IOException {
        while (true) {
            /* Active or Inactive check */
            if (!readMessage())
                continue;

            processReceivedMessage();
        }
    }

    // Initializes the object and runs it
    // * Do not change *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));

        client.run();
    }

}
