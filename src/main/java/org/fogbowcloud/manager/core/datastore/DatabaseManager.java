package org.fogbowcloud.manager.core.datastore;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager implements StableStorage {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class);

    private static final String URL = "jdbc:postgresql://localhost:21700/recovery";
    private static final String USERNAME = "fogbow";
    private static final String PASSWORD = "fogbow";

    private static final String ORDER_TABLE_NAME = "t_order";
    private static final String ORDER_ID = "order_id";
    private static final String INSTANCE_ID = "instance_id";

    private static final String INSERT_ORDER_SQL = "INSERT INTO " + ORDER_TABLE_NAME
            + " (" + ORDER_ID + "," + INSTANCE_ID + ")" + " VALUES (?,?)";

    private static DatabaseManager instance;

    private DatabaseManager() {
        // Database configuration must be in a propertie file
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }

        return instance;
    }

    @Override
    public void add(Order order) {
        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(INSERT_ORDER_SQL);

            orderStatement.setString(1, order.getId());
            orderStatement.setString(2, order.getInstanceId());

            orderStatement.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            LOGGER.error("Couldn't create order.", e);
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e1) {
                LOGGER.error("Couldn't rollback transaction.", e1);
            }
        } finally {
            closeConnection(orderStatement, connection);
        }
    }

    @Override
    public void update(Order order) {
    }

    @Override
    public SynchronizedDoublyLinkedList readActiveOrders(OrderState orderState) {
        if (orderState.equals(OrderState.CLOSED)) {
            // returns only orders with instanceId different than null
        }

        return new SynchronizedDoublyLinkedList();
    }

    private Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (SQLException e) {
            LOGGER.error("Error while getting a new connection from the connection pool.", e);
            throw e;
        }
    }

    private void closeConnection(PreparedStatement statement, Connection connection) {
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
