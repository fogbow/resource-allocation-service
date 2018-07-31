package org.fogbowcloud.manager.core.cloudconnector;

import org.fogbowcloud.manager.core.CloudPluginsHolder;
import org.fogbowcloud.manager.core.plugins.behavior.mapper.FederationToLocalMapperPlugin;
import org.hamcrest.CoreMatchers;
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

        CloudPluginsHolder cloudPluginsHolder = Mockito.mock(CloudPluginsHolder.class);
        this.cloudConnectorFactory.setCloudPluginsHolder(cloudPluginsHolder);

        // exercise
        CloudConnector localCloudConnector =
                this.cloudConnectorFactory.getCloudConnector(LOCAL_MEMBER_ID);

        // verify
        Assert.assertThat(localCloudConnector, CoreMatchers.instanceOf(LocalCloudConnector.class));
    }

    // test case: When calling getCloudConnector by passing a different memberId from a previously
    // configured local member, it must return an instance of RemoteCloudConnector.
    @Test
    public void testGetCloudConnectorRemote() {
        // exercise
        CloudConnector remoteCloudConnector =
                this.cloudConnectorFactory.getCloudConnector(Mockito.anyString());

        // verify
        Assert.assertThat(remoteCloudConnector,
                CoreMatchers.instanceOf(RemoteCloudConnector.class));
    }

}
