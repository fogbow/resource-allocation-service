package org.fogbowcloud.manager.core;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.threads.OpenProcessor;

public class ManagerController {

	private InstanceProvider localInstanceProvider;
	private InstanceProvider remoteInstanceProvider;

	private Thread openProcessorThread;

	private String localMemberId;

	private static final Logger LOGGER = Logger.getLogger(ManagerController.class);

	public ManagerController(Properties properties, InstanceProvider localInstanceProvider,
			InstanceProvider remoteInstanceProvider) {
		this.localMemberId = properties.getProperty(ConfigurationConstants.XMPP_ID_KEY);
		this.localInstanceProvider = localInstanceProvider;
		this.remoteInstanceProvider = remoteInstanceProvider;

		OpenProcessor openProcessor = new OpenProcessor(this.localInstanceProvider,
				this.remoteInstanceProvider, this.localMemberId, properties);
		this.openProcessorThread = new Thread(openProcessor);
		
		this.startManagerThreads();
	}

	/**
	 * This method starts all manager threads, if you defined a new manager
	 * operation and this operation require a new thread to run, you should
	 * start this thread at this method.
	 */
	private void startManagerThreads() {
		LOGGER.info("Starting Manager OpenProcessor Thread...");
		this.openProcessorThread.start();
	}

}
