package server;

public interface AuthService {
    boolean registration(String login, String password);
    boolean authent(String login, String password);
}
