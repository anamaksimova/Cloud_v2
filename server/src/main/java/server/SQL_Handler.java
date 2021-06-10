package server;

import java.sql.*;

public class SQL_Handler {
    private static Connection connection;
    private static PreparedStatement psRegistration;
    private static PreparedStatement psAuthent;

    public static boolean connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:cloud.db");
            System.out.println("DB connected");
            prepareAllStatements();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void prepareAllStatements() throws SQLException {

        psRegistration = connection.prepareStatement("INSERT INTO cloudusers(login, password) VALUES (? ,? );");
        psAuthent = connection.prepareStatement("SELECT COUNT(login) FROM  cloudusers WHERE login=? and password = ? ;");

    }

    public static boolean registration(String login, String password) {
        try {
            psRegistration.setString(1, login);
            psRegistration.setString(2, password);
            psRegistration.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static void disconnect() {
        try {
            psRegistration.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static boolean authent(String login, String password)  {
        boolean answer = false;
        try {
            psAuthent.setString(1, login);
            psAuthent.setString(2, password);
            ResultSet rs = psAuthent.executeQuery();
            if (rs.next()) {
             answer=rs.getBoolean(1);
            }

            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();

        }

        return answer;
    }
}
