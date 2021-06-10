package server;

public class DBAuthServise implements AuthService{
    @Override
    public boolean registration(String login, String password) {
        return SQL_Handler.registration(login, password);
    }

    @Override
    public boolean authent(String login, String password) {
        return SQL_Handler.authent(login, password);
    }
}
