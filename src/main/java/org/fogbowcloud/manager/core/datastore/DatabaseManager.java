package org.fogbowcloud.manager.core.datastore;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.plugins.cloud.openstack.util.CloudInitUserDataBuilder;

import java.sql.*;
import java.util.HashMap;

public class DatabaseManager implements StableStorage {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class);

    private static final String URL = "jdbc:sqlite:/home/lucas/mydatabase.db";
    private static final String MANAGER_DATASTORE_SQLITE_DRIVER = "org.sqlite.JDBC";

    private static DatabaseManager instance;

    private DatabaseManager() {
        // TODO: Database configuration must be in a propertie file
        try {
            Class.forName(MANAGER_DATASTORE_SQLITE_DRIVER);

            // Creating all tables
            createOrderTable(SQLCommands.CREATE_COMPUTE_ORDER_SQL);
            createOrderTable(SQLCommands.CREATE_NETWORK_ORDER_SQL);
            createOrderTable(SQLCommands.CREATE_VOLUME_ORDER_SQL);
            createOrderTable(SQLCommands.CREATE_ATTACHMENT_ORDER_SQL);
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
                addComputeOrder(computeOrder);
                break;
            case NETWORK:
                NetworkOrder networkOrder = (NetworkOrder) order;
                addNetworkOrder(networkOrder);
                break;
            case VOLUME:
                VolumeOrder volumeOrder = (VolumeOrder) order;
                addVolumeOrder(volumeOrder);
                break;
            case ATTACHMENT:
                AttachmentOrder attachmentOrder = (AttachmentOrder) order;
                addAttachmentOrder(attachmentOrder);
                break;
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

    private void addComputeOrder(ComputeOrder computeOrder) {
        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.INSERT_COMPUTE_ORDER_SQL);

            orderStatement.setString(1, computeOrder.getId());
            orderStatement.setString(2, computeOrder.getInstanceId());
            orderStatement.setString(3, computeOrder.getOrderState().name());
            orderStatement.setString(4, computeOrder.getFederationUser().getId());

            Gson gson = new Gson();
            String fedAttributes = gson.toJson(computeOrder.getFederationUser().getAttributes());

            orderStatement.setString(5, fedAttributes);
            orderStatement.setString(6, computeOrder.getRequestingMember());
            orderStatement.setString(7, computeOrder.getProvidingMember());
            orderStatement.setInt(8, computeOrder.getvCPU());
            orderStatement.setInt(9, computeOrder.getMemory());
            orderStatement.setInt(10, computeOrder.getDisk());
            orderStatement.setString(11, computeOrder.getImageId());
            orderStatement.setString(12, computeOrder.getUserData().getExtraUserDataFileContent());
            orderStatement.setString(13, computeOrder.getUserData().getExtraUserDataFileType().name());
            orderStatement.setString(14, computeOrder.getPublicKey());
            orderStatement.setInt(15, computeOrder.getActualAllocation().getvCPU());
            orderStatement.setInt(16, computeOrder.getActualAllocation().getRam());
            orderStatement.setInt(17, computeOrder.getActualAllocation().getInstances());

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

    private void addNetworkOrder(NetworkOrder networkOrder) {
        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.INSERT_NETWORK_ORDER_SQL);

            orderStatement.setString(1, networkOrder.getId());
            orderStatement.setString(2, networkOrder.getInstanceId());
            orderStatement.setString(3, networkOrder.getOrderState().name());
            orderStatement.setString(4, networkOrder.getFederationUser().getId());

            Gson gson = new Gson();
            String fedAttributes = gson.toJson(networkOrder.getFederationUser().getAttributes());

            orderStatement.setString(5, fedAttributes);
            orderStatement.setString(6, networkOrder.getRequestingMember());
            orderStatement.setString(7, networkOrder.getProvidingMember());
            orderStatement.setString(8, networkOrder.getGateway());
            orderStatement.setString(9, networkOrder.getAddress());
            orderStatement.setString(10, networkOrder.getAllocation().getValue());

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

    private void addVolumeOrder(VolumeOrder volumeOrder) {
        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.INSERT_VOLUME_ORDER_SQL);

            orderStatement.setString(1, volumeOrder.getId());
            orderStatement.setString(2, volumeOrder.getInstanceId());
            orderStatement.setString(3, volumeOrder.getOrderState().name());
            orderStatement.setString(4, volumeOrder.getFederationUser().getId());

            Gson gson = new Gson();
            String fedAttributes = gson.toJson(volumeOrder.getFederationUser().getAttributes());

            orderStatement.setString(5, fedAttributes);
            orderStatement.setString(6, volumeOrder.getRequestingMember());
            orderStatement.setString(7, volumeOrder.getProvidingMember());
            orderStatement.setInt(8, volumeOrder.getVolumeSize());

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

    private void addAttachmentOrder(AttachmentOrder attachmentOrder) {
        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.INSERT_ATTACHMENT_ORDER_SQL);

            orderStatement.setString(1, attachmentOrder.getId());
            orderStatement.setString(2, attachmentOrder.getInstanceId());
            orderStatement.setString(3, attachmentOrder.getOrderState().name());
            orderStatement.setString(4, attachmentOrder.getFederationUser().getId());

            Gson gson = new Gson();
            String fedAttributes = gson.toJson(attachmentOrder.getFederationUser().getAttributes());

            orderStatement.setString(5, fedAttributes);
            orderStatement.setString(6, attachmentOrder.getRequestingMember());
            orderStatement.setString(7, attachmentOrder.getProvidingMember());
            orderStatement.setString(8, attachmentOrder.getSource());
            orderStatement.setString(9, attachmentOrder.getTarget());
            orderStatement.setString(10, attachmentOrder.getDevice());

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

//    public static void main(String[] args) {
//        DatabaseManager databaseManager = new DatabaseManager();
//
//        FederationUser federationUser = new FederationUser("fed-id", new HashMap<>());
//        String requestingMember = "LOCAL_MEMBER_ID";
//        String providingMember = "LOCAL_MEMBER_ID";
//
//        UserData userData = new UserData("extraUserDataFileContent", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG);
//
//        ComputeAllocation computeAllocation = new ComputeAllocation(1, 1, 1);

//        ComputeOrder order = new ComputeOrder(federationUser, requestingMember, providingMember, 8, 1024,
//                30, "fake_image_name", userData, "fake_public_key");
//        order.setInstanceId("instance-id");
//        order.setOrderState(OrderState.OPEN);
//        order.setActualAllocation(computeAllocation);

//        NetworkOrder networkOrder = new NetworkOrder(federationUser, requestingMember, providingMember, "gat", "add", NetworkAllocation.STATIC);
//        networkOrder.setOrderState(OrderState.OPEN);

//        VolumeOrder volumeOrder = new VolumeOrder(federationUser, requestingMember, providingMember, 10);
//        volumeOrder.setOrderState(OrderState.OPEN);

//        AttachmentOrder attachmentOrder = new AttachmentOrder(federationUser, requestingMember, providingMember, "source", "target", "device");
//        attachmentOrder.setOrderState(OrderState.OPEN);

//        databaseManager.add(attachmentOrder);
//    }
}
