package org.fogbowcloud.ras.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;

import java.util.ArrayList;
import java.util.List;

public class CloudListController {
    private static final Logger LOGGER = Logger.getLogger(CloudListController.class);

    private List<String> cloudNames;

    public CloudListController() {
        String memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        this.cloudNames = new ArrayList<>();
        String cloudNamesList = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.CLOUD_NAMES_KEY);
        if (cloudNamesList == null || cloudNamesList.isEmpty()) {
            throw new FatalErrorException(Messages.Fatal.NO_CLOUD_SPECIFIED);
        }

        for (String cloud : cloudNamesList.split(",")) {
            cloud = cloud.trim();
            // Here we populate the cache of the CloudConnectorFactory and, at the same time, check if all
            // clouds have been correctly configured. If not, the RAS won't even start, and will throw a
            // fatal exception.
            CloudConnectorFactory.getInstance().getCloudConnector(memberId, cloud);
            this.cloudNames.add(cloud);
        }
    }

    public List<String> getCloudNames() {
        return this.cloudNames;
    }

    public String getDefaultCloudName() {
        return this.cloudNames.get(0);
    }
}
