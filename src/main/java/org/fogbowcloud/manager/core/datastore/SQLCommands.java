package org.fogbowcloud.manager.core.datastore;

public class SQLCommands {

    /** Order commands **/
    private static final String ORDER_TABLE_NAME = "t_compute_order";
    private static final String ORDER_ID = "order_id";
    private static final String ORDER_STATE = "order_state";
    private static final String FEDERATION_USER_ID = "fed_user_id";
    private static final String FEDERATION_USER_ATTR = "fed_user_attr";
    private static final String REQUESTING_MEMBER = "requesting_member";
    private static final String PROVIDING_MEMBER = "providing_member";
    private static final String INSTANCE_ID = "instance_id";
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

    protected static final String CREATE_COMPUTE_ORDER_SQL = "CREATE TABLE IF NOT EXISTS "
            + ORDER_TABLE_NAME + "(" + ORDER_ID + " VARCHAR(255) PRIMARY KEY, "
            + INSTANCE_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + FEDERATION_USER_ATTR + " VARCHAR(255), " + REQUESTING_MEMBER + " VARCHAR(255), " + PROVIDING_MEMBER + " VARCHAR(255), "
            + VCPU + " INTEGER, " + MEMORY + " INTEGER, " + DISK + " INTEGER, " + IMAGE_ID + " VARCHAR(255), "
            + USER_DATA_FILE_CONTENT + " VARCHAR(255), " + USER_DATA_FILE_TYPE + " VARCHAR(255), " + PUBLIC_KEY + " VARCHAR(255), "
            + ACTUAL_ALLOCATION_VCPU + " INTEGER, " + ACTUAL_ALLOCATION_RAM + " INTEGER, " + ACTUAL_ALLOCATION_INSTANCES + " INTEGER)";

    protected static final String CREATE_NETWORK_ORDER_SQL = "CREATE TABLE IF NOT EXISTS "
            + ORDER_TABLE_NAME + "(" + ORDER_ID + " VARCHAR(255) PRIMARY KEY, "
            + INSTANCE_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + FEDERATION_USER_ATTR + " VARCHAR(255), " + REQUESTING_MEMBER + " VARCHAR(255), " + PROVIDING_MEMBER + " VARCHAR(255), "
            + VCPU + " INTEGER, " + MEMORY + " INTEGER, " + DISK + " INTEGER, " + IMAGE_ID + " VARCHAR(255), "
            + USER_DATA_FILE_CONTENT + " VARCHAR(255), " + USER_DATA_FILE_TYPE + " VARCHAR(255), " + PUBLIC_KEY + " VARCHAR(255), "
            + ACTUAL_ALLOCATION_VCPU + " INTEGER, " + ACTUAL_ALLOCATION_RAM + " INTEGER, " + ACTUAL_ALLOCATION_INSTANCES + " INTEGER)";

    protected static final String CREATE_VOLUME_ORDER_SQL = "CREATE TABLE IF NOT EXISTS "
            + ORDER_TABLE_NAME + "(" + ORDER_ID + " VARCHAR(255) PRIMARY KEY, "
            + INSTANCE_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + FEDERATION_USER_ATTR + " VARCHAR(255), " + REQUESTING_MEMBER + " VARCHAR(255), " + PROVIDING_MEMBER + " VARCHAR(255), "
            + VCPU + " INTEGER, " + MEMORY + " INTEGER, " + DISK + " INTEGER, " + IMAGE_ID + " VARCHAR(255), "
            + USER_DATA_FILE_CONTENT + " VARCHAR(255), " + USER_DATA_FILE_TYPE + " VARCHAR(255), " + PUBLIC_KEY + " VARCHAR(255), "
            + ACTUAL_ALLOCATION_VCPU + " INTEGER, " + ACTUAL_ALLOCATION_RAM + " INTEGER, " + ACTUAL_ALLOCATION_INSTANCES + " INTEGER)";

    protected static final String CREATE_ATTACHMENT_ORDER_SQL = "CREATE TABLE IF NOT EXISTS "
            + ORDER_TABLE_NAME + "(" + ORDER_ID + " VARCHAR(255) PRIMARY KEY, "
            + INSTANCE_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + FEDERATION_USER_ATTR + " VARCHAR(255), " + REQUESTING_MEMBER + " VARCHAR(255), " + PROVIDING_MEMBER + " VARCHAR(255), "
            + VCPU + " INTEGER, " + MEMORY + " INTEGER, " + DISK + " INTEGER, " + IMAGE_ID + " VARCHAR(255), "
            + USER_DATA_FILE_CONTENT + " VARCHAR(255), " + USER_DATA_FILE_TYPE + " VARCHAR(255), " + PUBLIC_KEY + " VARCHAR(255), "
            + ACTUAL_ALLOCATION_VCPU + " INTEGER, " + ACTUAL_ALLOCATION_RAM + " INTEGER, " + ACTUAL_ALLOCATION_INSTANCES + " INTEGER)";

    protected static final String INSERT_ORDER_SQL = "INSERT INTO " + ORDER_TABLE_NAME
            + " (" + ORDER_ID + "," + INSTANCE_ID + ")" + " VALUES (?,?)";
}
