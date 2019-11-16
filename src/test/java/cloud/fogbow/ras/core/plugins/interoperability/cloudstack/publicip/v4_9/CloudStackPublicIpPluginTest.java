package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@PrepareForTest({DatabaseManager.class})
public class CloudStackPublicIpPluginTest extends BaseUnitTests {

    private Map<String, AsyncRequestInstanceState> asyncRequestInstanceStateMapMockEmpty = new HashMap<>();
    private CloudStackPublicIpPlugin plugin;
    private CloudStackHttpClient client;
    private CloudStackUser cloudStackUser;
    private String cloudStackUrl;

    @Before
    public void setUp() throws UnexpectedException {
        String cloudStackConfFilePath = CloudstackTestUtils.CLOUDSTACK_CONF_FILE_PATH;
        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);

        this.plugin = Mockito.spy(new CloudStackPublicIpPlugin(cloudStackConfFilePath));
        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin.setClient(this.client);
        this.plugin.setAsyncRequestInstanceStateMap(this.asyncRequestInstanceStateMapMockEmpty);

        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        this.testUtils.mockReadOrdersFromDataBase();
    }

    // test case: When calling the doGetInstance method with secondary methods mocked and the
    // asynchronous request instance is ready, it must verify if It returns the publicIpInstance ready.
    @Test
    public void testDoGetInstanceWhenIsReady() throws FogbowException {
        // set up
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
        String instanceId = "instanceId";
        Mockito.when(publicIpOrder.getId()).thenReturn(instanceId);
        AsyncRequestInstanceState asyncRequestInstanceStateReady = new AsyncRequestInstanceState(
                AsyncRequestInstanceState.StateType.READY, null, null);
        this.asyncRequestInstanceStateMapMockEmpty.put(instanceId, asyncRequestInstanceStateReady);

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.doGetInstance(publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackStateMapper.READY_STATUS, publicIpInstance.getCloudState());
    }

    // test case: When calling the doGetInstance method with secondary methods mocked and the
    // asynchronous request instance is neither ready or failed, it must verify if It returns
    // the current publicIpInstance returned by the buildCurrentPublicIpInstance.
    @Test
    public void testDoGetInstanceWhenIsNotReady() throws FogbowException {
        // set up
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
        String instanceId = "instanceId";
        Mockito.when(publicIpOrder.getId()).thenReturn(instanceId);
        AsyncRequestInstanceState asyncRequestInstanceStateNotReady = new AsyncRequestInstanceState(
                AsyncRequestInstanceState.StateType.CREATING_FIREWALL_RULE, null, null);
        this.asyncRequestInstanceStateMapMockEmpty.put(instanceId, asyncRequestInstanceStateNotReady);

        PublicIpInstance publicIpInstanceExcepted = Mockito.mock(PublicIpInstance.class);
        Mockito.doReturn(publicIpInstanceExcepted).when(this.plugin).buildCurrentPublicIpInstance(
                Mockito.eq(asyncRequestInstanceStateNotReady), Mockito.eq(publicIpOrder), Mockito.eq(this.cloudStackUser));

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.doGetInstance(publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(publicIpInstanceExcepted, publicIpInstance);
    }

    // test case: When calling the doGetInstance method with secondary methods mocked and the
    // asynchronous request instance is null because a memory lost, it must verify if It returns
    // the current publicIpInstance failure.
    @Test
    public void testDoGetInstanceFailWhenThereMemoryLost() throws FogbowException {
        // set up
        PublicIpOrder publicIpOrder = Mockito.mock(PublicIpOrder.class);
        String instanceId = "instanceId";
        Mockito.when(publicIpOrder.getId()).thenReturn(instanceId);
        this.asyncRequestInstanceStateMapMockEmpty = new HashMap<>();

        // exercise
        PublicIpInstance publicIpInstance = this.plugin.doGetInstance(publicIpOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(CloudStackStateMapper.FAILURE_STATUS, publicIpInstance.getCloudState());
    }

}
