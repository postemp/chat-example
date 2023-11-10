import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ClientHandler {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private Server server;
    private static int userCount = 0;
    Date loginDate;

    private ServerSocket serverSocket;

    public Date getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(Date loginDate) {
        this.loginDate = loginDate;
    }

    public String getUsername() {
        return username;
    }

    public Boolean AmIAdmin() {
        String myRole = server.getAuthenticationProvider().getRoleByUsername(this.username);
        if (!myRole.equals("ADMIN")) {
            return false;
        }
        return true;
    }

    public ClientHandler(Socket socket, Server server, ServerSocket serverSocket) throws IOException {
        this.socket = socket;
        this.server = server;
        this.serverSocket = serverSocket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        loginDate = new Date();
//        server.subscribe(this);
        new Thread(() -> {
            try {
                sendMessage("Введите логин пароль командой /auth login password");
                sendMessage("или зарегистрируйтесь командой /register login name password");
                authenticateUser(server);
                communicateWithUser(server);
            } catch (IOException e) {
                System.out.println("IOException string 50 ");
                e.printStackTrace();
//                throw new RuntimeException(e);
            } finally {
                System.out.println("ClientHandler Thread disconnect");
//                disconnect();
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
                    User user = server.getAuthenticationProvider().getUsernameByLoginAndPassword(login, password);
                    if (Objects.isNull(user)) {
                        sendMessage("Указан неверный логин/пароль");
                        break;
                    }
                    Date currentDate = new Date();
                    if (currentDate.before(user.getBannedTill())) {
                        sendMessage("Вы заблокированы до :" +user.getBannedTill());
                        break;
                    }
                    this.username = user.getUsername();
                    sendMessage(user.getUsername() + ", добро пожаловать в чат!");
                    server.subscribe(this);
                    isAuthenticated = true;
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
            String message = null;
            try {
                message = in.readUTF();
            } catch (Exception e) {
                System.out.println("Пользователя отключили, поэтому exception");
                break;
                //e.printStackTrace();
            }
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
                    case "/whoami": {
                        String myRole = server.getAuthenticationProvider().getRoleByUsername(this.username);
                        sendMessage("Вы " + username + " ваша роль:" + myRole + " дата блокировки до:");
                        continue;
                    }
                    case "/kick": {
                        if (!AmIAdmin()) {
                            sendMessage("У вас нет прав на отключение из чата пользователей");
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
                            sendMessage("Отключили пользователя:" + kickedUser);
                        } else {
                            sendMessage("Не нашли пользователя:" + kickedUser);
                        }
                        continue;
                    }
                    case "/ban": {
                        // /ban username цифра - время блокировки в минутах, 0 - навечно
                        if (!AmIAdmin()) {
                            sendMessage("Вы не админ, нет у вас таких прав");
                            continue;
                        }
                        String bannedUser;
                        try {
                            bannedUser = args[1];
                        } catch (ArrayIndexOutOfBoundsException e) {
                            sendMessage("Не указан пользователь для блокировки");
                            continue;
                        }
                        int bannedPeriod;
                        try {
                            bannedPeriod = Integer.parseInt(args[2]);
                        } catch (Exception e) {
                            sendMessage("Ошибка ввода периода блокировки, введите количество минут, 0 - до скончания веков, отрицательное число разблокирует пользователя");
                            continue;
                        }
                        sendMessage("Пользователь " + bannedUser + " заблокирован в БД, результат: " + server.getAuthenticationProvider().banUser(bannedUser, bannedPeriod));
                        if (server.kickUser(bannedUser, this.username)) {
                            sendMessage("Отключили пользователя:" + bannedUser);
                        } else {
                            sendMessage("Не нашли пользователя:" + bannedUser);
                        }
                        continue;
                    }
                    case "/unban": {
                        if (!AmIAdmin()) {
                            sendMessage("Вы не админ, нет у вас таких прав");
                            continue;
                        }
                        String unBannedUser;
                        try {
                            unBannedUser = args[1];
                        } catch (ArrayIndexOutOfBoundsException e) {
                            sendMessage("Не указан пользователь для разблокировки");
                            continue;
                        }
                        sendMessage("Пользователь " + unBannedUser + " разблокирован в БД, результат: " + server.getAuthenticationProvider().banUser(unBannedUser, -1000000000));
                        continue;
                    }
                    case "/role": {
                        String whatRole = server.getAuthenticationProvider().getRoleByUsername(this.username);
                        sendMessage("Моя роль:" + whatRole);
                        continue;
                    }
                    case "/allclients": {
                        if (!AmIAdmin()) {
                            sendMessage("Вы не админ, нет у вас таких прав");
                            continue;
                        }
                        sendMessage("Список всех клиентов из БД: " + server.getAuthenticationProvider().getAllClientsList());
                        continue;
                    }
                    case "/shutdown": {
                        if (!AmIAdmin()) {
                            sendMessage("Вы не админ, нет у вас таких прав");
                            continue;
                        }
                        sendMessage("Отключаем сервер");
                        server.serverShutdown();
                        new Socket(serverSocket.getInetAddress(), serverSocket.getLocalPort()).close();
                        this.disconnect();
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
                        boolean isWrittenToDB = server.getAuthenticationProvider().changeNickDB(this.username, newNick);
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
                break;
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
                System.out.println("socket exception ");
                throw new RuntimeException(e);
            }

        }
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
    }

    public void sendMessage(String message) {
        try {
//            System.out.println("We try to send message:"+ message);
            Date currentDate = new Date();
            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
            out.writeUTF(timeFormatter.format(currentDate) + " " + message);
        } catch (IOException e) {
            System.out.println("sendMessage exception string 244 ");
            e.printStackTrace();
            disconnect();
        }
    }
}
