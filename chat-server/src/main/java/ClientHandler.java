import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ClientHandler {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    private Server server;

    private static int userCount = 0;

    public String getUsername() {
        return username;
    }

    public Boolean ifIAmAdmin() {
        String myRole = server.getAuthenticationProvider().getRoleByUsername(this.username);
        if (!myRole.equals("ADMIN")) {
            return false;
        }
        return true;
    }


    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
//        server.subscribe(this);
        new Thread(() -> {
            try {
                sendMessage("Введите логин пароль командой /auth login password");
                sendMessage("или зарегистрируйтесь командой /register login name password");
                authenticateUser(server);
                communicateWithUser(server);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                disconnect();
            }
        }).start();
    }

    private void authenticateUser(Server server) throws IOException {
        boolean isAuthenticated = false;
        while (!isAuthenticated) {
            String message = in.readUTF();
            message = message.trim().replaceAll(" +", " "); // удаляем лишние случайные пробелы
//            /auth login password
//            /register login nick role password
            String[] args = message.split(" ");
            String command = args[0];
            switch (command) {
                case "/auth": {
                    String login = args[1];
                    String password = args[2];
                    String username = server.getAuthenticationProvider().getUsernameByLoginAndPassword(login, password);
                    if (username == null || username.isBlank()) {
                        sendMessage("Указан неверный логин/пароль");
                    } else {
                        this.username = username;
                        sendMessage(username + ", добро пожаловать в чат!");
                        server.subscribe(this);
                        isAuthenticated = true;
                    }
                    break;
                }
                case "/register": {
                    String login = args[1];
                    String username = args[2];
//                    String role = args[3];
//                    if (!(role.equals("ADMIN") || role.equals("USER"))) {
//                        System.out.println("Роль может быть ADMIN или USER");
//                        sendMessage("Роль может быть ADMIN или USER");
//                        continue;
//                    }
                    String password = args[3];
                    boolean isRegistred = server.getAuthenticationProvider().register(login, password, username);
                    if (!isRegistred) {
                        sendMessage("Указанный логин/никнейм уже заняты");
                    } else {
                        this.username = username;
                        sendMessage(username + ", добро пожаловать в чат!");
                        server.subscribe(this);
                        isAuthenticated = true;
                    }
                    break;
                }
                default: {
                    sendMessage("Авторизуйтесь сперва");
                }
            }

        }

    }

    private void communicateWithUser(Server server) throws IOException {
        while (true) {
            // /exit -> disconnect()
            // /w user message -> user

            String message = in.readUTF();
            if (message.startsWith("/")) {
                String[] args = message.split(" ");
                String command = args[0];
                System.out.println("command = " + command);
                switch (command) {
                    case "/exit": {
                        System.out.println("exit");
                        this.disconnect();
                        break;
                    }
                    case ("/activelist"): {
                        System.out.println("list");
                        List<String> userList = server.getUserList();
                        String joinedUsers =
                                String.join(", ", userList);
//                            userList.stream().collect(Collectors.joining(","));
                        sendMessage(joinedUsers);
                        continue;
                    }
                    case "/w": {
                        System.out.println("w");
                        String user = message.replaceAll("^/w\\s+(\\w+)\\s+.+", "$1");
                        message = message.replaceAll("^/w\\s+(\\w+)\\s+(.+)", "$2");
                        System.out.println("user = " + user + " message = " + message);
                        server.sendMessageToUser(user, message);
                        continue;
                    }
                    case "/kick": {
                        String myRole = server.getAuthenticationProvider().getRoleByUsername(this.username);
                        if (!myRole.equals("ADMIN")) {
                            sendMessage("У вас нет прав на удаление пользователей, ваша роль - " + myRole);
                            continue;
                        }
                        String kickedUser;
                        try {
                            kickedUser = args[1];
                        } catch (ArrayIndexOutOfBoundsException e) {
                            sendMessage("Не указан пользователь для удаления");
                            continue;
                        }
                        if (server.kickUser(kickedUser, this.username)) {
                            sendMessage("Удалили пользователя:" + kickedUser);
                        } else {
                            sendMessage("Не нашли пользователя:" + kickedUser);
                        }
                        continue;
                    }
                    case "/role": {
                        String whatRole = server.getAuthenticationProvider().getRoleByUsername(this.username);
                        sendMessage("Моя роль:" + whatRole);
                        continue;
                    }
                    case "/allclients": {
                        if (ifIAmAdmin()) {
                            sendMessage("Список всех клиентов из БД: " + server.getAuthenticationProvider().getAllClientsList());
                        } else {
                            sendMessage("Вы не админ, нет у вас таких прав");
                        }
                        continue;
                    }
                    case "/changenick": { // смена своего ника
                        String newNick;
                        try {
                            newNick = args[1];
                        } catch (ArrayIndexOutOfBoundsException e) {
                            sendMessage("Не указан новый ник");
                            continue;
                        }

                        boolean isWrittenToDB = server.getAuthenticationProvider().changeNick(this);
                        if (isWrittenToDB) {
                            this.username = newNick;
                            sendMessage("Ваш ник сменен на " + this.username);
                        } else {
                            sendMessage("Не удалось сменить ваш ник, при записи в БД возникли проблемы");
                        }
                        continue;
                    }
                    default: {
                        System.out.println("default");
                        sendMessage("Неопознанная команда");
                    }
                }
            } else {
                server.broadcastMessage("Broadcast message: " + message);
            }
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendMessage(String message) {
        try {
//            System.out.println("We try to send message:"+ message);
            Date currentDate = new Date();
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
            out.writeUTF(timeFormatter.format(currentDate) + " " + message);
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }
}
