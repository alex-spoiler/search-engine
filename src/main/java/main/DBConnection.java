package main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static Connection connection;

    private static final String DB_NAME = "search_engine";
    private static final String DB_USER = " ";
    private static final String DB_PASS = " ";

    private static StringBuilder insertQuery = new StringBuilder();
    private static final int MAX_LENGTH_QUERY = 40_000_000;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/" + DB_NAME +
                            "?user=" + DB_USER + "&password=" + DB_PASS);
                connection.createStatement().execute("DROP TABLE IF EXISTS page");
                connection.createStatement().execute("CREATE TABLE page(" +
                        "id INT NOT NULL AUTO_INCREMENT," +
                        "path TEXT NOT NULL," +
                        "code INT NOT NULL," +
                        "content MEDIUMTEXT NOT NULL," +
                        "PRIMARY KEY(id)," +
                        "KEY(path(50)))");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    public static void insertPage(String path, int code, String content) {
        if (insertQuery.length() > MAX_LENGTH_QUERY) {
            executeMultiInsert();
            insertQuery = new StringBuilder();
        }

        insertQuery.append(insertQuery.length() == 0 ? "" : ", ")
                .append("('").append(path).append("', ").append(code)
                .append(", '").append(content).append("')");
    }

    public static void executeMultiInsert() {
        try {
            String sql = "INSERT INTO page(path, code, content) VALUES " + insertQuery;
            DBConnection.getConnection().createStatement().execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
