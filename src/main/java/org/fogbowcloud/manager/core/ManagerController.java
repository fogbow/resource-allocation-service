package org.fogbowcloud.manager.core;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.orders.OrderRegistry;
import org.fogbowcloud.manager.core.threads.AttendOpenOrdersThread;

public class ManagerController {

	private OrderRegistry orderRegistry;

	private InstanceProvider localInstanceProvider;
	private InstanceProvider remoteInstanceProvider;

	private Thread attendOpenOrdersThread;

	private String localMemberId;

	private static final Logger LOGGER = Logger.getLogger(ManagerController.class);

	public ManagerController(Properties properties, InstanceProvider localInstanceProvider,
			InstanceProvider remoteInstanceProvider, OrderRegistry orderRegistry) {
		this.localMemberId = properties.getProperty(ConfigurationConstants.XMPP_ID_KEY);
		this.localInstanceProvider = localInstanceProvider;
		this.remoteInstanceProvider = remoteInstanceProvider;
		this.orderRegistry = orderRegistry;

		this.attendOpenOrdersThread = new AttendOpenOrdersThread(this.localInstanceProvider,
				this.remoteInstanceProvider, this.orderRegistry, this.localMemberId, properties);
		
		this.startManagerThreads();
	}

	/**
	 * This method starts all manager threads, if you defined a new manager
	 * operation and this operation require a new thread to run, you should
	 * start this thread at this method.
	 */
	private void startManagerThreads() {
		LOGGER.info("Starting Manager Threads...");
		this.attendOpenOrdersThread.start();
	}

}
