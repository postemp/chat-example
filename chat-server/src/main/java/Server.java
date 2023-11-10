import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class Server {
    private int port;
    private List<ClientHandler> clients;

    private final AuthenticationProvider authenticationProvider;

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    private boolean toExit = false;

    public void serverShutdown() {
        this.toExit = true;
    }

    public Server(int port, AuthenticationProvider authenticationProvider) {
        this.port = port;
        clients = new ArrayList<>();
        this.authenticationProvider = authenticationProvider;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
//            new Thread(() -> {
//                try {
////                    ClientHandler clientHandler; // для варианта с итератором
//                    while (!toExit) {
//                        // высчитываем разницу между временем логирования и текущим в минутах и отключаем клиента, если больше 20 мин.
//                        Date currentDate = new Date();
//                        System.out.println("begin----------------------------"+currentDate);
//                        long diffInMillies = 0;
//                        for (ClientHandler clientHandler : clients) {
//                            diffInMillies = Math.abs(currentDate.getTime() - clientHandler.getLoginDate().getTime()) / 60000; // don't forget to change  to 60000
////                            System.out.println("Username()=" + clientHandler.getUsername() + " has been logged for " + Long.toString(diffInMillies) + " min");
//                            if (diffInMillies >= 20) {  // по хорошему время нужно брать из переменной из файла настроек, но не успел....
////                            if (clientHandler.getUsername().equals("Sasha") && diffInMillies >= 20) {
//                                System.out.println("Отключаем пользователя "+ clientHandler.getUsername());
//                                clientHandler.sendMessage("Ну нельзя так долго сидеть в чате, идите работать! :)");
//                                clientHandler.disconnect();
////                                clients.remove(clientHandler);
//                                break;
//                            }
//                        }
////                    вариант с итератором, по моему, избыточен
////                    ListIterator<ClientHandler> clientHandlerIterator = clients.listIterator();
////                        while (clientHandlerIterator.hasNext()) {
////                            System.out.println("clientHandlerIterator.hasNext 46 string ");
////                            clientHandler =clientHandlerIterator.next();
////                            System.out.println("clientHandlerIterator.hasNext 48 string ");
////                            diffInMillies = Math.abs(currentDate.getTime() - clientHandler.getLoginDate().getTime()) / 5000; // don't forget to change  to 60000
////                            System.out.println("Username()=" + clientHandler.getUsername() + " has been logged for " + Long.toString(diffInMillies) + " min");
//////                            if (diffInMillies >= 20) {
////                            if (clientHandler.getUsername().equals("Sasha") && diffInMillies >= 1) {
////                                clientHandler.sendMessage("Ну нельзя так долго сидеть в чате, идите работать! :)");
////                                clientHandler.disconnect();
////                                clientHandlerIterator.remove();
//////                                clients.remove(clientHandler);
//////                                break;
////                            }
////                        }
//                        System.out.println("end----------------------------");
//                        Thread.sleep(60000); // 60000
//                    }
//                } catch (Exception e)  {
//                    System.out.println("Exception:" + e);
//                    throw new RuntimeException(e);
//
//                } finally {
//                }
//            }).start();
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
