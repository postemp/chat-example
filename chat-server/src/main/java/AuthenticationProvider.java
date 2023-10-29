public interface AuthenticationProvider {
    String getUsernameByLoginAndPassword(String login, String password);

    boolean register(String login, String password, String username, String nick);

    String getRoleByUsername(String username);

    String getActiveClientsList();

    String getAllClientsList();

}
