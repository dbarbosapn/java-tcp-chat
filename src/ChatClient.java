import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ChatClient {

    // UI Variables (Do not change)
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- End of UI variables

    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui

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

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui

    }

    // Method to send message over the server
    public void newMessage(String message) throws IOException {

    }

    // Run method of the client
    public void run() throws IOException {

    }

    // Initializes the object and runs it
    // * Do not change *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }

}