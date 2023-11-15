import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class InMemoryAuthenticationProvider implements AuthenticationProvider {
    private static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String dbUser = "postgres";
    private static final String dbPassword = "postgres";
    private static final String SELECT_ALL_USERS_WITH_ROLE = "select u.id as id, u.login as login, u.username as username, r.roles_name, u.password, u.banned_till from users u, roles r, user_to_roles ur " +
            "where ur.user_id = u.id and ur.role_id = r.id";

    private final List<User> users;

    public InMemoryAuthenticationProvider() {
        this.users = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, dbUser, dbPassword)) {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_ALL_USERS_WITH_ROLE)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String login = rs.getString("login");
                        String userName = rs.getString("username");
                        String role = rs.getString("roles_name");
                        String password = rs.getString("password");
                        Timestamp bannedTill = rs.getTimestamp("banned_till");
                        users.add(new User(login, password, role, userName, bannedTill));
                    }
                } catch (SQLException e) {
                    System.out.println(e);
                }
            } catch (SQLException e) {
                System.out.println(e);
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

    @Override
    public User getUsernameByLoginAndPassword(String login, String password) {
        for (User user : users) {
            if (Objects.equals(user.getPassword(), password) && Objects.equals(user.getLogin(), login)) {
                return user;
            }
        }
        return null;
    }


    @Override
    public synchronized boolean register(String login, String password, String username) {
        for (User user : users) {
            if (Objects.equals(user.getUsername(), username) || Objects.equals(user.getLogin(), login)) {
                return false;
            }
        }

        // добавляем запись нового юзера в БД по умолчанию с правами USER
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, dbUser, dbPassword)) {
            connection.setAutoCommit(false);
            int userId;
            PreparedStatement ps = connection.prepareStatement("insert into public.users ( login, username, password ) values (?,?,?);", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, login);
            ps.setString(2, username);
            ps.setString(3, password);
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                    System.out.println("UserId is- " + userId);
                } else {
                    connection.rollback();
                    throw new SQLException("User insertion has problem. No ID returned.");
                }
            }
            ps.close();
            ps = connection.prepareStatement("insert into public.user_to_roles (user_id, role_id) values (?,2);");
            ps.setInt(1, userId);
            ps.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            System.out.println("Connection to database failed");
            System.out.println(e);
        }
        // добавляем запись нового юзера в массив пользователей по умолчанию с правами USER
        users.add(new User(login, password, "USER", username));
        return true;
    }

    @Override
    public String getRoleByUsername(String username) {
        for (User user : users) {
            if (Objects.equals(user.getUsername(), username)) {
                return user.getRole();
            }
        }
        return null;
    }

    @Override
    public String getAllClientsList() {
        String activeClientList = "";
        for (User user : users) {
            activeClientList += user.getUsername() + "; ";
        }
        return activeClientList;
    }

    @Override
    public boolean changeNickDB(String oldNick, String newNick) {
        System.out.println("Old nick" + oldNick);
        boolean result = false;
        for (User user : users) {
            if (Objects.equals(user.getUsername(), oldNick)) {
                System.out.println("Мы нашли ник пользователя " + oldNick + " с логином: " + user.getLogin() + " в памяти,  меняем на новый ник" + newNick);
                user.setUsername(newNick);
                try (Connection connection = DriverManager.getConnection(DATABASE_URL, dbUser, dbPassword)) {
                    connection.setAutoCommit(false);
                    PreparedStatement ps = connection.prepareStatement("select id, login from public.users where username = ?");
                    ps.setString(1, oldNick);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            long userId = rs.getLong("id");
                            String login = rs.getString("login");
                            if (login.equals(user.getLogin())) {
                                System.out.println("Юзер найден в БД, меняем его ник");
                                ps.close();
                                ps = connection.prepareStatement("update public.users  set username = ? where id = ?;");
                                ps.setString(1, newNick);
                                ps.setLong(2, userId);
                                ps.executeUpdate();
                                connection.commit();
                                result = true;
                                break;
                            }
                        }
                    } catch (SQLException e) {
                        System.out.println(e);
                    }
                } catch (SQLException e) {
                    System.out.println("Connection to database failed");
                    System.out.println(e);
                }
                return result;
            }
        }
        return false;
    }

    @Override
    public boolean banUser(String bannedUser, int bannedPeriod) {
        boolean result = false;
        Date currentDate = new Date();
        long curTimeInMs = currentDate.getTime();
        Date blockedUntilDate;
        if (bannedPeriod == 0) {
            blockedUntilDate = new Date(121212212121212L); // устанавливаем оочень позднюю дату
        } else if (bannedPeriod < 0) {
            blockedUntilDate = new Date(1L); // устанавливаем самую раннюю дату 1970-01-01 03:00:00.001
        } else {
            blockedUntilDate = new Date(curTimeInMs + (bannedPeriod * 60000));
        }

        System.out.println("Устанавливаем дату блокировки до: " + blockedUntilDate);

        for (User user : users) {
            if (Objects.equals(user.getUsername(), bannedUser)) {
                System.out.println("Найден ник пользователя " + bannedUser + " с логином: " + user.getLogin() + " в памяти, устанавливаем время блокировки");
                user.setBannedTill(blockedUntilDate);

                try (Connection connection = DriverManager.getConnection(DATABASE_URL, dbUser, dbPassword)) {
                    connection.setAutoCommit(false);
                    PreparedStatement ps = connection.prepareStatement("select id, login from public.users where username = ?");
                    ps.setString(1, bannedUser);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            long userId = rs.getLong("id");
                            String login = rs.getString("login");
                            if (login.equals(user.getLogin())) {
                                System.out.println("Юзер найден в БД, устанавливаем блокировку");
                                ps.close();
                                ps = connection.prepareStatement("update public.users set banned_till = ? where id = ?;");
                                long time = blockedUntilDate.getTime();
                                ps.setTimestamp(1, new Timestamp(time));
                                ps.setLong(2, userId);
                                ps.executeUpdate();
                                connection.commit();
                                result = true;
                                break;
                            }
                        }
                    } catch (SQLException e) {
                        System.out.println(e);
                    }
                } catch (SQLException e) {
                    System.out.println("Connection to database failed");
                    System.out.println(e);
                }
            }
        }
        return result;
    }
}
