import java.sql.SQLException;

public interface AuthService {

    //Создаем интерфейс, который контрактует нам методы старта, стопа и получения логина.
    void start();

    void stop();

    String getNickByLoginAndPass(String login, String pass) throws SQLException;
    boolean createUser(String login, String password, String nickname);
    boolean changeNickname(String login, String newNickname);
}

