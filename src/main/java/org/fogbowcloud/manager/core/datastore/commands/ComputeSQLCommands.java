package org.fogbowcloud.manager.core.datastore.commands;

public class ComputeSQLCommands extends OrderTableAttributes {

    public static final String CREATE_COMPUTE_ORDER_TABLE_SQL = "CREATE TABLE IF NOT EXISTS "
            + COMPUTE_ORDER_TABLE_NAME + "(" + ORDER_ID + " VARCHAR(255) PRIMARY KEY, "
            + INSTANCE_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + FEDERATION_USER_ATTR + " VARCHAR(255), " + REQUESTING_MEMBER + " VARCHAR(255), " + PROVIDING_MEMBER + " VARCHAR(255), "
            + VCPU + " INTEGER, " + MEMORY + " INTEGER, " + DISK + " INTEGER, " + IMAGE_ID + " VARCHAR(255), "
            + USER_DATA_FILE_CONTENT + " VARCHAR(255), " + USER_DATA_FILE_TYPE + " VARCHAR(255), " + PUBLIC_KEY + " VARCHAR(255), "
            + ACTUAL_ALLOCATION_VCPU + " INTEGER, " + ACTUAL_ALLOCATION_RAM + " INTEGER, " + ACTUAL_ALLOCATION_INSTANCES + " INTEGER, "
            + NETWORKS_ID + " VARCHAR(255), " + CREATE_AT + " TIMESTAMP)";

    public static final String INSERT_COMPUTE_ORDER_SQL = "INSERT INTO " + COMPUTE_ORDER_TABLE_NAME
            + " (" + ORDER_ID + "," + INSTANCE_ID + "," + ORDER_STATE + "," + FEDERATION_USER_ID + ","
            + FEDERATION_USER_ATTR + "," + REQUESTING_MEMBER + "," + PROVIDING_MEMBER + "," + VCPU + ","
            + MEMORY + "," + DISK + "," + IMAGE_ID + "," + USER_DATA_FILE_CONTENT + ","
            + USER_DATA_FILE_TYPE + "," + PUBLIC_KEY + "," + ACTUAL_ALLOCATION_VCPU + "," + ACTUAL_ALLOCATION_RAM + ","
            + ACTUAL_ALLOCATION_INSTANCES + "," + NETWORKS_ID + "," + CREATE_AT +")"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public static final String SELECT_COMPUTE_ORDER_SQL = "SELECT * FROM " + COMPUTE_ORDER_TABLE_NAME
            + " WHERE order_state=?";

    public static final String SELECT_COMPUTE_ORDER_NOT_NULL_INSTANCE_ID = "SELECT * FROM " + COMPUTE_ORDER_TABLE_NAME
            + " WHERE order_state=? AND instance_id IS NOT NULL";

    public static final String UPDATE_COMPUTE_ORDER_SQL = "UPDATE " + COMPUTE_ORDER_TABLE_NAME + " SET "
            + "instance_id=?,order_state=?,actual_alloc_vcpu=?,actual_alloc_ram=?,actual_alloc_instances=? WHERE "
            + ORDER_ID + "=?";
}
