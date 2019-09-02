package cloud.fogbow.ras.core.plugins.interoperability.openstack.publicip.v2;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;

@PrepareForTest({ DatabaseManager.class, GetFloatingIpResponse.class, OpenStackCloudUtils.class })
public class OpenStackPublicIpPluginTest extends BaseUnitTests {

	private OpenStackPublicIpPlugin plugin;
	private OpenStackHttpClient client;

	private static final String ANY_VALUE = "anything";
	private static final String FAKE_NETWORK_PORT_ID = "fake-network-port-id";
    private static final String FAKE_PORT_ID_JSON_RESPONSE = "{\"ports\":[{\"id\":\"d80b1a3b-4fc1-49f3-952e-1e2ab7081d8b\"}]}";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_PUBLIC_IP_JSON_RESPONSE = "{\"floatingip\":{\"floating_ip_address\":\"fake-address\",\"id\":\"fake-instance-id\",\"status\": \"ACTIVE\"}}";
    private static final String FAKE_SECURITY_GROUP_ID = "fake-security-group-id";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String NEUTRON_PREFIX_ENDPOINT = "https://mycloud.domain:9696";
    
	@Before
	public void setUp() throws Exception {
	    this.testUtils.mockReadOrdersFromDataBase();
        this.client = Mockito.mock(OpenStackHttpClient.class);
		
        String openstackCloudConfPath = HomeDir.getPath() 
		        + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME 
		        + File.separator
				+ TestUtils.DEFAULT_CLOUD_NAME 
		        + File.separator 
		        + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        
		this.plugin = Mockito.spy(new OpenStackPublicIpPlugin(openstackCloudConfPath));
        this.plugin.setClient(this.client);
	}
	
	// test case: When calling the isReady method with the cloud states active,
    // this means that the state of public IP is READY and it must return true.
    @Test
    public void testIsReady() {
        // set up
        String cloudState = OpenStackStateMapper.ACTIVE_STATUS;

        // exercise
        boolean status = this.plugin.isReady(cloudState);

        // verify
        Assert.assertTrue(status);
    }
    
    // test case: When calling the isReady method with the cloud states different
    // than active, this means that the state of public IP is not READY and it must
    // return false.
    @Test
    public void testNotIsReady() {
        // set up
        String cloudState = ANY_VALUE;

        // exercise
        boolean status = this.plugin.isReady(cloudState);

        // verify
        Assert.assertFalse(status);
    }
    
    // test case: When calling the hasFailed method with the cloud states different
    // than active, this means that the state of the public IP is FAILED and it must
    // return true.
    @Ignore // TODO check the functionality of this method... 
    @Test
    public void testHasFailed() {
        // set up
        String cloudState = ANY_VALUE;

        // exercise
        boolean status = this.plugin.hasFailed(cloudState);

        // verify
        Assert.assertTrue(status);
    }
    
    // test case: When calling the hasFailed method with the cloud states active,
    // this means that the state of the public IP is not FAILED and it must return
    // false.
    @Test
    public void testNotHasFailed() {
        // set up
        String cloudState = OpenStackStateMapper.ACTIVE_STATUS;

        // exercise
        boolean status = this.plugin.hasFailed(cloudState);

        // verify
        Assert.assertFalse(status);
    }
    
    // test case: When invoking the requestInstance method with a valid public IP
    // order and a cloud user, it must verify that the call was successful.
    @Test
    public void testRequestInstance() throws FogbowException {
        // set up
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        OpenStackV3User cloudUser = createOpenStackUser();

        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        String securityGroupId = FAKE_SECURITY_GROUP_ID;

        PowerMockito.mockStatic(OpenStackCloudUtils.class);
        PowerMockito.when(OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser))).thenCallRealMethod();

        Mockito.doReturn(FAKE_NETWORK_PORT_ID).when(this.plugin).getNetworkPortId(Mockito.eq(order),
                Mockito.eq(cloudUser));
        Mockito.doReturn(instanceId).when(this.plugin).doRequestInstance(Mockito.any(CreateFloatingIpRequest.class),
                Mockito.eq(cloudUser));
        Mockito.doReturn(securityGroupId).when(this.plugin).doCreateSecurityGroup(Mockito.eq(instanceId),
                Mockito.eq(order), Mockito.eq(cloudUser));
        Mockito.doNothing().when(this.plugin).allowAllIngressSecurityRules(Mockito.eq(securityGroupId),
                Mockito.eq(cloudUser));
        Mockito.doNothing().when(this.plugin).associateSecurityGroup(Mockito.eq(securityGroupId),
                Mockito.eq(instanceId), Mockito.eq(order), Mockito.eq(cloudUser));

        // exercise
        this.plugin.requestInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenStackCloudUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getExternalNetworkId();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getNetworkPortId(Mockito.eq(order),
                Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doRequestInstance(Mockito.any(CreateFloatingIpRequest.class), Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doCreateSecurityGroup(Mockito.eq(instanceId),
                Mockito.eq(order), Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .allowAllIngressSecurityRules(Mockito.eq(securityGroupId), Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).associateSecurityGroup(
                Mockito.eq(securityGroupId), Mockito.eq(instanceId), Mockito.eq(order), Mockito.eq(cloudUser));
    }
    
    // test case: When invoking the getInstance method with a valid public IP order
    // and a cloud user, it must verify that the call was successful.
    @Test
    public void testGetInstance() throws FogbowException {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(instanceId);

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackPublicIpPlugin.FLOATINGIPS 
                + OpenStackPublicIpPlugin.ENDPOINT_SEPARATOR 
                + instanceId;

        String json = FAKE_PUBLIC_IP_JSON_RESPONSE;
        Mockito.doReturn(json).when(this.plugin).doGetResponseFromCloud(Mockito.eq(endpoint), Mockito.eq(cloudUser));

        GetFloatingIpResponse response = GetFloatingIpResponse.fromJson(json);
        PowerMockito.mockStatic(GetFloatingIpResponse.class);
        PowerMockito.when(GetFloatingIpResponse.fromJson(Mockito.eq(json))).thenReturn(response);

        // exercise
        this.plugin.getInstance(order, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getFloatingIpEndpoint();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetResponseFromCloud(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));

        PowerMockito.verifyStatic(GetFloatingIpResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        GetFloatingIpResponse.fromJson(Mockito.eq(json));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildPublicIpInstance(Mockito.eq(response));
    }
    
    // test case: When invoking the deleteInstance method with a valid public IP
    // order and a cloud user, it must verify that the call was successful.
    @Test
    public void testDeleteInstance() throws FogbowException {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(instanceId);

        String securityGroupId = FAKE_SECURITY_GROUP_ID;
        String securityGroupName = SystemConstants.PIP_SECURITY_GROUP_PREFIX + instanceId;
        Mockito.doReturn(securityGroupId).when(this.plugin).retrieveSecurityGroupId(Mockito.eq(securityGroupName),
                Mockito.eq(cloudUser));

        Mockito.doNothing().when(this.plugin).disassociateSecurityGroup(Mockito.eq(securityGroupName),
                Mockito.eq(order), Mockito.eq(cloudUser));
        Mockito.doNothing().when(this.plugin).deleteSecurityGroup(Mockito.eq(securityGroupId), Mockito.eq(cloudUser));
        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.eq(instanceId), Mockito.eq(cloudUser));

        // exercise
        this.plugin.deleteInstance(order, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupName(Mockito.eq(instanceId));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .retrieveSecurityGroupId(Mockito.eq(securityGroupName), Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .disassociateSecurityGroup(Mockito.eq(securityGroupName), Mockito.eq(order), Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).deleteSecurityGroup(Mockito.eq(securityGroupId),
                Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(Mockito.eq(instanceId),
                Mockito.eq(cloudUser));
    }
    
    // TODO Create a OpenStackTestUtils to unify common methods made these...
    private OpenStackV3User createOpenStackUser() {
        String userId = TestUtils.FAKE_USER_ID;
        String userName = TestUtils.FAKE_USER_NAME;
        String tokenValue = FAKE_TOKEN_VALUE;
        String projectId = FAKE_PROJECT_ID;
        return new OpenStackV3User(userId, userName, tokenValue, projectId);
    }
	
}
