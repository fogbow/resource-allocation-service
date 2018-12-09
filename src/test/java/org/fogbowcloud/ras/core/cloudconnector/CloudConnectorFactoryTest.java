package org.fogbowcloud.ras.core.cloudconnector;

import org.fogbowcloud.ras.core.plugins.mapper.FederationToLocalMapperPlugin;
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
    }

    // test case: When calling getCloudConnector by passing allocationAllowableValues memberId equal to allocationAllowableValues previously
    // configured local member, it must return an instance of LocalCloudConnector.
    @Test
    public void testGetCloudConnectorLocal() {
        // set up
        FederationToLocalMapperPlugin mapperPlugin =
                Mockito.mock(FederationToLocalMapperPlugin.class);

        // exercise
        CloudConnector localCloudConnector =
                this.cloudConnectorFactory.getCloudConnector(LOCAL_MEMBER_ID, "default");

        // verify
        Assert.assertTrue(localCloudConnector instanceof LocalCloudConnector);
    }

    // test case: When calling getCloudConnector by passing allocationAllowableValues different memberId from allocationAllowableValues previously
    // configured local member, it must return an instance of RemoteCloudConnector.
    @Test
    public void testGetCloudConnectorRemote() {
        // exercise
        CloudConnector remoteCloudConnector =
                this.cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString());

        // verify
        Assert.assertTrue(remoteCloudConnector instanceof RemoteCloudConnector);
    }

}
