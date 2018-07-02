package org.fogbowcloud.manager.core.datastore.commands;

public class AttachmentSQLCommands extends OrderTableAttributes {

    public static final String CREATE_ATTACHMENT_ORDER_TABLE_SQL = "CREATE TABLE IF NOT EXISTS "
            + ATTACHMENT_ORDER_TABLE_NAME + "(" + ORDER_ID + " VARCHAR(255) PRIMARY KEY, "
            + INSTANCE_ID + " VARCHAR(255), " + ORDER_STATE + " VARCHAR(255), " + FEDERATION_USER_ID + " VARCHAR(255), "
            + FEDERATION_USER_ATTR + " VARCHAR(255), " + REQUESTING_MEMBER + " VARCHAR(255), " + PROVIDING_MEMBER + " VARCHAR(255), "
            + SOURCE + " VARCHAR(255), " + TARGET + " VARCHAR(255), " + DEVICE + " VARCHAR(255), " + CREATE_AT + " TIMESTAMP)";

    public static final String INSERT_ATTACHMENT_ORDER_SQL = "INSERT INTO " + ATTACHMENT_ORDER_TABLE_NAME
            + " (" + ORDER_ID + "," + INSTANCE_ID + "," + ORDER_STATE + "," + FEDERATION_USER_ID + ","
            + FEDERATION_USER_ATTR + "," + REQUESTING_MEMBER + "," + PROVIDING_MEMBER + ","
            + SOURCE + "," + TARGET + "," + DEVICE + "," + CREATE_AT +")" + " VALUES (?,?,?,?,?,?,?,?,?,?,?)";

    public static String SELECT_ATTACHMENT_ORDER_SQL = "SELECT * FROM " + ATTACHMENT_ORDER_TABLE_NAME
            + " WHERE order_state=?";

    public static final String UPDATE_ATTACHMENT_ORDER_SQL = "UPDATE " + ATTACHMENT_ORDER_TABLE_NAME + " SET "
            + "instance_id=?,order_state=? WHERE " + ORDER_ID + "=?";

    public static void updateSelectCommand() {
        SELECT_ATTACHMENT_ORDER_SQL += NOT_NULL_INSTANCE_ID;
    }
}
