package org.fogbowcloud.manager.core.datastore.orderstorage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.datastore.commands.SQLCommands;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.token.FederationUser;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.Date;
import java.util.Map;

public class AttachmentOrderStorage extends OrderStorage {

    private static final Logger LOGGER = Logger.getLogger(AttachmentOrderStorage.class);

    public AttachmentOrderStorage() throws SQLException {
        Statement statement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            statement = connection.createStatement();

            statement.execute(SQLCommands.CREATE_ATTACHMENT_ORDER_TABLE_SQL);

            statement.close();
        } catch (SQLException e) {
            LOGGER.error("Error creating order table", e);
            throw new SQLException(e);
        } finally {
            closeConnection(statement, connection);
        }
    }

    public void addOrder(Order order) {
        AttachmentOrder attachmentOrder = (AttachmentOrder) order;

        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.INSERT_ATTACHMENT_ORDER_SQL);

            addOverallOrderAttributes(orderStatement, attachmentOrder);

            orderStatement.setString(8, attachmentOrder.getSource());
            orderStatement.setString(9, attachmentOrder.getTarget());
            orderStatement.setString(10, attachmentOrder.getDevice());
            orderStatement.setTimestamp(11, new Timestamp(new Date().getTime()));

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

    public void updateOrder(Order order) {
        AttachmentOrder attachmentOrder = (AttachmentOrder) order;

        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.UPDATE_ATTACHMENT_ORDER_SQL);

            orderStatement.setString(1, attachmentOrder.getInstanceId());
            orderStatement.setString(2, attachmentOrder.getOrderState().name());
            orderStatement.setString(3, attachmentOrder.getId());

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

    public void readOrdersByState(
            OrderState orderState, SynchronizedDoublyLinkedList synchronizedDoublyLinkedList) {

        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.SELECT_ATTACHMENT_ORDER_SQL);
            orderStatement.setString(1, orderState.name());

            ResultSet attachmentResult = orderStatement.executeQuery();

            while (attachmentResult.next()) {
                attachmentResult.getString(1);

                Map<String, String> federationUserAttr = getFederationUserAttrFromString(attachmentResult.getString(5));

                AttachmentOrder attachmentOrder = new AttachmentOrder(attachmentResult.getString(1),
                        new FederationUser(attachmentResult.getString(4), federationUserAttr),
                        attachmentResult.getString(6), attachmentResult.getString(7),
                        attachmentResult.getString(8), attachmentResult.getString(9),
                        attachmentResult.getString(10));

                attachmentOrder.setInstanceId(attachmentResult.getString(2));
                attachmentOrder.setOrderState(OrderState.fromValue(attachmentResult.getString(3)));

                synchronizedDoublyLinkedList.addItem(attachmentOrder);
            }

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

    private void addOverallOrderAttributes(PreparedStatement orderStatement, Order order) throws SQLException {
        orderStatement.setString(1, order.getId());
        orderStatement.setString(2, order.getInstanceId());
        orderStatement.setString(3, order.getOrderState().name());
        orderStatement.setString(4, order.getFederationUser().getId());

        Gson gson = new Gson();
        String fedAttributes = gson.toJson(order.getFederationUser().getAttributes());

        orderStatement.setString(5, fedAttributes);
        orderStatement.setString(6, order.getRequestingMember());
        orderStatement.setString(7, order.getProvidingMember());
    }

    private Map<String, String> getFederationUserAttrFromString(String jsonString) {
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, String>>(){}.getType();

        return gson.fromJson(jsonString, mapType);
    }
}
