package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class CloudListController {
    private static final Logger LOGGER = Logger.getLogger(CloudListController.class);

    private List<String> cloudNames;

    public CloudListController() {
        String providerId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);

        this.cloudNames = new ArrayList<>();
        String cloudNamesList = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.CLOUD_NAMES_KEY);
        if (cloudNamesList == null || cloudNamesList.isEmpty()) {
            throw new FatalErrorException(Messages.Fatal.NO_CLOUD_SPECIFIED);
        }

        for (String cloud : cloudNamesList.split(",")) {
            cloud = cloud.trim();
            // Here we populate the list of clouds configured and, at the same time, check if all
            // clouds have been correctly configured. If not, the RAS won't even start, and will throw a
            // fatal exception.
            CloudConnectorFactory.getInstance().getCloudConnector(providerId, cloud);
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
