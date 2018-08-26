package org.fogbowcloud.ras.core.cloudconnector;

import org.fogbowcloud.ras.core.InteroperabilityPluginsHolder;
import org.fogbowcloud.ras.core.plugins.aaa.mapper.FederationToLocalMapperPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class CloudConnectorFactoryTest {

    private static final String LOCAL_MEMBER_ID = "fake-localidentity-member";

    private CloudConnectorFactory cloudConnectorFactory;

    @Before
    public void setup() {
        this.cloudConnectorFactory = Mockito.spy(CloudConnectorFactory.getInstance());
        this.cloudConnectorFactory.setLocalMemberId(LOCAL_MEMBER_ID);
    }

    // test case: When calling getCloudConnector by passing a memberId equal to a previously
    // configured local member, it must return an instance of LocalCloudConnector.
    @Test
    public void testGetCloudConnectorLocal() {
        // set up
        FederationToLocalMapperPlugin mapperPlugin =
                Mockito.mock(FederationToLocalMapperPlugin.class);
        this.cloudConnectorFactory.setMapperPlugin(mapperPlugin);

        InteroperabilityPluginsHolder interoperabilityPluginsHolder = Mockito.mock(InteroperabilityPluginsHolder.class);
        this.cloudConnectorFactory.setInteroperabilityPluginsHolder(interoperabilityPluginsHolder);

        // exercise
        CloudConnector localCloudConnector =
                this.cloudConnectorFactory.getCloudConnector(LOCAL_MEMBER_ID);

        // verify
        Assert.assertTrue(localCloudConnector instanceof LocalCloudConnector);
    }

    // test case: When calling getCloudConnector by passing a different memberId from a previously
    // configured local member, it must return an instance of RemoteCloudConnector.
    @Test
    public void testGetCloudConnectorRemote() {
        // exercise
        CloudConnector remoteCloudConnector =
                this.cloudConnectorFactory.getCloudConnector(Mockito.anyString());

        // verify
        Assert.assertTrue(remoteCloudConnector instanceof RemoteCloudConnector);
    }

}
