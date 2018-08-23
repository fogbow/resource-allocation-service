package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.compute;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.tokens.CloudStackToken;
import org.fogbowcloud.manager.core.models.tokens.generators.cloudstack.CloudStackTokenGenerator;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.compute.v4_9.CloudStackComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.network.CloudStackNetworkPlugin;
import org.junit.Before;
import org.junit.Test;

public class CloudStackComputePluginTest {

    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");
    }

    @Test
    public void getTestTemp() throws UnexpectedException, FogbowManagerException {
    }

    @Test
    public void createTestTemp() throws UnexpectedException, FogbowManagerException {
    }
}
