package org.fogbowcloud.manager.core;

import java.util.Properties;

import org.fogbowcloud.manager.core.manager.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.statisticsprovider.LocalStatisticsProvider;
import org.fogbowcloud.manager.core.statisticsprovider.RemoteStatisticsProvider;
import org.fogbowcloud.manager.core.statisticsprovider.StatisticsProvider;

public class StatisticsController {
	
	StatisticsProvider localStatisticsProvider;
	StatisticsProvider remoteStatisticsProvider;
	String localMemberId;
	
	StatisticsController (
			LocalStatisticsProvider localStatisticsProvider,
			RemoteStatisticsProvider remoteStatisticsProvider,
			Properties properties
			){
		this.localStatisticsProvider = localStatisticsProvider;
		this.remoteStatisticsProvider = remoteStatisticsProvider;
		this.localMemberId = properties.getProperty(ConfigurationConstants.XMPP_ID_KEY);
	}
	
	StatisticsProvider getStatisticsProvider(String memberId) {
		if (this.localMemberId.equals(memberId)) {
			return this.localStatisticsProvider;
		} else {
			return this.remoteStatisticsProvider;
		}
	}
	
}
