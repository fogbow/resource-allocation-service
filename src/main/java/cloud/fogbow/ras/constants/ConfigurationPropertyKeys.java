package cloud.fogbow.ras.constants;

public class ConfigurationPropertyKeys {
    // INTERCOMPONENT configuration
    public static final String XMPP_JID_KEY = "xmpp_jid";
    public static final String XMPP_PASSWORD_KEY = "xmpp_password";
    public static final String XMPP_SERVER_IP_KEY = "xmpp_server_ip";
    public static final String XMPP_C2C_PORT_KEY = "xmpp_c2c_port";
    public static final String XMPP_TIMEOUT_KEY = "xmpp_timeout";

    // RAS configuration
    public static final String LOCAL_MEMBER_ID_KEY = XMPP_JID_KEY;
    public static final String OPEN_ORDERS_SLEEP_TIME_KEY = "open_orders_sleep_time";
    public static final String SPAWNING_ORDERS_SLEEP_TIME_KEY = "spawning_orders_sleep_time";
    public static final String FULFILLED_ORDERS_SLEEP_TIME_KEY = "fulfilled_orders_sleep_time";
    public static final String FAILED_ORDERS_SLEEP_TIME_KEY = "failed_orders_sleep_time";
    public static final String CLOSED_ORDERS_SLEEP_TIME_KEY = "closed_orders_sleep_period";
    public static final String CLOUD_NAMES_KEY = "cloud_names";
    public static final String BUILD_NUMBER_KEY = "build_number";

    // Plugins
    public static final String AUTHORIZATION_PLUGIN_CLASS_KEY = "authorization_plugin_class";
    public static final String PUBLIC_IP_PLUGIN_CLASS_KEY = "public_ip_plugin_class";
    public static final String ATTACHMENT_PLUGIN_CLASS_KEY = "attachment_plugin_class";
    public static final String COMPUTE_PLUGIN_CLASS_KEY = "compute_plugin_class";
    public static final String COMPUTE_QUOTA_PLUGIN_CLASS_KEY = "compute_quota_plugin_class";
    public static final String NETWORK_PLUGIN_CLASS_KEY = "network_plugin_class";
    public static final String VOLUME_PLUGIN_CLASS_KEY = "volume_plugin_class";
    public static final String IMAGE_PLUGIN_CLASS_KEY = "image_plugin_class";
    public static final String GENERIC_PLUGIN_CLASS_KEY = "generic_plugin_class";
    public static final String SECURITY_RULE_PLUGIN_CLASS_KEY = "security_rule_plugin_class";
    public static final String LOCAL_USER_CREDENTIALS_MAPPER_PLUGIN_CLASS_KEY =
            "local_user_credentials_mapper_plugin_class";
    public static final String TOKEN_GENERATOR_URL_KEY = "token_generator_url";

    // AS configuration
    public static final String AS_PORT_KEY = "as_port";
    public static final String AS_URL_KEY = "as_url";

    // SSH configuration
    public static final String SSH_COMMON_USER_KEY = "ssh_common_user";
}
