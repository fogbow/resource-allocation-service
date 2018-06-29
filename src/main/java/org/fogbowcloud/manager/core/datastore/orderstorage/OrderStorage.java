package org.fogbowcloud.manager.core.datastore.orderstorage;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class OrderStorage {

    private static final Logger LOGGER = Logger.getLogger(OrderStorage.class);

    private static String DATABASE_URL = "jdbc:sqlite:/home/lucas/mydatabase.db";
    private static final String MANAGER_DATASTORE_SQLITE_DRIVER = "org.sqlite.JDBC";

    public OrderStorage() {
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        DATABASE_URL = propertiesHolder.getProperty(ConfigurationConstants.DATABASE_URL);

        try {
            Class.forName(MANAGER_DATASTORE_SQLITE_DRIVER);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    protected Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(DATABASE_URL);
        } catch (SQLException e) {
            LOGGER.error("Error while getting a new connection from the connection pool.", e);
            throw e;
        }
    }

    protected void closeConnection(Statement statement, Connection connection) {
        if (statement != null) {
            try {
                if (!statement.isClosed()) {
                    statement.close();
                }
            } catch (SQLException e) {
                LOGGER.error("Couldn't close statement");
            }
        }

        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.error("Couldn't close connection");
            }
        }
    }
}
