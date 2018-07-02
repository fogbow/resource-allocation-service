package org.fogbowcloud.manager.core.datastore.orderstorage;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.datastore.commands.VolumeSQLCommands;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.token.FederationUser;

import java.sql.*;
import java.util.Date;
import java.util.Map;

public class VolumeOrderStorage extends OrderStorage {

    private static final Logger LOGGER = Logger.getLogger(VolumeOrderStorage.class);

    public VolumeOrderStorage() throws SQLException {
        Statement statement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            statement = connection.createStatement();

            statement.execute(VolumeSQLCommands.CREATE_VOLUME_ORDER_TABLE_SQL);

            statement.close();
        } catch (SQLException e) {
            LOGGER.error("Error creating volume order table", e);
            throw new SQLException(e);
        } finally {
            closeConnection(statement, connection);
        }
    }

    public void addOrder(Order order) {
        VolumeOrder volumeOrder = (VolumeOrder) order;

        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(VolumeSQLCommands.INSERT_VOLUME_ORDER_SQL);

            addOverallOrderAttributes(orderStatement, volumeOrder);

            orderStatement.setInt(8, volumeOrder.getVolumeSize());
            orderStatement.setTimestamp(9, new Timestamp(new Date().getTime()));

            orderStatement.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            LOGGER.error("Couldn't add the volume order.", e);
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
        VolumeOrder volumeOrder = (VolumeOrder) order;

        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(VolumeSQLCommands.UPDATE_VOLUME_ORDER_SQL);

            orderStatement.setString(1, volumeOrder.getInstanceId());
            orderStatement.setString(2, volumeOrder.getOrderState().name());
            orderStatement.setString(3, volumeOrder.getId());

            orderStatement.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            LOGGER.error("Couldn't update the volume order.", e);
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

            orderStatement = connection.prepareStatement(VolumeSQLCommands.SELECT_VOLUME_ORDER_SQL);
            orderStatement.setString(1, orderState.name());

            ResultSet volumeResult = orderStatement.executeQuery();

            while (volumeResult.next()) {
                volumeResult.getString(1);

                Map<String, String> federationUserAttr = getFederationUserAttrFromString(volumeResult.getString(5));

                VolumeOrder volumeOrder = new VolumeOrder(volumeResult.getString(1),
                        new FederationUser(volumeResult.getString(4), federationUserAttr),
                        volumeResult.getString(6), volumeResult.getString(7),
                        volumeResult.getInt(8));

                volumeOrder.setInstanceId(volumeResult.getString(2));
                volumeOrder.setOrderState(OrderState.fromValue(volumeResult.getString(3)));

                synchronizedDoublyLinkedList.addItem(volumeOrder);
            }

            connection.commit();
        } catch (SQLException e) {
            LOGGER.error("Couldn't read the volume order.", e);
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
