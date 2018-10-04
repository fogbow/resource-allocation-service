package org.fogbowcloud.ras.core.datastore.orderstorage;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.datastore.commands.TimestampSQLCommands;
import org.fogbowcloud.ras.core.models.orders.Order;

import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * This class is used to store the time when the order change its state.
 * Therefore, it can be useful for billing services or whatever you want.
 */
public class OrderTimestampStorage extends OrderStorage {
    private static final Logger LOGGER = Logger.getLogger(OrderTimestampStorage.class);

    public OrderTimestampStorage() throws SQLException {
        Statement statement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            statement = connection.createStatement();

            statement.execute(TimestampSQLCommands.CREATE_TIMESTAMP_TABLE_SQL);

            statement.close();
        } catch (SQLException e) {
            LOGGER.error(Messages.Error.UNABLE_TO_CREATE_TIMESTAMP_TABLE, e);
            throw new SQLException(e);
        } finally {
            closeConnection(statement, connection);
        }
    }

    public void addOrder(Order order) throws SQLException {
        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(TimestampSQLCommands.INSERT_TIMESTAMP_SQL);

            orderStatement.setString(1, order.getId());
            orderStatement.setString(2, order.getOrderState().name());
            orderStatement.setString(3, String.valueOf(order.getType()));
            orderStatement.setString(4, order.getSpec());
            orderStatement.setString(5, order.getFederationUserToken().getUserId());
            orderStatement.setString(6, order.getFederationUserToken().getUserName());
            orderStatement.setString(7, order.getRequestingMember());
            orderStatement.setString(8, order.getProvidingMember());
            orderStatement.setTimestamp(9, new Timestamp(new Date().getTime()));

            orderStatement.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            LOGGER.error(Messages.Error.UNABLE_TO_ADD_TIMESTAMP, e);
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e1) {
                LOGGER.error(Messages.Error.UNABLE_TO_ROLLBACK_TRANSACTION, e1);
                throw e1;
            }

            throw e;
        } finally {
            closeConnection(orderStatement, connection);
        }
    }

    // Used for tests. Returns a map of found order and the list of states
    protected Map<String, List<String>> selectOrderById(String orderId) throws SQLException {
        PreparedStatement selectMemberStatement = null;

        Connection connection = null;

        Map<String, List<String>> listOfOrders = new HashMap<>();
        listOfOrders.put(orderId, new ArrayList<>());
        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            selectMemberStatement = connection
                    .prepareStatement(TimestampSQLCommands.SELECT_TIMESTAMP_BY_ORDER_ID_SQL);

            selectMemberStatement.setString(1, orderId);

            ResultSet rs = selectMemberStatement.executeQuery();
            while (rs.next()) {
                String state = rs.getString("order_state");
                listOfOrders.get(orderId).add(state);
            }

            connection.commit();

        } catch (SQLException e) {
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e1) {
                e1.printStackTrace();
                LOGGER.error(Messages.Error.UNABLE_TO_ROLLBACK_TRANSACTION);
            }

        } finally {
            closeConnection(selectMemberStatement, connection);
        }

        return listOfOrders;
    }
}
