package org.fogbowcloud.manager.core.datastore;

public class SQLCommands {

    /**
     * Order attributes
     **/
    private static final String ORDER_ID = "order_id";
    private static final String ORDER_STATE = "order_state";
    private static final String FEDERATION_USER_ID = "fed_user_id";
    private static final String FEDERATION_USER_ATTR = "fed_user_attr";
    private static final String REQUESTING_MEMBER = "requesting_member";
    private static final String PROVIDING_MEMBER = "providing_member";
    private static final String INSTANCE_ID = "instance_id";

    /**
     * Compute order attributes
     **/
    private static final String COMPUTE_ORDER_TABLE_NAME = "t_compute_order";
    private static final String VCPU = "vcpu";
    private static final String MEMORY = "memory";
    private static final String DISK = "disk";
    private static final String IMAGE_ID = "image_id";
    private static final String USER_DATA_FILE_CONTENT = "user_data_file_content";
    private static final String USER_DATA_FILE_TYPE = "user_data_file_type";
    private static final String PUBLIC_KEY = "public_key";
    private static final String ACTUAL_ALLOCATION_VCPU = "actual_alloc_vcpu";
    private static final String ACTUAL_ALLOCATION_RAM = "actual_alloc_ram";
    private static final String ACTUAL_ALLOCATION_INSTANCES = "actual_alloc_instances";
    private static final String NETWORKS_ID = "networks_id";

    /**
     * Network order attributes
     **/
    private static final String NETWORK_ORDER_TABLE_NAME = "t_network_order";
    private static final String GATEWAY = "gateway";
    private static final String ADDRESS = "address";
    private static final String ALLOCATION = "allocation";

    /**
     * Volume order attributes
     **/
    private static final String VOLUME_ORDER_TABLE_NAME = "t_volume_order";
    private static final String VOLUME_SIZE = "volume_size";

    /**
     * Attachment order attributes
     **/
    private static final String ATTACHMENT_ORDER_TABLE_NAME = "t_attachment_order";
    private static final String SOURCE = "source";
    private static final String TARGET = "target";
    private static final String DEVICE = "device";

    /**
     * Timestamp table attributes
     */
    private static final String TIMESTAMP_TABLE_NAME = "timestamp";
    private static final String TIMESTAMP = "timestamp";

    /**
     * Date attribute
     **/
    private static final String CREATE_AT = "create_at";

    /**
     * Commands to create tables
     **/
    protected static final String CREATE_COMPUTE_ORDER_TABLE_SQL = "CREATE TABLE IF NOT EXISTS "
            + COMPUTE_ORDER_TABLE_NAME + "(" + ORDER_ID + " VARCHAR(255) PRIMARY KEY, "
            + INSTANCE_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + FEDERATION_USER_ATTR + " VARCHAR(255), " + REQUESTING_MEMBER + " VARCHAR(255), " + PROVIDING_MEMBER + " VARCHAR(255), "
            + VCPU + " INTEGER, " + MEMORY + " INTEGER, " + DISK + " INTEGER, " + IMAGE_ID + " VARCHAR(255), "
            + USER_DATA_FILE_CONTENT + " VARCHAR(255), " + USER_DATA_FILE_TYPE + " VARCHAR(255), " + PUBLIC_KEY + " VARCHAR(255), "
            + ACTUAL_ALLOCATION_VCPU + " INTEGER, " + ACTUAL_ALLOCATION_RAM + " INTEGER, " + ACTUAL_ALLOCATION_INSTANCES + " INTEGER, "
            + NETWORKS_ID + " VARCHAR(255), " + CREATE_AT + " TIMESTAMP)";

    protected static final String CREATE_NETWORK_ORDER_TABLE_SQL = "CREATE TABLE IF NOT EXISTS "
            + NETWORK_ORDER_TABLE_NAME + "(" + ORDER_ID + " VARCHAR(255) PRIMARY KEY, "
            + INSTANCE_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + FEDERATION_USER_ATTR + " VARCHAR(255), " + REQUESTING_MEMBER + " VARCHAR(255), " + PROVIDING_MEMBER + " VARCHAR(255), "
            + GATEWAY + " VARCHAR(255), " + ADDRESS + " VARCHAR(255), " + ALLOCATION + " VARCHAR(255), "
            + CREATE_AT + " TIMESTAMP)";

    protected static final String CREATE_VOLUME_ORDER_TABLE_SQL = "CREATE TABLE IF NOT EXISTS "
            + VOLUME_ORDER_TABLE_NAME + "(" + ORDER_ID + " VARCHAR(255) PRIMARY KEY, "
            + INSTANCE_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + FEDERATION_USER_ATTR + " VARCHAR(255), " + REQUESTING_MEMBER + " VARCHAR(255), " + PROVIDING_MEMBER + " VARCHAR(255), "
            + VOLUME_SIZE + " TIMESTAMP, " + CREATE_AT + " INTEGER)";

    protected static final String CREATE_ATTACHMENT_ORDER_TABLE_SQL = "CREATE TABLE IF NOT EXISTS "
            + ATTACHMENT_ORDER_TABLE_NAME + "(" + ORDER_ID + " VARCHAR(255) PRIMARY KEY, "
            + INSTANCE_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + FEDERATION_USER_ATTR + " VARCHAR(255), " + REQUESTING_MEMBER + " VARCHAR(255), " + PROVIDING_MEMBER + " VARCHAR(255), "
            + SOURCE + " VARCHAR(255), " + TARGET + " VARCHAR(255), " + DEVICE + " VARCHAR(255), " + CREATE_AT + " TIMESTAMP)";

    protected static final String CREATE_TIMESTAMP_TABLE_SQL = "CREATE TABLE IF NOT EXISTS " + TIMESTAMP_TABLE_NAME
            + "(" + ORDER_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + TIMESTAMP + " TIMESTAMP, PRIMARY KEY (" + ORDER_ID + ", " + ORDER_STATE + "))";

    /**
     * Commands to insert orders into table
     **/
    protected static final String INSERT_COMPUTE_ORDER_SQL = "INSERT INTO " + COMPUTE_ORDER_TABLE_NAME
            + " (" + ORDER_ID + "," + INSTANCE_ID + "," + ORDER_STATE + "," + FEDERATION_USER_ID + ","
            + FEDERATION_USER_ATTR + "," + REQUESTING_MEMBER + "," + PROVIDING_MEMBER + "," + VCPU + ","
            + MEMORY + "," + DISK + "," + IMAGE_ID + "," + USER_DATA_FILE_CONTENT + ","
            + USER_DATA_FILE_TYPE + "," + PUBLIC_KEY + "," + ACTUAL_ALLOCATION_VCPU + "," + ACTUAL_ALLOCATION_RAM + ","
            + ACTUAL_ALLOCATION_INSTANCES + "," + NETWORKS_ID + "," + CREATE_AT +")"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    protected static final String INSERT_NETWORK_ORDER_SQL = "INSERT INTO " + NETWORK_ORDER_TABLE_NAME
            + " (" + ORDER_ID + "," + INSTANCE_ID + "," + ORDER_STATE + "," + FEDERATION_USER_ID + ","
            + FEDERATION_USER_ATTR + "," + REQUESTING_MEMBER + "," + PROVIDING_MEMBER + "," + GATEWAY
            + "," + ADDRESS + "," + ALLOCATION + "," + CREATE_AT +")" + " VALUES (?,?,?,?,?,?,?,?,?,?,?)";

    protected static final String INSERT_VOLUME_ORDER_SQL = "INSERT INTO " + VOLUME_ORDER_TABLE_NAME
            + " (" + ORDER_ID + "," + INSTANCE_ID + "," + ORDER_STATE + "," + FEDERATION_USER_ID + ","
            + FEDERATION_USER_ATTR + "," + REQUESTING_MEMBER + "," + PROVIDING_MEMBER + ","
            + VOLUME_SIZE + "," + CREATE_AT +")" + " VALUES (?,?,?,?,?,?,?,?,?)";

    protected static final String INSERT_ATTACHMENT_ORDER_SQL = "INSERT INTO " + ATTACHMENT_ORDER_TABLE_NAME
            + " (" + ORDER_ID + "," + INSTANCE_ID + "," + ORDER_STATE + "," + FEDERATION_USER_ID + ","
            + FEDERATION_USER_ATTR + "," + REQUESTING_MEMBER + "," + PROVIDING_MEMBER + ","
            + SOURCE + "," + TARGET + "," + DEVICE + "," + CREATE_AT +")" + " VALUES (?,?,?,?,?,?,?,?,?,?,?)";

    protected static final String INSERT_TIMESTAMP_SQL = "INSERT INTO " + TIMESTAMP_TABLE_NAME
            + " (" + ORDER_ID + "," + ORDER_STATE + "," + FEDERATION_USER_ID + "," + TIMESTAMP + ")"
            + " VALUES (?,?,?,?)";

    /**
     * Commands to select orders from the table
     **/
    protected static final String NOT_NULL_INSTANCE_ID = " AND instance_id IS NOT NULL";

    protected static String SELECT_COMPUTE_ORDER_SQL = "SELECT * FROM " + COMPUTE_ORDER_TABLE_NAME
            + " WHERE order_state=?";

    protected static String SELECT_NETWORK_ORDER_SQL = "SELECT * FROM " + NETWORK_ORDER_TABLE_NAME
            + " WHERE order_state=?";

    protected static String SELECT_VOLUME_ORDER_SQL = "SELECT * FROM " + VOLUME_ORDER_TABLE_NAME
            + " WHERE order_state=?";

    protected static String SELECT_ATTACHMENT_ORDER_SQL = "SELECT * FROM " + ATTACHMENT_ORDER_TABLE_NAME
            + " WHERE order_state=?";

    /**
     * Commands to update orders in the table
     **/
    protected static final String UPDATE_COMPUTE_ORDER_SQL = "UPDATE " + COMPUTE_ORDER_TABLE_NAME + " SET "
            + "instance_id=?,order_state=?,actual_alloc_vcpu=?,actual_alloc_ram=?,actual_alloc_instances=? WHERE "
            + ORDER_ID + "=?";

    protected static final String UPDATE_NETWORK_ORDER_SQL = "UPDATE " + NETWORK_ORDER_TABLE_NAME + " SET "
            + "instance_id=?,order_state=? WHERE " + ORDER_ID + "=?";

    protected static final String UPDATE_VOLUME_ORDER_SQL = "UPDATE " + VOLUME_ORDER_TABLE_NAME + " SET "
            + "instance_id=?,order_state=? WHERE " + ORDER_ID + "=?";

    protected static final String UPDATE_ATTACHMENT_ORDER_SQL = "UPDATE " + ATTACHMENT_ORDER_TABLE_NAME + " SET "
            + "order_state=? WHERE " + ORDER_ID + "=?";
}
