package org.fogbowcloud.manager.core.datastore.orderstorage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.datastore.commands.ComputeSQLCommands;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.plugins.cloud.util.CloudInitUserDataBuilder;

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

            statement.execute(ComputeSQLCommands.CREATE_COMPUTE_ORDER_TABLE_SQL);

            statement.close();
        } catch (SQLException e) {
            LOGGER.error("Error creating compute order table", e);
            throw e;
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

            orderStatement = connection.prepareStatement(ComputeSQLCommands.INSERT_COMPUTE_ORDER_SQL);

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
            LOGGER.error("Couldn't add the compute order.", e);
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

            orderStatement = connection.prepareStatement(ComputeSQLCommands.UPDATE_COMPUTE_ORDER_SQL);

            orderStatement.setString(1, computeOrder.getInstanceId());
            orderStatement.setString(2, computeOrder.getOrderState().name());
            if (computeOrder.getActualAllocation() == null) {
                orderStatement.setInt(3, 0);
                orderStatement.setInt(4, 0);
                orderStatement.setInt(5, 0);
            } else {
                orderStatement.setInt(3, computeOrder.getActualAllocation().getvCPU());
                orderStatement.setInt(4, computeOrder.getActualAllocation().getRam());
                orderStatement.setInt(5, computeOrder.getActualAllocation().getInstances());
            }
            orderStatement.setString(6, computeOrder.getId());

            orderStatement.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            LOGGER.error("Couldn't update the compute order.", e);
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

            String sqlCommand = ComputeSQLCommands.SELECT_COMPUTE_ORDER_SQL;

            if (orderState.equals(OrderState.CLOSED)) {
                sqlCommand = ComputeSQLCommands.SELECT_COMPUTE_ORDER_NOT_NULL_INSTANCE_ID;
            }

            orderStatement = connection.prepareStatement(sqlCommand);
            orderStatement.setString(1, orderState.name());

            ResultSet computeResult = orderStatement.executeQuery();

            while (computeResult.next()) {
                computeResult.getString(1);

                Map<String, String> federationUserAttr = getFederationUserAttrFromString(computeResult.getString(5));
                List<String> networksid = getNetworksIdFromString(computeResult.getString(18));
                
                CloudInitUserDataBuilder.FileType extraUserDataFileType = null;
                
                if (computeResult.getString(13) != null) {
                    extraUserDataFileType = CloudInitUserDataBuilder.
                            FileType.valueOf(computeResult.getString(13));
                }
                
                ComputeOrder computeOrder = new ComputeOrder(computeResult.getString(1),
                        new FederationUser(computeResult.getString(4), federationUserAttr),
                        computeResult.getString(6), computeResult.getString(7), computeResult.getInt(8),
                        computeResult.getInt(9), computeResult.getInt(10), computeResult.getString(11),
                        new UserData(computeResult.getString(12), extraUserDataFileType), 
                        computeResult.getString(14), networksid);

                computeOrder.setInstanceId(computeResult.getString(2));
                computeOrder.setOrderStateInRecoveryMode(OrderState.valueOf(computeResult.getString(3)));

                ComputeAllocation computeAllocation = new ComputeAllocation(
                        computeResult.getInt(15),
                        computeResult.getInt(16),
                        computeResult.getInt(17));

                computeOrder.setActualAllocation(computeAllocation);

                synchronizedDoublyLinkedList.addItem(computeOrder);
            }

            connection.commit();
        } catch (SQLException e) {
            LOGGER.error("Couldn't read the compute order.", e);
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e1) {
                LOGGER.error("Couldn't rollback transaction.", e1);
            }
        } catch (InvalidParameterException e) {
            LOGGER.error(e);
        } finally {
            closeConnection(orderStatement, connection);
        }
    }

    private List<String> getNetworksIdFromString(String jsonString) {
        Gson gson = new Gson();
        Type mapType = new TypeToken<List<String>>(){}.getType();

        return gson.fromJson(jsonString, mapType);
    }
}
