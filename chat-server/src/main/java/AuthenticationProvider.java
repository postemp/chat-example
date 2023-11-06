public interface AuthenticationProvider {
    String getUsernameByLoginAndPassword(String login, String password);

    boolean register(String login, String password, String username);

    String getRoleByUsername(String username);

    String getAllClientsList();

    boolean changeNickDB(String oldNick, String newNick);

    boolean banUser(String bannedUser, int bannedPeriod);
}
