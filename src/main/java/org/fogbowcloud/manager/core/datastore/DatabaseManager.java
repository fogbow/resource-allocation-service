package org.fogbowcloud.manager.core.datastore;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;

import java.sql.*;

public class DatabaseManager implements StableStorage {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class);

    private static final String URL = "jdbc:sqlite:/home/lucas/mydatabase.db";
    private static final String MANAGER_DATASTORE_SQLITE_DRIVER = "org.sqlite.JDBC";

    private static DatabaseManager instance;

    private DatabaseManager() {
        // TODO: Database configuration must be in a propertie file
        try {
            Class.forName(MANAGER_DATASTORE_SQLITE_DRIVER);

            createOrderTable(SQLCommands.CREATE_COMPUTE_ORDER_SQL);
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }

        return instance;
    }

    @Override
    public void add(Order order) {
        switch (order.getType()) {
            case COMPUTE:
                ComputeOrder computeOrder = (ComputeOrder) order;
                // TODO: Add in compute order table
                break;
        }

        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.INSERT_ORDER_SQL);

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

    private void createOrderTable(String orderSql) throws SQLException {
        Statement statement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            statement = connection.createStatement();
            statement.execute(orderSql);
            statement.close();
        } catch (SQLException e) {
            LOGGER.error("Error creating order table", e);
            throw new SQLException(e);
        } finally {
            closeConnection(statement, connection);
        }
    }

    private Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            LOGGER.error("Error while getting a new connection from the connection pool.", e);
            throw e;
        }
    }

    private void closeConnection(Statement statement, Connection connection) {
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
