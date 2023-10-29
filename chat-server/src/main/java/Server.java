import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Server {
    private int port;
    private List<ClientHandler> clients;

    private final AuthenticationProvider authenticationProvider;

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public Server(int port, AuthenticationProvider authenticationProvider) {
        this.port = port;
        clients = new ArrayList<>();
        this.authenticationProvider = authenticationProvider;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                System.out.println("server has started on port: " + port);
                Socket socket = serverSocket.accept();
                new ClientHandler(socket, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastMessage("Client " + clientHandler.getUsername() + " has connected to chat");

    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            System.out.println("message to: " + client.getUsername());
            client.sendMessage(message);
        }
    }

    public synchronized void sendMessageToUser(String user, String message) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(user)) {
                System.out.println("message to: " + client.getUsername());
                client.sendMessage(message);
            }
        }
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastMessage("Client " + clientHandler.getUsername() + " has disconnected");
    }

    public synchronized List<String> getUserList() {
//                вот так правильнее, но непонятно, поэтому пока переписал под свой уровень что бы разобраться.
//                return clients.stream()
//                .map(ClientHandler::getUsername)
//                .collect(Collectors.toList());

        List<String> userList = new ArrayList<>();
        for (ClientHandler clientHandler : clients) {
            System.out.println("clientHandler.getUsername()=" + clientHandler.getUsername());
            userList.add(clientHandler.getUsername());
        }
        return userList;


    }

    public synchronized boolean kickUser(String user, String whoDoes) {
        System.out.println("Отключает пользователь: " + whoDoes);
        for (ClientHandler client : clients) {
            System.out.println("User:" + client.getUsername());
            if (client.getUsername().equals(user)) {
                System.out.println("kick user: " + client.getUsername());
                client.sendMessage("Вас отключают");
                client.disconnect();
                return true;
            }
        }
        return false;
    }
}
