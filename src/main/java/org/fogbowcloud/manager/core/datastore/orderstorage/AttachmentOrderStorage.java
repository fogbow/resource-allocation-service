package org.fogbowcloud.manager.core.datastore.orderstorage;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.datastore.commands.AttachmentSQLCommands;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;

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

            statement.execute(AttachmentSQLCommands.CREATE_ATTACHMENT_ORDER_TABLE_SQL);

            statement.close();
        } catch (SQLException e) {
            LOGGER.error("Error creating attachment order table", e);
            throw e;
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

            orderStatement = connection.prepareStatement(AttachmentSQLCommands.INSERT_ATTACHMENT_ORDER_SQL);

            addOverallOrderAttributes(orderStatement, attachmentOrder);

            orderStatement.setString(8, attachmentOrder.getSource());
            orderStatement.setString(9, attachmentOrder.getTarget());
            orderStatement.setString(10, attachmentOrder.getDevice());
            orderStatement.setTimestamp(11, new Timestamp(new Date().getTime()));

            orderStatement.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            LOGGER.error("Couldn't add the attachment order.", e);
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

            orderStatement = connection.prepareStatement(AttachmentSQLCommands.UPDATE_ATTACHMENT_ORDER_SQL);

            orderStatement.setString(1, attachmentOrder.getInstanceId());
            orderStatement.setString(2, attachmentOrder.getOrderState().name());
            orderStatement.setString(3, attachmentOrder.getId());

            orderStatement.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            LOGGER.error("Couldn't update the attachment order.", e);
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

            String sqlCommand = AttachmentSQLCommands.SELECT_ATTACHMENT_ORDER_SQL;

            if (orderState.equals(OrderState.CLOSED)) {
                sqlCommand = AttachmentSQLCommands.SELECT_ATTACHMENT_ORDER_NOT_NULL_INSTANCE_ID;
            }

            orderStatement = connection.prepareStatement(sqlCommand);
            orderStatement.setString(1, orderState.name());

            ResultSet attachmentResult = orderStatement.executeQuery();

            while (attachmentResult.next()) {
                attachmentResult.getString(1);

                Map<String, String> federationUserAttr = getFederationUserAttrFromString(attachmentResult.getString(5));

                AttachmentOrder attachmentOrder = new AttachmentOrder(attachmentResult.getString(1),
                        new FederationUserToken(attachmentResult.getString(4), null),
                        attachmentResult.getString(6), attachmentResult.getString(7),
                        attachmentResult.getString(8), attachmentResult.getString(9),
                        attachmentResult.getString(10));

                attachmentOrder.setInstanceId(attachmentResult.getString(2));
                attachmentOrder.setOrderStateInRecoveryMode(OrderState.valueOf(attachmentResult.getString(3)));

                synchronizedDoublyLinkedList.addItem(attachmentOrder);
            }

            connection.commit();
        } catch (SQLException e) {
            LOGGER.error("Couldn't read the attachment order.", e);
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
