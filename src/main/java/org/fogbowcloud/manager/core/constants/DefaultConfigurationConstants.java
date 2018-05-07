package org.fogbowcloud.manager.core.constants;

import java.util.concurrent.TimeUnit;

public class DefaultConfigurationConstants {

	// SCHEDULER PERIODS
	public static final String OPEN_ORDERS_SLEEP_TIME = Long.toString(TimeUnit.SECONDS.toMillis(1)); // 1 second
	public static final String FULFILLED_ORDERS_SLEEP_TIME = Long.toString(TimeUnit.SECONDS.toMillis(1)); // 1 second
}
