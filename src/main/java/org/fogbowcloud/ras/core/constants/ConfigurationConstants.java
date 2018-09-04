package org.fogbowcloud.ras.core.constants;

public class ConfigurationConstants {
    // PLUGINS CLASSES
    // Interoperability
    public static final String PUBLIC_IP_PLUGIN_CLASS_KEY = "public_ip_plugin_class";
    public static final String ATTACHMENT_PLUGIN_CLASS_KEY = "attachment_plugin_class";
    public static final String COMPUTE_PLUGIN_CLASS_KEY = "compute_plugin_class";
    public static final String COMPUTE_QUOTA_PLUGIN_CLASS_KEY = "compute_quota_plugin_class";
    public static final String NETWORK_PLUGIN_CLASS_KEY = "network_plugin_class";
    public static final String VOLUME_PLUGIN_CLASS_KEY = "volume_plugin_class";
    public static final String IMAGE_PLUGIN_CLASS_KEY = "image_plugin_class";
    // AAA
    public static final String TOKEN_GENERATOR_PLUGIN_CLASS = "token_generator_plugin_class";
    public static final String FEDERATION_IDENTITY_PLUGIN_CLASS_KEY = "federation_identity_plugin_class";
    public static final String AUTHENTICATION_PLUGIN_CLASS_KEY = "authentication_plugin_class";
    public static final String AUTHORIZATION_PLUGIN_CLASS_KEY = "authorization_plugin_class";
    public static final String LOCAL_USER_CREDENTIALS_MAPPER_PLUGIN_CLASS_KEY =
            "local_user_credentials_mapper_plugin_class";

    // RAS CONF
    public static final String RAS_SSH_PUBLIC_KEY_FILE_PATH = "ras_ssh_public_key_file_path";
    public static final String OPEN_ORDERS_SLEEP_TIME_KEY = "open_orders_sleep_time";
    public static final String SPAWNING_ORDERS_SLEEP_TIME_KEY = "spawning_orders_sleep_time";
    public static final String FULFILLED_ORDERS_SLEEP_TIME_KEY = "fulfilled_orders_sleep_time";
    public static final String CLOSED_ORDERS_SLEEP_TIME_KEY = "closed_orders_scheduler_period";
    public static final String HTTP_REQUEST_TIMEOUT = "http_request_timeout";

    // HISTORY & RECOVERY DATABASE CONF
    public static final String DATABASE_URL = "jdbc_database_url";
    public static final String DATABASE_USERNAME = "jdbc_database_username";
    public static final String DATABASE_PASSWORD = "jdbc_database_password";

    // SSH CONF
    public static final String SSH_COMMON_USER_KEY = "ssh_common_user";

    // INTERCOMPONENT CONF
    public static final String XMPP_JID_KEY = "xmpp_jid";
    public static final String XMPP_PASSWORD_KEY = "xmpp_password";
    public static final String XMPP_SERVER_IP_KEY = "xmpp_server_ip";
    public static final String XMPP_SERVER_PORT_KEY = "xmpp_server_port";
    public static final String XMPP_TIMEOUT_KEY = "xmpp_timeout";

    // Alias
    public static final String LOCAL_MEMBER_ID = XMPP_JID_KEY;
}
