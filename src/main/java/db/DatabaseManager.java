package db;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DatabaseManager {

    private volatile Connection connection = null;
    private final Supplier<String> username;

    private static final String URL = "jdbc:sqlite:";
    private static final String POSTFIX = ".db";

    private static final String CREATE_SUBSCRIBERS_TABLE =
            "CREATE TABLE IF NOT EXISTS Subscribers (userId LONG PRIMARY KEY)";
    private static final String INSERT_SUBSCRIBER = "INSERT OR IGNORE INTO Subscribers (userId) VALUES (?)";
    private static final String DELETE_SUBSCRIBER = "DELETE FROM Subscribers WHERE userId =?";
    private static final String SELECT_ALL_SUBSCRIBERS = "SELECT userId from Subscribers";

    private static final String CREATE_CONSTANTS_TABLE =
            "CREATE TABLE IF NOT EXISTS Constants (textKey TEXT PRIMARY KEY, textValue TEXT NOT NULL)";
    private static final String INSERT_CONSTANT =
            "INSERT OR REPLACE INTO Constants (textKey, textValue) VALUES (?,?)";
    private static final String SELECT_CONSTANT = "SELECT textValue FROM Constants where textKey =?";

    private static final String CREATE_MAILING_TABLE =
            "CREATE TABLE IF NOT EXISTS Mailing (userId LONG, messageId INTEGER, PRIMARY KEY(userId, messageId))";
    private static final String INSERT_USER_MESSAGE_PAIR =
            "INSERT OR IGNORE INTO Mailing (userId, messageId) VALUES (?,?)";
    private static final String SELECT_USER_MESSAGE_PAIR = "SELECT 1 FROM Mailing WHERE userId =? AND messageId=?";

    public DatabaseManager(Supplier<String> username) {
        this.username = username;
    }

    private Connection getConnection() {
        if (connection == null) {
            synchronized (this) {
                if (connection == null) {
                    try {
                        connection = DriverManager.getConnection(URL + username.get() + POSTFIX);
                        var statement = connection.createStatement();
                        statement.execute(CREATE_SUBSCRIBERS_TABLE);
                        statement.execute(CREATE_CONSTANTS_TABLE);
                        statement.execute(CREATE_MAILING_TABLE);
                        Logger.getRootLogger().log(Level.INFO, "Connecting to DB");
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return connection;
    }

    // Methods to work with subscribers
    public void manageUserSubscription(Long userId, boolean subscribe) {
        Connection connection = getConnection();
        try (var preparedStatement = connection.prepareStatement(subscribe ?
                INSERT_SUBSCRIBER : DELETE_SUBSCRIBER)) {
            Logger.getRootLogger().log(Level.INFO,
                    "Changing user subscription " + userId + ", subscribe: " + subscribe);
            preparedStatement.setLong(1, userId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Long> getAllSubscribers() {
        Connection connection = getConnection();
        var subscribers = new ArrayList<Long>();
        try (var preparedStatement = connection.prepareStatement(SELECT_ALL_SUBSCRIBERS)) {
            Logger.getRootLogger().log(Level.INFO, "Getting subscribers from DB");
            var resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                subscribers.add(resultSet.getLong(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Logger.getRootLogger().log(Level.INFO, "There are " + subscribers.size() + " subscribers in DB");

        return subscribers;
    }

    public void userMailedSuccess(Long subscriberId, int messageId) {
        Connection connection = getConnection();
        try (var preparedStatement = connection.prepareStatement(INSERT_USER_MESSAGE_PAIR)) {
            Logger.getRootLogger().log(Level.INFO, "User " + subscriberId + " mail success, message id: "
                    + messageId);
            preparedStatement.setLong(1, subscriberId);
            preparedStatement.setInt(2, messageId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasUserBeenMailed(Long subscriberId, int messageId) {
        Connection connection = getConnection();
        try (var preparedStatement = connection.prepareStatement(SELECT_USER_MESSAGE_PAIR)) {
            preparedStatement.setLong(1, subscriberId);
            preparedStatement.setInt(1, messageId);
            var resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Methods to work with bot message constants
    public void setConstant(String key, String value) {
        Connection connection = getConnection();
        try (var preparedStatement = connection.prepareStatement(INSERT_CONSTANT)) {
            Logger.getRootLogger().log(Level.INFO, "Setting new constant, " + "key: " + key + ", value: " + value);
            preparedStatement.setString(1, key);
            preparedStatement.setString(2, value);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getConstant(String key) {
        Connection connection = getConnection();
        try (var preparedStatement = connection.prepareStatement(SELECT_CONSTANT)) {
            preparedStatement.setString(1, key);
            var resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Logger.getRootLogger().log(Level.WARN, "Constant with key: " + key + " is empty");
        return "";
    }
}
