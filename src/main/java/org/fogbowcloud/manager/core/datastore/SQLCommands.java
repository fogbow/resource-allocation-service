package org.fogbowcloud.manager.core.datastore;

public class SQLCommands {

    /** Order attributes **/
    private static final String ORDER_ID = "order_id";
    private static final String ORDER_STATE = "order_state";
    private static final String FEDERATION_USER_ID = "fed_user_id";
    private static final String FEDERATION_USER_ATTR = "fed_user_attr";
    private static final String REQUESTING_MEMBER = "requesting_member";
    private static final String PROVIDING_MEMBER = "providing_member";
    private static final String INSTANCE_ID = "instance_id";

    /** Compute order attributes **/
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

    /** Network order attributes **/
    private static final String NETWORK_ORDER_TABLE_NAME = "t_network_order";
    private static final String GATEWAY = "gateway";
    private static final String ADDRESS = "address";
    private static final String ALLOCATION = "allocation";

    /** Volume order attributes **/
    private static final String VOLUME_ORDER_TABLE_NAME = "t_volume_order";
    private static final String VOLUME_SIZE = "volume_size";

    /** Attachment order attributes **/
    private static final String ATTACHMENT_ORDER_TABLE_NAME = "t_attachment_order";
    private static final String SOURCE = "source";
    private static final String TARGET = "target";
    private static final String DEVICE = "device";

    /** Commands to create tables **/
    protected static final String CREATE_COMPUTE_ORDER_SQL = "CREATE TABLE IF NOT EXISTS "
            + COMPUTE_ORDER_TABLE_NAME + "(" + ORDER_ID + " VARCHAR(255) PRIMARY KEY, "
            + INSTANCE_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + FEDERATION_USER_ATTR + " VARCHAR(255), " + REQUESTING_MEMBER + " VARCHAR(255), " + PROVIDING_MEMBER + " VARCHAR(255), "
            + VCPU + " INTEGER, " + MEMORY + " INTEGER, " + DISK + " INTEGER, " + IMAGE_ID + " VARCHAR(255), "
            + USER_DATA_FILE_CONTENT + " VARCHAR(255), " + USER_DATA_FILE_TYPE + " VARCHAR(255), " + PUBLIC_KEY + " VARCHAR(255), "
            + ACTUAL_ALLOCATION_VCPU + " INTEGER, " + ACTUAL_ALLOCATION_RAM + " INTEGER, " + ACTUAL_ALLOCATION_INSTANCES + " INTEGER)";

    protected static final String CREATE_NETWORK_ORDER_SQL = "CREATE TABLE IF NOT EXISTS "
            + NETWORK_ORDER_TABLE_NAME + "(" + ORDER_ID + " VARCHAR(255) PRIMARY KEY, "
            + INSTANCE_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + FEDERATION_USER_ATTR + " VARCHAR(255), " + REQUESTING_MEMBER + " VARCHAR(255), " + PROVIDING_MEMBER + " VARCHAR(255), "
            + GATEWAY + " VARCHAR(255), " + ADDRESS + " VARCHAR(255), " + ALLOCATION + " VARCHAR(255))";

    protected static final String CREATE_VOLUME_ORDER_SQL = "CREATE TABLE IF NOT EXISTS "
            + VOLUME_ORDER_TABLE_NAME + "(" + ORDER_ID + " VARCHAR(255) PRIMARY KEY, "
            + INSTANCE_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + FEDERATION_USER_ATTR + " VARCHAR(255), " + REQUESTING_MEMBER + " VARCHAR(255), " + PROVIDING_MEMBER + " VARCHAR(255), "
            + VOLUME_SIZE + " INTEGER)";

    protected static final String CREATE_ATTACHMENT_ORDER_SQL = "CREATE TABLE IF NOT EXISTS "
            + ATTACHMENT_ORDER_TABLE_NAME + "(" + ORDER_ID + " VARCHAR(255) PRIMARY KEY, "
            + INSTANCE_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + FEDERATION_USER_ATTR + " VARCHAR(255), " + REQUESTING_MEMBER + " VARCHAR(255), " + PROVIDING_MEMBER + " VARCHAR(255), "
            + SOURCE + " VARCHAR(255), " + TARGET + " VARCHAR(255), " + DEVICE + " VARCHAR(255))";

    /** Commands to insert orders into table **/
    protected static final String INSERT_COMPUTE_ORDER_SQL = "INSERT INTO " + COMPUTE_ORDER_TABLE_NAME
            + " (" + ORDER_ID + "," + INSTANCE_ID + "," + ORDER_STATE + "," + FEDERATION_USER_ID + ","
            + FEDERATION_USER_ATTR + "," + REQUESTING_MEMBER + "," + PROVIDING_MEMBER + "," + VCPU + ","
            + MEMORY + "," + DISK + "," + IMAGE_ID + "," + USER_DATA_FILE_CONTENT + ","
            + USER_DATA_FILE_TYPE + "," + PUBLIC_KEY + "," + ACTUAL_ALLOCATION_VCPU + "," + ACTUAL_ALLOCATION_RAM + ","
            + ACTUAL_ALLOCATION_INSTANCES +")" + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    protected static final String INSERT_NETWORK_ORDER_SQL = "INSERT INTO " + NETWORK_ORDER_TABLE_NAME
            + " (" + ORDER_ID + "," + INSTANCE_ID + "," + ORDER_STATE + "," + FEDERATION_USER_ID + ","
            + FEDERATION_USER_ATTR + "," + REQUESTING_MEMBER + "," + PROVIDING_MEMBER + "," + GATEWAY
            + "," + ADDRESS + "," + ALLOCATION +")" + " VALUES (?,?,?,?,?,?,?,?,?,?)";

    protected static final String INSERT_VOLUME_ORDER_SQL = "INSERT INTO " + VOLUME_ORDER_TABLE_NAME
            + " (" + ORDER_ID + "," + INSTANCE_ID + "," + ORDER_STATE + "," + FEDERATION_USER_ID + ","
            + FEDERATION_USER_ATTR + "," + REQUESTING_MEMBER + "," + PROVIDING_MEMBER + ","
            + VOLUME_SIZE +")" + " VALUES (?,?,?,?,?,?,?,?)";

    protected static final String INSERT_ATTACHMENT_ORDER_SQL = "INSERT INTO " + ATTACHMENT_ORDER_TABLE_NAME
            + " (" + ORDER_ID + "," + INSTANCE_ID + "," + ORDER_STATE + "," + FEDERATION_USER_ID + ","
            + FEDERATION_USER_ATTR + "," + REQUESTING_MEMBER + "," + PROVIDING_MEMBER + ","
            + SOURCE + "," + TARGET + "," + DEVICE +")" + " VALUES (?,?,?,?,?,?,?,?,?,?)";

    /** Commands to update orders in the table **/

    /** Commands to select orders from the table **/
}
