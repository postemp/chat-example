import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class Server {
    private int port;
    private List<ClientHandler> clients;

    private final AuthenticationProvider authenticationProvider;

    private ServerSocket serverSocket;

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    private boolean toExit = false;

    public void serverShutdown() {
        this.toExit = true;

        new Thread(() -> {
            try {
                Iterator<ClientHandler> clientHandlerIterator = clients.iterator();
                while (clientHandlerIterator.hasNext()) {
                    ClientHandler clientHandler = clientHandlerIterator.next();
                    clientHandler.sendMessage("Сервер отключается, все на выход!");
                    System.out.println("Отключаем пользователя " + clientHandler.getUsername());
                    Socket socket = clientHandler.getSocket();
                    DataInputStream in = clientHandler.getIn();
                    DataOutputStream out = clientHandler.getOut();
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            System.out.println("in exception ");
                            throw new RuntimeException(e);
                        }
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            System.out.println("out exception ");
                            throw new RuntimeException(e);
                        }
                    }

                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            System.out.println("socket exception ");
                            throw new RuntimeException(e);
                        }

                    }
                    clientHandlerIterator.remove();
                }
            } catch (Exception e) {
                System.out.println("Exception:" + e);
                throw new RuntimeException(e);
            }
        }).start();


        // отключаем сервер
        try {
            new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort()).close();
        } catch (IOException e) {
            System.out.println("exception during closing serverSocket");
            throw new RuntimeException(e);
        }
    }

    public Server(int port, AuthenticationProvider authenticationProvider) {
        this.port = port;
        clients = new ArrayList<>();
        this.authenticationProvider = authenticationProvider;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            new Thread(() -> {
                try {
                    while (!toExit) {
                        // высчитываем разницу между временем логирования и текущим в минутах и отключаем клиента, если больше 20 мин.
                        Date currentDate = new Date();
                        System.out.println("begin----------------------------" + currentDate);
                        long diffInMillies = 0;
                        Iterator<ClientHandler> clientHandlerIterator = clients.iterator();
                        while (clientHandlerIterator.hasNext()) {
                            System.out.println("clientHandlerIterator.hasNext 46 string ");
                            ClientHandler clientHandler = clientHandlerIterator.next();
                            System.out.println("clientHandlerIterator.hasNext 48 string ");
                            diffInMillies = Math.abs(currentDate.getTime() - clientHandler.getLoginDate().getTime()) / 60000; // don't forget to change  to 60000
                            System.out.println("Username()=" + clientHandler.getUsername() + " has been logged for " + Long.toString(diffInMillies) + " min");
                            if (diffInMillies >= 20) {
//                            if (clientHandler.getUsername().equals("Pasha") && diffInMillies >= 1) {
                                clientHandler.sendMessage("Ну нельзя так долго сидеть в чате, идите работать! :)");
                                System.out.println("Отключаем пользователя " + clientHandler.getUsername());
                                Socket socket = clientHandler.getSocket();
                                DataInputStream in = clientHandler.getIn();
                                DataOutputStream out = clientHandler.getOut();

                                if (in != null) {
                                    try {
                                        in.close();
                                    } catch (IOException e) {
                                        System.out.println("in exception ");
                                        throw new RuntimeException(e);
                                    }
                                }
                                if (out != null) {
                                    try {
                                        out.close();
                                    } catch (IOException e) {
                                        System.out.println("out exception ");
                                        throw new RuntimeException(e);
                                    }
                                }

                                if (socket != null) {
                                    try {
                                        socket.close();
                                    } catch (IOException e) {
                                        System.out.println("socket exception ");
                                        throw new RuntimeException(e);
                                    }

                                }
                                clientHandlerIterator.remove();
                            }
                        }
//                        System.out.println("end----------------------------");
                        Thread.sleep(60000); // 60000
                    }
                } catch (Exception e) {
                    System.out.println("Exception:" + e);
                    throw new RuntimeException(e);

                } finally {
                }
            }).start();
            while (!toExit) {
                System.out.println("waiting for request from client on port: " + port);
                Socket socket = serverSocket.accept();
                System.out.println("a new serverSocket.accept()");
                if (toExit) {
                    socket.close();
                    System.out.println("Отключаем сервер по команде пользователя");
                    return;
                }
                new ClientHandler(socket, this, serverSocket);
            }
        } catch (
                IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
