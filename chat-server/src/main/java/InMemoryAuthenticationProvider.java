import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InMemoryAuthenticationProvider implements AuthenticationProvider {
    private static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String dbUser = "postgres";
    private static final String dbPassword = "postgres";
    private static final String SELECT_ALL_USERS_WITH_ROLE = "select u.id as id, u.login as login, u.username as username, r.roles_name, u.password  from users u, roles r, user_to_roles ur " +
            "where ur.user_id = u.id and ur.role_id = r.id";

    private final List<User> users;

    public InMemoryAuthenticationProvider() {
        this.users = new ArrayList<> ();
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, dbUser, dbPassword)) {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_ALL_USERS_WITH_ROLE)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        String login = rs.getString("login");
                        String userName = rs.getString("username");
                        String role = rs.getString("roles_name");
                        String password = rs.getString("password");
                        users.add(new User(login, password, role, userName));
                    }
                } catch (SQLException e) {
                }
            } catch (SQLException e) {
            }
        } catch (SQLException e) {
        }
    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
        for (User user: users) {
            if (Objects.equals(user.getPassword(), password) && Objects.equals(user.getLogin(), login)) {
                return user.getUsername();
            }
        }
        return null;
    }


    @Override
    public synchronized boolean register(String login, String password, String username) {
        for (User user : users) {
            if (Objects.equals(user.getUsername(), username) && Objects.equals(user.getLogin(), login)) {
                return false;
            }
        }

        // добавляем запись нового юзера в БД по умолчанию с правами USER
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, dbUser, dbPassword)) {
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            int userId;
            PreparedStatement ps = connection.prepareStatement("insert into public.users ( login, username, password ) values (?,?,?);", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1,login);
            ps.setString(2,username);
            ps.setString(3,password);
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                    System.out.println("UserId is- " + userId);
                }
                else {
                    connection.rollback();
                    throw new SQLException("User insertion has problem. No ID returned.");
                }
            }
            ps.close();
            ps = connection.prepareStatement("insert into public.user_to_roles (user_id, role_id) values (?,2);");
            ps.setInt(1,userId);
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
    public String getRoleByUsername(String username){
        for (User user : users) {
            if (Objects.equals(user.getUsername(), username)) {
                return user.getRole();
            }
        }
        return null;
    }

    @Override
    public String getActiveClientsList() {
        String activeClientList = "";
        // придумать как получить список активных клиентов
        return activeClientList;
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
    public boolean changeNick(ClientHandler clientHandler){
        System.out.println("clientHandler.getUsername()"+clientHandler.getUsername());
        // tie to the current nickname clientHandler.getUsername()) and change nik in the DB
        return false;
    }
}