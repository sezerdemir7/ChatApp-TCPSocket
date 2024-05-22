import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Pattern;

public class ClientGUI extends JFrame {

    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public ClientGUI() {
        setTitle("Chat Client");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());

        String ipAddress = null;
        int port = 0;
        boolean validInput = false;

        while (!validInput) {
            ipAddress = JOptionPane.showInputDialog(null, "Enter Server IP Address:", "Server IP", JOptionPane.QUESTION_MESSAGE);
            String portStr = JOptionPane.showInputDialog(null, "Enter Server Port Number:", "Server Port", JOptionPane.QUESTION_MESSAGE);

            if (isValidIPAddress(ipAddress) && isValidPort(portStr)) {
                port = Integer.parseInt(portStr);
                validInput = true;
            } else {
                JOptionPane.showMessageDialog(null, "Invalid IP address or port number. Please try again.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        }

        connectToServer(ipAddress, port);

        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = messageField.getText();
                sendMessage(message);
                messageField.setText("");
            }
        });

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.SOUTH);

        add(panel);

        // Handle window closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sendMessage("disconnect");
                try {
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        clientSocket.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                super.windowClosing(e);
            }
        });

        setVisible(true);
    }

    private void connectToServer(String ipAddress, int port) {
        try {
            clientSocket = new Socket(ipAddress, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            while (true) {
                String nickname = JOptionPane.showInputDialog(null, "Enter Your Nickname (at least 5 characters):", "Nickname", JOptionPane.QUESTION_MESSAGE);
                if (nickname != null && nickname.length() >= 5) {
                    sendMessage(nickname);
                    String response = in.readLine();
                    if ("NICKNAME_ACCEPTED".equals(response)) {
                        break;
                    } else {
                        JOptionPane.showMessageDialog(null, "Nickname is already taken or invalid. Please try again.", "Invalid Nickname", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Nickname must be at least 5 characters long.", "Invalid Nickname", JOptionPane.ERROR_MESSAGE);
                }
            }

            startListening();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not connect to the server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startListening() {
        Thread thread = new Thread(() -> {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    chatArea.append(message + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(ClientGUI.this, "Disconnected from the server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        thread.start();
    }

    private void sendMessage(String message) {
        out.println(message);
    }

    private boolean isValidIPAddress(String ip) {
        String ipPattern =
                "^(([0-9]{1,3})\\.){3}([0-9]{1,3})$";
        return Pattern.matches(ipPattern, ip);
    }

    private boolean isValidPort(String portStr) {
        try {
            int port = Integer.parseInt(portStr);
            return port >= 1 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}
