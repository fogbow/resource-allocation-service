package org.fogbowcloud.manager.core.datastore.commands;

public class OrderTableAttributes {

    /**
     * Order attributes
     **/
    protected static final String ORDER_ID = "order_id";
    protected static final String ORDER_STATE = "order_state";
    protected static final String FEDERATION_USER_ID = "fed_user_id";
    protected static final String FEDERATION_USER_ATTR = "fed_user_attr";
    protected static final String REQUESTING_MEMBER = "requesting_member";
    protected static final String PROVIDING_MEMBER = "providing_member";
    protected static final String INSTANCE_ID = "instance_id";

    /**
     * Compute order attributes
     **/
    protected static final String COMPUTE_ORDER_TABLE_NAME = "t_compute_order";
    protected static final String VCPU = "vcpu";
    protected static final String MEMORY = "memory";
    protected static final String DISK = "disk";
    protected static final String IMAGE_ID = "image_id";
    protected static final String USER_DATA_FILE_CONTENT = "user_data_file_content";
    protected static final String USER_DATA_FILE_TYPE = "user_data_file_type";
    protected static final String PUBLIC_KEY = "public_key";
    protected static final String ACTUAL_ALLOCATION_VCPU = "actual_alloc_vcpu";
    protected static final String ACTUAL_ALLOCATION_RAM = "actual_alloc_ram";
    protected static final String ACTUAL_ALLOCATION_INSTANCES = "actual_alloc_instances";
    protected static final String NETWORKS_ID = "networks_id";

    /**
     * Network order attributes
     **/
    protected static final String NETWORK_ORDER_TABLE_NAME = "t_network_order";
    protected static final String GATEWAY = "gateway";
    protected static final String ADDRESS = "address";
    protected static final String ALLOCATION = "allocation";

    /**
     * Volume order attributes
     **/
    protected static final String VOLUME_ORDER_TABLE_NAME = "t_volume_order";
    protected static final String VOLUME_SIZE = "volume_size";

    /**
     * Attachment order attributes
     **/
    protected static final String ATTACHMENT_ORDER_TABLE_NAME = "t_attachment_order";
    protected static final String SOURCE = "source";
    protected static final String TARGET = "target";
    protected static final String DEVICE = "device";

    /**
     * Timestamp table attributes
     */
    protected static final String TIMESTAMP_TABLE_NAME = "timestamp";
    protected static final String TIMESTAMP = "timestamp";

    /**
     * Date attribute
     **/
    protected static final String CREATE_AT = "create_at";

    /**
     * Command to select orders from the table with not null instance id
     **/
    protected static final String NOT_NULL_INSTANCE_ID = " AND instance_id IS NOT NULL";
}
