package org.fogbowcloud.ras.core.datastore.orderstorage;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class OrderStorage {
    private static final Logger LOGGER = Logger.getLogger(OrderStorage.class);

    private static final String MANAGER_DATASTORE_SQLITE_DRIVER = "org.sqlite.JDBC";
    private String databaseUrl;
    private String databaseUsername;
    private String databasePassword;

    public OrderStorage() {
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.databaseUrl = propertiesHolder.getProperty(ConfigurationConstants.DATABASE_URL);
        LOGGER.debug(String.format(Messages.Info.DATABASE_URL, this.databaseUrl));
        this.databaseUsername = propertiesHolder.getProperty(ConfigurationConstants.DATABASE_USERNAME);
        this.databasePassword = propertiesHolder.getProperty(ConfigurationConstants.DATABASE_PASSWORD);

        try {
            Class.forName(MANAGER_DATASTORE_SQLITE_DRIVER);
        } catch (ClassNotFoundException e) {
            LOGGER.error(Messages.Error.INVALID_DATASTORE_DRIVER, e);
        }
    }

    protected Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(this.databaseUrl, this.databaseUsername, this.databasePassword);
        } catch (SQLException e) {
            LOGGER.error(Messages.Error.ERROR_WHILE_GETTING_NEW_CONNECTION, e);
            throw e;
        }
    }

    protected void closeConnection(Statement statement, Connection connection) throws SQLException {
        if (statement != null) {
            try {
                if (!statement.isClosed()) {
                    statement.close();
                }
            } catch (SQLException e) {
                LOGGER.error(Messages.Error.UNABLE_TO_CLOSE_STATEMENT, e);
                throw e;
            }
        }

        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.error(Messages.Error.UNABLE_TO_CLOSE_CONNECTION, e);
                throw e;
            }
        }
    }
}
