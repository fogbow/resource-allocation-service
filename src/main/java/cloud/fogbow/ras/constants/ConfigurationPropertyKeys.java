package cloud.fogbow.ras.constants;

public class ConfigurationPropertyKeys {
    // INTERCOMPONENT configuration
    public static final String XMPP_ENABLED_KEY = "xmpp_enabled";
    public static final String XMPP_PASSWORD_KEY = "xmpp_password";
    public static final String XMPP_SERVER_IP_KEY = "xmpp_server_ip";
    public static final String XMPP_C2C_PORT_KEY = "xmpp_c2c_port";
    public static final String XMPP_TIMEOUT_KEY = "xmpp_timeout";

    // RAS configuration
    public static final String PROVIDER_ID_KEY = "provider_id";
    public static final String OPEN_ORDERS_SLEEP_TIME_KEY = "open_orders_sleep_time";
    public static final String SPAWNING_ORDERS_SLEEP_TIME_KEY = "spawning_orders_sleep_time";
    public static final String FULFILLED_ORDERS_SLEEP_TIME_KEY = "fulfilled_orders_sleep_time";
    public static final String UNABLE_TO_CHECK_ORDERS_SLEEP_TIME_KEY = "unable_to_check_orders_sleep_time";
    public static final String CHECKING_DELETION_ORDERS_SLEEP_TIME_KEY = "checking_deletion_orders_sleep_period";
    public static final String ASSIGNED_FOR_DELETION_ORDERS_SLEEP_TIME_KEY = "assigned_for_deletion_orders_sleep_period";
    public static final String REMOTE_ORDER_STATE_SYNCHRONIZATION_SLEEP_TIME_KEY = "remote_order_state_synchronization_sleep_period";
    public static final String PAUSING_ORDERS_SLEEP_TIME_KEY = "pausing_order_sleep_period";
    public static final String HIBERNATING_ORDERS_SLEEP_TIME_KEY = "hibernating_order_sleep_period";
    public static final String STOPPING_ORDERS_SLEEP_TIME_KEY = "stopping_orders_sleep_period";
    public static final String RESUMING_ORDERS_SLEEP_TIME_KEY = "resuming_order_sleep_period";
    public static final String CLOUD_NAMES_KEY = "cloud_names";
    public static final String BUILD_NUMBER_KEY = "build_number";

    // Plugins
    public static final String AUTHORIZATION_PLUGIN_CLASS_KEY = "authorization_plugin_class";
    public static final String PUBLIC_IP_PLUGIN_CLASS_KEY = "public_ip_plugin_class";
    public static final String ATTACHMENT_PLUGIN_CLASS_KEY = "attachment_plugin_class";
    public static final String COMPUTE_PLUGIN_CLASS_KEY = "compute_plugin_class";
    public static final String NETWORK_PLUGIN_CLASS_KEY = "network_plugin_class";
    public static final String VOLUME_PLUGIN_CLASS_KEY = "volume_plugin_class";
    public static final String IMAGE_PLUGIN_CLASS_KEY = "image_plugin_class";
    public static final String SECURITY_RULE_PLUGIN_CLASS_KEY = "security_rule_plugin_class";
    public static final String SYSTEM_TO_CLOUD_MAPPER_PLUGIN_CLASS_KEY = "system_to_cloud_mapper_plugin_class";
    public static final String CLOUD_IDENTITY_PROVIDER_URL_KEY = "cloud_identity_provider_url";
    public static final String QUOTA_PLUGIN_CLASS_KEY = "quota_plugin_class";

    // AS configuration
    public static final String AS_PORT_KEY = "as_port";
    public static final String AS_URL_KEY = "as_url";

    // SSH configuration
    public static final String SSH_COMMON_USER_KEY = "ssh_common_user";
    
    // Authorization
    public static final String ADMIN_ROLE = "admin_role";
    public static final String AUTHORIZATION_ROLES_KEY = "roles";
    public static final String DEFAULT_AUTH_PLUGIN_KEY = "default_auth_plugin_class";
    public static final String DEFAULT_ROLE_KEY = "default_role";
    public static final String FS_AUTHORIZED_ENDPOINT = "fs_authorized_endpoint";
    public static final String FS_PORT_KEY = "fs_port";
    public static final String FS_PUBLIC_KEY_ENDPOINT_KEY = "fs_public_key_endpoint";
    public static final String FS_URL_KEY = "fs_url";
    public static final String MS_PORT_KEY = "ms_port";
    public static final String MS_URL_KEY = "ms_url";
    public static final String POLICY_CLASS_KEY = "policy_class";
    public static final String POLICY_FILE_KEY = "policy_file";
    public static final String SUPERUSER_ROLE_KEY = "superuser_role";
    public static final String USER_NAMES_KEY = "users";
}
