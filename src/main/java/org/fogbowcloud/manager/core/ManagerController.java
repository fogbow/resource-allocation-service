package org.fogbowcloud.manager.core;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;

public class ManagerController {

	private InstanceProvider localInstanceProvider;
	private InstanceProvider remoteInstanceProvider;

	private String localMemberId;

	private static final Logger LOGGER = Logger.getLogger(ManagerController.class);

	public ManagerController(Properties properties, InstanceProvider localInstanceProvider,
			InstanceProvider remoteInstanceProvider) {
		
	}

}
