package org.fogbowcloud.manager.core.datastore.orderstorage;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.datastore.commands.NetworkSQLCommands;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;

import java.sql.*;
import java.util.Date;
import java.util.Map;

public class NetworkOrderStorage extends OrderStorage {

    private static final Logger LOGGER = Logger.getLogger(NetworkOrderStorage.class);

    public NetworkOrderStorage() throws SQLException {
        Statement statement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            statement = connection.createStatement();

            statement.execute(NetworkSQLCommands.CREATE_NETWORK_ORDER_TABLE_SQL);

            statement.close();
        } catch (SQLException e) {
            LOGGER.error("Error creating network order table", e);
            throw new SQLException(e);
        } finally {
            closeConnection(statement, connection);
        }
    }

    public void addOrder(Order order) {
        NetworkOrder networkOrder = (NetworkOrder) order;

        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(NetworkSQLCommands.INSERT_NETWORK_ORDER_SQL);

            addOverallOrderAttributes(orderStatement, networkOrder);

            orderStatement.setString(8, networkOrder.getGateway());
            orderStatement.setString(9, networkOrder.getAddress());
            orderStatement.setString(10, networkOrder.getAllocation().getValue());
            orderStatement.setTimestamp(11, new Timestamp(new Date().getTime()));

            orderStatement.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            LOGGER.error("Couldn't add the network order.", e);
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

    public void updateOrder(Order order) {
        NetworkOrder networkOrder = (NetworkOrder) order;

        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(NetworkSQLCommands.UPDATE_NETWORK_ORDER_SQL);

            orderStatement.setString(1, networkOrder.getInstanceId());
            orderStatement.setString(2, networkOrder.getOrderState().name());
            orderStatement.setString(3, networkOrder.getId());

            orderStatement.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            LOGGER.error("Couldn't update the network order.", e);
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

    public void readOrdersByState(
            OrderState orderState, SynchronizedDoublyLinkedList synchronizedDoublyLinkedList) {

        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            String sqlCommand = NetworkSQLCommands.SELECT_NETWORK_ORDER_SQL;

            if (orderState.equals(OrderState.CLOSED)) {
                sqlCommand = NetworkSQLCommands.SELECT_NETWORK_ORDER_NOT_NULL_INSTANCE_ID;
            }

            orderStatement = connection.prepareStatement(sqlCommand);
            orderStatement.setString(1, orderState.name());

            ResultSet networkResult = orderStatement.executeQuery();

            while (networkResult.next()) {
                networkResult.getString(1);

                Map<String, String> federationUserAttr = getFederationUserAttrFromString(networkResult.getString(5));

                NetworkOrder networkOrder = new NetworkOrder(networkResult.getString(1),
                        new FederationUser(networkResult.getString(4), federationUserAttr),
                        networkResult.getString(6), networkResult.getString(7),
                        networkResult.getString(8), networkResult.getString(9),
                        NetworkAllocationMode.fromValue(networkResult.getString(10)));

                networkOrder.setInstanceId(networkResult.getString(2));
                networkOrder.setOrderState(OrderState.fromValue(networkResult.getString(3)));

                synchronizedDoublyLinkedList.addItem(networkOrder);
            }

            connection.commit();
        } catch (SQLException e) {
            LOGGER.error("Couldn't read the network order.", e);
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e1) {
                LOGGER.error("Couldn't rollback transaction.", e1);
            }
        } catch (UnexpectedException e) {
            LOGGER.error(e);
        } finally {
            closeConnection(orderStatement, connection);
        }
    }
}
