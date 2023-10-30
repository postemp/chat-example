public interface AuthenticationProvider {
    String getUsernameByLoginAndPassword(String login, String password);

    boolean register(String login, String password, String username);

    String getRoleByUsername(String username);

    String getActiveClientsList();

    String getAllClientsList();

    boolean changeNick(ClientHandler clientHandler);
}
