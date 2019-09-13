package cloud.fogbow.ras.core.plugins.interoperability.openstack.publicip.v2;

import java.io.File;

import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.AddSecurityGroupToServerRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.CreateSecurityGroupRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.CreateSecurityGroupResponse;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.CreateSecurityGroupRuleRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.RemoveSecurityGroupFromServerRequest;

@PrepareForTest({ 
    CreateFloatingIpResponse.class, 
    CreateSecurityGroupResponse.class, 
    DatabaseManager.class, 
    GetFloatingIpResponse.class, 
    GetNetworkPortsResponse.class, 
    GetSecurityGroupsResponse.class, 
    OpenStackCloudUtils.class, 
    OpenStackHttpToFogbowExceptionMapper.class 
})
public class OpenStackPublicIpPluginTest extends BaseUnitTests {

	private static final String EMPTY_PORTS_FROM_JSON_RESPONSE = "{\"ports\":[]}";
	private static final String EMPTY_SECURITY_GROUP_FROM_JSON_RESPONSE = "{\"security_groups\":[]}";
	private static final String FAKE_CREATE_FLOATING_IP_FROM_JSON_RESPONSE = "{\"floatingip\":{\"id\":\"fake-instance-id\"}}";
	private static final String FAKE_CREATE_SECURITY_GROUP_FROM_JSON_RESPONSE = "{\"security_group\":{\"id\":\"fake-security-group-id\"}}";
	private static final String FAKE_FLOATING_NETWORK_ID = "fake-floating-network-id";
	private static final String FAKE_GET_PORTS_FROM_JSON_RESPONSE = "{\"ports\":[{\"id\":\"fake-network-port-id\"}]}";
	private static final String FAKE_GET_SECURITY_GROUPS_FROM_JSON_RESPONSE = "{\"security_groups\":[{\"id\":\"fake-security-group-id\"}]}";
	private static final String FAKE_NETWORK_PORT_ID = "fake-network-port-id";
	private static final String FAKE_NETWORK_PORTS_SUFIX_ENDPOINT = "?device_id=fake-compute-id&network_id=fake-network-id";
    private static final String FAKE_PUBLIC_IP_FROM_JSON_RESPONSE = "{\"floatingip\":{\"floating_ip_address\":\"fake-address\",\"id\":\"fake-instance-id\",\"status\": \"ACTIVE\"}}";
    private static final String FAKE_SECURITY_GROUP_NAME = "fogbow-sg-pip-fake-instance-id";
    private static final String NEUTRON_PREFIX_ENDPOINT = "https://mycloud.domain:9696";
    private static final String COMPUTE_PREFIX_ENDPOINT = "https://mycloud.domain:8774";
    
    private static final int RUN_TWICE = 2;
    
    private OpenStackPublicIpPlugin plugin;
    private OpenStackHttpClient client;
    
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
        String cloudState = TestUtils.ANY_VALUE;

        // exercise
        boolean status = this.plugin.isReady(cloudState);

        // verify
        Assert.assertFalse(status);
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
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();

        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;

        PowerMockito.mockStatic(OpenStackCloudUtils.class);
        PowerMockito.when(OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser))).thenCallRealMethod();

        Mockito.doReturn(FAKE_NETWORK_PORT_ID).when(this.plugin).getNetworkPortId(Mockito.eq(order),
                Mockito.eq(cloudUser));
        Mockito.doReturn(instanceId).when(this.plugin).doRequestInstance(Mockito.any(CreateFloatingIpRequest.class),
                Mockito.eq(cloudUser));
        Mockito.doReturn(securityGroupId).when(this.plugin).doCreateSecurityGroup(Mockito.eq(instanceId),
                Mockito.eq(cloudUser));
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
                Mockito.eq(cloudUser));
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
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(instanceId);

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackPublicIpPlugin.FLOATINGIPS 
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR 
                + instanceId;

        PublicIpInstance instance = createPublicIpInstance();
        Mockito.doReturn(instance).when(this.plugin).doGetInstance(Mockito.anyString(), Mockito.eq(cloudUser));

        // exercise
        this.plugin.getInstance(order, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getFloatingIpEndpoint();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));
    }
    
    // test case: When invoking the deleteInstance method with a valid public IP
    // order and a cloud user, it must verify that the call was successful.
    @Test
    public void testDeleteInstance() throws FogbowException {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(instanceId);

        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;
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
    
    // test case: When calling the doDeleteInstance method, it must verify that the
    // call was successful.
    @Test
    public void testDoDeleteInstance() throws Exception {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String instanceId = TestUtils.FAKE_INSTANCE_ID;

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackPublicIpPlugin.FLOATINGIPS 
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR 
                + instanceId;

        // exercise
        this.plugin.doDeleteInstance(instanceId, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getFloatingIpEndpoint();
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doDeleteRequest(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));
    }
    
    // test case: When calling the doDeleteInstance method and an unexpected error
    // occurs, it must verify that the map method of the
    // OpenStackHttpToFogbowExceptionMapper class has been called.
    @Test
    public void testDoDeleteInstanceFail() throws Exception {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String instanceId = TestUtils.FAKE_INSTANCE_ID;

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackPublicIpPlugin.FLOATINGIPS 
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR 
                + instanceId;
        
        HttpResponseException expectedException = new HttpResponseException(TestUtils.ERROR_STATUS_CODE,
                TestUtils.MESSAGE_STATUS_CODE);
        Mockito.doThrow(expectedException).when(this.client).doDeleteRequest(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, TestUtils.MAP_METHOD,
                Mockito.any());

        try {
            // exercise
            this.plugin.doDeleteInstance(instanceId, cloudUser);
            Assert.fail();
        } catch (Exception e) {
            // verify
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(expectedException));
        }
    }
    
    // test case: When calling the disassociateSecurityGroup method, it must verify
    // that the call was successful.
    @Test
    public void testDisassociateSecurityGroup() throws Exception {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(instanceId);

        PowerMockito.mockStatic(OpenStackCloudUtils.class);
        PowerMockito.when(OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser))).thenCallRealMethod();

        String endpoint = COMPUTE_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.COMPUTE_V2_API_ENDPOINT
                + cloudUser.getProjectId() 
                + OpenStackCloudUtils.SERVERS 
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR
                + order.getComputeId() 
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR 
                + OpenStackCloudUtils.ACTION;

        String name = FAKE_SECURITY_GROUP_NAME;
        RemoveSecurityGroupFromServerRequest request = new RemoveSecurityGroupFromServerRequest.Builder()
                .name(name)
                .build();

        // exercise
        this.plugin.disassociateSecurityGroup(name, order, cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenStackCloudUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getComputeAssociationEndpoint(Mockito.eq(cloudUser.getProjectId()), Mockito.eq(order.getComputeId()));
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doPostRequest(Mockito.eq(endpoint),
                Mockito.eq(request.toJson()), Mockito.eq(cloudUser));
    }
    
    // test case: When calling the disassociateSecurityGroup method and an
    // unexpected error occurs, it must verify that the map method of the
    // OpenStackHttpToFogbowExceptionMapper class has been called.
    @Test
    public void testDisassociateSecurityGroupFail() throws Exception {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(instanceId);
        
        String endpoint = COMPUTE_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.COMPUTE_V2_API_ENDPOINT
                + cloudUser.getProjectId()
                + OpenStackCloudUtils.SERVERS
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR
                + order.getComputeId()
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR
                + OpenStackCloudUtils.ACTION;
        
        String name = FAKE_SECURITY_GROUP_NAME;
        RemoveSecurityGroupFromServerRequest request = new RemoveSecurityGroupFromServerRequest.Builder()
                .name(name)
                .build();
        
        HttpResponseException expectedException = new HttpResponseException(TestUtils.ERROR_STATUS_CODE,
                TestUtils.MESSAGE_STATUS_CODE);
        Mockito.doThrow(expectedException).when(this.client).doPostRequest(Mockito.eq(endpoint), Mockito.eq(request.toJson()),
                Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, TestUtils.MAP_METHOD,
                Mockito.any());

        try {
            // exercise
            this.plugin.disassociateSecurityGroup(name, order, cloudUser);
            Assert.fail();
        } catch (Exception e) {
            // verify
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(expectedException));
        }
    }
    
    // test case: When calling the retrieveSecurityGroupId method, it must verify
    // that the call was successful.
    @Test
    public void testRetrieveSecurityGroupId() throws FogbowException {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String securityGroupName = FAKE_SECURITY_GROUP_NAME;

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackCloudUtils.SECURITY_GROUPS 
                + OpenStackPublicIpPlugin.QUERY_NAME 
                + securityGroupName;

        String json = FAKE_GET_SECURITY_GROUPS_FROM_JSON_RESPONSE;
        Mockito.doReturn(json).when(this.plugin).doGetResponseFromCloud(Mockito.eq(endpoint), Mockito.eq(cloudUser));

        GetSecurityGroupsResponse response = GetSecurityGroupsResponse.fromJson(json);
        PowerMockito.mockStatic(GetSecurityGroupsResponse.class);
        PowerMockito.when(GetSecurityGroupsResponse.fromJson(Mockito.eq(json))).thenReturn(response);

        String expected = TestUtils.FAKE_SECURITY_GROUP_ID;

        // exercise
        String securityGroupId = this.plugin.retrieveSecurityGroupId(securityGroupName, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupsEndpoint();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetResponseFromCloud(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));

        PowerMockito.verifyStatic(GetSecurityGroupsResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        GetSecurityGroupsResponse.fromJson(Mockito.eq(json));

        Assert.assertEquals(expected, securityGroupId);
    }
    
    // test case: When calling the retrieveSecurityGroupId method with a
    // non-existent ID, it must return to an empty security group and throw an
    // UnexpectedException.
    @Test
    public void testRetrieveSecurityGroupIdFail() throws FogbowException {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String securityGroupName = FAKE_SECURITY_GROUP_NAME;

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackCloudUtils.SECURITY_GROUPS 
                + OpenStackPublicIpPlugin.QUERY_NAME 
                + securityGroupName;

        String json = EMPTY_SECURITY_GROUP_FROM_JSON_RESPONSE;
        Mockito.doReturn(json).when(this.plugin).doGetResponseFromCloud(Mockito.eq(endpoint), Mockito.eq(cloudUser));

        GetSecurityGroupsResponse response = GetSecurityGroupsResponse.fromJson(json);
        PowerMockito.mockStatic(GetSecurityGroupsResponse.class);
        PowerMockito.when(GetSecurityGroupsResponse.fromJson(Mockito.eq(json))).thenReturn(response);

        String expected = String.format(Messages.Exception.NO_SECURITY_GROUP_FOUND, json);

        try {
            // exercise
            this.plugin.retrieveSecurityGroupId(securityGroupName, cloudUser);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doGetSecurityGroupsResponseFrom method with a
    // JSON malformed, it must verify that a UnexpectedException was throw.
    @Test
    public void testDoGetSecurityGroupsResponseFromJsonMalformed() throws FogbowException {
        // set up
        String json = TestUtils.JSON_MALFORMED;
        String expected = String.format(Messages.Error.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                OpenStackCloudUtils.SECURITY_GROUP_RESOURCE);
        try {
            // exercise
            this.plugin.doGetSecurityGroupsResponseFrom(json);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doGetInstance method, it must verify that the
    // call was successful.
    @Test
    public void testDoGetInstance() throws FogbowException {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(instanceId);

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackPublicIpPlugin.FLOATINGIPS 
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR 
                + instanceId;

        String json = FAKE_PUBLIC_IP_FROM_JSON_RESPONSE;
        Mockito.doReturn(json).when(this.plugin).doGetResponseFromCloud(Mockito.eq(endpoint), Mockito.eq(cloudUser));

        GetFloatingIpResponse response = GetFloatingIpResponse.fromJson(json);
        PowerMockito.mockStatic(GetFloatingIpResponse.class);
        PowerMockito.when(GetFloatingIpResponse.fromJson(Mockito.eq(json))).thenReturn(response);

        // exercise
        this.plugin.doGetInstance(endpoint, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetResponseFromCloud(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));

        PowerMockito.verifyStatic(GetFloatingIpResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        GetFloatingIpResponse.fromJson(Mockito.eq(json));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildPublicIpInstance(Mockito.eq(response));
    }
    
    // test case: When calling the doGetFloatingIpResponseFrom method with a
    // JSON malformed, it must verify that a UnexpectedException was throw.
    @Test
    public void testDoGetFloatingIpResponseFromJsonMalformed() throws FogbowException {
        // set up
        String json = TestUtils.JSON_MALFORMED;
        String expected = String.format(Messages.Error.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                OpenStackPublicIpPlugin.PUBLIC_IP_RESOURCE);
        try {
            // exercise
            this.plugin.doGetFloatingIpResponseFrom(json);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the associateSecurityGroup method, it must verify
    // that the call was successful.
    @Test
    public void testAssociateSecurityGroup() throws Exception {
        // set up
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(instanceId);

        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        PowerMockito.mockStatic(OpenStackCloudUtils.class);
        PowerMockito.when(OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser))).thenCallRealMethod();
        
        String endpoint = COMPUTE_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.COMPUTE_V2_API_ENDPOINT
                + cloudUser.getProjectId() 
                + OpenStackCloudUtils.SERVERS 
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR
                + order.getComputeId() 
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR 
                + OpenStackCloudUtils.ACTION;
        
        String name = FAKE_SECURITY_GROUP_NAME;
        AddSecurityGroupToServerRequest request = new AddSecurityGroupToServerRequest.Builder()
                .name(name)
                .build();

        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;

        // exercise
        this.plugin.associateSecurityGroup(securityGroupId, instanceId, order, cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenStackCloudUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getComputeAssociationEndpoint(Mockito.eq(cloudUser.getProjectId()), Mockito.eq(order.getComputeId()));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupName(Mockito.eq(instanceId));
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doPostRequest(Mockito.eq(endpoint),
                Mockito.eq(request.toJson()), Mockito.eq(cloudUser));
    }
    
    // test case: When calling the associateSecurityGroup method and an unexpected
    // error occurs, it must verify that the map method of the
    // OpenStackHttpToFogbowExceptionMapper class and the deleteSecurityGroup method
    // from this class were been called.
    @Test
    public void testAssociateSecurityGroupFail() throws Exception {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(instanceId);

        String endpoint = COMPUTE_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.COMPUTE_V2_API_ENDPOINT
                + cloudUser.getProjectId() 
                + OpenStackCloudUtils.SERVERS 
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR
                + order.getComputeId() 
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR 
                + OpenStackCloudUtils.ACTION;

        AddSecurityGroupToServerRequest request = new AddSecurityGroupToServerRequest.Builder()
                .name(FAKE_SECURITY_GROUP_NAME)
                .build();

        HttpResponseException expectedException = new HttpResponseException(TestUtils.ERROR_STATUS_CODE,
                TestUtils.MESSAGE_STATUS_CODE);
        Mockito.doThrow(expectedException).when(this.client).doPostRequest(Mockito.eq(endpoint),
                Mockito.eq(request.toJson()), Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, TestUtils.MAP_METHOD,
                Mockito.any());

        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        try {
            // exercise
            this.plugin.associateSecurityGroup(securityGroupId, instanceId, order, cloudUser);
            Assert.fail();
        } catch (Exception e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                    .deleteSecurityGroup(Mockito.eq(securityGroupId), Mockito.eq(cloudUser));

            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(expectedException));
        }
    }
    
    // test case: When calling the deleteSecurityGroup method, it must verify
    // that the call was successful.
    @Test
    public void testDeleteSecurityGroup() throws FogbowException, HttpResponseException {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackCloudUtils.SECURITY_GROUPS 
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR 
                + securityGroupId;

        Mockito.doNothing().when(this.client).doDeleteRequest(Mockito.eq(endpoint), Mockito.eq(cloudUser));

        // exercise
        this.plugin.deleteSecurityGroup(securityGroupId, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupsEndpoint();
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doDeleteRequest(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));
    }
    
    // test case: When calling the deleteSecurityGroup method and an
    // unexpected error occurs, it must verify that the map method of the
    // OpenStackHttpToFogbowExceptionMapper class has been called.
    @Test
    public void testDeleteSecurityGroupFail() throws Exception {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackCloudUtils.SECURITY_GROUPS 
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR 
                + securityGroupId;

        HttpResponseException expectedException = new HttpResponseException(TestUtils.ERROR_STATUS_CODE,
                TestUtils.MESSAGE_STATUS_CODE);
        Mockito.doThrow(expectedException).when(this.client).doDeleteRequest(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, TestUtils.MAP_METHOD,
                Mockito.any());

        try {
            // exercise
            this.plugin.deleteSecurityGroup(securityGroupId, cloudUser);
            Assert.fail();
        } catch (Exception e) {
            // verify
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(expectedException));
        }
    }
    
    // test case: When calling the allowAllIngressSecurityRules method, it must verify
    // that the doPostRequestFromCloud method was called two times.
    @Test
    public void testAllowAllIngressSecurityRules() throws FogbowException {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        
        // exercise
        this.plugin.allowAllIngressSecurityRules(securityGroupId, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(RUN_TWICE)).doPostRequestFromCloud(Mockito.any(), Mockito.eq(cloudUser));
    }
    
    // test case: When calling the doPostRequestFromCloud method, it must verify
    // that the call was successful.
    public void testDoPostRequestFromCloud() throws Exception {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackCloudUtils.SECURITY_GROUP_RULES;

        CreateSecurityGroupRuleRequest request = new CreateSecurityGroupRuleRequest.Builder()
                .direction(OpenStackCloudUtils.INGRESS_DIRECTION)
                .etherType(OpenStackPublicIpPlugin.IPV4_ETHER_TYPE)
                .securityGroupId(securityGroupId)
                .build();

        // exercise
        this.plugin.doPostRequestFromCloud(request, cloudUser);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doPostRequest(Mockito.eq(endpoint),
                Mockito.eq(request.toJson()), Mockito.eq(cloudUser));
    }
    
    // test case: When calling the doPostRequestFromCloud method and an
    // unexpected error occurs, it must verify that the map method of the
    // OpenStackHttpToFogbowExceptionMapper class has been called.
    @Test
    public void testDoPostRequestFromCloudFail() throws Exception {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackCloudUtils.SECURITY_GROUP_RULES;

        CreateSecurityGroupRuleRequest request = new CreateSecurityGroupRuleRequest.Builder()
                .direction(OpenStackCloudUtils.INGRESS_DIRECTION)
                .etherType(OpenStackPublicIpPlugin.IPV4_ETHER_TYPE)
                .securityGroupId(securityGroupId)
                .build();
        
        HttpResponseException expectedException = new HttpResponseException(TestUtils.ERROR_STATUS_CODE,
                TestUtils.MESSAGE_STATUS_CODE);
        Mockito.doThrow(expectedException).when(this.client).doPostRequest(Mockito.eq(endpoint),
                Mockito.eq(request.toJson()), Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, TestUtils.MAP_METHOD,
                Mockito.any());

        try {
            // exercise
            this.plugin.doPostRequestFromCloud(request, cloudUser);
            Assert.fail();
        } catch (Exception e) {
            // verify
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(expectedException));
        }
    }
    
    // test case: When calling the doCreateSecurityGroup method, it must verify
    // that the call was successful.
    @Test
    public void testDoCreateSecurityGroup() throws Exception {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(instanceId);

        PowerMockito.mockStatic(OpenStackCloudUtils.class);
        PowerMockito.when(OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser))).thenCallRealMethod();

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackCloudUtils.SECURITY_GROUPS;

        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest.Builder()
                .name(FAKE_SECURITY_GROUP_NAME)
                .projectId(cloudUser.getProjectId())
                .build();

        String json = FAKE_CREATE_SECURITY_GROUP_FROM_JSON_RESPONSE;
        Mockito.when(this.client.doPostRequest(Mockito.eq(endpoint), Mockito.eq(request.toJson()), Mockito.eq(cloudUser)))
                .thenReturn(json);

        CreateSecurityGroupResponse response = CreateSecurityGroupResponse.fromJson(json);
        PowerMockito.mockStatic(CreateSecurityGroupResponse.class);
        PowerMockito.when(CreateSecurityGroupResponse.fromJson(Mockito.eq(json))).thenReturn(response);

        // exercise
        this.plugin.doCreateSecurityGroup(instanceId, cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenStackCloudUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getSecurityGroupName(Mockito.eq(instanceId));
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doPostRequest(Mockito.eq(endpoint),
                Mockito.eq(request.toJson()), Mockito.eq(cloudUser));

        PowerMockito.verifyStatic(CreateSecurityGroupResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        CreateSecurityGroupResponse.fromJson(Mockito.eq(json));
    }
    
    // test case: When calling the doCreateSecurityGroup method and an
    // unexpected error occurs, it must verify that the map method of the
    // OpenStackHttpToFogbowExceptionMapper class has been called.
    @Test
    public void testDoCreateSecurityGroupFail() throws Exception {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(instanceId);

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackCloudUtils.SECURITY_GROUPS;

        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest.Builder()
                .name(FAKE_SECURITY_GROUP_NAME)
                .projectId(cloudUser.getProjectId())
                .build();

        HttpResponseException expectedException = new HttpResponseException(TestUtils.ERROR_STATUS_CODE,
                TestUtils.MESSAGE_STATUS_CODE);
        Mockito.doThrow(expectedException).when(this.client).doPostRequest(Mockito.eq(endpoint),
                Mockito.eq(request.toJson()), Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, TestUtils.MAP_METHOD,
                Mockito.any());

        try {
            // exercise
            this.plugin.doCreateSecurityGroup(instanceId, cloudUser);
            Assert.fail();
        } catch (Exception e) {
            // verify
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(expectedException));
        }
    }
    
    // test case: When calling the doCreateSecurityGroupResponseFrom method with a
    // JSON malformed, it must verify that a UnexpectedException was throw.
    @Test
    public void testDoCreateSecurityGroupResponseFromJsonMalformed() throws FogbowException {
        // set up
        String json = TestUtils.JSON_MALFORMED;
        String expected = String.format(Messages.Error.ERROR_WHILE_CREATING_RESOURCE_S,
                OpenStackCloudUtils.SECURITY_GROUP_RESOURCE);
        try {
            // exercise
            this.plugin.doCreateSecurityGroupResponseFrom(json);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doRequestInstance method, it must verify
    // that the call was successful.
    @Test
    public void testDoRequestInstance() throws Exception {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();
        
        String endpoint = NEUTRON_PREFIX_ENDPOINT
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT 
                + OpenStackPublicIpPlugin.FLOATINGIPS;
        
        CreateFloatingIpRequest request = new CreateFloatingIpRequest.Builder()
                .floatingNetworkId(FAKE_FLOATING_NETWORK_ID)
                .portId(FAKE_NETWORK_PORT_ID)
                .projectId(TestUtils.FAKE_PROJECT_ID)
                .build();
        
        String json = FAKE_CREATE_FLOATING_IP_FROM_JSON_RESPONSE;
        Mockito.when(this.client.doPostRequest(Mockito.eq(endpoint), Mockito.eq(request.toJson()), Mockito.eq(cloudUser)))
                .thenReturn(json);

        PowerMockito.mockStatic(CreateFloatingIpResponse.class);
        PowerMockito.doCallRealMethod().when(CreateFloatingIpResponse.class, TestUtils.FROM_JSON_METHOD, Mockito.eq(json));

        // exercise
        this.plugin.doRequestInstance(request, cloudUser);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doPostRequest(Mockito.eq(endpoint),
                Mockito.eq(request.toJson()), Mockito.eq(cloudUser));

        PowerMockito.verifyStatic(CreateFloatingIpResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        CreateFloatingIpResponse.fromJson(Mockito.eq(json));
    }
    
    // test case: When calling the doRequestInstance method and an
    // unexpected error occurs, it must verify that the map method of the
    // OpenStackHttpToFogbowExceptionMapper class has been called.
    @Test
    public void testDoRequestInstanceFail() throws Exception {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackPublicIpPlugin.FLOATINGIPS;

        CreateFloatingIpRequest request = new CreateFloatingIpRequest.Builder()
                .floatingNetworkId(FAKE_FLOATING_NETWORK_ID)
                .portId(FAKE_NETWORK_PORT_ID)
                .projectId(TestUtils.FAKE_PROJECT_ID)
                .build();

        HttpResponseException expectedException = new HttpResponseException(TestUtils.ERROR_STATUS_CODE,
                TestUtils.MESSAGE_STATUS_CODE);
        Mockito.doThrow(expectedException).when(this.client).doPostRequest(Mockito.eq(endpoint),
                Mockito.eq(request.toJson()), Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, TestUtils.MAP_METHOD,
                Mockito.any());

        try {
            // exercise
            this.plugin.doRequestInstance(request, cloudUser);
            Assert.fail();
        } catch (Exception e) {
            // verify
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(expectedException));
        }
    }
    
    // test case: When calling the doCreateFloatingIpResponseFrom method with a
    // JSON malformed, it must verify that a UnexpectedException was thrown.
    @Test
    public void testDoCreateFloatingIpResponseFromJsonMalformed() throws FogbowException {
        // set up
        String json = TestUtils.JSON_MALFORMED;
        String expected = String.format(Messages.Error.ERROR_WHILE_CREATING_RESOURCE_S,
                OpenStackPublicIpPlugin.PUBLIC_IP_RESOURCE);
        try {
            // exercise
            this.plugin.doCreateFloatingIpResponseFrom(json);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getNetworkPortId method, it must verify
    // that the call was successful.
    @Test
    public void testGetNetworkPortId() throws Exception {
        // set up
        String computeId = TestUtils.FAKE_COMPUTE_ID;
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        computeOrder.setInstanceId(computeId);
        SharedOrderHolders.getInstance().getActiveOrdersMap().put(computeOrder.getId(), computeOrder);

        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(computeOrder.getId());
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();

        String endpointBase = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackPublicIpPlugin.PORTS;

        String defaultNetworkId = TestUtils.FAKE_NETWORK_ID;
        String endpoint = endpointBase + FAKE_NETWORK_PORTS_SUFIX_ENDPOINT;
        Mockito.doReturn(endpoint).when(this.plugin).buildNetworkPortsEndpoint(Mockito.eq(computeId),
                Mockito.eq(defaultNetworkId), Mockito.eq(endpointBase));

        String json = FAKE_GET_PORTS_FROM_JSON_RESPONSE;
        Mockito.doReturn(json).when(this.plugin).doGetResponseFromCloud(Mockito.anyString(), Mockito.eq(cloudUser));

        GetNetworkPortsResponse response = GetNetworkPortsResponse.fromJson(json);
        PowerMockito.mockStatic(GetNetworkPortsResponse.class);
        PowerMockito.when(GetNetworkPortsResponse.fromJson(Mockito.eq(json))).thenReturn(response);

        String expected = FAKE_NETWORK_PORT_ID;

        // exercise
        String networkPortId = this.plugin.getNetworkPortId(order, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getDefaultNetworkId();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getNetworkPortsEndpoint();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildNetworkPortsEndpoint(
                Mockito.eq(order.getComputeId()), Mockito.eq(defaultNetworkId), Mockito.eq(endpointBase));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetResponseFromCloud(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));

        PowerMockito.verifyStatic(GetNetworkPortsResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        GetNetworkPortsResponse.fromJson(Mockito.eq(json));

        Assert.assertEquals(expected, networkPortId);
    }
    
    // test case: When calling the getNetworkPortId method with a non-existent ID,
    // it must return to an empty network ports and throw an UnexpectedException.
    @Test
    public void testGetNetworkPortIdFail() throws Exception {
        // set up
        String computeId = TestUtils.FAKE_COMPUTE_ID;
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        computeOrder.setInstanceId(computeId);
        SharedOrderHolders.getInstance().getActiveOrdersMap().put(computeOrder.getId(), computeOrder);

        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(computeOrder.getId());
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();

        String endpointBase = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackPublicIpPlugin.PORTS;

        String defaultNetworkId = TestUtils.FAKE_NETWORK_ID;
        String endpoint = endpointBase + FAKE_NETWORK_PORTS_SUFIX_ENDPOINT;
        Mockito.doReturn(endpoint).when(this.plugin).buildNetworkPortsEndpoint(Mockito.eq(computeId),
                Mockito.eq(defaultNetworkId), Mockito.eq(endpointBase));

        String json = EMPTY_PORTS_FROM_JSON_RESPONSE;
        Mockito.doReturn(json).when(this.plugin).doGetResponseFromCloud(Mockito.anyString(), Mockito.eq(cloudUser));

        GetNetworkPortsResponse response = GetNetworkPortsResponse.fromJson(json);
        PowerMockito.mockStatic(GetNetworkPortsResponse.class);
        PowerMockito.when(GetNetworkPortsResponse.fromJson(Mockito.eq(json))).thenReturn(response);

        String expected = String.format(Messages.Exception.PORT_NOT_FOUND, computeId, defaultNetworkId);

        try {
            // exercise
            this.plugin.getNetworkPortId(order, cloudUser);
            Assert.fail();
        } catch (Exception e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doGetNetworkPortsResponseFrom method with a
    // JSON malformed, it must verify that a UnexpectedException was throw.
    @Test
    public void testDoGetNetworkPortsResponseFromJsonMalformed() throws FogbowException {
        // set up
        String json = TestUtils.JSON_MALFORMED;
        String expected = String.format(Messages.Error.ERROR_WHILE_GETTING_RESOURCE_S_FROM_CLOUD,
                OpenStackCloudUtils.NETWORK_PORTS_RESOURCE);
        try {
            // exercise
            this.plugin.doGetNetworkPortsResponseFrom(json);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doGetResponseFromCloud method, it must verify
    // that the call was successful.
    @Test
    public void testDoGetResponseFromCloud() throws Exception {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackPublicIpPlugin.FLOATINGIPS 
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR
                + TestUtils.FAKE_INSTANCE_ID;

        // exercise
        this.plugin.doGetResponseFromCloud(endpoint, cloudUser);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));
    }
    
    // test case: When calling the doGetResponseFromCloud method and an unexpected
    // error occurs, it must verify that the map method of the
    // OpenStackHttpToFogbowExceptionMapper class has been called.
    @Test
    public void testDoGetResponseFromCloudFail() throws Exception {
        // set up
        OpenStackV3User cloudUser = this.testUtils.createOpenStackUser();

        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackPublicIpPlugin.FLOATINGIPS 
                + OpenStackCloudUtils.ENDPOINT_SEPARATOR
                + TestUtils.FAKE_INSTANCE_ID;

        HttpResponseException expectedException = new HttpResponseException(TestUtils.ERROR_STATUS_CODE,
                TestUtils.MESSAGE_STATUS_CODE);
        Mockito.doThrow(expectedException).when(this.client).doGetRequest(Mockito.eq(endpoint), Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, TestUtils.MAP_METHOD,
                Mockito.any());

        try {
            // exercise
            this.plugin.doGetResponseFromCloud(endpoint, cloudUser);
            Assert.fail();
        } catch (Exception e) {
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(expectedException));
        }
    }
    
    // test case: When calling the buildNetworkPortsEndpoint method with consistent
    // parameters, it must compose a valid URL.
    @Test
    public void testBuildNetworkPortsEndpoint() throws Exception {
        // set up
        String deviceId = TestUtils.FAKE_COMPUTE_ID;
        String networkId = TestUtils.FAKE_NETWORK_ID;
        String endpoint = NEUTRON_PREFIX_ENDPOINT 
                + OpenStackCloudUtils.NETWORK_V2_API_ENDPOINT
                + OpenStackPublicIpPlugin.PORTS;

        String expected = endpoint + FAKE_NETWORK_PORTS_SUFIX_ENDPOINT;

        // exercise
        String url = this.plugin.buildNetworkPortsEndpoint(deviceId, networkId, endpoint);

        // verify
        Assert.assertEquals(expected, url);
    }
    
    // test case: When calling the buildNetworkPortsEndpoint method with inconsistent
    // parameters, it must throw a InvalidParameterException.
    @Test
    public void testBuildNetworkPortsEndpointFail() throws Exception {
        // set up
        String deviceId = TestUtils.FAKE_COMPUTE_ID;
        String networkId = TestUtils.FAKE_NETWORK_ID;
        String endpoint = TestUtils.JSON_MALFORMED;

        String expected = String.format(Messages.Exception.WRONG_URI_SYNTAX, endpoint);

        try {
            // exercise
            this.plugin.buildNetworkPortsEndpoint(deviceId, networkId, endpoint);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the checkProperties method with inconsistent default
    // network ID from the configuration file, it must throw a FatalErrorException.
    @Test
    public void testCheckPropertiesWithoutDefaultNetworkId() {
        // set up
        String[] defaultNetworkIds = { null, TestUtils.EMPTY_STRING };
        String expected = Messages.Fatal.DEFAULT_NETWORK_NOT_FOUND;

        for (String defaultNetworkId : defaultNetworkIds) {
            Mockito.doReturn(defaultNetworkId).when(this.plugin).getDefaultNetworkId();

            try {
                // exercise
                this.plugin.checkProperties();
                Assert.fail();

            } catch (FatalErrorException e) {
                // verify
                Assert.assertEquals(expected, e.getMessage());
            }
        }
    }
    
    // test case: When calling the checkProperties method with inconsistent external
    // network ID from the configuration file, it must throw a FatalErrorException.
    @Test
    public void testCheckPropertiesWithoutExternalNetworkId() {
        // set up
        String[] defaultNetworkIds = { null, TestUtils.EMPTY_STRING };
        String expected = Messages.Fatal.EXTERNAL_NETWORK_NOT_FOUND;

        for (String defaultNetworkId : defaultNetworkIds) {
            Mockito.doReturn(defaultNetworkId).when(this.plugin).getExternalNetworkId();

            try {
                // exercise
                this.plugin.checkProperties();
                Assert.fail();

            } catch (FatalErrorException e) {
                // verify
                Assert.assertEquals(expected, e.getMessage());
            }
        }
    }

    // test case: When calling the checkProperties method with inconsistent prefix
    // end-point from the configuration file, it must throw a FatalErrorException.
    @Test
    public void testCheckPropertiesWithoutgetNeutronPrefixEndpoint() {
        // set up
        String[] defaultNetworkIds = { null, TestUtils.EMPTY_STRING };
        String expected = Messages.Fatal.NEUTRON_ENDPOINT_NOT_FOUND;

        for (String defaultNetworkId : defaultNetworkIds) {
            Mockito.doReturn(defaultNetworkId).when(this.plugin).getNeutronPrefixEndpoint();

            try {
                // exercise
                this.plugin.checkProperties();
                Assert.fail();

            } catch (FatalErrorException e) {
                // verify
                Assert.assertEquals(expected, e.getMessage());
            }
        }
    }
    
    private PublicIpInstance createPublicIpInstance() {
        String id = TestUtils.FAKE_INSTANCE_ID;
        String cloudState = OpenStackStateMapper.ACTIVE_STATUS;
        String ip = TestUtils.FAKE_ADDRESS;
        return new PublicIpInstance(id, cloudState, ip);
    }
    
}
