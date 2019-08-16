package cloud.fogbow.ras.core;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.DependencyDetectedException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.RemoteCommunicationException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.CryptoUtil;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.common.util.connectivity.FogbowGenericRequest;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
import cloud.fogbow.common.util.connectivity.HttpRequest;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceStatus;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteGetCloudNamesRequest;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.authorization.DefaultAuthorizationPlugin;

@PrepareForTest({ 
    AuthenticationUtil.class, 
    CloudConnectorFactory.class, 
    CryptoUtil.class, 
    DatabaseManager.class, 
    PacketSenderHolder.class, 
    RasPublicKeysHolder.class, 
    RemoteGetCloudNamesRequest.class, 
    ServiceAsymmetricKeysHolder.class, 
    SharedOrderHolders.class 
})
public class ApplicationFacadeTest extends BaseUnitTests {

    private static final String ANY_VALUE = "anything";
	private static final String BUILD_NUMBER_FORMAT = "%s-abcd";
	private static final String BUILD_NUMBER_FORMAT_FOR_TESTING = "%s-[testing mode]";
	private static final String EMPTY_STRING = "";
	private static final String FAKE_CLOUD_NAME = "fake-cloud-name";
	private static final String FAKE_CONTENT = "fooBar";
	private static final String FAKE_PROVIDER_ID = "fake-provider-id";
	private static final String FAKE_OWNER_USER_ID_VALUE = "fake-owner-user-id";
	private static final String FAKE_RULE_ID = "fake-rule-id";
	private static final String FAKE_URL = "https://www.foo.bar";
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
		this.cloudListController = mockCloudListController();
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
        String providerId = null;
        String cloudName = EMPTY_STRING;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));

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
        String providerId = null;
        String cloudName = EMPTY_STRING;
        String imageId = TestUtils.FAKE_IMAGE_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));
        
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
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        this.orderController.activateOrder(order);
        
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));

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
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        this.orderController.activateOrder(order);

        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));

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
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        this.orderController.activateOrder(order);

        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));

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
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        this.orderController.activateOrder(order);

        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));

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
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        this.orderController.activateOrder(order);

        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));

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
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        this.orderController.activateOrder(order);

        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));

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
        String cloudName = TestUtils.DEFAULT_CLOUD_NAME;
        String providerId = TestUtils.LOCAL_MEMBER_ID;
        String genericRequest = ANY_VALUE;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).getAuthenticationFromRequester(Mockito.eq(userToken));

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
    
	// test case: When calling the getAsPublicKey method, verify that this call was
	// successful.
	@Ignore // FIXME
    @Test
	public void testGetAuthenticationServicePublicKeySuccessfully() throws Exception {
		// set up
		PowerMockito.mockStatic(RasPublicKeysHolder.class);
		RasPublicKeysHolder pkHolder = Mockito.mock(RasPublicKeysHolder.class);
		PowerMockito.when(RasPublicKeysHolder.getInstance()).thenReturn(pkHolder);

		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.when(pkHolder.getAsPublicKey()).thenReturn(keyRSA);

		// exercise
		this.facade.getAsPublicKey();

		// verify
		PowerMockito.verifyStatic(RasPublicKeysHolder.class, Mockito.times(1));
		RasPublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();
	}	
	
	// test case: When calling the authorizeOrder method with a different resource
	// type of the order, it must throw a InstanceNotFoundException.
	@Ignore // FIXME
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testAuthorizeOrderThrowsInstanceNotFoundException() throws Exception {
		// set up
		SystemUser systemUser = null;
		String cloudName = null;
		Operation operation = null;
		ResourceType resourceType = ResourceType.VOLUME;
		ComputeOrder order = new ComputeOrder();
		
		// exercise
		this.facade.authorizeOrder(systemUser, cloudName, operation, resourceType, order);
	}
	
	// test case: When calling the authorizeOrder method with a system user
	// different from the order requester, it must throw an
	// UnauthorizedRequestException.
	@Ignore // FIXME
	@Test(expected = UnauthorizedRequestException.class) // verify
	public void testAuthorizeOrderThrowsUnauthorizedRequestException() throws Exception {
		// set up
		SystemUser owner = new SystemUser(FAKE_OWNER_USER_ID_VALUE, null, null);
		String cloudName = null;
		Operation operation = null;
		ResourceType resourceType = ResourceType.COMPUTE;
		ComputeOrder order = this.testUtils.createLocalComputeOrder();

		// exercise
		this.facade.authorizeOrder(owner, cloudName, operation, resourceType, order);
	}

	// test case: When calling the deleteNetwork method passing a network order with a compute dependency,
	// a DependencyDetectedException should be thrown
	@Ignore // FIXME
	@Test(expected = DependencyDetectedException.class) // verify
	public void testDeleteNetworkOrderWithComputeDependency() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		NetworkOrder networkOrder1 = this.testUtils.createLocalNetworkOrder();
		networkOrder1.setSystemUser(systemUser);
		networkOrder1.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
		
		NetworkOrder networkOrder2 = this.testUtils.createLocalNetworkOrder();
		networkOrder2.setSystemUser(systemUser);
		networkOrder2.setInstanceId(TestUtils.FAKE_INSTANCE_ID);

		this.orderController.activateOrder(networkOrder1);
		this.orderController.activateOrder(networkOrder2);

		List<String> networkIds = new LinkedList<>();
		networkIds.add(networkOrder1.getId());
		networkIds.add(networkOrder2.getId());

		ComputeOrder computeOrder = new ComputeOrder(
		        this.testUtils.createSystemUser(),
                        TestUtils.LOCAL_MEMBER_ID,
                        TestUtils.LOCAL_MEMBER_ID,
                        TestUtils.DEFAULT_CLOUD_NAME, 
                        TestUtils.FAKE_INSTANCE_NAME,
                        TestUtils.CPU_VALUE,
                        TestUtils.MEMORY_VALUE,
                        TestUtils.DISK_VALUE,
                        TestUtils.FAKE_IMAGE_ID,
                        this.testUtils.mockUserData(),
                        TestUtils.FAKE_PUBLIC_KEY,
                        networkIds);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.COMPUTE,
				TestUtils.DEFAULT_CLOUD_NAME,
				computeOrder);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		ComputeInstance computeInstance = new ComputeInstance(computeOrder.getId());
		computeOrder.setInstanceId(computeInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(computeOrder.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createCompute(computeOrder, SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
		
		Assert.assertEquals(expectedOrderState, computeOrder.getOrderState());

		// exercise
		this.facade.deleteNetwork(networkOrder2.getId(), SYSTEM_USER_TOKEN_VALUE);
	}

	// test case: When calling the deleteCompute method passing a compute order with an attachment dependency,
	// a DependencyDetectedException should be thrown
	@Ignore // FIXME
	@Test(expected = DependencyDetectedException.class) // verify
	public void testDeleteComputeOrderWithAttachmentDependency() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        computeOrder.setInstanceId(computeOrder.getId());
        this.orderController.activateOrder(computeOrder);
        
        VolumeOrder volumeOrder = this.testUtils.createLocalVolumeOrder();
        volumeOrder.setInstanceId(volumeOrder.getId());
        this.orderController.activateOrder(volumeOrder);

        AttachmentOrder attachmentOrder = this.testUtils.createLocalAttachmentOrder(computeOrder, volumeOrder);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.ATTACHMENT,
				TestUtils.DEFAULT_CLOUD_NAME,
				attachmentOrder);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		AttachmentInstance attachmentInstance = new AttachmentInstance(attachmentOrder.getId());
		attachmentOrder.setInstanceId(attachmentInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(attachmentOrder.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createAttachment(attachmentOrder, SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, attachmentOrder.getOrderState());

		// exercise
		this.facade.deleteCompute(attachmentOrder.getComputeOrderId(), SYSTEM_USER_TOKEN_VALUE);
	}

	// test case: When calling the deleteVolume method passing a volume order with an attachment dependency,
	// a DependencyDetectedException should be thrown
	@Ignore // FIXME
	@Test(expected = DependencyDetectedException.class) // verify
	public void testDeleteVolumeOrderWithAttachmentDependency() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        computeOrder.setInstanceId(computeOrder.getId());
		this.orderController.activateOrder(computeOrder);
        
        VolumeOrder volumeOrder = this.testUtils.createLocalVolumeOrder();
        volumeOrder.setInstanceId(volumeOrder.getId());
        this.orderController.activateOrder(volumeOrder);

        AttachmentOrder attachmentOrder = this.testUtils.createLocalAttachmentOrder(computeOrder, volumeOrder);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.ATTACHMENT,
				TestUtils.DEFAULT_CLOUD_NAME,
				attachmentOrder);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		AttachmentInstance attachmentInstance = new AttachmentInstance(attachmentOrder.getId());
		attachmentOrder.setInstanceId(attachmentInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(attachmentOrder.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createAttachment(attachmentOrder, SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, attachmentOrder.getOrderState());

		// exercise
		this.facade.deleteVolume(attachmentOrder.getVolumeOrderId(), SYSTEM_USER_TOKEN_VALUE);
	}

	// test case: When calling the deleteCompute method with a PublicIp associated with the compute that is to be
	// deleted, a DependencyDetectedException should be thrown
	@Ignore // FIXME
	@Test(expected = DependencyDetectedException.class) // verify
	public void testDeleteComputeOrderWithPublicIpDependency() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();

		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		PublicIpOrder order = spyPublicIpOrder(systemUser);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.PUBLIC_IP,
				TestUtils.DEFAULT_CLOUD_NAME,
				order
		);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		PublicIpInstance publicIpInstance = new PublicIpInstance(order.getId());
		order.setInstanceId(publicIpInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createPublicIp(order, SYSTEM_USER_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, order.getOrderState());

		// exercise
		this.facade.deleteCompute(order.getComputeOrderId(), SYSTEM_USER_TOKEN_VALUE);

		// verify
		Mockito.verify(this.orderController, Mockito.times(1)).deleteOrder(null);
	}

	private PublicIpOrder spyPublicIpOrder(SystemUser systemUser) {
		String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);

		ComputeOrder computeOrder = new ComputeOrder();
		computeOrder.setSystemUser(systemUser);
		computeOrder.setOrderStateInTestMode(OrderState.FULFILLED);
		computeOrder.setRequester(localMemberId);
		computeOrder.setProvider(localMemberId);
		computeOrder.setCloudName(TestUtils.DEFAULT_CLOUD_NAME);
		computeOrder.setName(TestUtils.FAKE_ORDER_NAME);
		computeOrder.setId(TestUtils.FAKE_COMPUTE_ID);
		ComputeInstance computeInstance = new ComputeInstance(TestUtils.FAKE_INSTANCE_ID);
		computeOrder.setInstanceId(computeInstance.getId());
		
		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		Map<String, Order> activeOrdersMap = Mockito.spy(sharedOrderHolders.getActiveOrdersMap());
		activeOrdersMap.put(computeOrder.getId(), computeOrder);
		
		String computeOrderId = computeOrder.getId();
		PublicIpOrder order = Mockito.spy(
				new PublicIpOrder(systemUser,
						localMemberId,
						localMemberId,
						TestUtils.DEFAULT_CLOUD_NAME, 
						computeOrderId));

		return order;
	}

	private SystemUser createFederationUserAuthenticate(RSAPublicKey keyRSA) throws FogbowException {
		SystemUser systemUser = this.testUtils.createSystemUser();
		PowerMockito.mockStatic(AuthenticationUtil.class);
		PowerMockito.when(AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString()))
				.thenReturn(systemUser);
		
		return systemUser;
	}

    private RSAPublicKey mockRSAPublicKey() throws FogbowException {
        RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
        Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
        return keyRSA;
    }
    
	private AuthorizationPlugin<RasOperation> mockAuthorizationPlugin(SystemUser systemUser, RasOperation rasOperation)
			throws UnexpectedException, UnauthorizedRequestException {

		AuthorizationPlugin<RasOperation> authorization = Mockito.mock(DefaultAuthorizationPlugin.class);
		Mockito.when(authorization.isAuthorized(Mockito.eq(systemUser), Mockito.eq(rasOperation))).thenReturn(true);

		this.facade.setAuthorizationPlugin(authorization);
		return authorization;
	}
	// TODO the methods below are up to date...

    private ArrayList<UserData> generateVeryLongUserDataFileContent() {
        char[] value = new char[UserData.MAX_EXTRA_USER_DATA_FILE_CONTENT + 1];
        String extraUserDataFileContent = new String(value);

        UserData userData = new UserData();
        userData.setExtraUserDataFileContent(extraUserDataFileContent);

        ArrayList<UserData> userDataScripts = new ArrayList<>();
        userDataScripts.add(userData);
        return userDataScripts;
    }
	
    private CloudListController mockCloudListController() {
        CloudListController controller = Mockito.mock(CloudListController.class);
        List<String> cloudNames = new ArrayList<>();
        Mockito.doReturn(cloudNames).when(controller).getCloudNames();
        Mockito.doReturn(TestUtils.DEFAULT_CLOUD_NAME).when(controller).getDefaultCloudName();
        return controller;
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
