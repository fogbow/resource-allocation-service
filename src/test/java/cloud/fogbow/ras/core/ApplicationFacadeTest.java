package cloud.fogbow.ras.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceStatus;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.api.http.response.quotas.allocation.NetworkAllocation;
import cloud.fogbow.ras.api.http.response.quotas.allocation.PublicIpAllocation;
import cloud.fogbow.ras.api.http.response.quotas.allocation.VolumeAllocation;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.intercomponent.RemoteFacade;
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

@PrepareForTest({ CloudConnectorFactory.class, DatabaseManager.class, ServiceAsymmetricKeysHolder.class, 
                  SynchronizationManager.class, PropertiesHolder.class, RasPublicKeysHolder.class,
                  AuthorizationPluginInstantiator.class, RemoteFacade.class })
public class ApplicationFacadeTest extends BaseUnitTests {

    private static final String AUTHORIZATION_PLUGIN = "authorization_plugin";
    private static final String CLOUD_NAMES = "cloud";
    private static final String PRIVATE_KEY = "private_key";
    private static final String PUBLIC_KEY = "public_key";
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
	private AuthorizationPlugin<RasOperation> authorizationPlugin;
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
    // Path, it must throw an InternalServerErrorException.
    @Test(expected = InternalServerErrorException.class) // verify
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
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        RasOperation expectedOperation = new RasOperation(Operation.GET, ResourceType.CLOUD_NAME,
                localMember, localMember);

        // exercise
        this.facade.getCloudNames(localMember, userToken);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.cloudListController, Mockito.times(TestUtils.RUN_ONCE)).getCloudNames();
    }
    
    // test case: When calling the getCloudNames method with a remote member, it must
    // verify that this call was successful.
    @Test
    public void testGetCloudNamesWithRemoteMember() throws Exception {
        // set up
        String localMember = TestUtils.LOCAL_MEMBER_ID;
        String remoteMember = TestUtils.FAKE_REMOTE_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));

        RemoteGetCloudNamesRequest cloudNamesRequest = Mockito.mock(RemoteGetCloudNamesRequest.class);
        Mockito.doReturn(cloudNamesRequest).when(this.facade).getCloudNamesFromRemoteRequest(Mockito.eq(remoteMember),
                Mockito.eq(systemUser));
        
        RasOperation expectedOperation = new RasOperation(Operation.GET, ResourceType.CLOUD_NAME,
                localMember, remoteMember);

        // exercise
        this.facade.getCloudNames(remoteMember, userToken);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(cloudNamesRequest, Mockito.times(TestUtils.RUN_ONCE)).send();
    }
    
    // test case: When calling the getCloudNames method from a remote member and
    // remote communication is not set, it must throw a
    // FogbowException.
    @Test(expected = FogbowException.class) // verify
    public void testGetCloudNamesWithRemoteMemberThrowsAnException() throws FogbowException {
        // set up
        String remoteMember = TestUtils.FAKE_REMOTE_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));

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
    
    // test case: When calling the pauseUserComputes method, it must call
    // the pauseOrder method of the OrderController to pause the 
    // correct computes.
    @Test
    public void testPauseUserComputes() throws FogbowException {
    	// set up
    	String userToken = SYSTEM_USER_TOKEN_VALUE;
    	String orderId1 = "orderId1";
    	String orderId2 = "orderId2";
    	
    	SystemUser systemUser = this.testUtils.createSystemUser();
    	Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
    	
    	InstanceStatus instance1 = Mockito.mock(InstanceStatus.class);
    	InstanceStatus instance2 = Mockito.mock(InstanceStatus.class);
    	
    	Mockito.when(instance1.getInstanceId()).thenReturn(orderId1);
    	Mockito.when(instance2.getInstanceId()).thenReturn(orderId2);
    	
    	List<InstanceStatus> instanceStatusList = new ArrayList<InstanceStatus>();
    	instanceStatusList.add(instance1);
    	instanceStatusList.add(instance2);
    	
    	Order order1 = Mockito.mock(Order.class);
    	Order order2 = Mockito.mock(Order.class);
    	
    	Mockito.doReturn(instanceStatusList).when(orderController).
    	getUserInstancesStatus(TestUtils.FAKE_USER_ID, TestUtils.LOCAL_MEMBER_ID, ResourceType.COMPUTE);
    	
    	Mockito.doReturn(order1).when(orderController).getOrder(orderId1);
    	Mockito.doReturn(order2).when(orderController).getOrder(orderId2);
    	
    	Mockito.doNothing().when(orderController).pauseOrder(order1);
    	Mockito.doNothing().when(orderController).pauseOrder(order2);
    	
    	// exercise
    	this.facade.pauseUserComputes(TestUtils.FAKE_USER_ID, TestUtils.LOCAL_MEMBER_ID, userToken);
    	
    	// verify
    	Mockito.verify(orderController).pauseOrder(order1);
    	Mockito.verify(orderController).pauseOrder(order2);
    }
    
    // test case: When calling the hibernateUserComputes method, it must call
    // the hibernateOrder method of the OrderController to hibernate the 
    // correct computes.
    @Test
    public void testHibernateUserComputes() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        String orderId1 = "orderId1";
        String orderId2 = "orderId2";
        
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        InstanceStatus instance1 = Mockito.mock(InstanceStatus.class);
        InstanceStatus instance2 = Mockito.mock(InstanceStatus.class);
        
        Mockito.when(instance1.getInstanceId()).thenReturn(orderId1);
        Mockito.when(instance2.getInstanceId()).thenReturn(orderId2);
        
        List<InstanceStatus> instanceStatusList = new ArrayList<InstanceStatus>();
        instanceStatusList.add(instance1);
        instanceStatusList.add(instance2);
        
        Order order1 = Mockito.mock(Order.class);
        Order order2 = Mockito.mock(Order.class);
        
        Mockito.doReturn(instanceStatusList).when(orderController).
        getUserInstancesStatus(TestUtils.FAKE_USER_ID, TestUtils.LOCAL_MEMBER_ID, ResourceType.COMPUTE);
        
        Mockito.doReturn(order1).when(orderController).getOrder(orderId1);
        Mockito.doReturn(order2).when(orderController).getOrder(orderId2);
        
        Mockito.doNothing().when(orderController).hibernateOrder(order1);
        Mockito.doNothing().when(orderController).hibernateOrder(order2);
        
        // exercise
        this.facade.hibernateUserComputes(TestUtils.FAKE_USER_ID, TestUtils.LOCAL_MEMBER_ID, userToken);
        
        // verify
        Mockito.verify(orderController).hibernateOrder(order1);
        Mockito.verify(orderController).hibernateOrder(order2);
    }
    
    // test case: When calling the stopCompute method, it must call the 
    // stopOrder method of the OrderController to stop the correct compute.
    @Test
    public void testStopCompute() throws FogbowException {
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        String orderId = "orderId";
        
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        Order order = Mockito.mock(ComputeOrder.class);
        Mockito.when(order.getType()).thenReturn(ResourceType.COMPUTE);
        
        Mockito.doReturn(order).when(orderController).getOrder(orderId);
        Mockito.doNothing().when(orderController).stopOrder(order);
        
        this.facade.stopCompute(orderId, userToken, ResourceType.COMPUTE);
        
        Mockito.verify(orderController).stopOrder(order);
    }
    
    // test case: When calling the stopCompute method passing an order which
    // is not a ComputeOrder, it must throw an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testStopComputeFailsIfNotComputeOrder() throws FogbowException {
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        String orderId = "orderId";
        
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getType()).thenReturn(ResourceType.VOLUME);
        
        Mockito.doReturn(order).when(orderController).getOrder(orderId);
        Mockito.doNothing().when(orderController).stopOrder(order);
        
        this.facade.stopCompute(orderId, userToken, ResourceType.COMPUTE);
    }
    
    // test case: When calling the stopUserComputes method, it must call
    // the stopOrder method of the OrderController to stop the 
    // correct computes.
    @Test
    public void testStopUserComputes() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        String orderId1 = "orderId1";
        String orderId2 = "orderId2";
        
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        InstanceStatus instance1 = Mockito.mock(InstanceStatus.class);
        InstanceStatus instance2 = Mockito.mock(InstanceStatus.class);
        
        Mockito.when(instance1.getInstanceId()).thenReturn(orderId1);
        Mockito.when(instance2.getInstanceId()).thenReturn(orderId2);
        
        List<InstanceStatus> instanceStatusList = new ArrayList<InstanceStatus>();
        instanceStatusList.add(instance1);
        instanceStatusList.add(instance2);
        
        Order order1 = Mockito.mock(Order.class);
        Order order2 = Mockito.mock(Order.class);
        
        Mockito.doReturn(instanceStatusList).when(orderController).
        getUserInstancesStatus(TestUtils.FAKE_USER_ID, TestUtils.LOCAL_MEMBER_ID, ResourceType.COMPUTE);
        
        Mockito.doReturn(order1).when(orderController).getOrder(orderId1);
        Mockito.doReturn(order2).when(orderController).getOrder(orderId2);
        
        Mockito.doNothing().when(orderController).stopOrder(order1);
        Mockito.doNothing().when(orderController).stopOrder(order2);
        
        // exercise
        this.facade.stopUserComputes(TestUtils.FAKE_USER_ID, TestUtils.LOCAL_MEMBER_ID, userToken);
        
        // verify
        Mockito.verify(orderController).stopOrder(order1);
        Mockito.verify(orderController).stopOrder(order2);
    }
    
    // test case: When calling the resumeUserComputes method, it must call
    // the resumeOrder method of the OrderController to resume the 
    // correct computes.
    @Test
    public void testResumeUserComputes() throws FogbowException {
    	// set up
    	String userToken = SYSTEM_USER_TOKEN_VALUE;
    	String orderId1 = "orderId1";
    	String orderId2 = "orderId2";
    	
    	SystemUser systemUser = this.testUtils.createSystemUser();
    	Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
    	
    	InstanceStatus instance1 = Mockito.mock(InstanceStatus.class);
    	InstanceStatus instance2 = Mockito.mock(InstanceStatus.class);
    	
    	Mockito.when(instance1.getInstanceId()).thenReturn(orderId1);
    	Mockito.when(instance2.getInstanceId()).thenReturn(orderId2);
    	
    	List<InstanceStatus> instanceStatusList = new ArrayList<InstanceStatus>();
    	instanceStatusList.add(instance1);
    	instanceStatusList.add(instance2);
    	
    	Order order1 = Mockito.mock(Order.class);
    	Order order2 = Mockito.mock(Order.class);
    	
    	Mockito.doReturn(instanceStatusList).when(orderController).
    	getUserInstancesStatus(TestUtils.FAKE_USER_ID, TestUtils.LOCAL_MEMBER_ID, ResourceType.COMPUTE);
    	
    	Mockito.doReturn(order1).when(orderController).getOrder(orderId1);
    	Mockito.doReturn(order2).when(orderController).getOrder(orderId2);
    	
    	Mockito.doNothing().when(orderController).resumeOrder(order1);
    	Mockito.doNothing().when(orderController).resumeOrder(order2);
    	
    	// exercise
    	this.facade.resumeUserComputes(TestUtils.FAKE_USER_ID, TestUtils.LOCAL_MEMBER_ID, userToken);
    	
    	// verify
    	Mockito.verify(orderController).resumeOrder(order1);
    	Mockito.verify(orderController).resumeOrder(order2);
    }
    
    // test case: When calling the purgeUser method, it must call the deleteOrder 
    // method of OrderController to delete all the given users' orders.
    @Test
    public void testPurgeUser() throws FogbowException {
        // set up authentication
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
   
        // set up orders
        
        // public IP, attachment and network orders in this example have no dependencies and 
        // are deleted after the first state check.
        Order orderPublicIp1 = Mockito.mock(Order.class);
        Mockito.when(orderPublicIp1.getOrderState()).thenReturn(OrderState.FULFILLED, OrderState.CLOSED);
        Mockito.doReturn(true).when(orderController).dependenciesAreClosed(orderPublicIp1);
        
        Order orderPublicIp2 = Mockito.mock(Order.class);
        Mockito.when(orderPublicIp2.getOrderState()).thenReturn(OrderState.FULFILLED, OrderState.CLOSED);
        Mockito.doReturn(true).when(orderController).dependenciesAreClosed(orderPublicIp2);
        
        Order orderAttachment1 = Mockito.mock(Order.class);
        Mockito.when(orderAttachment1.getOrderState()).thenReturn(OrderState.FULFILLED, OrderState.CLOSED);
        Mockito.doReturn(true).when(orderController).dependenciesAreClosed(orderAttachment1);
        
        Order orderAttachment2 = Mockito.mock(Order.class);
        Mockito.when(orderAttachment2.getOrderState()).thenReturn(OrderState.FULFILLED, OrderState.CLOSED);
        Mockito.doReturn(true).when(orderController).dependenciesAreClosed(orderAttachment2);
        
        Order orderNetwork1 = Mockito.mock(Order.class);
        Mockito.when(orderNetwork1.getOrderState()).thenReturn(OrderState.FULFILLED, OrderState.CLOSED);
        Mockito.doReturn(true).when(orderController).dependenciesAreClosed(orderNetwork1);
        
        Order orderNetwork2 = Mockito.mock(Order.class);
        Mockito.when(orderNetwork2.getOrderState()).thenReturn(OrderState.FULFILLED, OrderState.CLOSED);
        Mockito.doReturn(true).when(orderController).dependenciesAreClosed(orderNetwork2);
        
        // compute and volume orders have dependencies and their deletion require another iteration 
        // over the orders list. Therefore, here we duplicate the OrderState.FULFILLED state.
        Order orderCompute1 = Mockito.mock(Order.class);
        Mockito.when(orderCompute1.getOrderState()).thenReturn(OrderState.FULFILLED, OrderState.FULFILLED, 
                OrderState.CLOSED);
        Mockito.doReturn(false).doReturn(true).when(orderController).dependenciesAreClosed(orderCompute1);
        
        Order orderCompute2 = Mockito.mock(Order.class);
        Mockito.when(orderCompute2.getOrderState()).thenReturn(OrderState.FULFILLED, OrderState.FULFILLED, 
                OrderState.CLOSED);
        Mockito.doReturn(false).doReturn(true).when(orderController).dependenciesAreClosed(orderCompute2);
        
        // volume orders in this case will take more time to delete and will have the ASSIGNED_FOR_DELETION
        // and CHECKING_DELETION states before the CLOSED state.
        Order orderVolume1 = Mockito.mock(Order.class);
        Mockito.when(orderVolume1.getOrderState()).thenReturn(OrderState.FULFILLED, OrderState.FULFILLED, 
                OrderState.ASSIGNED_FOR_DELETION, OrderState.CHECKING_DELETION, OrderState.CLOSED);
        Mockito.doReturn(false).doReturn(true).when(orderController).dependenciesAreClosed(orderVolume1);
        
        Order orderVolume2 = Mockito.mock(Order.class);
        Mockito.when(orderVolume2.getOrderState()).thenReturn(OrderState.FULFILLED, OrderState.FULFILLED, 
                OrderState.ASSIGNED_FOR_DELETION, OrderState.CHECKING_DELETION, OrderState.CLOSED);
        Mockito.doReturn(false).doReturn(true).when(orderController).dependenciesAreClosed(orderVolume2);

        // set up order controller
        
        Mockito.doReturn(Arrays.asList(orderPublicIp1, orderPublicIp2)).
        when(orderController).getAllOrders(systemUser, ResourceType.PUBLIC_IP);
        
        Mockito.doReturn(Arrays.asList(orderAttachment1, orderAttachment2)).
        when(orderController).getAllOrders(systemUser, ResourceType.ATTACHMENT);
        
        Mockito.doReturn(Arrays.asList(orderVolume1, orderVolume2)).
        when(orderController).getAllOrders(systemUser, ResourceType.VOLUME);
        
        Mockito.doReturn(Arrays.asList(orderCompute1, orderCompute2)).
        when(orderController).getAllOrders(systemUser, ResourceType.COMPUTE);
        
        Mockito.doReturn(Arrays.asList(orderNetwork1, orderNetwork2)).
        when(orderController).getAllOrders(systemUser, ResourceType.NETWORK);
        
        Mockito.doNothing().when(orderController).deleteOrder(orderPublicIp1);
        Mockito.doNothing().when(orderController).deleteOrder(orderPublicIp2);
        Mockito.doNothing().when(orderController).deleteOrder(orderAttachment1);
        Mockito.doNothing().when(orderController).deleteOrder(orderAttachment2);
        Mockito.doNothing().when(orderController).deleteOrder(orderVolume1);
        Mockito.doNothing().when(orderController).deleteOrder(orderVolume2);
        Mockito.doNothing().when(orderController).deleteOrder(orderCompute1);
        Mockito.doNothing().when(orderController).deleteOrder(orderCompute2);
        Mockito.doNothing().when(orderController).deleteOrder(orderNetwork1);
        Mockito.doNothing().when(orderController).deleteOrder(orderNetwork2);
        
        // exercise
        this.facade.purgeUser(userToken, TestUtils.FAKE_USER_ID, TestUtils.LOCAL_MEMBER_ID);
        
        // verify
        Mockito.verify(orderController).deleteOrder(orderPublicIp1);
        Mockito.verify(orderController).deleteOrder(orderPublicIp2);
        Mockito.verify(orderController).deleteOrder(orderAttachment1);
        Mockito.verify(orderController).deleteOrder(orderAttachment2);
        Mockito.verify(orderController).deleteOrder(orderVolume1);
        Mockito.verify(orderController).deleteOrder(orderVolume2);
        Mockito.verify(orderController).deleteOrder(orderCompute1);
        Mockito.verify(orderController).deleteOrder(orderCompute2);
        Mockito.verify(orderController).deleteOrder(orderNetwork1);
        Mockito.verify(orderController).deleteOrder(orderNetwork2);
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
    
    // test case: When calling the getVolumeAllocation method it must check that
    // getUserAllocation method was called.
    @Test
    public void testGetVolumeAllocation() throws FogbowException {
        // set up
        String providerId = TestUtils.LOCAL_MEMBER_ID;
        String cloudName = TestUtils.DEFAULT_CLOUD_NAME;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        VolumeAllocation volumeAllocation = Mockito.mock(VolumeAllocation.class);
        Mockito.doReturn(volumeAllocation).when(this.facade).getUserAllocation(Mockito.eq(providerId),
                Mockito.eq(cloudName), Mockito.eq(userToken), Mockito.eq(ResourceType.VOLUME));

        // exercise
        this.facade.getVolumeAllocation(providerId, cloudName, userToken);

        // verify
        Mockito.verify(this.facade).getUserAllocation(Mockito.eq(providerId), Mockito.eq(cloudName),
                Mockito.eq(userToken), Mockito.eq(ResourceType.VOLUME));
    }
    
    // test case: When calling the getNetworkAllocation method it must check that
    // getUserAllocation method was called.
    @Test
    public void testGetNetworkAllocation() throws FogbowException {
        // set up
        String providerId = TestUtils.LOCAL_MEMBER_ID;
        String cloudName = TestUtils.DEFAULT_CLOUD_NAME;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        NetworkAllocation networkAllocation = Mockito.mock(NetworkAllocation.class);
        Mockito.doReturn(networkAllocation).when(this.facade).getUserAllocation(Mockito.eq(providerId),
                Mockito.eq(cloudName), Mockito.eq(userToken), Mockito.eq(ResourceType.NETWORK));

        // exercise
        this.facade.getNetworkAllocation(providerId, cloudName, userToken);

        // verify
        Mockito.verify(this.facade).getUserAllocation(Mockito.eq(providerId), Mockito.eq(cloudName),
                Mockito.eq(userToken), Mockito.eq(ResourceType.NETWORK));
    }
    
    // test case: When calling the getPublicIpAllocation method it must check that
    // getUserAllocation method was called.
    @Test
    public void testGetPublicIpAllocation() throws FogbowException {
        // set up
        String providerId = TestUtils.LOCAL_MEMBER_ID;
        String cloudName = TestUtils.DEFAULT_CLOUD_NAME;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        PublicIpAllocation publicIpAllocation = Mockito.mock(PublicIpAllocation.class);
        Mockito.doReturn(publicIpAllocation).when(this.facade).getUserAllocation(Mockito.eq(providerId),
                Mockito.eq(cloudName), Mockito.eq(userToken), Mockito.eq(ResourceType.PUBLIC_IP));

        // exercise
        this.facade.getPublicIpAllocation(providerId, cloudName, userToken);

        // verify
        Mockito.verify(this.facade).getUserAllocation(Mockito.eq(providerId), Mockito.eq(cloudName),
                Mockito.eq(userToken), Mockito.eq(ResourceType.PUBLIC_IP));
    }
    
    // test case: When calling the getResourceQuota method it must check that
    // getUserQuota method was called.
    @Test
    public void testGetResourceQuota() throws FogbowException {
        // set up
        String providerId = TestUtils.LOCAL_MEMBER_ID;
        String cloudName = TestUtils.DEFAULT_CLOUD_NAME;
        String userToken = SYSTEM_USER_TOKEN_VALUE;

        ResourceQuota resourceQuota = Mockito.mock(ResourceQuota.class);
        Mockito.doReturn(resourceQuota).when(this.facade).getUserQuota(Mockito.eq(providerId), Mockito.eq(cloudName),
                Mockito.eq(userToken));

        // exercise
        this.facade.getResourceQuota(providerId, cloudName, userToken);

        // verify
        Mockito.verify(this.facade).getUserQuota(Mockito.eq(providerId), Mockito.eq(cloudName), Mockito.eq(userToken));
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
        String localMember = TestUtils.LOCAL_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        ResourceType resourceType = ResourceType.COMPUTE;
        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, resourceType,
                localMember, localMember);

        // exercise
        this.facade.getAllInstancesStatus(userToken, resourceType);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
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
        String localMember = TestUtils.LOCAL_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        ResourceType resourceType = ResourceType.VOLUME;
        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, resourceType,
                localMember, localMember);

        // exercise
        this.facade.getAllInstancesStatus(userToken, resourceType);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
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
        String localMember = TestUtils.LOCAL_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        ResourceType resourceType = ResourceType.ATTACHMENT;
        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, resourceType,
                localMember, localMember);

        // exercise
        this.facade.getAllInstancesStatus(userToken, resourceType);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
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
        String localMember = TestUtils.LOCAL_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        ResourceType resourceType = ResourceType.NETWORK;
        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, resourceType,
                localMember, localMember);

        // exercise
        this.facade.getAllInstancesStatus(userToken, resourceType);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
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
        String localMember = TestUtils.LOCAL_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        ResourceType resourceType = ResourceType.PUBLIC_IP;
        
        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, resourceType,
                localMember, localMember);

        // exercise
        this.facade.getAllInstancesStatus(userToken, resourceType);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
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
        String localMember = TestUtils.LOCAL_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        String providerId = null;
        String cloudName = EMPTY_STRING;

        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, ResourceType.IMAGE,
                TestUtils.DEFAULT_CLOUD_NAME, localMember, null);

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
        String localMember = TestUtils.LOCAL_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        String providerId = null;
        String cloudName = EMPTY_STRING;
        String imageId = TestUtils.FAKE_IMAGE_ID;
        
        RasOperation expectedOperation = new RasOperation(Operation.GET, ResourceType.IMAGE,
                TestUtils.DEFAULT_CLOUD_NAME, localMember, null);

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
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        this.orderController.activateOrder(order);

        SecurityRule securityRule = Mockito.mock(SecurityRule.class);

        RasOperation expectedOperation = new RasOperation(Operation.CREATE, ResourceType.SECURITY_RULE,
                TestUtils.DEFAULT_CLOUD_NAME, order);

        // exercise
        this.facade.createSecurityRule(order.getId(), securityRule, userToken, ResourceType.NETWORK);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
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
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        this.orderController.activateOrder(order);

        SecurityRule securityRule = Mockito.mock(SecurityRule.class);

        RasOperation expectedOperation = new RasOperation(Operation.CREATE, ResourceType.SECURITY_RULE,
                TestUtils.DEFAULT_CLOUD_NAME, order);

        // exercise
        this.facade.createSecurityRule(order.getId(), securityRule, userToken, ResourceType.PUBLIC_IP);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
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
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        NetworkOrder order = this.testUtils.createLocalNetworkOrder();
        this.orderController.activateOrder(order);

        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, ResourceType.SECURITY_RULE,
                TestUtils.DEFAULT_CLOUD_NAME, order);

        // exercise
        this.facade.getAllSecurityRules(order.getId(), userToken, ResourceType.NETWORK);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
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
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        PublicIpOrder order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        this.orderController.activateOrder(order);

        RasOperation expectedOperation = new RasOperation(Operation.GET_ALL, ResourceType.SECURITY_RULE,
                TestUtils.DEFAULT_CLOUD_NAME, order);

        // exercise
        this.facade.getAllSecurityRules(order.getId(), userToken, ResourceType.PUBLIC_IP);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
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
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
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
                .authenticate(Mockito.eq(userToken));
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
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
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
                .authenticate(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.securityRuleController, Mockito.times(TestUtils.RUN_ONCE)).deleteSecurityRule(
                Mockito.eq(order.getProvider()), Mockito.eq(cloudName), Mockito.eq(securityRuleId),
                Mockito.eq(systemUser));
    }
    
    // test case: When calling the activateOrder method with an attachment order, it
    // must verify that this call was successful.
    @Test
    public void testActivateAttachmentOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
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
                .authenticate(Mockito.eq(userToken));
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
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
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
                .authenticate(Mockito.eq(userToken));
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
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        Order order = testUtils.createLocalComputeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        RasOperation operation = new RasOperation(Operation.GET, order.getType(), order.getCloudName(), order);

        ComputeInstance instance = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(instance).when(this.orderController).getResourceInstance(Mockito.eq(order));

        // exercise
        this.facade.getResourceInstance(order.getId(), userToken, ResourceType.COMPUTE);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                        Mockito.eq(operation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getResourceInstance(Mockito.eq(order));
    }
    
    // test case: When calling the getResourceInstance method with a valid volume
    // order, it must verify that this call was successful.
    @Test
    public void testGetResourceInstanceForVolumeOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        Order order = testUtils.createLocalVolumeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        RasOperation operation = new RasOperation(Operation.GET, order.getType(), order.getCloudName(), order);

        ComputeInstance instance = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(instance).when(this.orderController).getResourceInstance(Mockito.eq(order));

        // exercise
        this.facade.getResourceInstance(order.getId(), userToken, ResourceType.VOLUME);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                        Mockito.eq(operation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getResourceInstance(Mockito.eq(order));
    }
    
    // test case: When calling the getResourceInstance method with a valid
    // attachment order, it must verify that this call was successful.
    @Test
    public void testGetResourceInstanceForAttachmentOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        AttachmentOrder order = this.testUtils.createLocalAttachmentOrder(this.testUtils.createLocalComputeOrder(),
                this.testUtils.createLocalVolumeOrder());
        order.setProvider(TestUtils.LOCAL_MEMBER_ID);
        this.orderController.activateOrder(order);

        RasOperation operation = new RasOperation(Operation.GET, order.getType(), order.getCloudName(), order);

        ComputeInstance instance = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(instance).when(this.orderController).getResourceInstance(Mockito.eq(order));

        // exercise
        this.facade.getResourceInstance(order.getId(), userToken, order.getType());

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                        Mockito.eq(operation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getResourceInstance(Mockito.eq(order));
    }
    
    // test case: When calling the getResourceInstance method with a valid network
    // order, it must verify that this call was successful.
    @Test
    public void testGetResourceInstanceForNetworkOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        Order order = testUtils.createLocalNetworkOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        RasOperation operation = new RasOperation(Operation.GET, order.getType(), order.getCloudName(), order);

        ComputeInstance instance = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(instance).when(this.orderController).getResourceInstance(Mockito.eq(order));

        // exercise
        this.facade.getResourceInstance(order.getId(), userToken, ResourceType.NETWORK);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                        Mockito.eq(operation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getResourceInstance(Mockito.eq(order));
    }
    
    // test case: When calling the getResourceInstance method with a valid public IP
    // order, it must verify that this call was successful.
    @Test
    public void testGetResourceInstanceForPublicIpOrder() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        Order order = testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        RasOperation operation = new RasOperation(Operation.GET, order.getType(), order.getCloudName(), order);

        ComputeInstance instance = Mockito.mock(ComputeInstance.class);
        Mockito.doReturn(instance).when(this.orderController).getResourceInstance(Mockito.eq(order));

        // exercise
        this.facade.getResourceInstance(order.getId(), userToken, ResourceType.PUBLIC_IP);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                        Mockito.eq(operation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getResourceInstance(Mockito.eq(order));
    }
    
    // test case: When calling the deleteOrder method with a valid compute
    // order, it must verify that this call was successful.
    @Test
    public void testDeleteOrderForComputeResourceType() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));

        Order order = testUtils.createLocalComputeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        RasOperation operation = new RasOperation(Operation.DELETE, order.getType(), order.getCloudName(), order);

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.eq(order));

        // exercise
        this.facade.deleteOrder(order.getId(), userToken, ResourceType.COMPUTE);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(operation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).deleteOrder(Mockito.eq(order));
    }
    
    // test case: When calling the deleteOrder method with a valid volume
    // order, it must verify that this call was successful.
    @Test
    public void testDeleteOrderForVolumeResourceType() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        Order order = testUtils.createLocalVolumeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        RasOperation operation = new RasOperation(Operation.DELETE, order.getType(), order.getCloudName(), order);

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.eq(order));

        // exercise
        this.facade.deleteOrder(order.getId(), userToken, ResourceType.VOLUME);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                        Mockito.eq(operation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).deleteOrder(Mockito.eq(order));
    }
    
    // test case: When calling the deleteOrder method with a valid attachment
    // order, it must verify that this call was successful.
    @Test
    public void testDeleteOrderForAttachmentResourceType() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        Order order = testUtils.createLocalAttachmentOrder(this.testUtils.createLocalComputeOrder(),
                this.testUtils.createLocalVolumeOrder());
        order.setProvider(TestUtils.LOCAL_MEMBER_ID);
        this.orderController.activateOrder(order);

        RasOperation operation = new RasOperation(Operation.DELETE, order.getType(), order.getCloudName(), order);

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.eq(order));

        // exercise
        this.facade.deleteOrder(order.getId(), userToken, ResourceType.ATTACHMENT);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                        Mockito.eq(operation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).deleteOrder(Mockito.eq(order));
    }
    
    // test case: When calling the deleteOrder method with a valid network
    // order, it must verify that this call was successful.
    @Test
    public void testDeleteOrderForNetworkResourceType() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));

        Order order = testUtils.createLocalNetworkOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        RasOperation operation = new RasOperation(Operation.DELETE, order.getType(), order.getCloudName(), order);

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.eq(order));

        // exercise
        this.facade.deleteOrder(order.getId(), userToken, ResourceType.NETWORK);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                        Mockito.eq(operation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).deleteOrder(Mockito.eq(order));
    }
    
    // test case: When calling the deleteOrder method with a valid public IP
    // order, it must verify that this call was successful.
    @Test
    public void testDeleteOrderForPublicIpResourceType() throws FogbowException {
        // set up
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));

        Order order = testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.orderController.activateOrder(order);

        RasOperation operation = new RasOperation(Operation.DELETE, order.getType(), order.getCloudName(), order);

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.eq(order));

        // exercise
        this.facade.deleteOrder(order.getId(), userToken, ResourceType.PUBLIC_IP);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).getOrder(Mockito.eq(order.getId()));
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                        Mockito.eq(operation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE)).deleteOrder(Mockito.eq(order));
    }
    
    // test case: When calling the getUserAllocation method with null or empty cloud
    // name, it must set as default cloud name and verify that this call was
    // successful.
    @Test
    public void testGetUserAllocationWithNullCloudName() throws FogbowException {
        // set up
        String localMember = TestUtils.LOCAL_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));

        String cloudName = null;
        String providerId = TestUtils.LOCAL_MEMBER_ID;
        ResourceType resourceType = ResourceType.COMPUTE;

        RasOperation expectedOperation = new RasOperation(Operation.GET_USER_ALLOCATION, resourceType, TestUtils.DEFAULT_CLOUD_NAME,
                localMember, localMember);

        // exercise
        this.facade.getUserAllocation(providerId, cloudName, userToken, ResourceType.COMPUTE);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
        Mockito.verify(this.cloudListController, Mockito.times(TestUtils.RUN_ONCE)).getDefaultCloudName();
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.orderController, Mockito.times(TestUtils.RUN_ONCE))
                .getUserAllocation(Mockito.eq(providerId), Mockito.eq(TestUtils.DEFAULT_CLOUD_NAME), Mockito.eq(systemUser), Mockito.eq(resourceType));
    }
    
    // test case: When calling the getUserQuota method with null or empty cloud
    // name, it must set the default cloud name and verify that this call was
    // successful.
    @Test
    public void testGetUserQuotaWithEmptyCloudName() throws FogbowException {
        // set up
        String localMember = TestUtils.LOCAL_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));

        String cloudName = EMPTY_STRING;
        String providerId = TestUtils.LOCAL_MEMBER_ID;
        ResourceType resourceType = ResourceType.COMPUTE;

        RasOperation expectedOperation = new RasOperation(Operation.GET, ResourceType.QUOTA, TestUtils.DEFAULT_CLOUD_NAME,
                localMember, localMember);

        // exercise
        this.facade.getUserQuota(providerId, cloudName, userToken);

        // verify
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
        Mockito.verify(this.cloudListController, Mockito.times(TestUtils.RUN_ONCE)).getDefaultCloudName();
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).getUserQuota(Mockito.eq(systemUser)
        );
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
    // resource type not supported, it must throws an InternalServerErrorException.
    @Test
    public void testCheckEmbeddedOrdersConsistencyWithUnsupportedResourceType() throws FogbowException {
        // set up
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getType()).thenReturn(ResourceType.INVALID_RESOURCE);

        String expected = String.format(Messages.Exception.UNSUPPORTED_REQUEST_TYPE_S, order.getType());

        try {
            // exercise
            this.facade.checkEmbeddedOrdersConsistency(order);
            Assert.fail();
        } catch (InternalServerErrorException e) {
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
    public void testCheckPublicIpOrderConsistency() throws FogbowException {
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

        String expected = Messages.Exception.CLOUD_NAMES_DO_NOT_MATCH;

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
    
    @Test
    public void testReloadWithConcurrentOperations() throws FogbowException {

        //
        // set up
        //
        
        String localMember = TestUtils.LOCAL_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        String publicKey = PUBLIC_KEY;
        String privateKey = PRIVATE_KEY;
        String cloudNames = CLOUD_NAMES;
        String authorizationPluginName = AUTHORIZATION_PLUGIN;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        // set up SynchronizationManager
        PowerMockito.mockStatic(SynchronizationManager.class);
        SynchronizationManager syncManager = Mockito.mock(SynchronizationManager.class);
        Mockito.doReturn(false).when(syncManager).isReloading();
        BDDMockito.given(SynchronizationManager.getInstance()).willReturn(syncManager);

        // set up RemoteFacade
        PowerMockito.mockStatic(RemoteFacade.class);
        RemoteFacade remoteFacade = Mockito.mock(RemoteFacade.class);
        Mockito.doReturn(true).when(remoteFacade).noOnGoingRequests();
        BDDMockito.given(RemoteFacade.getInstance()).willReturn(remoteFacade);
        
        // set up PropertiesHolder 
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.doReturn(publicKey).when(propertiesHolder).getProperty(FogbowConstants.PUBLIC_KEY_FILE_PATH);
        Mockito.doReturn(privateKey).when(propertiesHolder).getProperty(FogbowConstants.PRIVATE_KEY_FILE_PATH);
        Mockito.doReturn(cloudNames).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.CLOUD_NAMES_KEY);     
        Mockito.doReturn(authorizationPluginName).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.AUTHORIZATION_PLUGIN_CLASS_KEY);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);

        // set up RasPublicKeysHolder
        PowerMockito.mockStatic(RasPublicKeysHolder.class);
        RasPublicKeysHolder publicKeysHolder = Mockito.mock(RasPublicKeysHolder.class);
        BDDMockito.given(RasPublicKeysHolder.getInstance()).willReturn(publicKeysHolder);

        // set up AuthorizationPluginInstantiator        
        PowerMockito.mockStatic(AuthorizationPluginInstantiator.class);
        AuthorizationPlugin<RasOperation> authorizationPlugin = mockAuthorizationPlugin();
        BDDMockito.given(AuthorizationPluginInstantiator.getAuthorizationPlugin(Mockito.anyString())).willReturn(authorizationPlugin);
        
        // set up ServiceAsymmetricKeysHolder
        PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
        
        //         
        Thread thread = new Thread(new FacadeOperationFinisher(this.facade));

        // Increment the onGoingRequests counter
        this.facade.startOperation();
        
        // Start thread to decrement counter
        thread.start();
        
        // Wait until decrement
        this.facade.reload(userToken);
        
        //
        // verify
        //
        
        // authenticates
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
                
        // authorizes
        RasOperation expectedOperation = new RasOperation(Operation.RELOAD, ResourceType.CONFIGURATION,
                localMember, localMember);
        
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
                
        // sets as reloading
        Mockito.verify(syncManager, Mockito.times(TestUtils.RUN_ONCE)).setAsReloading();
        
        // resets PropertiesHolder
        PowerMockito.verifyStatic(PropertiesHolder.class, Mockito.times(TestUtils.RUN_ONCE));
        PropertiesHolder.reset();
        
        // resets AS keys
        PowerMockito.verifyStatic(RasPublicKeysHolder.class, Mockito.times(TestUtils.RUN_ONCE));
        RasPublicKeysHolder.reset();
        
        // resets authorization plugin
        PowerMockito.verifyStatic(AuthorizationPluginInstantiator.class, Mockito.times(TestUtils.RUN_ONCE));
        AuthorizationPluginInstantiator.getAuthorizationPlugin(authorizationPluginName);
        
        // resets keys
        PowerMockito.verifyStatic(ServiceAsymmetricKeysHolder.class, Mockito.times(TestUtils.RUN_ONCE));
        ServiceAsymmetricKeysHolder.reset(publicKey, privateKey);
        
        // reloads remote facade
        Mockito.verify(remoteFacade, Mockito.times(TestUtils.RUN_ONCE)).reload();
        
        // reloads synchronization manager  
        Mockito.verify(syncManager, Mockito.times(TestUtils.RUN_ONCE)).reload();
    }

    private class FacadeOperationFinisher implements Runnable {

        private ApplicationFacade facade;

        public FacadeOperationFinisher(ApplicationFacade facade) {
            this.facade = facade;    
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1000);
                this.facade.finishOperation();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testReloadWithoutConcurrentOperations() throws FogbowException {

        //
        // set up
        //
        
        String localMember = TestUtils.LOCAL_MEMBER_ID;
        String userToken = SYSTEM_USER_TOKEN_VALUE;
        String publicKey = PUBLIC_KEY;
        String privateKey = PRIVATE_KEY;
        String cloudNames = CLOUD_NAMES;
        String authorizationPluginName = AUTHORIZATION_PLUGIN;
        SystemUser systemUser = this.testUtils.createSystemUser();
        Mockito.doReturn(systemUser).when(this.facade).authenticate(Mockito.eq(userToken));
        
        // set up SynchronizationManager
        PowerMockito.mockStatic(SynchronizationManager.class);
        SynchronizationManager syncManager = Mockito.mock(SynchronizationManager.class);
        BDDMockito.given(SynchronizationManager.getInstance()).willReturn(syncManager);
        
        // set up RemoteFacade
        PowerMockito.mockStatic(RemoteFacade.class);
        RemoteFacade remoteFacade = Mockito.mock(RemoteFacade.class);
        Mockito.doReturn(true).when(remoteFacade).noOnGoingRequests();
        BDDMockito.given(RemoteFacade.getInstance()).willReturn(remoteFacade);
        
        // set up PropertiesHolder 
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.doReturn(publicKey).when(propertiesHolder).getProperty(FogbowConstants.PUBLIC_KEY_FILE_PATH);
        Mockito.doReturn(privateKey).when(propertiesHolder).getProperty(FogbowConstants.PRIVATE_KEY_FILE_PATH);
        Mockito.doReturn(cloudNames).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.CLOUD_NAMES_KEY);     
        Mockito.doReturn(authorizationPluginName).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.AUTHORIZATION_PLUGIN_CLASS_KEY);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);

        // set up RasPublicKeysHolder
        PowerMockito.mockStatic(RasPublicKeysHolder.class);
        RasPublicKeysHolder publicKeysHolder = Mockito.mock(RasPublicKeysHolder.class);
        BDDMockito.given(RasPublicKeysHolder.getInstance()).willReturn(publicKeysHolder);

        // set up AuthorizationPluginInstantiator        
        PowerMockito.mockStatic(AuthorizationPluginInstantiator.class);
        AuthorizationPlugin<RasOperation> authorizationPlugin = mockAuthorizationPlugin();
        BDDMockito.given(AuthorizationPluginInstantiator.getAuthorizationPlugin(Mockito.anyString())).willReturn(authorizationPlugin);
        
        // set up ServiceAsymmetricKeysHolder
        PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
        
        
        this.facade.reload(userToken);


        //
        // verify
        //
        
        // authenticates
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE))
                .authenticate(Mockito.eq(userToken));
                
        // authorizes
        RasOperation expectedOperation = new RasOperation(Operation.RELOAD, ResourceType.CONFIGURATION,
                localMember, localMember);
        
        Mockito.verify(this.authorizationPlugin, Mockito.times(TestUtils.RUN_ONCE)).isAuthorized(Mockito.eq(systemUser),
                Mockito.eq(expectedOperation));
                
        // sets as reloading
        Mockito.verify(syncManager, Mockito.times(TestUtils.RUN_ONCE)).setAsReloading();
        
        // resets PropertiesHolder
        PowerMockito.verifyStatic(PropertiesHolder.class, Mockito.times(TestUtils.RUN_ONCE));
        PropertiesHolder.reset();
        
        // resets AS keys
        PowerMockito.verifyStatic(RasPublicKeysHolder.class, Mockito.times(TestUtils.RUN_ONCE));
        RasPublicKeysHolder.reset();
        
        // resets authorization plugin
        PowerMockito.verifyStatic(AuthorizationPluginInstantiator.class, Mockito.times(TestUtils.RUN_ONCE));
        AuthorizationPluginInstantiator.getAuthorizationPlugin(authorizationPluginName);
        
        // resets keys
        PowerMockito.verifyStatic(ServiceAsymmetricKeysHolder.class, Mockito.times(TestUtils.RUN_ONCE));
        ServiceAsymmetricKeysHolder.reset(publicKey, privateKey);
        
        // reloads remote facade
        Mockito.verify(remoteFacade, Mockito.times(TestUtils.RUN_ONCE)).reload();
        
        // reloads synchronization manager  
        Mockito.verify(syncManager, Mockito.times(TestUtils.RUN_ONCE)).reload();
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
	
    private AuthorizationPlugin<RasOperation> mockAuthorizationPlugin() {
        AuthorizationPlugin<RasOperation> plugin = Mockito.mock(DefaultAuthorizationPlugin.class);
        return plugin;
    }

    private ServiceAsymmetricKeysHolder mockServiceAsymmetricKeysHolder() {
        ServiceAsymmetricKeysHolder sakHolder = Mockito.mock(ServiceAsymmetricKeysHolder.class);
        PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
        PowerMockito.when(ServiceAsymmetricKeysHolder.getInstance()).thenReturn(sakHolder);
        return sakHolder;
    }
    
}
