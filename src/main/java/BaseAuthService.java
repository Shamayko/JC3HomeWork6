

import java.sql.SQLException;


public class BaseAuthService implements AuthService {

    private UsersDB usersDB;

    //Создаем класс для хранения логинов, паролей и ников
    private static class Entry {
        private String nick;
        private String login;
        private String pass;

        //Создаем конструктор класса
        public Entry(String login, String pass, String nick) {
            this.nick = nick;
            this.login = login;
            this.pass = pass;
        }
    }

    //Объявляем переменную типа Лист для хранений авторизационных данных
//    private final List<Entry> entries;

    //Создаем объект класса BaseAuthService и наполняем его авторизационными данными DB.
    public BaseAuthService() {

        usersDB = new UsersDB();
        try {
            usersDB.createTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


//   Old version
//    public BaseAuthService() {
//        entries = new ArrayList<>();
//        entries.add(new Entry("l1", "p1", "Ctulhu"));
//        entries.add(new Entry("l2", "p2", "PinkPanter"));
//        entries.add(new Entry("l3", "p3", "Homer"));
//    }

    //Переопределяем методы старт, стоп и получения Ника по логину и паролю.
    @Override
    public void start() {
        System.out.println(this.getClass().getName() + " server started");
    }

    @Override
    public void stop() {
        System.out.println(this.getClass().getName() + " server stopped");
    }

    @Override
    public String getNickByLoginAndPass(String login, String pass) {
        return usersDB.getNickname(login, pass);
    }

    @Override
    public boolean changeNickname(String login, String newNickname) {
        return usersDB.changeNickname(login, newNickname);
    }

    @Override
    public boolean createUser(String login, String password, String nickname) {
        return usersDB.createUser(login, password, nickname);
    }
}