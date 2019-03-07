package cloud.fogbow.ras.core.cloudconnector;

import cloud.fogbow.ras.core.plugins.mapper.SystemToCloudMapperPlugin;
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

    // test case: When calling getCloudConnector by passing a memberId equal to a previously
    // configured local member, it must return an instance of LocalCloudConnector.
    @Test
    public void testGetCloudConnectorLocal() {
        // set up
        SystemToCloudMapperPlugin mapperPlugin =
                Mockito.mock(SystemToCloudMapperPlugin.class);

        // exercise
        CloudConnector localCloudConnector =
                this.cloudConnectorFactory.getCloudConnector(LOCAL_MEMBER_ID, "default");

        // verify
        Assert.assertTrue(localCloudConnector instanceof LocalCloudConnector);
    }

    // test case: When calling getCloudConnector by passing a different memberId from a previously
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
