package org.fogbowcloud.manager.core.datastore.orderstorage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.datastore.commands.SQLCommands;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.plugins.cloud.openstack.util.CloudInitUserDataBuilder;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ComputeOrderStorage extends OrderStorage {

    private static final Logger LOGGER = Logger.getLogger(ComputeOrderStorage.class);

    public ComputeOrderStorage() throws SQLException {
        Statement statement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            statement = connection.createStatement();

            statement.execute(SQLCommands.CREATE_COMPUTE_ORDER_TABLE_SQL);

            statement.close();
        } catch (SQLException e) {
            LOGGER.error("Error creating order table", e);
            throw new SQLException(e);
        } finally {
            closeConnection(statement, connection);
        }
    }

    public void addOrder(Order order) {
        ComputeOrder computeOrder = (ComputeOrder) order;

        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.INSERT_COMPUTE_ORDER_SQL);

            addOverallOrderAttributes(orderStatement, computeOrder);

            orderStatement.setInt(8, computeOrder.getvCPU());
            orderStatement.setInt(9, computeOrder.getMemory());
            orderStatement.setInt(10, computeOrder.getDisk());
            orderStatement.setString(11, computeOrder.getImageId());

            if (computeOrder.getUserData() != null) {
                orderStatement.setString(12, computeOrder.getUserData().getExtraUserDataFileContent());
                orderStatement.setString(13, computeOrder.getUserData().getExtraUserDataFileType().name());
            }

            orderStatement.setString(14, computeOrder.getPublicKey());

            Gson gson = new Gson();
            String networksId = gson.toJson(computeOrder.getNetworksId());

            orderStatement.setString(18, networksId);
            orderStatement.setTimestamp(19, new Timestamp(new Date().getTime()));

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
        ComputeOrder computeOrder = (ComputeOrder) order;

        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.UPDATE_COMPUTE_ORDER_SQL);

            orderStatement.setString(1, computeOrder.getInstanceId());
            orderStatement.setString(2, computeOrder.getOrderState().name());
            orderStatement.setInt(3, computeOrder.getActualAllocation().getvCPU());
            orderStatement.setInt(4, computeOrder.getActualAllocation().getRam());
            orderStatement.setInt(5, computeOrder.getActualAllocation().getInstances());
            orderStatement.setString(6, computeOrder.getId());

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

            orderStatement = connection.prepareStatement(SQLCommands.SELECT_COMPUTE_ORDER_SQL);
            orderStatement.setString(1, orderState.name());

            ResultSet computeResult = orderStatement.executeQuery();

            while (computeResult.next()) {
                computeResult.getString(1);

                Map<String, String> federationUserAttr = getFederationUserAttrFromString(computeResult.getString(5));
                List<String> networksid = getNetworksIdFromString(computeResult.getString(18));

                ComputeOrder computeOrder = new ComputeOrder(computeResult.getString(1),
                        new FederationUser(computeResult.getString(4), federationUserAttr),
                        computeResult.getString(6), computeResult.getString(7), computeResult.getInt(8),
                        computeResult.getInt(9), computeResult.getInt(10), computeResult.getString(11),
                        new UserData(computeResult.getString(12), CloudInitUserDataBuilder.FileType.valueOf(
                                computeResult.getString(13))), computeResult.getString(14), networksid);

                computeOrder.setInstanceId(computeResult.getString(2));
                computeOrder.setOrderState(OrderState.fromValue(computeResult.getString(3)));

                ComputeAllocation computeAllocation = new ComputeAllocation(
                        computeResult.getInt(15),
                        computeResult.getInt(16),
                        computeResult.getInt(17));

                computeOrder.setActualAllocation(computeAllocation);

                synchronizedDoublyLinkedList.addItem(computeOrder);
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

    private List<String> getNetworksIdFromString(String jsonString) {
        Gson gson = new Gson();
        Type mapType = new TypeToken<List<String>>(){}.getType();

        return gson.fromJson(jsonString, mapType);
    }
}
