package org.fogbowcloud.manager.core.constants;

import java.util.concurrent.TimeUnit;

public class DefaultConfigurationConstants {

    // CONFIGURATION FILES PATHS
    public static final String MANAGER_CONF_FILE_FULL_PATH =
            "src/main/resources/manager.properties";
    public static final String REVERSE_TUNNEL_CONF_FILE_FULL_PATH =
            "src/main/resources/reverse-tunnel.properties";
    public static final String INTERCOMPONENT_CONF_FILE_FULL_PATH =
            "src/main/resources/intercomponent.properties";
    public static final String CLOUD_CONF_FILE_FULL_PATH =
            "src/main/resources/cloud.properties";
    public static final String BEHAVIOR_CONF_FILE_FULL_PATH =
            "src/main/resources/behavior.properties";

    // SCHEDULER PERIODS
    public static final String OPEN_ORDERS_SLEEP_TIME =
            Long.toString(TimeUnit.SECONDS.toMillis(1)); // 1 second
    public static final String CLOSED_ORDERS_SLEEP_TIME =
            Long.toString(TimeUnit.SECONDS.toMillis(1)); // 1 second
    public static final String SPAWNING_ORDERS_SLEEP_TIME =
            Long.toString(TimeUnit.SECONDS.toMillis(5)); // reference value is 5 seconds
    public static final String DEFAULT_INSTANCE_IP_MONITORING_TIME =
            Long.toString(TimeUnit.SECONDS.toMillis(10)); // reference value is 10 seconds
    public static final String FULFILLED_ORDERS_SLEEP_TIME =
            Long.toString(TimeUnit.SECONDS.toMillis(1)); // 1 second

    // DEFAULT VALUE
    public static final String SSH_COMMON_USER = "fogbow";
}
