package org.fogbowcloud.manager.core.datastore.commands;

public class NetworkSQLCommands extends OrderTableAttributes {

    public static final String CREATE_NETWORK_ORDER_TABLE_SQL = "CREATE TABLE IF NOT EXISTS "
            + NETWORK_ORDER_TABLE_NAME + "(" + ORDER_ID + " VARCHAR(255) PRIMARY KEY, "
            + INSTANCE_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + FEDERATION_USER_ATTR + " VARCHAR(255), " + REQUESTING_MEMBER + " VARCHAR(255), " + PROVIDING_MEMBER + " VARCHAR(255), "
            + GATEWAY + " VARCHAR(255), " + ADDRESS + " VARCHAR(255), " + ALLOCATION + " VARCHAR(255), "
            + CREATE_AT + " TIMESTAMP)";

    public static final String INSERT_NETWORK_ORDER_SQL = "INSERT INTO " + NETWORK_ORDER_TABLE_NAME
            + " (" + ORDER_ID + "," + INSTANCE_ID + "," + ORDER_STATE + "," + FEDERATION_USER_ID + ","
            + FEDERATION_USER_ATTR + "," + REQUESTING_MEMBER + "," + PROVIDING_MEMBER + "," + GATEWAY
            + "," + ADDRESS + "," + ALLOCATION + "," + CREATE_AT +")" + " VALUES (?,?,?,?,?,?,?,?,?,?,?)";

    public static final String SELECT_NETWORK_ORDER_SQL = "SELECT * FROM " + NETWORK_ORDER_TABLE_NAME
            + " WHERE order_state=?";

    public static final String SELECT_NETWORK_ORDER_NOT_NULL_INSTANCE_ID = "SELECT * FROM " + NETWORK_ORDER_TABLE_NAME
            + " WHERE order_state=? AND instance_id IS NOT NULL";

    public static final String UPDATE_NETWORK_ORDER_SQL = "UPDATE " + NETWORK_ORDER_TABLE_NAME + " SET "
            + "instance_id=?,order_state=? WHERE " + ORDER_ID + "=?";
}
