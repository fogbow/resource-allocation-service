package org.fogbowcloud.manager.core.constants;

import java.util.concurrent.TimeUnit;

public class DefaultConfigurationConstants {

	// SCHEDULER PERIODS
	public static final String OPEN_ORDERS_SLEEP_TIME = Long.toString(TimeUnit.SECONDS.toMillis(1)); // 1 second

	public static final String FULFILLED_ORDERS_SLEEP_TIME = Long.toString(TimeUnit.SECONDS.toMillis(1)); // 1 second

	public static final String SPAWNING_ORDERS_SLEEP_TIME = Long.toString(TimeUnit.SECONDS.toMillis(5)); // reference value is 5 seconds

	public static final String DEFAULT_INSTANCE_IP_MONITORING_TIME = Long.toString(TimeUnit.SECONDS.toMillis(10)); // reference value is 10 seconds
	
	// DEFAULT VALUE
	public static final String SSH_COMMON_USER_KEY = "fogbow";

	public static final String SSH_COMMON_USER = "fogbow";
}
