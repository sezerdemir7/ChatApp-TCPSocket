import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerGUI extends JFrame {
    private final JTextArea logTextArea;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;
    private final Map<String, PrintWriter> clientWriters;

    public ServerGUI() {
        setTitle("Chat Server");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        add(panel);

        clientWriters = new HashMap<>();
    }

    public void run() {
        try {
            server = new ServerSocket(12345);
            pool = Executors.newCachedThreadPool();
            logTextArea.append("Server started\n");
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                pool.execute(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        try {
            done = true;
            if (!server.isClosed()) {
                server.close();
            }
            if (pool != null) {
                pool.shutdown();
            }
            logTextArea.append("Server stopped\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ConnectionHandler implements Runnable {
        private final Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String username;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                while (true) {
                    username = in.readLine();
                    if (isNicknameValid(username)) {
                        synchronized (clientWriters) {
                            clientWriters.put(username, out);
                        }
                        out.println("NICKNAME_ACCEPTED");
                        logTextArea.append(username + " connected!\n");
                        broadcast(username + " connected!");
                        break;
                    } else {
                        out.println("NICKNAME_INVALID");
                    }
                }

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equals("disconnect")) {
                        logTextArea.append(username + " disconnected\n");
                        broadcast(username + " disconnected");
                        break;
                    }
                   // logTextArea.append(username + ": " + message + "\n");
                    broadcast(username + ": " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                    if (!client.isClosed()) {
                        client.close();
                    }
                    synchronized (clientWriters) {
                        clientWriters.remove(username);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean isNicknameValid(String nickname) {
            synchronized (clientWriters) {
                return !clientWriters.containsKey(nickname) && nickname.length() >= 5;
            }
        }

        private void broadcast(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters.values()) {
                    writer.println(message);
                }
            }
        }
    }

    public static void main(String[] args) {
        ServerGUI serverGUI = new ServerGUI();
        serverGUI.setVisible(true);
        serverGUI.run();
    }
}
