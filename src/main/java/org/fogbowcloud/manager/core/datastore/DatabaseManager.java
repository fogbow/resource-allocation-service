package org.fogbowcloud.manager.core.datastore;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.plugins.cloud.openstack.util.CloudInitUserDataBuilder;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DatabaseManager implements StableStorage {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class);

    // TODO: get this value only from conf file (still here for testing)
    private static String DATABASE_URL = "jdbc:sqlite:/home/lucas/mydatabase.db";
    private static final String MANAGER_DATASTORE_SQLITE_DRIVER = "org.sqlite.JDBC";

    private static DatabaseManager instance;

    private DatabaseManager() {
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        DATABASE_URL = propertiesHolder.getProperty(ConfigurationConstants.DATABASE_URL);

        try {
            Class.forName(MANAGER_DATASTORE_SQLITE_DRIVER);

            // Creating all tables
            createOrderTables();
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
                addTimestamp(computeOrder);
                break;
            case NETWORK:
                NetworkOrder networkOrder = (NetworkOrder) order;
                addNetworkOrder(networkOrder);
                addTimestamp(networkOrder);
                break;
            case VOLUME:
                VolumeOrder volumeOrder = (VolumeOrder) order;
                addVolumeOrder(volumeOrder);
                addTimestamp(volumeOrder);
                break;
            case ATTACHMENT:
                AttachmentOrder attachmentOrder = (AttachmentOrder) order;
                addAttachmentOrder(attachmentOrder);
                addTimestamp(attachmentOrder);
                break;
        }
    }

    @Override
    public void update(Order order) {
        switch (order.getType()) {
            case COMPUTE:
                ComputeOrder computeOrder = (ComputeOrder) order;
                updateComputeOrder(computeOrder);
                addTimestamp(computeOrder);
                break;
            case NETWORK:
                NetworkOrder networkOrder = (NetworkOrder) order;
                updateNetworkOrder(networkOrder);
                addTimestamp(networkOrder);
                break;
            case VOLUME:
                VolumeOrder volumeOrder = (VolumeOrder) order;
                updateVolumeOrder(volumeOrder);
                addTimestamp(volumeOrder);
                break;
            case ATTACHMENT:
                AttachmentOrder attachmentOrder = (AttachmentOrder) order;
                updateAttachmentOrder(attachmentOrder);
                addTimestamp(attachmentOrder);
                break;
        }
    }

    @Override
    public SynchronizedDoublyLinkedList readActiveOrders(OrderState orderState) {
        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList = new SynchronizedDoublyLinkedList();

        if (orderState.equals(OrderState.CLOSED)) {
            // returns only orders with instanceId different than null
            SQLCommands.SELECT_COMPUTE_ORDER_SQL += SQLCommands.NOT_NULL_INSTANCE_ID;
            SQLCommands.SELECT_VOLUME_ORDER_SQL += SQLCommands.NOT_NULL_INSTANCE_ID;
            SQLCommands.SELECT_NETWORK_ORDER_SQL += SQLCommands.NOT_NULL_INSTANCE_ID;
            SQLCommands.SELECT_ATTACHMENT_ORDER_SQL += SQLCommands.NOT_NULL_INSTANCE_ID;
        }

        Connection connection = null;
        PreparedStatement orderStatement = null;

        // Getting all orders by state from data base
        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.SELECT_COMPUTE_ORDER_SQL);
            orderStatement.setString(1, orderState.name());

            ResultSet computeResult = orderStatement.executeQuery();
            addAllComputeOrdersToList(computeResult, synchronizedDoublyLinkedList);

            orderStatement = connection.prepareStatement(SQLCommands.SELECT_NETWORK_ORDER_SQL);
            orderStatement.setString(1, orderState.name());

            ResultSet networkResult = orderStatement.executeQuery();
            addAllNetworkOrdersToList(networkResult, synchronizedDoublyLinkedList);

            orderStatement = connection.prepareStatement(SQLCommands.SELECT_VOLUME_ORDER_SQL);
            orderStatement.setString(1, orderState.name());

            ResultSet volumeResult = orderStatement.executeQuery();
            addAllVolumeOrdersToList(volumeResult, synchronizedDoublyLinkedList);

            orderStatement = connection.prepareStatement(SQLCommands.SELECT_ATTACHMENT_ORDER_SQL);
            orderStatement.setString(1, orderState.name());

            ResultSet attachmentResult = orderStatement.executeQuery();
            addAllAttachmentOrdersToList(attachmentResult, synchronizedDoublyLinkedList);

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

        return synchronizedDoublyLinkedList;
    }

    /**
     * Methods to update orders in the table.
     **/

    private void updateComputeOrder(ComputeOrder computeOrder) {
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

    private void updateNetworkOrder(NetworkOrder networkOrder) {
        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.UPDATE_NETWORK_ORDER_SQL);

            orderStatement.setString(1, networkOrder.getInstanceId());
            orderStatement.setString(2, networkOrder.getOrderState().name());
            orderStatement.setString(3, networkOrder.getId());

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

    private void updateVolumeOrder(VolumeOrder volumeOrder) {
        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.UPDATE_VOLUME_ORDER_SQL);

            orderStatement.setString(1, volumeOrder.getInstanceId());
            orderStatement.setString(2, volumeOrder.getOrderState().name());
            orderStatement.setString(3, volumeOrder.getId());

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

    private void updateAttachmentOrder(AttachmentOrder attachmentOrder) {
        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.UPDATE_ATTACHMENT_ORDER_SQL);

            orderStatement.setString(1, attachmentOrder.getOrderState().name());
            orderStatement.setString(2, attachmentOrder.getId());

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

    /**
     * Methods to add orders into the SynchronizedDoublyLinkedList.
     **/

    private void addAllComputeOrdersToList(ResultSet computeResult, SynchronizedDoublyLinkedList synchronizedDoublyLinkedList)
            throws SQLException {

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
    }

    private void addAllNetworkOrdersToList(ResultSet networkResult, SynchronizedDoublyLinkedList synchronizedDoublyLinkedList)
            throws SQLException {

        while (networkResult.next()) {
            networkResult.getString(1);

            Map<String, String> federationUserAttr = getFederationUserAttrFromString(networkResult.getString(5));

            NetworkOrder networkOrder = new NetworkOrder(networkResult.getString(1),
                    new FederationUser(networkResult.getString(4), federationUserAttr),
                    networkResult.getString(6), networkResult.getString(7),
                    networkResult.getString(8), networkResult.getString(9),
                    NetworkAllocation.fromValue(networkResult.getString(10)));

            synchronizedDoublyLinkedList.addItem(networkOrder);
        }
    }

    private void addAllVolumeOrdersToList(ResultSet volumeResult, SynchronizedDoublyLinkedList synchronizedDoublyLinkedList)
            throws SQLException {

        while (volumeResult.next()) {
            volumeResult.getString(1);

            Map<String, String> federationUserAttr = getFederationUserAttrFromString(volumeResult.getString(5));

            VolumeOrder volumeOrder = new VolumeOrder(volumeResult.getString(1),
                    new FederationUser(volumeResult.getString(4), federationUserAttr),
                    volumeResult.getString(6), volumeResult.getString(7),
                    volumeResult.getInt(8));

            synchronizedDoublyLinkedList.addItem(volumeOrder);
        }
    }

    private void addAllAttachmentOrdersToList(ResultSet attachmentResult, SynchronizedDoublyLinkedList synchronizedDoublyLinkedList)
            throws SQLException {

        while (attachmentResult.next()) {
            attachmentResult.getString(1);

            Map<String, String> federationUserAttr = getFederationUserAttrFromString(attachmentResult.getString(5));

            AttachmentOrder attachmentOrder = new AttachmentOrder(attachmentResult.getString(1),
                    new FederationUser(attachmentResult.getString(4), federationUserAttr),
                    attachmentResult.getString(6), attachmentResult.getString(7),
                    attachmentResult.getString(8), attachmentResult.getString(9),
                    attachmentResult.getString(10));

            synchronizedDoublyLinkedList.addItem(attachmentOrder);
        }
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

    /**
     * Method to add an order into timestamp table.
     */

    private void addTimestamp(Order order) {
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

    /**
     * Method to create orders tables and timestamp table.
     **/

    private void createOrderTables() throws SQLException {
        Statement statement = null;
        Connection connection = null;

        try {
            connection = getConnection();

            statement = connection.createStatement();
            statement.execute(SQLCommands.CREATE_COMPUTE_ORDER_TABLE_SQL);
            statement.execute(SQLCommands.CREATE_NETWORK_ORDER_TABLE_SQL);
            statement.execute(SQLCommands.CREATE_VOLUME_ORDER_TABLE_SQL);
            statement.execute(SQLCommands.CREATE_ATTACHMENT_ORDER_TABLE_SQL);
            statement.execute(SQLCommands.CREATE_TIMESTAMP_TABLE_SQL);

            statement.close();
        } catch (SQLException e) {
            LOGGER.error("Error creating order table", e);
            throw new SQLException(e);
        } finally {
            closeConnection(statement, connection);
        }
    }

    /**
     * Methods to add orders into the database.
     **/

    private void addOrder(PreparedStatement orderStatement, Order order) throws SQLException {
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

    private void addComputeOrder(ComputeOrder computeOrder) {
        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.INSERT_COMPUTE_ORDER_SQL);

            addOrder(orderStatement, computeOrder);

            orderStatement.setInt(8, computeOrder.getvCPU());
            orderStatement.setInt(9, computeOrder.getMemory());
            orderStatement.setInt(10, computeOrder.getDisk());
            orderStatement.setString(11, computeOrder.getImageId());
            orderStatement.setString(12, computeOrder.getUserData().getExtraUserDataFileContent());
            orderStatement.setString(13, computeOrder.getUserData().getExtraUserDataFileType().name());
            orderStatement.setString(14, computeOrder.getPublicKey());

            // TODO: It is not necessary when creating an order (can be null)
            if (computeOrder.getActualAllocation() == null) {
                orderStatement.setInt(15, -1);
                orderStatement.setInt(16, -1);
                orderStatement.setInt(17, -1);
            } else {
                orderStatement.setInt(15, computeOrder.getActualAllocation().getvCPU());
                orderStatement.setInt(16, computeOrder.getActualAllocation().getRam());
                orderStatement.setInt(17, computeOrder.getActualAllocation().getInstances());
            }

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

    private void addNetworkOrder(NetworkOrder networkOrder) {
        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.INSERT_NETWORK_ORDER_SQL);

            addOrder(orderStatement, networkOrder);

            orderStatement.setString(8, networkOrder.getGateway());
            orderStatement.setString(9, networkOrder.getAddress());
            orderStatement.setString(10, networkOrder.getAllocation().getValue());
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

    private void addVolumeOrder(VolumeOrder volumeOrder) {
        Connection connection = null;
        PreparedStatement orderStatement = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            orderStatement = connection.prepareStatement(SQLCommands.INSERT_VOLUME_ORDER_SQL);

            addOrder(orderStatement, volumeOrder);

            orderStatement.setInt(8, volumeOrder.getVolumeSize());
            orderStatement.setTimestamp(9, new Timestamp(new Date().getTime()));

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

            addOrder(orderStatement, attachmentOrder);

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

    /**
     * Methods to manage database connection.
     **/

    private Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(DATABASE_URL);
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
//        Map<String, String> attr = new HashMap<>();
//        attr.put("oi", "aras");
//
//        FederationUser federationUser = new FederationUser("fed-id", attr);
//        String requestingMember = "LOCAL_MEMBER_ID";
//        String providingMember = "LOCAL_MEMBER_ID";
//
//        UserData userData = new UserData("extraUserDataFileContent", CloudInitUserDataBuilder.FileType.CLOUD_CONFIG);
//
//        ComputeAllocation computeAllocation = new ComputeAllocation(1, 1, 1);
//
//        List<String> netIds = new ArrayList<>();
//        netIds.add("netid1");
//        netIds.add("netid2");
//
//        ComputeOrder order = new ComputeOrder("x", federationUser, requestingMember, providingMember, 8, 1024,
//                30, "fake_image_name", userData, "fake_public_key", netIds);
//        order.setInstanceId("instance-id");
//        order.setOrderState(OrderState.OPEN);
//        order.setActualAllocation(computeAllocation);
//
//        NetworkOrder networkOrder = new NetworkOrder("a", federationUser, requestingMember, providingMember, "gat", "add", NetworkAllocation.STATIC);
//        networkOrder.setOrderState(OrderState.CLOSED);
//
//        VolumeOrder volumeOrder = new VolumeOrder("b", federationUser, requestingMember, providingMember, 10);
//        volumeOrder.setOrderState(OrderState.OPEN);
//
//        AttachmentOrder attachmentOrder = new AttachmentOrder("f", federationUser, requestingMember, providingMember, "source", "target", "device");
//        attachmentOrder.setOrderState(OrderState.OPEN);
//
////        databaseManager.add(order);
//
////        attachmentOrder.setOrderState(OrderState.CLOSED);
////        databaseManager.update(attachmentOrder);
//
//
////        Date date = new Date();
////        System.out.println(date.getTime());
//
//        networkOrder.setOrderState(OrderState.OPEN);
//        databaseManager.update(networkOrder);
//
//
////        databaseManager.add(networkOrder);
////        databaseManager.add(volumeOrder);
////        databaseManager.add(attachmentOrder);
//
//
//        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList = databaseManager.readActiveOrders(OrderState.OPEN);
////
//        Order orderToPrint;
//
//        while ((orderToPrint = synchronizedDoublyLinkedList.getNext()) != null) {
//            System.out.println(orderToPrint.toString());
//            System.out.println(orderToPrint.getFederationUser().getAttributes().toString());
//        }
//    }
}
