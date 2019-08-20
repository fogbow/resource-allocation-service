package cloud.fogbow.ras.core;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.RemoteCommunicationException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteGetCloudNamesRequest;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.authorization.DefaultAuthorizationPlugin;

@PrepareForTest({ CloudConnectorFactory.class, DatabaseManager.class, ServiceAsymmetricKeysHolder.class, })
public class ApplicationFacadeTest extends BaseUnitTests {

    private static final String ANY_VALUE = "anything";
	private static final String BUILD_NUMBER_FORMAT = "%s-abcd";
	private static final String BUILD_NUMBER_FORMAT_FOR_TESTING = "%s-[testing mode]";
	private static final String EMPTY_STRING = "";
	private static final String SYSTEM_USER_TOKEN_VALUE = "system-user-token-value";
	private static final String VALID_PATH_CONF = "ras.conf";
	private static final String VALID_PATH_CONF_WITHOUT_BUILD_PROPERTY = "ras-without-build-number.conf";
    
	private ApplicationFacade facade;
	private OrderController orderController;
	private LocalCloudConnector localCloudConnector;
	private AuthorizationPlugin authorizationPlugin;
	private CloudListController cloudListController;
    private SecurityRuleController securityRuleController;

	@Before
	public void setUp() throws FogbowException {
		this.testUtils.mockReadOrdersFromDataBase();
		this.localCloudConnector = this.testUtils.mockLocalCloudConnectorFromFactory();
		this.orderController = Mockito.spy(new OrderController());
		this.authorizationPlugin = mockAuthorizationPlugin();
		this.cloudListController = Mockito.spy(new CloudListController());
		this.securityRuleController = Mockito.spy(new SecurityRuleController());
		this.facade = Mockito.spy(ApplicationFacade.getInstance());
		this.facade.setOrderController(this.orderController);
		this.facade.setAuthorizationPlugin(this.authorizationPlugin);
		this.facade.setCloudListController(this.cloudListController);
		this.facade.setSecurityRuleController(this.securityRuleController);
	}

    // test case: When calling the setBuildNumber method, it must generate correct
    // build number version.
    @Test
    public void testVersion() throws Exception {
        // set up
        this.facade.setBuildNumber(HomeDir.getPath() + VALID_PATH_CONF);
        String expected = String.format(BUILD_NUMBER_FORMAT, SystemConstants.API_VERSION_NUMBER);

        // exercise
        String build = this.facade.getVersionNumber();

        // verify
        Assert.assertEquals(expected, build);
    }

    // test case: When calling the setBuildNumber method without a build property
    // valid in the configuration file, it must generate a testing mode build number
    // version.
    @Test
    public void testVersionWithoutBuildProperty() throws Exception {
        // set up
        this.facade.setBuildNumber(HomeDir.getPath() + VALID_PATH_CONF_WITHOUT_BUILD_PROPERTY);
        String expected = String.format(BUILD_NUMBER_FORMAT_FOR_TESTING, SystemConstants.API_VERSION_NUMBER);

        // exercise
        String build = this.facade.getVersionNumber();

        // verify
        Assert.assertEquals(expected, build);
    }
	
    // test case: When calling the getPublicKey method with a null Public Key File
    // Path, it must throw an UnauthorizedRequestException.
    @Test(expected = UnexpectedException.class) // verify
    public void testGetPublicKeyThrowsUnexpectedException() throws Exception {
        // set up
        ServiceAsymmetricKeysHolder sakHolder = mockServiceAsymmetricKeysHolder();
        sakHolder.setPublicKeyFilePath(null);

        // exercise
        this.facade.getPublicKey();
    }

    // test case: When calling the getCloudNames method with a local member, it must
    // verify that this call was successful.
    @Test
    public void testGetCloudNamesWithLocalMember() throws FogbowException {
        // set up
        String localMember = TestUtils.LOCAL_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        RasOperation expectedOperation = new RasOperation(Operation.GET, ResourceType.CLOUD_NAMES);

        // exercise
        this.facade.getCloudNames(localMember, userToken);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.cloudListController, Mockito.times(TestUtils.RUN_ONCE)).getCloudNames();
    }
    
    // test case: When calling the getCloudNames method with a remote member, it must
    // verify that this call was successful.
    @Test
    public void testGetCloudNamesWithRemoteMember() throws Exception {
        // set up
        String remoteMember = TestUtils.FAKE_REMOTE_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));

        RemoteGetCloudNamesRequest cloudNamesRequest = Mockito.mock(RemoteGetCloudNamesRequest.class);
        Mockito.doReturn(cloudNamesRequest).when(this.facade).getCloudNamesFromRemoteRequest(Mockito.eq(remoteMember),
                Mockito.eq(systemUser));
        
        RasOperation expectedOperation = new RasOperation(Operation.GET, ResourceType.CLOUD_NAMES);

        // exercise
        this.facade.getCloudNames(remoteMember, userToken);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(cloudNamesRequest, Mockito.times(TestUtils.RUN_ONCE)).send();
    }
    
    // test case: When calling the getCloudNames method without set a remote
    // communication, it must throw a RemoteCommunicationException.
    @Test(expected = RemoteCommunicationException.class) // verify
    public void testGetCloudNamesWithRemoteMemberThrowsAnException() throws FogbowException {
        // set up
        String remoteMember = TestUtils.FAKE_REMOTE_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));

        // exercise
        this.facade.getCloudNames(remoteMember, userToken);
    }
    
    // test case: When calling the createCompute method with null user data, it must
    // set an empty list in the userData field and check that activateOrder method
    // was called.
    @Test
    public void testCreateComputeOrderWithUserDataNull() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        ComputeOrder order = this.testUtils.createLocalComputeOrder();
        order.setUserData(null);
        
        Mockito.doReturn(TestUtils.FAKE_INSTANCE_ID).when(this.facade).activateOrder(Mockito.eq(order),
                Mockito.eq(userToken));
        
        // exercise
        this.facade.createCompute(order, userToken);

        // verify
        Assert.assertNotNull(order.getUserData());
        Assert.assertTrue(order.getUserData().isEmpty());

        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).activateOrder(Mockito.eq(order),
                Mockito.eq(userToken));
    }
    
    // test case: When calling the createCompute method with very long user data
    // file content, it must throw an InvalidParameterException.
    @Test
    public void testCreateComputeOrderWithLengthUserDataFileContentExceeded() throws FogbowException {
        // set up
        ComputeOrder order = this.testUtils.createLocalComputeOrder();
        order.setUserData(generateVeryLongUserDataFileContent());

        String userToken = SYSTEM_USER_TOKEN_VALUE;
        String expected = Messages.Exception.TOO_BIG_USER_DATA_FILE_CONTENT;

        // exercise
        try {
            this.facade.createCompute(order, userToken);
            Assert.fail();

        } catch (InvalidParameterException e) {
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getCompute method it must check that
    // getResourceInstance method was called.
    @Test
    public void testGetCompute() throws FogbowException {
        // set up
        String orderId = TestUtils.FAKE_ORDER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        ComputeInstance instance = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(instance).when(this.facade).getResourceInstance(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.COMPUTE));

        // exercise
        this.facade.getCompute(orderId, userToken);

        // verify
        Mockito.verify(this.facade).getResourceInstance(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.COMPUTE));
    }
    
    // test case: When calling the deleteCompute method it must check that
    // deleteOrder method was called.
    @Test
    public void testDeleteCompute() throws FogbowException {
        // set up
        String orderId = TestUtils.FAKE_ORDER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        Mockito.doNothing().when(this.facade).deleteOrder(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.COMPUTE));

        // exercise
        this.facade.deleteCompute(orderId, userToken);

        // verify
        Mockito.verify(this.facade).deleteOrder(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.COMPUTE));
    }
    
    // test case: When calling the getComputeAllocation method it must check that
    // getUserAllocation method was called.
    @Test
    public void testGetComputeAllocation() throws FogbowException {
        // set up
        String providerId = TestUtils.LOCAL_MEMBER_ID;
        String cloudName = TestUtils.DEFAULT_CLOUD_NAME;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        ComputeAllocation computeAllocation = Mockito.mock(ComputeAllocation.class);
        Mockito.doReturn(computeAllocation).when(this.facade).getUserAllocation(Mockito.eq(providerId),
                Mockito.eq(cloudName), Mockito.eq(userToken), Mockito.eq(ResourceType.COMPUTE));

        // exercise
        this.facade.getComputeAllocation(providerId, cloudName, userToken);

        // verify
        Mockito.verify(this.facade).getUserAllocation(Mockito.eq(providerId), Mockito.eq(cloudName),
                Mockito.eq(userToken), Mockito.eq(ResourceType.COMPUTE));
    }
    
    // test case: When calling the getComputeQuota method it must check that
    // getUserQuota method was called.
    @Test
    public void testGetComputeQuota() throws FogbowException {
        // set up
        String providerId = TestUtils.LOCAL_MEMBER_ID;
        String cloudName = TestUtils.DEFAULT_CLOUD_NAME;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        ComputeQuota computeQuota = Mockito.mock(ComputeQuota.class);
        Mockito.doReturn(computeQuota).when(this.facade).getUserQuota(Mockito.eq(providerId), Mockito.eq(cloudName),
                Mockito.eq(userToken), Mockito.eq(ResourceType.COMPUTE));

        // exercise
        this.facade.getComputeQuota(providerId, cloudName, userToken);

        // verify
        Mockito.verify(this.facade).getUserQuota(Mockito.eq(providerId), Mockito.eq(cloudName), Mockito.eq(userToken),
                Mockito.eq(ResourceType.COMPUTE));
    }
    
    // test case: When calling the createVolume method it must check that
    // activateOrder method was called.
    @Test
    public void testCreateVolume() throws FogbowException {
        // set up
        VolumeOrder order = this.testUtils.createLocalVolumeOrder();
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        Mockito.doReturn(TestUtils.FAKE_INSTANCE_ID).when(this.facade).activateOrder(Mockito.eq(order),
                Mockito.eq(userToken));

        // exercise
        this.facade.createVolume(order, userToken);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).activateOrder(Mockito.eq(order),
                Mockito.eq(userToken));
    }
    
    // test case: When calling the getVolume method it must check that
    // getResourceInstance method was called.
    @Test
    public void testGetVolume() throws FogbowException {
        // set up
        String orderId = TestUtils.FAKE_ORDER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        VolumeInstance instance = Mockito.mock(VolumeInstance.class);
        Mockito.doReturn(instance).when(this.facade).getResourceInstance(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.VOLUME));

        // exercise
        this.facade.getVolume(orderId, userToken);

        // verify
        Mockito.verify(this.facade).getResourceInstance(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.VOLUME));
    }
    
    // test case: When calling the deleteVolume method it must check that
    // deleteOrder method was called.
    @Test
    public void testDeleteVolume() throws FogbowException {
        // set up
        String orderId = TestUtils.FAKE_ORDER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        Mockito.doNothing().when(this.facade).deleteOrder(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.VOLUME));

        // exercise
        this.facade.deleteVolume(orderId, userToken);

        // verify
        Mockito.verify(this.facade).deleteOrder(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.VOLUME));
    }
    
    // test case: When calling the createNetwork method it must check that
    // activateOrder method was called.
    @Test
    public void testCreateNetwork() throws FogbowException {
        // set up
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        Mockito.doReturn(TestUtils.FAKE_INSTANCE_ID).when(this.facade).activateOrder(Mockito.eq(order),
                Mockito.eq(userToken));

        // exercise
        this.facade.createNetwork(order, userToken);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).activateOrder(Mockito.eq(order),
                Mockito.eq(userToken));
    }
    
    // test case: When calling the getNetwork method it must check that
    // getResourceInstance method was called.
    @Test
    public void testGetNetwork() throws FogbowException {
        // set up
        String orderId = TestUtils.FAKE_ORDER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        NetworkInstance instance = Mockito.mock(NetworkInstance.class);
        Mockito.doReturn(instance).when(this.facade).getResourceInstance(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.NETWORK));

        // exercise
        this.facade.getNetwork(orderId, userToken);

        // verify
        Mockito.verify(this.facade).getResourceInstance(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.NETWORK));
    }
    
    // test case: When calling the deleteNetwork method it must check that
    // deleteOrder method was called.
    @Test
    public void testDeleteNetwork() throws FogbowException {
        // set up
        String orderId = TestUtils.FAKE_ORDER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        Mockito.doNothing().when(this.facade).deleteOrder(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.NETWORK));

        // exercise
        this.facade.deleteNetwork(orderId, userToken);

        // verify
        Mockito.verify(this.facade).deleteOrder(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.NETWORK));
    }
    
    // test case: When calling the createAttachment method it must check that
    // activateOrder method was called.
    @Test
    public void testCreateAttachment() throws FogbowException {
        // set up
        AttachmentOrder order = this.testUtils.createLocalAttachmentOrder(this.testUtils.createLocalComputeOrder(),
                this.testUtils.createLocalVolumeOrder());
        
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        Mockito.doReturn(TestUtils.FAKE_INSTANCE_ID).when(this.facade).activateOrder(Mockito.eq(order),
                Mockito.eq(userToken));

        // exercise
        this.facade.createAttachment(order, userToken);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).activateOrder(Mockito.eq(order),
                Mockito.eq(userToken));
    }
    
    // test case: When calling the getAttachment method it must check that
    // getResourceInstance method was called.
    @Test
    public void testGetAttachment() throws FogbowException {
        // set up
        String orderId = TestUtils.FAKE_ORDER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        AttachmentInstance instance = Mockito.mock(AttachmentInstance.class);
        Mockito.doReturn(instance).when(this.facade).getResourceInstance(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.ATTACHMENT));

        // exercise
        this.facade.getAttachment(orderId, userToken);

        // verify
        Mockito.verify(this.facade).getResourceInstance(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.ATTACHMENT));
    }
    
    // test case: When calling the deleteAttachment method it must check that
    // deleteOrder method was called.
    @Test
    public void testDeleteAttachment() throws FogbowException {
        // set up
        String orderId = TestUtils.FAKE_ORDER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        Mockito.doNothing().when(this.facade).deleteOrder(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.ATTACHMENT));

        // exercise
        this.facade.deleteAttachment(orderId, userToken);

        // verify
        Mockito.verify(this.facade).deleteOrder(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.ATTACHMENT));
    }
    
    // test case: When calling the createPublicIp method it must check that
    // activateOrder method was called.
    @Test
    public void testCreatePublicIp() throws FogbowException {
        // set up
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        Mockito.doReturn(TestUtils.FAKE_INSTANCE_ID).when(this.facade).activateOrder(Mockito.eq(order),
                Mockito.eq(userToken));

        // exercise
        this.facade.createPublicIp(order, userToken);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).activateOrder(Mockito.eq(order),
                Mockito.eq(userToken));
    }
    
    // test case: When calling the getPublicIp method it must check that
    // getResourceInstance method was called.
    @Test
    public void testGetPublicIp() throws FogbowException {
        // set up
        String orderId = TestUtils.FAKE_ORDER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        PublicIpInstance instance = Mockito.mock(PublicIpInstance.class);
        Mockito.doReturn(instance).when(this.facade).getResourceInstance(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.PUBLIC_IP));

        // exercise
        this.facade.getPublicIp(orderId, userToken);

        // verify
        Mockito.verify(this.facade).getResourceInstance(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.PUBLIC_IP));
    }
    
    // test case: When calling the deletePublicIp method it must check that
    // deleteOrder method was called.
    @Test
    public void testDeletePublicIp() throws FogbowException {
        // set up
        String orderId = TestUtils.FAKE_ORDER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        Mockito.doNothing().when(this.facade).deleteOrder(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.PUBLIC_IP));

        // exercise
        this.facade.deletePublicIp(orderId, userToken);

        // verify
        Mockito.verify(this.facade).deleteOrder(Mockito.eq(orderId), Mockito.eq(userToken),
                Mockito.eq(ResourceType.PUBLIC_IP));
    }
    
    // test case: When calling the getAllInstancesStatus method from a COMPUTE
    // resource type, it must check that this call was successful for this resource
    // type.
    @Test
    public void testGetAllInstancesStatusForComputeResourceType() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        ResourceType resourceType = ResourceType.COMPUTE;
        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, resourceType);

        // exercise
        this.facade.getAllInstancesStatus(userToken, resourceType);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE))
                .getInstancesStatus(Mockito.eq(systemUser), Mockito.eq(resourceType));
    }
    
    // test case: When calling the getAllInstancesStatus method from a Volume
    // resource type, it must check that this call was successful for this resource
    // type.
    @Test
    public void testGetAllInstancesStatusForVolumeResourceType() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        ResourceType resourceType = ResourceType.VOLUME;
        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, resourceType);

        // exercise
        this.facade.getAllInstancesStatus(userToken, resourceType);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE))
                .getInstancesStatus(Mockito.eq(systemUser), Mockito.eq(resourceType));
    }
    
    // test case: When calling the getAllInstancesStatus method from a ATTACHMENT
    // resource type, it must check that this call was successful for this resource
    // type.
    @Test
    public void testGetAllInstancesStatusForAttachmentResourceType() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        ResourceType resourceType = ResourceType.ATTACHMENT;
        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, resourceType);

        // exercise
        this.facade.getAllInstancesStatus(userToken, resourceType);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE))
                .getInstancesStatus(Mockito.eq(systemUser), Mockito.eq(resourceType));
    }
    
    // test case: When calling the getAllInstancesStatus method from a NETWORK
    // resource type, it must check that this call was successful for this resource
    // type.
    @Test
    public void testGetAllInstancesStatusForNetworkResourceType() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        ResourceType resourceType = ResourceType.NETWORK;
        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, resourceType);

        // exercise
        this.facade.getAllInstancesStatus(userToken, resourceType);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE))
                .getInstancesStatus(Mockito.eq(systemUser), Mockito.eq(resourceType));
    }
    
    // test case: When calling the getAllInstancesStatus method from a PUBLIC_IP
    // resource type, it must check that this call was successful for this resource
    // type.
    @Test
    public void testGetAllInstancesStatusForPublicIpResourceType() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        ResourceType resourceType = ResourceType.PUBLIC_IP;
        
        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, resourceType);

        // exercise
        this.facade.getAllInstancesStatus(userToken, resourceType);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE))
                .getInstancesStatus(Mockito.eq(systemUser), Mockito.eq(resourceType));
    }
    
    // test case: When calling the getAllImages method even with a null provider ID
    // and an empty cloud name, it must verify that this call was successful.
    @Test
    public void testGetAllImagesWithNullProviderIdAndEmptyCloudName() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        String providerId = null;
        String cloudName = EMPTY_STRING;

        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, ResourceType.IMAGE,
                TestUtils.DEFAULT_CLOUD_NAME);

        // exercise
        this.facade.getAllImages(providerId, cloudName, userToken);

        // verify
        Mockito.verify(this.cloudListController, Mockito.times(TestUtils.RUN_ONCE)).getDefaultCloudName();
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .getAllImages(Mockito.eq(systemUser));
    }
    
    // test case: When calling the getImage method even with a null provider ID
    // and an empty cloud name, it must verify that this call was successful.
    @Test
    public void testGetImageWithNullProviderIdAndEmptyCloudName() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        String providerId = null;
        String cloudName = EMPTY_STRING;
        String imageId = TestUtils.FAKE_IMAGE_ID;
        
        RasOperation expectedOperation = new RasOperation(Operation.GET, ResourceType.IMAGE,
                TestUtils.DEFAULT_CLOUD_NAME);

        // exercise
        this.facade.getImage(providerId, cloudName, imageId, userToken);

        // verify
        Mockito.verify(this.cloudListController, Mockito.times(TestUtils.RUN_ONCE)).getDefaultCloudName();
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).getImage(Mockito.eq(imageId),
                Mockito.eq(systemUser));
    }
    
    // test case: When calling the createSecurityRule method with resource type
    // different from order, it must throws an InstanceNotFoundException;
    @Test
    public void testCreateSecurityRuleWithResourceTypeDifferentFromOrder() throws FogbowException {
        // set up
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        this.orderController.activateOrder(order);

        ResourceType resourceType = ResourceType.PUBLIC_IP;
        SecurityRule securityRule = Mockito.mock(SecurityRule.class);
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        String expected = String.format(Messages.Exception.RESOURCE_TYPE_NOT_COMPATIBLE_S, resourceType);

        try {
            // exercise
            this.facade.createSecurityRule(order.getId(), securityRule, userToken, resourceType);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the createSecurityRule method with the same resource
    // type form a network order, it must verify that this call was successful.
    @Test
    public void testCreateSecurityRuleWithSameResourceTypeFromNetworkOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        this.orderController.activateOrder(order);

        SecurityRule securityRule = Mockito.mock(SecurityRule.class);

        RasOperation expectedOperation = new RasOperation(Operation.CREATE, ResourceType.SECURITY_RULE,
                TestUtils.DEFAULT_CLOUD_NAME, order);

        // exercise
        this.facade.createSecurityRule(order.getId(), securityRule, userToken, ResourceType.NETWORK);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.securityRuleController, Mockito.times(TestUtils.RUN_ONCE))
                .createSecurityRule(Mockito.eq(order), Mockito.eq(securityRule), Mockito.eq(systemUser));
    }
    
    // test case: When calling the createSecurityRule method with the same resource
    // type form a public IP order, it must verify that this call was successful.
    @Test
    public void testCreateSecurityRuleWithSameResourceTypeFromPublicIpOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        this.orderController.activateOrder(order);

        SecurityRule securityRule = Mockito.mock(SecurityRule.class);

        RasOperation expectedOperation = new RasOperation(Operation.CREATE, ResourceType.SECURITY_RULE,
                TestUtils.DEFAULT_CLOUD_NAME, order);

        // exercise
        this.facade.createSecurityRule(order.getId(), securityRule, userToken, ResourceType.PUBLIC_IP);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.securityRuleController, Mockito.times(TestUtils.RUN_ONCE))
                .createSecurityRule(Mockito.eq(order), Mockito.eq(securityRule), Mockito.eq(systemUser));
    }
    
    // test case: When calling the getAllSecurityRules method with resource type
    // different from order, it must throws an InstanceNotFoundException;
    @Test
    public void testGetAllSecurityRulesWithResourceTypeDifferentFromOrder() throws FogbowException {
     // set up
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        this.orderController.activateOrder(order);

        ResourceType resourceType = ResourceType.PUBLIC_IP;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        String expected = String.format(Messages.Exception.RESOURCE_TYPE_NOT_COMPATIBLE_S, resourceType);

        try {
            // exercise
            this.facade.getAllSecurityRules(order.getId(), userToken, resourceType);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the getAllSecurityRules method with the same resource
    // type form a network order, it must verify that this call was successful.
    @Test
    public void testGetAllSecurityRulesWithSameResourceTypeFromNetworkOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        this.orderController.activateOrder(order);

        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, ResourceType.SECURITY_RULE,
                TestUtils.DEFAULT_CLOUD_NAME, order);

        // exercise
        this.facade.getAllSecurityRules(order.getId(), userToken, ResourceType.NETWORK);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.securityRuleController, Mockito.times(TestUtils.RUN_ONCE))
                .getAllSecurityRules(Mockito.eq(order), Mockito.eq(systemUser));
    }
    
    // test case: When calling the getAllSecurityRules method with the same resource
    // type form a public IP order, it must verify that this call was successful.
    @Test
    public void testGetAllSecurityRulesWithSameResourceTypeFromPublicIpOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        this.orderController.activateOrder(order);

        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, ResourceType.SECURITY_RULE,
                TestUtils.DEFAULT_CLOUD_NAME, order);

        // exercise
        this.facade.getAllSecurityRules(order.getId(), userToken, ResourceType.PUBLIC_IP);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.securityRuleController, Mockito.times(TestUtils.RUN_ONCE))
                .getAllSecurityRules(Mockito.eq(order), Mockito.eq(systemUser));
    }
    
    // test case: When calling the deleteSecurityRule method with resource type
    // different from order, it must throws an InstanceNotFoundException;
    @Test
    public void testDeleteSecurityRuleWithResourceTypeDifferentFromOrder() throws FogbowException {
        // set up
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        this.orderController.activateOrder(order);

        ResourceType resourceType = ResourceType.PUBLIC_IP;
        String securityRuleId = TestUtils.FAKE_SECURITY_RULE_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        String expected = String.format(Messages.Exception.RESOURCE_TYPE_NOT_COMPATIBLE_S, resourceType);

        try {
            // exercise
            this.facade.deleteSecurityRule(order.getId(), securityRuleId, userToken, resourceType);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the deleteSecurityRule method with the same resource
    // type form a network order, it must verify that this call was successful.
    @Test
    public void testDeleteSecurityRuleWithSameResourceTypeFromNetworkOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        this.orderController.activateOrder(order);

        String cloudName = TestUtils.DEFAULT_CLOUD_NAME;
        RasOperation expectedOperation = new RasOperation(Operation.DELETE, ResourceType.SECURITY_RULE, cloudName,
                order);

        String securityRuleId = TestUtils.FAKE_SECURITY_RULE_ID;

        // exercise
        this.facade.deleteSecurityRule(order.getId(), securityRuleId, userToken, ResourceType.NETWORK);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.securityRuleController, Mockito.times(TestUtils.RUN_ONCE)).deleteSecurityRule(
                Mockito.eq(order.getProvider()), Mockito.eq(cloudName), Mockito.eq(securityRuleId),
                Mockito.eq(systemUser));
    }
    
    // test case: When calling the deleteSecurityRule method with the same resource
    // type form a public IP order, it must verify that this call was successful.
    @Test
    public void testDeleteSecurityRuleWithSameResourceTypeFromPublicIpOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        this.orderController.activateOrder(order);

        String cloudName = TestUtils.DEFAULT_CLOUD_NAME;
        RasOperation expectedOperation = new RasOperation(Operation.DELETE, ResourceType.SECURITY_RULE, cloudName,
                order);

        String securityRuleId = TestUtils.FAKE_SECURITY_RULE_ID;

        // exercise
        this.facade.deleteSecurityRule(order.getId(), securityRuleId, userToken, ResourceType.PUBLIC_IP);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.securityRuleController, Mockito.times(TestUtils.RUN_ONCE)).deleteSecurityRule(
                Mockito.eq(order.getProvider()), Mockito.eq(cloudName), Mockito.eq(securityRuleId),
                Mockito.eq(systemUser));
    }
    
    // test case: When calling the genericRequest method, it must verify that this
    // call was successful.
    @Test
    public void testGenericRequest() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        String cloudName = TestUtils.DEFAULT_CLOUD_NAME;
        String providerId = TestUtils.LOCAL_MEMBER_ID;
        String genericRequest = ANY_VALUE;

        RasOperation expectedOperation = new RasOperation(Operation.GENERIC_REQUEST, ResourceType.GENERIC_RESOURCE,
                cloudName, genericRequest);

        // exercise
        this.facade.genericRequest(cloudName, providerId, genericRequest, userToken);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .genericRequest(Mockito.eq(genericRequest), Mockito.eq(systemUser));
    }
    
    // test case: When calling the activateOrder method with an attachment order, it
    // must verify that this call was successful.
    @Test
    public void testActivateAttachmentOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        computeOrder.setInstanceId(TestUtils.FAKE_COMPUTE_ID);
        SharedOrderHolders.getInstance().getActiveOrdersMap().put(computeOrder.getId(), computeOrder);

        VolumeOrder volumeOrder = this.testUtils.createLocalVolumeOrder();
        volumeOrder.setInstanceId(TestUtils.FAKE_VOLUME_ID);
        SharedOrderHolders.getInstance().getActiveOrdersMap().put(volumeOrder.getId(), volumeOrder);

        AttachmentOrder order = this.testUtils.createLocalAttachmentOrder(computeOrder, volumeOrder);
        
        Mockito.doNothing().when(this.facade).checkEmbeddedOrdersConsistency(Mockito.eq(order));

        RasOperation expectedOperation = new RasOperation(Operation.CREATE, ResourceType.ATTACHMENT,
                TestUtils.DEFAULT_CLOUD_NAME, order);

        // exercise
        this.facade.activateOrder(order, userToken);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .checkEmbeddedOrdersConsistency(Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).activateOrder(Mockito.eq(order));
    }
    
    // test case: When calling the activateOrder method with a public IP order, it
    // must verify that this call was successful.
    @Test
    public void testActivatePublicIpOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        computeOrder.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        SharedOrderHolders.getInstance().getActiveOrdersMap().put(computeOrder.getId(), computeOrder);

        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(computeOrder.getId());
        
        Mockito.doNothing().when(this.facade).checkEmbeddedOrdersConsistency(Mockito.eq(order));

        RasOperation expectedOperation = new RasOperation(Operation.CREATE, ResourceType.PUBLIC_IP,
                TestUtils.DEFAULT_CLOUD_NAME, order);

        // exercise
        this.facade.activateOrder(order, userToken);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .checkEmbeddedOrdersConsistency(Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).activateOrder(Mockito.eq(order));
    }
    
    // test case: When calling the activateOrder method with a null provider from
    // compute order, it must set a local provider ID and verify that this call was
    // successful.
    @Test
    public void testActivateComputeOrderWithNullProvider() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        ComputeOrder order = this.testUtils.createLocalComputeOrder();
        order.setProvider(null);
        
        Mockito.doNothing().when(this.facade).checkEmbeddedOrdersConsistency(Mockito.eq(order));

        RasOperation expectedOperation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE,
                TestUtils.DEFAULT_CLOUD_NAME, order);

        // exercise
        this.facade.activateOrder(order, userToken);

        // verify
        Assert.assertNotNull(order.getProvider());
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .checkEmbeddedOrdersConsistency(Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).activateOrder(Mockito.eq(order));
    }
    
    // test case: When calling the activateOrder method with an empty provider from
    // compute order, it must set a local provider ID and verify that this call was
    // successful.
    @Test
    public void testActivateComputeOrderWithEmptyProvider() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        ComputeOrder order = this.testUtils.createLocalComputeOrder();
        order.setProvider(EMPTY_STRING);
        
        Mockito.doNothing().when(this.facade).checkEmbeddedOrdersConsistency(Mockito.eq(order));

        RasOperation expectedOperation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE,
                TestUtils.DEFAULT_CLOUD_NAME, order);

        // exercise
        this.facade.activateOrder(order, userToken);

        // verify
        Assert.assertFalse(order.getProvider().isEmpty());
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .checkEmbeddedOrdersConsistency(Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).activateOrder(Mockito.eq(order));
    }
    
    // test case: When calling the activateOrder method with a null cloud name from
    // network order, it must set a default cloud name and verify that this call was
    // successful.
    @Test
    public void testActivateNetworkOrderWithNullCloudName() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        order.setCloudName(null);
        
        Mockito.doNothing().when(this.facade).checkEmbeddedOrdersConsistency(Mockito.eq(order));

        RasOperation expectedOperation = new RasOperation(Operation.CREATE, ResourceType.NETWORK,
                TestUtils.DEFAULT_CLOUD_NAME, order);

        // exercise
        this.facade.activateOrder(order, userToken);

        // verify
        Mockito.verify(this.cloudListController, Mockito.times(TestUtils.RUN_ONCE)).getDefaultCloudName();
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .checkEmbeddedOrdersConsistency(Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).activateOrder(Mockito.eq(order));
    }
    
    // test case: When calling the activateOrder method with an empty cloud name
    // from volume order, it must set a default cloud name and verify that this call
    // was successful.
    @Test
    public void testActivateVolumeOrderWithNullCloudName() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        VolumeOrder order = this.testUtils.createLocalVolumeOrder();
        order.setCloudName(EMPTY_STRING);
        
        Mockito.doNothing().when(this.facade).checkEmbeddedOrdersConsistency(Mockito.eq(order));

        RasOperation expectedOperation = new RasOperation(Operation.CREATE, ResourceType.VOLUME,
                TestUtils.DEFAULT_CLOUD_NAME, order);

        // exercise
        this.facade.activateOrder(order, userToken);

        // verify
        Mockito.verify(this.cloudListController, Mockito.times(TestUtils.RUN_ONCE)).getDefaultCloudName();
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .checkEmbeddedOrdersConsistency(Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).activateOrder(Mockito.eq(order));
    }
    
    // test case: When calling the getResourceInstance method with a valid compute
    // order, it must verify that this call was successful.
    @Test
    public void testGetResourceInstanceForComputeOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        Order order = testUtils.createLocalComputeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        ComputeInstance instance = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(instance).when(this.orderController).getResourceInstance(Mockito.eq(order));

        // exercise
        this.facade.getResourceInstance(order.getId(), userToken, ResourceType.COMPUTE);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).authorizeOrder(Mockito.eq(systemUser),
                Mockito.eq(TestUtils.DEFAULT_CLOUD_NAME), Mockito.eq(Operation.GET), Mockito.eq(ResourceType.COMPUTE),
                Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getResourceInstance(Mockito.eq(order));
    }
    
    // test case: When calling the getResourceInstance method with a valid volume
    // order, it must verify that this call was successful.
    @Test
    public void testGetResourceInstanceForVolumeOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        Order order = testUtils.createLocalVolumeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        ComputeInstance instance = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(instance).when(this.orderController).getResourceInstance(Mockito.eq(order));

        // exercise
        this.facade.getResourceInstance(order.getId(), userToken, ResourceType.VOLUME);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).authorizeOrder(Mockito.eq(systemUser),
                Mockito.eq(TestUtils.DEFAULT_CLOUD_NAME), Mockito.eq(Operation.GET), Mockito.eq(ResourceType.VOLUME),
                Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getResourceInstance(Mockito.eq(order));
    }
    
    // test case: When calling the getResourceInstance method with a valid
    // attachment order, it must verify that this call was successful.
    @Test
    public void testGetResourceInstanceForAttachmentOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        AttachmentOrder order = this.testUtils.createLocalAttachmentOrder(this.testUtils.createLocalComputeOrder(),
                this.testUtils.createLocalVolumeOrder());
        order.setProvider(TestUtils.LOCAL_MEMBER_ID);
        this.orderController.activateOrder(order);

        ComputeInstance instance = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(instance).when(this.orderController).getResourceInstance(Mockito.eq(order));

        // exercise
        this.facade.getResourceInstance(order.getId(), userToken, order.getType());

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).authorizeOrder(Mockito.eq(systemUser),
                Mockito.eq(TestUtils.DEFAULT_CLOUD_NAME), Mockito.eq(Operation.GET),
                Mockito.eq(ResourceType.ATTACHMENT), Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getResourceInstance(Mockito.eq(order));
    }
    
    // test case: When calling the getResourceInstance method with a valid network
    // order, it must verify that this call was successful.
    @Test
    public void testGetResourceInstanceForNetworkOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        Order order = testUtils.createLocalNetworkOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        ComputeInstance instance = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(instance).when(this.orderController).getResourceInstance(Mockito.eq(order));

        // exercise
        this.facade.getResourceInstance(order.getId(), userToken, ResourceType.NETWORK);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).authorizeOrder(Mockito.eq(systemUser),
                Mockito.eq(TestUtils.DEFAULT_CLOUD_NAME), Mockito.eq(Operation.GET), Mockito.eq(ResourceType.NETWORK),
                Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getResourceInstance(Mockito.eq(order));
    }
    
    // test case: When calling the getResourceInstance method with a valid public IP
    // order, it must verify that this call was successful.
    @Test
    public void testGetResourceInstanceForPublicIpOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        Order order = testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        ComputeInstance instance = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(instance).when(this.orderController).getResourceInstance(Mockito.eq(order));

        // exercise
        this.facade.getResourceInstance(order.getId(), userToken, ResourceType.PUBLIC_IP);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).authorizeOrder(Mockito.eq(systemUser),
                Mockito.eq(TestUtils.DEFAULT_CLOUD_NAME), Mockito.eq(Operation.GET), Mockito.eq(ResourceType.PUBLIC_IP),
                Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getResourceInstance(Mockito.eq(order));
    }
    
    // test case: When calling the deleteOrder method with a valid compute
    // order, it must verify that this call was successful.
    @Test
    public void testDeleteOrderForComputeResourceType() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        Order order = testUtils.createLocalComputeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.eq(order));

        // exercise
        this.facade.deleteOrder(order.getId(), userToken, ResourceType.COMPUTE);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).authorizeOrder(Mockito.eq(systemUser),
                Mockito.eq(TestUtils.DEFAULT_CLOUD_NAME), Mockito.eq(Operation.DELETE),
                Mockito.eq(ResourceType.COMPUTE), Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).deleteOrder(Mockito.eq(order));
    }
    
    // test case: When calling the deleteOrder method with a valid volume
    // order, it must verify that this call was successful.
    @Test
    public void testDeleteOrderForVolumeResourceType() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        Order order = testUtils.createLocalVolumeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.eq(order));

        // exercise
        this.facade.deleteOrder(order.getId(), userToken, ResourceType.VOLUME);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).authorizeOrder(Mockito.eq(systemUser),
                Mockito.eq(TestUtils.DEFAULT_CLOUD_NAME), Mockito.eq(Operation.DELETE), Mockito.eq(ResourceType.VOLUME),
                Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).deleteOrder(Mockito.eq(order));
    }
    
    // test case: When calling the deleteOrder method with a valid attachment
    // order, it must verify that this call was successful.
    @Test
    public void testDeleteOrderForAttachmentResourceType() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
        Order order = testUtils.createLocalAttachmentOrder(this.testUtils.createLocalComputeOrder(),
                this.testUtils.createLocalVolumeOrder());
        order.setProvider(TestUtils.LOCAL_MEMBER_ID);
        this.orderController.activateOrder(order);

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.eq(order));

        // exercise
        this.facade.deleteOrder(order.getId(), userToken, ResourceType.ATTACHMENT);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).authorizeOrder(Mockito.eq(systemUser),
                Mockito.eq(TestUtils.DEFAULT_CLOUD_NAME), Mockito.eq(Operation.DELETE),
                Mockito.eq(ResourceType.ATTACHMENT), Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).deleteOrder(Mockito.eq(order));
    }
    
    // test case: When calling the deleteOrder method with a valid network
    // order, it must verify that this call was successful.
    @Test
    public void testDeleteOrderForNetworkResourceType() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));

        Order order = testUtils.createLocalNetworkOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.eq(order));

        // exercise
        this.facade.deleteOrder(order.getId(), userToken, ResourceType.NETWORK);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).authorizeOrder(Mockito.eq(systemUser),
                Mockito.eq(TestUtils.DEFAULT_CLOUD_NAME), Mockito.eq(Operation.DELETE), Mockito.eq(ResourceType.NETWORK),
                Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).deleteOrder(Mockito.eq(order));
    }
    
    // test case: When calling the deleteOrder method with a valid public IP
    // order, it must verify that this call was successful.
    @Test
    public void testDeleteOrderForPublicIpResourceType() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));

        Order order = testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.eq(order));

        // exercise
        this.facade.deleteOrder(order.getId(), userToken, ResourceType.PUBLIC_IP);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).authorizeOrder(Mockito.eq(systemUser),
                Mockito.eq(TestUtils.DEFAULT_CLOUD_NAME), Mockito.eq(Operation.DELETE), Mockito.eq(ResourceType.PUBLIC_IP),
                Mockito.eq(order));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).deleteOrder(Mockito.eq(order));
    }
    
    // test case: When calling the getUserAllocation method with null or empty cloud
    // name, it must set as default cloud name and verify that this call was
    // successful.
    @Test
    public void testGetUserAllocationWithNullCloudName() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));

        String cloudName = null;
        String providerId = TestUtils.LOCAL_MEMBER_ID;
        ResourceType resourceType = ResourceType.COMPUTE;

        RasOperation expectedOperation = new RasOperation(Operation.GET_USER_ALLOCATION, resourceType, TestUtils.DEFAULT_CLOUD_NAME);

        // exercise
        this.facade.getUserAllocation(providerId, cloudName, userToken, ResourceType.COMPUTE);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.cloudListController, Mockito.times(TestUtils.RUN_ONCE)).getDefaultCloudName();
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE))
                .getUserAllocation(Mockito.eq(providerId), Mockito.eq(systemUser), Mockito.eq(resourceType));
    }
    
    // test case: When calling the getUserQuota method with null or empty cloud
    // name, it must set as default cloud name and verify that this call was
    // successful.
    @Test
    public void testGetUserQuotaWithEmptyCloudName() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));

        String cloudName = EMPTY_STRING;
        String providerId = TestUtils.LOCAL_MEMBER_ID;
        ResourceType resourceType = ResourceType.COMPUTE;

        RasOperation expectedOperation = new RasOperation(Operation.GET_USER_QUOTA, resourceType,
                TestUtils.DEFAULT_CLOUD_NAME);

        // exercise
        this.facade.getUserQuota(providerId, cloudName, userToken, resourceType);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .getAuthenticationFromRequester(Mockito.eq(userToken));
        Mockito.verify(this.cloudListController, Mockito.times(TestUtils.RUN_ONCE)).getDefaultCloudName();
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).getUserQuota(Mockito.eq(systemUser),
                Mockito.eq(resourceType));
    }
    
    // test case: When calling the authorizeOrder method with resource type
    // different from order, it must throws an InstanceNotFoundException;
    @Test
    public void testAuthorizeOrderWithDifferentResourceType() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalComputeOrder();
        
        String expected = Messages.Exception.MISMATCHING_RESOURCE_TYPE;

        try {
            // exercise
            this.facade.authorizeOrder(null, null, null, ResourceType.GENERIC_RESOURCE, order);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the authorizeOrder method with requester
    // different from order owner, it must throws an InstanceNotFoundException;
    @Test
    public void testAuthorizeOrderWithDifferentRequester() throws FogbowException {
        // set up
        SystemUser requester = Mockito.mock(SystemUser.class);
        Order order = this.testUtils.createLocalComputeOrder();
        
        String expected = Messages.Exception.REQUESTER_DOES_NOT_OWN_REQUEST;

        try {
            // exercise
            this.facade.authorizeOrder(requester, null, null, ResourceType.COMPUTE, order);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the checkEmbeddedOrdersConsistency method with a
    // COMPUTE resource type, it must verify that checkComputeOrderConsistency
    // method was called.
    @Test
    public void testCheckEmbeddedOrdersConsistencyWithComputeResourceType() throws FogbowException {
        // set up
        ComputeOrder order = this.testUtils.createLocalComputeOrder();
        Mockito.doNothing().when(this.facade).checkComputeOrderConsistency(Mockito.eq(order));

        // exercise
        this.facade.checkEmbeddedOrdersConsistency(order);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).checkComputeOrderConsistency(Mockito.eq(order));
    }
    
    // test case: When calling the checkEmbeddedOrdersConsistency method with a
    // ATTACHMENT resource type, it must verify that checkAttachmentOrderConsistency
    // method was called.
    @Test
    public void testCheckEmbeddedOrdersConsistencyWithAttachmentResourceType() throws FogbowException {
        // set up
        AttachmentOrder order = this.testUtils.createLocalAttachmentOrder(this.testUtils.createLocalComputeOrder(),
                this.testUtils.createLocalVolumeOrder());
        Mockito.doNothing().when(this.facade).checkAttachmentOrderConsistency(Mockito.eq(order));

        // exercise
        this.facade.checkEmbeddedOrdersConsistency(order);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .checkAttachmentOrderConsistency(Mockito.eq(order));
    }
    
    // test case: When calling the checkEmbeddedOrdersConsistency method with a
    // PUBLIC_IP resource type, it must verify that checkPublicIpOrderConsistency
    // method was called.
    @Test
    public void testCheckEmbeddedOrdersConsistencyWithPublicIpResourceType() throws FogbowException {
        // set up
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        Mockito.doNothing().when(this.facade).checkPublicIpOrderConsistency(Mockito.eq(order));

        // exercise
        this.facade.checkEmbeddedOrdersConsistency(order);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).checkPublicIpOrderConsistency(Mockito.eq(order));
    }
    
    // test case: When calling the checkEmbeddedOrdersConsistency method with a
    // resource type not supported, it must throws an UnexpectedException.
    @Test
    public void testCheckEmbeddedOrdersConsistencyWithUnsupportedResourceType() throws FogbowException {
        // set up
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getType()).thenReturn(ResourceType.GENERIC_RESOURCE);

        String expected = String.format(Messages.Exception.UNSUPPORTED_REQUEST_TYPE, order.getType());

        try {
            // exercise
            this.facade.checkEmbeddedOrdersConsistency(order);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the checkComputeOrderConsistency method with a
    // valid list of network order IDs, it must verify that this call was
    // successful.
    @Test
    public void testCheckComputeOrderConsistencyWithValidNetworkIDsList() throws FogbowException {
        // set up
        NetworkOrder networkOrder = this.testUtils.createLocalNetworkOrder();
        networkOrder.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(networkOrder);

        List<String> networkOrderIds = new ArrayList<>();
        networkOrderIds.add(networkOrder.getId());

        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder(networkOrderIds);

        // exercise
        this.facade.checkComputeOrderConsistency(computeOrder);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).getNetworkOrders(Mockito.eq(computeOrder.getNetworkOrderIds()));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .checkConsistencyOfEmbeddedOrder(Mockito.eq(computeOrder), Mockito.eq(networkOrder));
    }
    
    // test case: When calling the checkAttachmentOrderConsistency method with a
    // valid attachment order, it must verify that this call was successful.
    @Test
    public void testCheckAttachmentOrderConsistency() throws FogbowException {
        // set up
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        computeOrder.setInstanceId(TestUtils.FAKE_COMPUTE_ID);
        this.orderController.activateOrder(computeOrder);

        VolumeOrder volumeOrder = this.testUtils.createLocalVolumeOrder();
        volumeOrder.setInstanceId(TestUtils.FAKE_VOLUME_ID);
        this.orderController.activateOrder(volumeOrder);

        AttachmentOrder order = this.testUtils.createLocalAttachmentOrder(computeOrder, volumeOrder);

        // exercise
        this.facade.checkAttachmentOrderConsistency(order);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .checkConsistencyOfEmbeddedOrder(Mockito.eq(order), Mockito.eq(computeOrder));
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .checkConsistencyOfEmbeddedOrder(Mockito.eq(order), Mockito.eq(volumeOrder));
    }
    
    // test case: When calling the checkPublicIpOrderConsistency method with a
    // valid attachment order, it must verify that this call was successful.
    @Test
    public void testcheckPublicIpOrderConsistency() throws FogbowException {
        // set up
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        computeOrder.setInstanceId(TestUtils.FAKE_COMPUTE_ID);
        this.orderController.activateOrder(computeOrder);

        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(computeOrder.getId());

        // exercise
        this.facade.checkPublicIpOrderConsistency(order);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .checkConsistencyOfEmbeddedOrder(Mockito.eq(order), Mockito.eq(computeOrder));
    }
    
    // test case: When calling the checkConsistencyOfEmbeddedOrder method with
    // null orders, it must throws an InvalidParameterException.
    @Test
    public void testCheckConsistencyOfEmbeddedOrderWithNullValues() {
        // set up
        String expected = Messages.Exception.INVALID_RESOURCE;

        try {
            // exercise
            this.facade.checkConsistencyOfEmbeddedOrder(null, null);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the checkConsistencyOfEmbeddedOrder method with a
    // system user different of main order , it must throws an
    // InvalidParameterException.
    @Test
    public void testCheckConsistencyOfEmbeddedOrderWithSystemUserDifferentOfMainOrder() {
        // set up
        SystemUser systemUser = Mockito.mock(SystemUser.class);
        ComputeOrder embeddedOrder = this.testUtils.createLocalComputeOrder();
        PublicIpOrder mainOrder = this.testUtils.createLocalPublicIpOrder(embeddedOrder.getId());
        mainOrder.setSystemUser(systemUser);

        String expected = Messages.Exception.TRYING_TO_USE_RESOURCES_FROM_ANOTHER_USER;

        try {
            // exercise
            this.facade.checkConsistencyOfEmbeddedOrder(mainOrder, embeddedOrder);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the checkConsistencyOfEmbeddedOrder method with a
    // provider different of main order , it must throws an
    // InvalidParameterException.
    @Test
    public void testCheckConsistencyOfEmbeddedOrderWithProviderDifferentOfMainOrder() {
        // set up
        ComputeOrder embeddedOrder = this.testUtils.createLocalComputeOrder();
        PublicIpOrder mainOrder = this.testUtils.createLocalPublicIpOrder(embeddedOrder.getId());
        mainOrder.setProvider(TestUtils.FAKE_REMOTE_MEMBER_ID);

        String expected = Messages.Exception.PROVIDERS_DONT_MATCH;

        try {
            // exercise
            this.facade.checkConsistencyOfEmbeddedOrder(mainOrder, embeddedOrder);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the checkConsistencyOfEmbeddedOrder method with a
    // cloud name different of main order , it must throws an
    // InvalidParameterException.
    @Test
    public void testCheckConsistencyOfEmbeddedOrderWithCloudNameDifferentOfMainOrder() {
        // set up
        ComputeOrder embeddedOrder = this.testUtils.createLocalComputeOrder();
        PublicIpOrder mainOrder = this.testUtils.createLocalPublicIpOrder(embeddedOrder.getId());
        mainOrder.setCloudName(ANY_VALUE);

        String expected = Messages.Exception.CLOUD_NAMES_DONT_MATCH;

        try {
            // exercise
            this.facade.checkConsistencyOfEmbeddedOrder(mainOrder, embeddedOrder);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the checkConsistencyOfEmbeddedOrder method with a
    // null instance ID, it must throws an InvalidParameterException.
    @Test
    public void testCheckConsistencyOfEmbeddedOrderWithNullInstance() {
        // set up
        ComputeOrder embeddedOrder = this.testUtils.createLocalComputeOrder();
        PublicIpOrder mainOrder = this.testUtils.createLocalPublicIpOrder(embeddedOrder.getId());
        Assert.assertNull(embeddedOrder.getInstanceId());

        String expected = String.format(Messages.Exception.INSTANCE_NULL_S, embeddedOrder.getId());

        try {
            // exercise
            this.facade.checkConsistencyOfEmbeddedOrder(mainOrder, embeddedOrder);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the checkEmbeddedOrdersConsistency method with an
    // invalid list of network order IDs, it must must throws an
    // InvalidParameterException.
    @Test
    public void testGetNetworkOrdersFail() {
        // set up
        List<String> networkOrderIds = new ArrayList<>();
        networkOrderIds.add(TestUtils.FAKE_ORDER_ID);

        String expected = Messages.Exception.INVALID_PARAMETER;

        try {
            // exercise
            this.facade.getNetworkOrders(networkOrderIds);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    private ArrayList<UserData> generateVeryLongUserDataFileContent() {
        char[] value = new char[UserData.MAX_EXTRA_USER_DATA_FILE_CONTENT + 1];
        String extraUserDataFileContent = new String(value);

        UserData userData = new UserData();
        userData.setExtraUserDataFileContent(extraUserDataFileContent);

        ArrayList<UserData> userDataScripts = new ArrayList<>();
        userDataScripts.add(userData);
        return userDataScripts;
    }
	
    private AuthorizationPlugin mockAuthorizationPlugin() throws FogbowException {
        AuthorizationPlugin plugin = Mockito.mock(DefaultAuthorizationPlugin.class);
        Mockito.when(plugin.isAuthorized(Mockito.any(SystemUser.class), Mockito.any(RasOperation.class)))
                .thenReturn(true);
        return plugin;
    }

    private ServiceAsymmetricKeysHolder mockServiceAsymmetricKeysHolder() {
        ServiceAsymmetricKeysHolder sakHolder = Mockito.mock(ServiceAsymmetricKeysHolder.class);
        PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
        PowerMockito.when(ServiceAsymmetricKeysHolder.getInstance()).thenReturn(sakHolder);
        return sakHolder;
    }
    
}
