import java.util.Date;

public class User {
    private String login;
    private String password;
    private String username;
    private String role; // USER, ADMIN

    private Date bannedTill;
    public Date getBannedTill() {
        if (bannedTill == null) {
            return new Date(0);
        }
        return bannedTill;
    }
    public void setBannedTill(Date bannedTill) {
        this.bannedTill = bannedTill;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() { return role; }

    public User(String login, String password, String role, String username, Date bannedTill) {
        this.login = login;
        this.password = password;
        this.role = role;
        this.username = username;
        this.bannedTill = bannedTill;
    }

    public User(String login, String password, String role, String username) {
        this.login = login;
        this.password = password;
        this.role = role;
        this.username = username;
    }
}
