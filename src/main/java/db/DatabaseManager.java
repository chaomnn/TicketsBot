package db;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class DatabaseManager {

    private static volatile DatabaseManager INSTANCE;
    private Connection connection = null;

    private static final String URL = "jdbc:sqlite:tickets.db";

    private static final String CREATE_SUBSCRIBERS_TABLE =
            "CREATE TABLE IF NOT EXISTS Subscribers (userId LONG PRIMARY KEY)";
    private static final String INSERT_SUBSCRIBER = "INSERT INTO Subscribers (userId) VALUES (?)";
    private static final String DELETE_SUBSCRIBER = "DELETE FROM Subscribers WHERE userId =?";
    private static final String SELECT_SUBSCRIBER = "SELECT userId FROM Subscribers WHERE userId =?";
    private static final String SELECT_ALL_SUBSCRIBERS = "SELECT userId from Subscribers";

    private static final String CREATE_CONSTANTS_TABLE =
            "CREATE TABLE IF NOT EXISTS Constants (textKey TEXT PRIMARY KEY, textValue TEXT NOT NULL)";
    private static final String INSERT_CONSTANT =
            "INSERT OR REPLACE INTO Constants (textKey, textValue) VALUES (?,?)";
    private static final String SELECT_CONSTANT = "SELECT textValue FROM Constants where textKey =?";

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (INSTANCE == null) {
            synchronized (DatabaseManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DatabaseManager();
                }
            }
        }
        return INSTANCE;
    }

    public void connect() {
        try {
            if (connection == null) {
                connection = DriverManager.getConnection(URL);
                var statement = connection.createStatement();
                statement.execute(CREATE_SUBSCRIBERS_TABLE);
                statement.execute(CREATE_CONSTANTS_TABLE);
                Logger.getRootLogger().log(Level.INFO, "Connecting to DB");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Methods to work with subscribers
    public boolean isUserSubscribed(Long userId) {
        try (var preparedStatement = connection.prepareStatement(SELECT_SUBSCRIBER)) {
            preparedStatement.setLong(1, userId);
            var resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Logger.getRootLogger().log(Level.INFO, "User " + userId + " is already subscribed");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Logger.getRootLogger().log(Level.INFO, "User " + userId + " is not subscribed");
        return false;
    }

    public void manageUserSubscription(Long userId, boolean subscribe) {
        if (connection != null) {
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
    }

    public Set<Long> getAllSubscribers() {
        var subscribers = new HashSet<Long>();
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

    // Methods to work with bot message constants
    public void setConstant(String key, String value) {
        if (connection != null) {
            try (var preparedStatement = connection.prepareStatement(INSERT_CONSTANT)) {
                Logger.getRootLogger().log(Level.INFO, "Setting new constant, " + "key: " + key + ", value: " + value);
                preparedStatement.setString(1, key);
                preparedStatement.setString(2, value);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public String getConstant(String key) {
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
