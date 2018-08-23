package org.fogbowcloud.manager.core.constants;

import java.util.concurrent.TimeUnit;

public class DefaultConfigurationConstants {
    // CONFIGURATION FILES PATHS
    public static final String MANAGER_CONF_FILE_NAME = "manager.conf";
    public static final String INTERCOMPONENT_CONF_FILE_NAME = "intercomponent.conf";
    public static final String CLOUD_CONF_FILE_NAME = "cloud.conf";
    public static final String BEHAVIOR_CONF_FILE_NAME = "behavior.conf";
    
    // CLOUD PLUGINS CONF FILES PATHS
    public static final String OPENSTACK_CONF_FILE_NAME = "openstack.conf";

    // MANAGER CONF DEFAULTS
    public static final String OPEN_ORDERS_SLEEP_TIME =
            Long.toString(TimeUnit.SECONDS.toMillis(1)); // 1 second
    public static final String CLOSED_ORDERS_SLEEP_TIME =
            Long.toString(TimeUnit.SECONDS.toMillis(1)); // 1 second
    public static final String SPAWNING_ORDERS_SLEEP_TIME =
            Long.toString(TimeUnit.SECONDS.toMillis(5)); // reference value is 5 seconds
    public static final String FULFILLED_ORDERS_SLEEP_TIME =
            Long.toString(TimeUnit.SECONDS.toMillis(1)); // 1 second
    public static final String HTTP_REQUEST_TIMEOUT = Long.toString(TimeUnit.MINUTES.toMillis(1)); // 1 minute

    // INTERCOMPONENT CONF DEFAULTS
    public static final String XMPP_SERVER_PORT = "5347";
    public static final String XMPP_TIMEOUT = Long.toString(TimeUnit.SECONDS.toMillis(5)); // reference value is 5 seconds

    // SSH CONF DEFAULTS
    public static final String SSH_COMMON_USER = "fogbow";
}
