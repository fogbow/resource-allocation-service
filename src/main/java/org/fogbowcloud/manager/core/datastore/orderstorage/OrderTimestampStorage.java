package org.fogbowcloud.manager.core.datastore.orderstorage;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.datastore.commands.SQLCommands;
import org.fogbowcloud.manager.core.models.orders.Order;

import java.sql.*;
import java.util.Date;

public class OrderTimestampStorage extends OrderStorage {

    private static final Logger LOGGER = Logger.getLogger(OrderTimestampStorage.class);

    public OrderTimestampStorage() throws SQLException {
        Statement statement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            statement = connection.createStatement();

            statement.execute(SQLCommands.CREATE_TIMESTAMP_TABLE_SQL);

            statement.close();
        } catch (SQLException e) {
            LOGGER.error("Error creating order table", e);
            throw new SQLException(e);
        } finally {
            closeConnection(statement, connection);
        }
    }

    public void addOrder(Order order) {
        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.INSERT_TIMESTAMP_SQL);

            orderStatement.setString(1, order.getId());
            orderStatement.setString(2, order.getOrderState().name());
            orderStatement.setString(3, order.getFederationUser().getId());
            orderStatement.setTimestamp(4, new Timestamp(new Date().getTime()));

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
}
