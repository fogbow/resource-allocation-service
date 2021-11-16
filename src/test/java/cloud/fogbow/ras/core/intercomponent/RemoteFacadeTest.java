package cloud.fogbow.ras.core.intercomponent;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.ras.api.http.response.*;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.*;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.plugins.authorization.DefaultAuthorizationPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.ArrayList;
import java.util.List;

@PrepareForTest({ CloudConnectorFactory.class, DatabaseManager.class, PacketSenderHolder.class,
    AuthorizationPluginInstantiator.class, PropertiesHolder.class, SynchronizationManager.class })
public class RemoteFacadeTest extends BaseUnitTests {

	private static final String DEFAULT_CLOUD_NAME = "default";
	private static final String FAKE_IMAGE_ID = "fake-image-id";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_LOCALIDENTITY_PROVIDER = "fake-localidentity-provider";
	private static final String FAKE_OWNER_USER_ID_VALUE = "fake-owner-user-id";
    private static final String FAKE_REQUESTER_USER_ID_VALUE = "fake-requester-user-id";
    private static final String FAKE_REQUESTER_ID = "fake-requester-id";
	private static final String FAKE_RULE_ID = "fake-rule-id";

	private RemoteFacade facade;
	private OrderController orderController;

    @Before
    public void setUp() throws InternalServerErrorException {
        this.testUtils.mockReadOrdersFromDataBase();
        this.orderController = Mockito.spy(new OrderController());
        this.facade = Mockito.spy(RemoteFacade.getInstance());
        this.facade.setOrderController(this.orderController);
    }

	// test case: When calling the getOrder method, it must return the order expected.
	@Test
	public void testGetOrderSuccessfully() throws FogbowException {
		// set up
		SystemUser systemUser = createFederationUser();

		String cloudName = DEFAULT_CLOUD_NAME;
		String requester = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
		Order orderExpected = spyComputeOrder(systemUser, cloudName, requester);
		this.orderController.activateOrder(orderExpected);

		// exercise
		Order order = this.facade.getOrder(requester, orderExpected.getId());

		// verify
		Assert.assertEquals(orderExpected, order);
	}

	// test case: When calling the getOrder method, it must return the order expected.
	@Test(expected = InstanceNotFoundException.class)
	public void testGetOrderFail() throws FogbowException {
		// set up
		SystemUser systemUser = createFederationUser();

		String cloudName = DEFAULT_CLOUD_NAME;
		String requester = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
		Order orderExpected = spyComputeOrder(systemUser, cloudName, requester);
		String orderId = orderExpected.getId();
		InstanceNotFoundException instanceNotFoundException = new InstanceNotFoundException();
		Mockito.doThrow(instanceNotFoundException).when(this.orderController)
				.getOrder(Mockito.eq(orderId));

		// exercise
		this.facade.getOrder(requester, orderId);
	}

	// test case: When calling the activateOrder method with a requesting member
	// different of the order requester, it must throw an InternalServerErrorException.
	@Test(expected = InternalServerErrorException.class) // verify
	public void testRemoteActivateOrderThrowsUnexpectedException() throws Exception {
		// set up
		Order order = new ComputeOrder();
		order.setRequester(FAKE_REQUESTER_ID);
		order.setProvider(FAKE_REQUESTER_ID);

		// exercise
		this.facade.activateOrder(FAKE_REQUESTER_ID, order);
	}

	// test case: When calling the activateOrder method with a providing member
	// different of the orders provider, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class) // verify
	public void testRemoteActivateOrderThrowsInvalidParameterException() throws Exception {
		// set up
		Order order = new ComputeOrder();
		order.setRequester(FAKE_LOCALIDENTITY_PROVIDER);
		order.setProvider(FAKE_LOCALIDENTITY_PROVIDER);

		// exercise
		this.facade.activateOrder(FAKE_REQUESTER_ID, order);
	}

	// test case: When calling the activateOrder method a new Order passed by
	// parameter, it must return to Open OrderState after its activation.
	@Test
	public void testRemoteActivateOrderSuccessfully() throws FogbowException {
		// set up
		SystemUser systemUser = createFederationUser();

		String cloudName = DEFAULT_CLOUD_NAME;
		String requester = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
		Order order = spyComputeOrder(systemUser, cloudName, requester);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.COMPUTE,
				DEFAULT_CLOUD_NAME,
				order
		);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		cloud.fogbow.ras.core.models.orders.OrderState expectedOrderState = cloud.fogbow.ras.core.models.orders.OrderState.OPEN;

		// exercise
		this.facade.activateOrder(requester, order);

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getResourceInstance method and the instance of the
	// requested order is not found, it must throw an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testRemoteGetResourceInstanceThrowsInstanceNotFoundException() throws Exception {
		// set up
		SystemUser systemUser = createFederationUser();
		Order order = new ComputeOrder();

		// exercise
		this.facade.getResourceInstance(FAKE_REQUESTER_ID, order.getId(), systemUser, order.getType());
	}

	// test case: When calling the getResourceInstance method, it must return an
	// Instance of the Order ID passed per parameter.
	@Test
	public void testRemoteGetResourceInstanceSuccessfully() throws Exception {
		// set up
		OrderController orderController = Mockito.spy(new OrderController());
		this.facade.setOrderController(orderController);

		SystemUser systemUser = createFederationUser();

		String cloudName = DEFAULT_CLOUD_NAME;
		String requester = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
		Order order = spyComputeOrder(systemUser, cloudName, requester);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.GET,
				ResourceType.COMPUTE,
				DEFAULT_CLOUD_NAME,
				order
		);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		Instance expectedInstance = new ComputeInstance(FAKE_INSTANCE_ID);
		Mockito.doReturn(expectedInstance).when(orderController).getResourceInstance(Mockito.any(Order.class));

		// exercise
		Instance instance = this.facade.getResourceInstance(requester, order.getId(), systemUser,
				order.getType());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
		Assert.assertSame(expectedInstance, instance);
	}

	// test case: When calling the deleteOrder method and the instance of the
	// requested order is not found, it must throw an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testRemoteDeleteOrderThrowsInstanceNotFoundException() throws Exception {
		// set up
		SystemUser systemUser = createFederationUser();

		AuthorizationPlugin<RasOperation> authorization = Mockito.mock(DefaultAuthorizationPlugin.class);
		Mockito.when(authorization.isAuthorized(Mockito.eq(systemUser), Mockito.eq(new RasOperation(Operation.GET, ResourceType.COMPUTE, 
		        FAKE_REQUESTER_ID, FAKE_LOCALIDENTITY_PROVIDER)))).thenReturn(true);

		this.facade.setAuthorizationPlugin(authorization);

		String cloudName = DEFAULT_CLOUD_NAME;
		String provider = FAKE_LOCALIDENTITY_PROVIDER;
		Order order = spyComputeOrder(systemUser, cloudName, provider);

		CloudConnectorFactory factory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(factory);

		Mockito.doThrow(new InstanceNotFoundException()).when(cloudConnector).deleteInstance(Mockito.eq(order));

		// exercise
		this.facade.deleteOrder(FAKE_REQUESTER_ID, order.getId(), systemUser, order.getType());
	}

	// test case: When calling the deleteOrder method with an Order passed as
	// parameter, it must return its OrderState as ASSIGNED_FOR_DELETION.
	@Test
	public void testRemoteDeleteOrderSuccessfully() throws Exception {
		// set up
		String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
		SystemUser systemUser = createFederationUser();

		String cloudName = DEFAULT_CLOUD_NAME;
		String provider = localMemberId;
		Order order = spyComputeOrder(systemUser, cloudName, provider);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.DELETE,
				ResourceType.COMPUTE,
				DEFAULT_CLOUD_NAME,
				order
		);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		// checking that the order has a state and is not null
		Assert.assertNotNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.ASSIGNED_FOR_DELETION;

		// exercise
		this.facade.deleteOrder(localMemberId, order.getId(), systemUser, order.getType());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the stopOrder method, it must authorize the operation and
	// call the stopOrder method of the OrderController.
	@Test
	public void testRemoteStopOrder() throws FogbowException {
	    // set up
        String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
        SystemUser systemUser = createFederationUser();

        String cloudName = DEFAULT_CLOUD_NAME;
        String provider = localMemberId;
        Order order = spyComputeOrder(systemUser, cloudName, provider);
        this.orderController.activateOrder(order);

        RasOperation operation = new RasOperation(
                Operation.STOP,
                ResourceType.COMPUTE,
                DEFAULT_CLOUD_NAME,
                order
        );

        Mockito.doNothing().when(this.orderController).stopOrder(order);
        
        AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

        // checking that the order has a state and is not null
        Assert.assertNotNull(order.getOrderState());

        // exercise
        this.facade.stopOrder(localMemberId, order.getId(), systemUser, order.getType());

        // verify
        Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
        Mockito.verify(this.orderController).stopOrder(order);
	}
	
	// test case: When calling the getUserQuota method with valid parameters, it
	// must return the User Quota from that.
	@Test
	public void testRemoteGetUserQuotaSuccessfully() throws Exception {
		// set up
		SystemUser systemUser = createFederationUser();

		RasOperation operation = new RasOperation(
				Operation.GET,
				ResourceType.QUOTA,
				DEFAULT_CLOUD_NAME,
				FAKE_REQUESTER_ID, 
				FAKE_LOCALIDENTITY_PROVIDER
		);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		String cloudName = DEFAULT_CLOUD_NAME;

		CloudConnectorFactory factory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(factory);

		Quota expectedQuota = createComputeQuota();
		Mockito.doReturn(expectedQuota).when(cloudConnector).getUserQuota(systemUser);

		// exercise
		Quota quota = this.facade.getUserQuota(FAKE_REQUESTER_ID, cloudName, systemUser);

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Mockito.verify(cloudConnector, Mockito.times(1)).getUserQuota(Mockito.eq(systemUser)
		);

		Assert.assertSame(expectedQuota, quota);
		Assert.assertEquals(expectedQuota.getTotalQuota(), quota.getTotalQuota());
		Assert.assertEquals(expectedQuota.getUsedQuota(), quota.getUsedQuota());
	}

	// test case: Verifies getImage method behavior inside Remote Facade, i.e. it
	// needs to isAuthorized the request, and also get the correct cloud connector,
	// before getting an image.
	@Test
	public void testRemoteGetImageSuccessfully() throws Exception {
		// set up
		SystemUser systemUser = createFederationUser();

		RasOperation operation = new RasOperation(
				Operation.GET,
				ResourceType.IMAGE,
				DEFAULT_CLOUD_NAME,
				FAKE_REQUESTER_ID, 
				FAKE_LOCALIDENTITY_PROVIDER
		);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		ImageInstance imageInstance = Mockito.mock(ImageInstance.class);
		Mockito.when(cloudConnector.getImage(Mockito.anyString(), Mockito.eq(systemUser))).thenReturn(imageInstance);

		String cloudName = DEFAULT_CLOUD_NAME;
		String imageId = FAKE_IMAGE_ID;

		// exercise
		this.facade.getImage(FAKE_REQUESTER_ID, cloudName, imageId, systemUser);

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Mockito.verify(cloudConnector, Mockito.times(1)).getImage(Mockito.anyString(), Mockito.eq(systemUser));
	}

	// test case: Verifies getAllImage method behavior inside Remote Facade, i.e. it
	// needs to isAuthorized the request, and also get the correct cloud connector,
	// before getting all available images.
	@Test
	public void testRemoteGetAllImagesSuccessfully() throws Exception {
		// set up
		SystemUser systemUser = createFederationUser();

		RasOperation operation = new RasOperation(
				Operation.GET_ALL,
				ResourceType.IMAGE,
				DEFAULT_CLOUD_NAME,
				FAKE_REQUESTER_ID,
				FAKE_LOCALIDENTITY_PROVIDER
		);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		List<ImageSummary> imageSummaryList = new ArrayList<>();
		Mockito.when(cloudConnector.getAllImages(Mockito.eq(systemUser))).thenReturn(imageSummaryList);

		String cloudName = DEFAULT_CLOUD_NAME;

		// exercise
		this.facade.getAllImages(FAKE_REQUESTER_ID, cloudName, systemUser);

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Mockito.verify(cloudConnector, Mockito.times(1)).getAllImages(Mockito.eq(systemUser));
	}

	// test case: Verifies getCloudNames method behavior inside Remote Facade, i.e.
	// it needs to isAuthorized the request, and also get the correct cloud connector,
	// before getting a list of cloud names.
	@Test
	public void testGetCloudNamesSuccessfully() throws Exception {
		// set up
		SystemUser systemUser = createFederationUser();
		String requester = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
		
		RasOperation operation = new RasOperation(
				Operation.GET,
				ResourceType.CLOUD_NAME, 
				FAKE_REQUESTER_ID,
				requester
		);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		List<String> cloudNames = new ArrayList<>();

		CloudListController cloudListController = Mockito.mock(CloudListController.class);
		Mockito.doReturn(cloudNames).when(cloudListController).getCloudNames();
		this.facade.setCloudListController(cloudListController);

		// exercise
		this.facade.getCloudNames(FAKE_REQUESTER_ID, systemUser);

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Mockito.verify(cloudListController, Mockito.times(1)).getCloudNames();
	}

	// test case: Verifies createSecurityRule method behavior inside Remote Facade,
	// i.e. it needs to isAuthorized the request, and make the call the corresponding
	// method in the SecurityRuleController class, responsible for delegating to the
	// cloud connector the create rule request.
	@Test
	public void testCreateSecurityRuleSuccessfully() throws Exception {
		// set up
		SystemUser systemUser = createFederationUser();

		String cloudName = DEFAULT_CLOUD_NAME;
		String requester = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
		Order order = spyComputeOrder(systemUser, cloudName, requester);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.SECURITY_RULE,
				DEFAULT_CLOUD_NAME,
				order
		);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);

		SecurityRuleController securityRuleController = Mockito.mock(SecurityRuleController.class);
		Mockito.when(securityRuleController.createSecurityRule(Mockito.eq(order), Mockito.eq(securityRule),
				Mockito.eq(systemUser))).thenReturn(FAKE_INSTANCE_ID);
		this.facade.setSecurityRuleController(securityRuleController);

		// exercise
		this.facade.createSecurityRule(requester, order.getId(), securityRule, systemUser);

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Mockito.verify(securityRuleController, Mockito.times(1)).createSecurityRule(Mockito.eq(order),
				Mockito.eq(securityRule), Mockito.eq(systemUser));
	}

	// test case: Verifies getAllSecurityRules method behavior inside Remote Facade,
	// i.e. it needs to isAuthorized the request, and make the call the corresponding
	// method in the SecurityRuleController class, responsible for delegating to the
	// cloud connector the all rules request.
	@Test
	public void testGetAllSecurityRulesSuccessfully() throws Exception {
		// set up
		SystemUser systemUser = createFederationUser();

		String cloudName = DEFAULT_CLOUD_NAME;
		String requester = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
		Order order = spyComputeOrder(systemUser, cloudName, requester);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.GET_ALL,
				ResourceType.SECURITY_RULE,
				DEFAULT_CLOUD_NAME,
				order
		);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		SecurityRuleInstance securityRuleInstance = Mockito.mock(SecurityRuleInstance.class);
		List<SecurityRuleInstance> expectedSecurityRuleInstances = new ArrayList<>();
		expectedSecurityRuleInstances.add(securityRuleInstance);

		SecurityRuleController securityRuleController = Mockito.mock(SecurityRuleController.class);
		Mockito.when(securityRuleController.getAllSecurityRules(Mockito.eq(order), Mockito.eq(systemUser)))
				.thenReturn(expectedSecurityRuleInstances);
		this.facade.setSecurityRuleController(securityRuleController);

		// exercise
		List<SecurityRuleInstance> securityRuleInstances = this.facade.getAllSecurityRules(requester, order.getId(),
				systemUser);

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Mockito.verify(securityRuleController, Mockito.times(1)).getAllSecurityRules(Mockito.eq(order),
				Mockito.eq(systemUser));

		Assert.assertSame(expectedSecurityRuleInstances, securityRuleInstances);
	}

	// case test: Verifies deleteSecurityRule method behavior inside Remote Facade,
	// i.e. it needs to isAuthorized the request, and make the call the corresponding
	// method in the SecurityRuleController class, responsible for delegating to the
	// cloud connector the removing of the rule.
	@Test
	public void testdeleteSecurityRuleSuccessfully() throws Exception {
		// set up
		SystemUser systemUser = createFederationUser();

		RasOperation operation = new RasOperation(
				Operation.DELETE,
				ResourceType.SECURITY_RULE,
				DEFAULT_CLOUD_NAME, 
				FAKE_REQUESTER_ID,
				FAKE_LOCALIDENTITY_PROVIDER
		);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		String cloudName = DEFAULT_CLOUD_NAME;
		String provider = FAKE_LOCALIDENTITY_PROVIDER;
		Order order = spyComputeOrder(systemUser, cloudName, provider);
		this.orderController.activateOrder(order);

		SecurityRuleController securityRuleController = Mockito.mock(SecurityRuleController.class);
		Mockito.doNothing().when(securityRuleController).deleteSecurityRule(Mockito.anyString(), Mockito.eq(cloudName),
				Mockito.anyString(), Mockito.eq(systemUser));
		this.facade.setSecurityRuleController(securityRuleController);

		String ruleId = FAKE_RULE_ID;

		// exercise
		this.facade.deleteSecurityRule(FAKE_REQUESTER_ID, cloudName, ruleId, systemUser);

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Mockito.verify(securityRuleController, Mockito.times(1)).deleteSecurityRule(Mockito.anyString(),
				Mockito.eq(cloudName), Mockito.anyString(), Mockito.eq(systemUser));
	}

	// test case: When calling the closeOrderAtRemoteRequester method from a pending
	// remote order, it must close order.
	@Test
	public void testCloseOrderAtRemoteRequester() throws Exception {
		// set up
		OrderState orderState = OrderState.CLOSED;

		String signallingMember = TestUtils.FAKE_REMOTE_MEMBER_ID;

		String provider = signallingMember;
		String requester = TestUtils.LOCAL_MEMBER_ID;
		Order remoteOrder = new ComputeOrder();
		remoteOrder.setRequester(requester);
		remoteOrder.setProvider(provider);
		remoteOrder.setOrderState(OrderState.PENDING);

		this.orderController.activateOrder(remoteOrder);

		// exercise
		this.facade.closeOrderAtRemoteRequester(signallingMember, remoteOrder.getId());

		// verify
		Order activeOrder = SharedOrderHolders.getInstance().getActiveOrdersMap().get(remoteOrder.getId());
		Assert.assertNull(activeOrder);
	}

	// test case: When calling the handleRemoteEvent method with a provider
	// different from the ordering provider, it must throw an InternalServerErrorException.
	@Test(expected = InternalServerErrorException.class) // verify
	public void testRemoteHandleRemoteEvent() throws Exception {
		// set up
		OrderState orderState = OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST;

		String signallingMember = FAKE_REQUESTER_ID;
		String requester = signallingMember;
		String provider = TestUtils.LOCAL_MEMBER_ID;

		Order remoteOrder = new ComputeOrder();
		remoteOrder.setRequester(requester);
		remoteOrder.setProvider(provider);
		remoteOrder.setOrderState(cloud.fogbow.ras.core.models.orders.OrderState.PENDING);

		this.orderController.activateOrder(remoteOrder);

		// exercise
		this.facade.closeOrderAtRemoteRequester(signallingMember, remoteOrder.getId());
	}

	// test case: When calling the reload method, it must recreate the AuthorizationPlugin
	// and the CloudListController
	@Test
	public void testReload() {
	    PowerMockito.mockStatic(AuthorizationPluginInstantiator.class);
	    AuthorizationPlugin authorizationPlugin = Mockito.mock(AuthorizationPlugin.class);
	    PowerMockito.when(AuthorizationPluginInstantiator.getAuthorizationPlugin(Mockito.anyString())).thenReturn(authorizationPlugin);
	    
	    String cloudNames = "cloud";
	    String authorizationPluginName = "plugin";
	    
        // set up PropertiesHolder 
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.doReturn(cloudNames).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.CLOUD_NAMES_KEY);     
        Mockito.doReturn(authorizationPluginName).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.AUTHORIZATION_PLUGIN_CLASS_KEY);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
	    
        // used on CloudListController instantiation
        CloudConnectorFactory factory = mockCloudConnectorFactory();
        mockCloudConnector(factory);
        
	    this.facade.reload();
	    
	    PowerMockito.verifyStatic(AuthorizationPluginInstantiator.class, Mockito.times(TestUtils.RUN_ONCE));
	}
	
	// test case: the number of ongoing operations, changed through 
	// calls to startOperation and finishOperation, must be reflected 
	// properly on the return value of the method noOnGoingRequests
	@Test
	public void testStartAndFinishOperation() {
	    PowerMockito.mockStatic(SynchronizationManager.class);
	    SynchronizationManager synchronizationManager = Mockito.mock(SynchronizationManager.class);
	    Mockito.doReturn(false).when(synchronizationManager).isReloading();
        PowerMockito.when(SynchronizationManager.getInstance()).thenReturn(synchronizationManager);
        
        // At the beginning, no on going requests
	    Assert.assertTrue(this.facade.noOnGoingRequests());
	    
	    // noOnGoingRequests return must reflect the new request
	    this.facade.startOperation();
	    Assert.assertFalse(this.facade.noOnGoingRequests());
	    
	    this.facade.startOperation();
	    Assert.assertFalse(this.facade.noOnGoingRequests());
	    
	    // finishing just one operation is not enough to 
	    this.facade.finishOperation();
	    Assert.assertFalse(this.facade.noOnGoingRequests());
	    
	    this.facade.finishOperation();
	    
	    Assert.assertTrue(this.facade.noOnGoingRequests());
	}
	
	private AuthorizationPlugin mockAuthorizationPlugin(SystemUser systemUser, RasOperation operation)
			throws InternalServerErrorException, UnauthorizedRequestException {
		AuthorizationPlugin<RasOperation> authorization = Mockito.mock(DefaultAuthorizationPlugin.class);

		Mockito.when(authorization.isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation))).thenReturn(true);

		this.facade.setAuthorizationPlugin(authorization);
		return authorization;
	}

	private Order spyComputeOrder(SystemUser systemUser, String cloudName, String provider) {
    	String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
		Order order = Mockito.spy(new ComputeOrder());
		order.setSystemUser(systemUser);
		order.setRequester(localMemberId);
		order.setProvider(provider);
		order.setCloudName(cloudName);
		return order;
	}

	private SystemUser createFederationUser() {
		return new SystemUser(FAKE_REQUESTER_USER_ID_VALUE, FAKE_REQUESTER_USER_ID_VALUE, null);
	}

	private CloudConnector mockCloudConnector(CloudConnectorFactory cloudConnectorFactory) {
		CloudConnector cloudConnector = Mockito.mock(CloudConnector.class);
		Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(cloudConnector);
		return cloudConnector;
	}

	private CloudConnectorFactory mockCloudConnectorFactory() {
		PowerMockito.mockStatic(CloudConnectorFactory.class);
		CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
		PowerMockito.when(CloudConnectorFactory.getInstance()).thenReturn(cloudConnectorFactory);
		return cloudConnectorFactory;
	}
	
	private Quota createComputeQuota() {
		ComputeAllocation totalQuota = new ComputeAllocation(2, 8, 2048);
        ComputeAllocation usedQuota = new ComputeAllocation(1, 4, 1024);

        Quota quota = new ComputeQuota(totalQuota, usedQuota);
		return quota;
	}	

}
