import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private int port;
    private List<ClientHandler> clients;

    public Server(int port) {
        this.port = port;
        clients = new ArrayList<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                System.out.println("server has started on port" + port);
                Socket socket = serverSocket.accept();
                new ClientHandler(socket, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler){
        clients.add(clientHandler);
        broadcastMessage("Client "+ clientHandler.getUsername() + " has connected to chat");

    }
    public synchronized void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            System.out.println("message to: "+client.getUsername());
            client.sendMessage(message);
        }
    }

    public synchronized void sendMessageToUser(String user, String message) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(user)){
                System.out.println("message to: "+client.getUsername());
                client.sendMessage(message);
            }
        }
    }
    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastMessage("Client "+ clientHandler.getUsername() + " has disconnected");
    }
}
