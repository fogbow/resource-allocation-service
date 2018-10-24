package org.fogbowcloud.ras.core.constants;

import java.util.concurrent.TimeUnit;

public class DefaultConfigurationConstants {
    // CONFIGURATION FILES PATHS
    public static final String RAS_CONF_FILE_NAME = "ras.conf";
    public static final String INTERCOMPONENT_CONF_FILE_NAME = "intercomponent.conf";
    public static final String INTEROPERABILITY_CONF_FILE_NAME = "interoperability.conf";
    public static final String AAA_CONF_FILE_NAME = "aaa.conf";
    public static final String SHIBBOLETH_CONF_FILE_NAME = "shibboleth.conf";

    // CLOUD PLUGINS CONF FILES PATHS
    public static final String OPENSTACK_CONF_FILE_NAME = "openstack.conf";
    public static final String CLOUDSTACK_CONF_FILE_NAME = "cloudstack.conf";

    // RAS CONF DEFAULTS
    // reference value is 1 second
    public static final String OPEN_ORDERS_SLEEP_TIME = Long.toString(TimeUnit.SECONDS.toMillis(1));
    // reference value is 1 second
    public static final String CLOSED_ORDERS_SLEEP_TIME = Long.toString(TimeUnit.SECONDS.toMillis(1));
    // reference value is 5 seconds
    public static final String SPAWNING_ORDERS_SLEEP_TIME = Long.toString(TimeUnit.SECONDS.toMillis(10));
    // reference value is 1 second
    public static final String FULFILLED_ORDERS_SLEEP_TIME = Long.toString(TimeUnit.SECONDS.toMillis(10));
    // reference value is 1 minute
    public static final String HTTP_REQUEST_TIMEOUT = Long.toString(TimeUnit.MINUTES.toMillis(1));

    // INTERCOMPONENT CONF DEFAULTS
    // reference value is 5 seconds
    public static final String XMPP_TIMEOUT = Long.toString(TimeUnit.SECONDS.toMillis(5));

    // SSH CONF DEFAULTS
    public static final String SSH_COMMON_USER = "fogbow";
}
