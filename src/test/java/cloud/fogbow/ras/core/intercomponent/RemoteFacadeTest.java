package cloud.fogbow.ras.core.intercomponent;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
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
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.Instance;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.common.util.connectivity.FogbowGenericRequest;
import cloud.fogbow.common.util.connectivity.HttpRequest;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.plugins.authorization.DefaultAuthorizationPlugin;
import org.jamppa.component.PacketSender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.xmpp.packet.IQ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudConnectorFactory.class, DatabaseManager.class, PacketSenderHolder.class})
public class RemoteFacadeTest extends BaseUnitTests {

	private static final String DEFAULT_CLOUD_NAME = "default";
	private static final String FAKE_CONTENT = "fooBar";
	private static final String FAKE_IMAGE_ID = "fake-image-id";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_LOCAL_IDENTITY_MEMBER = "fake-localidentity-provider";
	private static final String FAKE_OWNER_USER_ID_VALUE = "fake-owner-user-id";
    private static final String FAKE_REQUESTER_USER_ID_VALUE = "fake-requester-user-id";
    private static final String FAKE_REQUESTING_MEMBER_ID = "fake-requesting-provider-id";
    private static final String FAKE_URL = "https://www.foo.bar";
	private static final String FAKE_RULE_ID = "fake-rule-id";

	private RemoteFacade facade;
	private OrderController orderController;

    @Before
    public void setUp() throws UnexpectedException {
        super.mockReadOrdersFromDataBase();
        this.orderController = new OrderController();
        this.facade = Mockito.spy(RemoteFacade.getInstance());
        this.facade.setOrderController(this.orderController);
    }

	// test case: When calling the activateOrder method with a requesting member
	// different of the order requester, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class) // verify
	public void testRemoteActivateOrderThrowsInvalidParameterException() throws Exception {
		// set up
		Order order = new ComputeOrder();
		order.setRequester(FAKE_LOCAL_IDENTITY_MEMBER);
		order.setProvider(FAKE_LOCAL_IDENTITY_MEMBER);

		// exercise
		this.facade.activateOrder(FAKE_REQUESTING_MEMBER_ID, order);
	}

	// test case: When calling the activateOrder method with a providing member
	// different of the orders provider, it must throw an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testRemoteActivateOrderThrowsInstanceNotFoundException() throws Exception {
		// set up
		Order order = new ComputeOrder();
		order.setRequester(FAKE_REQUESTING_MEMBER_ID);
		order.setProvider(FAKE_REQUESTING_MEMBER_ID);

		// exercise
		this.facade.activateOrder(FAKE_REQUESTING_MEMBER_ID, order);
	}

	// test case: When calling the activateOrder method a new Order passed by
	// parameter, it must return to Open OrderState after its activation.
	@Test
	public void testRemoteActivateOrderSuccessfully() throws FogbowException {
		// set up
		SystemUser systemUser = createFederationUser();

		String cloudName = DEFAULT_CLOUD_NAME;
		String requester = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);
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
		this.facade.getResourceInstance(FAKE_REQUESTING_MEMBER_ID, order.getId(), systemUser, order.getType());
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
		String requester = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);
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
		Mockito.when(authorization.isAuthorized(Mockito.eq(systemUser), Mockito.eq(new RasOperation(Operation.GET, ResourceType.COMPUTE)))).thenReturn(true);

		this.facade.setAuthorizationPlugin(authorization);

		String cloudName = DEFAULT_CLOUD_NAME;
		String provider = FAKE_LOCAL_IDENTITY_MEMBER;
		Order order = spyComputeOrder(systemUser, cloudName, provider);

		CloudConnectorFactory factory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(factory);

		Mockito.doThrow(new InstanceNotFoundException()).when(cloudConnector).deleteInstance(Mockito.eq(order));

		// exercise
		this.facade.deleteOrder(FAKE_REQUESTING_MEMBER_ID, order.getId(), systemUser, order.getType());
	}

	// test case: When calling the deleteOrder method with an Order passed per
	// parameter, it must return its OrderState to Closed.
	@Test
	public void testRemoteDeleteOrderSuccessfully() throws Exception {
		// set up
		String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);
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
		OrderState expectedOrderState = OrderState.CLOSED;

		// exercise
		this.facade.deleteOrder(localMemberId, order.getId(), systemUser, order.getType());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getUserQuota method with valid parameters, it
	// must return the User Quota from that.
	@Test
	public void testRemoteGetUserQuotaSuccessfully() throws Exception {
		// set up
		SystemUser systemUser = createFederationUser();

		RasOperation operation = new RasOperation(
				Operation.GET_USER_QUOTA,
				ResourceType.COMPUTE,
				DEFAULT_CLOUD_NAME
		);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		String cloudName = DEFAULT_CLOUD_NAME;
		ResourceType resourceType = ResourceType.COMPUTE;

		CloudConnectorFactory factory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(factory);

		Quota expectedQuota = createComputeQuota();
		Mockito.doReturn(expectedQuota).when(cloudConnector).getUserQuota(systemUser, resourceType);

		// exercise
		Quota quota = this.facade.getUserQuota(FAKE_REQUESTING_MEMBER_ID, cloudName, systemUser, resourceType);

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Mockito.verify(cloudConnector, Mockito.times(1)).getUserQuota(Mockito.eq(systemUser),
				Mockito.eq(resourceType));

		Assert.assertSame(expectedQuota, quota);
		Assert.assertEquals(expectedQuota.getTotalQuota(), quota.getTotalQuota());
		Assert.assertEquals(expectedQuota.getUsedQuota(), quota.getUsedQuota());
	}

	// test case: Verifies generic request behavior inside Remote Facade, i.e. it
	// needs to isAuthorized the request, and also get the correct cloud connector,
	// before passing a generic request.
	@Test
	public void testGenericRequestSuccessfully() throws Exception {
		// set up
		SystemUser systemUser = createFederationUser();

		HttpMethod method = HttpMethod.GET;
		String url = FAKE_URL;
		HashMap<String, String> headers = new HashMap<>();
		HashMap<String, String> body = new HashMap<>();
		FogbowGenericRequest fogbowGenericRequest = new HttpRequest(method, url, body, headers);
		String serializedGenericRequest = GsonHolder.getInstance().toJson(fogbowGenericRequest);

		String responseContent = FAKE_CONTENT;
		FogbowGenericResponse expectedResponse = new FogbowGenericResponse(responseContent);

		RasOperation operation = new RasOperation(
				Operation.GENERIC_REQUEST,
				ResourceType.GENERIC_RESOURCE,
				DEFAULT_CLOUD_NAME,
				serializedGenericRequest
		);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		CloudConnectorFactory factory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(factory);

		Mockito.when(cloudConnector.genericRequest(Mockito.eq(serializedGenericRequest), Mockito.eq(systemUser)))
				.thenReturn(expectedResponse);

		String cloudName = DEFAULT_CLOUD_NAME;

		// exercise
		FogbowGenericResponse fogbowGenericResponse = facade.genericRequest(FAKE_REQUESTING_MEMBER_ID, cloudName,
                serializedGenericRequest, systemUser);

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Mockito.verify(cloudConnector, Mockito.times(1)).genericRequest(Mockito.eq(serializedGenericRequest),
				Mockito.eq(systemUser));

		Assert.assertEquals(expectedResponse, fogbowGenericResponse);
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
				DEFAULT_CLOUD_NAME
		);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		ImageInstance imageInstance = Mockito.mock(ImageInstance.class);
		Mockito.when(cloudConnector.getImage(Mockito.anyString(), Mockito.eq(systemUser))).thenReturn(imageInstance);

		String cloudName = DEFAULT_CLOUD_NAME;
		String imageId = FAKE_IMAGE_ID;

		// exercise
		this.facade.getImage(FAKE_REQUESTING_MEMBER_ID, cloudName, imageId, systemUser);

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
				DEFAULT_CLOUD_NAME
		);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		Map<String, String> images = new HashMap<>();
		Mockito.when(cloudConnector.getAllImages(Mockito.eq(systemUser))).thenReturn(images);

		String cloudName = DEFAULT_CLOUD_NAME;

		// exercise
		this.facade.getAllImages(FAKE_REQUESTING_MEMBER_ID, cloudName, systemUser);

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

		RasOperation operation = new RasOperation(
				Operation.GET,
				ResourceType.CLOUD_NAMES
		);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		List<String> cloudNames = new ArrayList<>();

		CloudListController cloudListController = Mockito.mock(CloudListController.class);
		Mockito.doReturn(cloudNames).when(cloudListController).getCloudNames();
		this.facade.setCloudListController(cloudListController);

		// exercise
		this.facade.getCloudNames(FAKE_REQUESTING_MEMBER_ID, systemUser);

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
		String requester = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);
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
		String requester = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);
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
				DEFAULT_CLOUD_NAME
		);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		String cloudName = DEFAULT_CLOUD_NAME;
		String provider = FAKE_LOCAL_IDENTITY_MEMBER;
		Order order = spyComputeOrder(systemUser, cloudName, provider);
		this.orderController.activateOrder(order);

		SecurityRuleController securityRuleController = Mockito.mock(SecurityRuleController.class);
		Mockito.doNothing().when(securityRuleController).deleteSecurityRule(Mockito.anyString(), Mockito.eq(cloudName),
				Mockito.anyString(), Mockito.eq(systemUser));
		this.facade.setSecurityRuleController(securityRuleController);

		String ruleId = FAKE_RULE_ID;

		// exercise
		this.facade.deleteSecurityRule(FAKE_REQUESTING_MEMBER_ID, cloudName, ruleId, systemUser);

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Mockito.verify(securityRuleController, Mockito.times(1)).deleteSecurityRule(Mockito.anyString(),
				Mockito.eq(cloudName), Mockito.anyString(), Mockito.eq(systemUser));
	}

	// test case: When calling the isAuthorizedOrder method with a different resource
	// type of the order, it must throw a InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testisAuthorizedOrderThrowsInstanceNotFoundException() throws Exception {
		// set up
		SystemUser systemUser = null;
		String cloudName = null;
		Operation operation = null;
		ResourceType resourceType = ResourceType.VOLUME;
		ComputeOrder order = new ComputeOrder();

		// exercise
		this.facade.authorizeOrder(systemUser, cloudName, operation, resourceType, order);
	}

	// test case: When calling the isAuthorizedOrder method with a federation user
	// different of the order requester, it must throw an
	// UnisAuthorizeddRequestException.
	@Test(expected = UnauthorizedRequestException.class) // verify
	public void testisAuthorizedOrderThrowsUnisAuthorizeddRequestException() throws Exception {
		// set up
		SystemUser ownerUser = new SystemUser(FAKE_OWNER_USER_ID_VALUE, FAKE_OWNER_USER_ID_VALUE, null
        );

		SystemUser requesterUser = new SystemUser(FAKE_REQUESTER_USER_ID_VALUE, FAKE_REQUESTER_USER_ID_VALUE, null
        );

		String cloudName = null;
		String provider = null;

		Order order = spyComputeOrder(requesterUser, cloudName, provider);

		Operation operation = null;
		ResourceType resourceType = ResourceType.COMPUTE;

		// exercise
		this.facade.authorizeOrder(ownerUser, cloudName, operation, resourceType, order);
	}

	// test case: When calling the RemoteHandleRemoteEvent method from a pending
	// remote order, it must return its OrderState FULFILLED, based on the match of
	// the event passed by parameter.
	@Test
	public void testRemoteHandleRemoteEventWithInstanceFulfilled() throws Exception {
		// set up
		OrderState orderState = OrderState.FULFILLED;

		String signallingMember = LOCAL_MEMBER_ID;
		String requester = FAKE_REQUESTING_MEMBER_ID;
		String provider = signallingMember;

		Order remoteOrder = new ComputeOrder();
		remoteOrder.setRequester(requester);
		remoteOrder.setProvider(provider);
		remoteOrder.setOrderState(cloud.fogbow.ras.core.models.orders.OrderState.PENDING);

		this.orderController.activateOrder(remoteOrder);

		IQ response = Mockito.mock(IQ.class);
		PacketSender packetSender = Mockito.mock(PacketSender.class);
		PowerMockito.mockStatic(PacketSenderHolder.class);
		Mockito.when(PacketSenderHolder.getPacketSender()).thenReturn(packetSender);
		Mockito.when(packetSender.syncSendPacket(Mockito.any(IQ.class))).thenReturn(response);

		SharedOrderHolders ordersHolder = SharedOrderHolders.getInstance();
		SynchronizedDoublyLinkedList<Order> origin = ordersHolder.getOrdersList(cloud.fogbow.ras.core.models.orders.OrderState.PENDING);
		origin.addItem(remoteOrder);

		// exercise
		this.facade.handleRemoteEvent(signallingMember, orderState, remoteOrder);

		// verify
		cloud.fogbow.ras.core.models.orders.OrderState expectedOrderState = cloud.fogbow.ras.core.models.orders.OrderState.FULFILLED;
		Assert.assertEquals(expectedOrderState, remoteOrder.getOrderState());
	}

	// test case: When calling the RemoteHandleRemoteEvent method from a pending
	// remote order, it must return its OrderState FAILED_AFTER_SUCCESSFUL_REQUEST,
	// based on the match of the event passed by parameter.
	@Test
	public void testRemoteHandleRemoteEventWithInstanceFailed() throws Exception {
		// set up
		OrderState orderState = OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST;

		String signallingMember = LOCAL_MEMBER_ID;
		String requester = FAKE_REQUESTING_MEMBER_ID;
		String provider = signallingMember;

		Order remoteOrder = new ComputeOrder();
		remoteOrder.setRequester(requester);
		remoteOrder.setProvider(provider);
		remoteOrder.setOrderState(cloud.fogbow.ras.core.models.orders.OrderState.PENDING);

		this.orderController.activateOrder(remoteOrder);

		IQ iqResponse = Mockito.mock(IQ.class);
		PacketSender packetSender = Mockito.mock(PacketSender.class);
		PowerMockito.mockStatic(PacketSenderHolder.class);
		Mockito.when(PacketSenderHolder.getPacketSender()).thenReturn(packetSender);
		Mockito.when(packetSender.syncSendPacket(Mockito.any(IQ.class))).thenReturn(iqResponse);

		SharedOrderHolders ordersHolder = SharedOrderHolders.getInstance();
		SynchronizedDoublyLinkedList<Order> origin = ordersHolder.getOrdersList(cloud.fogbow.ras.core.models.orders.OrderState.PENDING);
		origin.addItem(remoteOrder);

		// exercise
		this.facade.handleRemoteEvent(signallingMember, orderState, remoteOrder);

		// verify
		cloud.fogbow.ras.core.models.orders.OrderState expectedOrderState = cloud.fogbow.ras.core.models.orders.OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST;
		Assert.assertEquals(expectedOrderState, remoteOrder.getOrderState());
	}

	// test case: When calling the handleRemoteEvent method with a provider
	// different from the ordering provider, it must throw an UnexpectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testRemoteHandleRemoteEvent() throws Exception {
		// set up
		OrderState orderState = OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST;

		String signallingMember = FAKE_REQUESTING_MEMBER_ID;
		String requester = signallingMember;
		String provider = LOCAL_MEMBER_ID;

		Order remoteOrder = new ComputeOrder();
		remoteOrder.setRequester(requester);
		remoteOrder.setProvider(provider);
		remoteOrder.setOrderState(cloud.fogbow.ras.core.models.orders.OrderState.PENDING);

		this.orderController.activateOrder(remoteOrder);

		// exercise
		this.facade.handleRemoteEvent(signallingMember, orderState, remoteOrder);
	}

	private AuthorizationPlugin mockAuthorizationPlugin(SystemUser systemUser, RasOperation operation)
			throws UnexpectedException, UnauthorizedRequestException {
		AuthorizationPlugin<RasOperation> authorization = Mockito.mock(DefaultAuthorizationPlugin.class);

		Mockito.when(authorization.isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation))).thenReturn(true);

		this.facade.setAuthorizationPlugin(authorization);
		return authorization;
	}

	private Order spyComputeOrder(SystemUser systemUser, String cloudName, String provider) {
    	String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);
		Order order = Mockito.spy(new ComputeOrder());
		order.setSystemUser(systemUser);
		order.setRequester(localMemberId);
		order.setProvider(provider);
		order.setCloudName(cloudName);
		return order;
	}

	private SystemUser createFederationUser() {
		return new SystemUser(FAKE_REQUESTER_USER_ID_VALUE, FAKE_REQUESTER_USER_ID_VALUE, null
        );
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
		ComputeAllocation totalQuota = new ComputeAllocation(8, 2048, 2);
        ComputeAllocation usedQuota = new ComputeAllocation(4, 1024, 1);

        Quota quota = new ComputeQuota(totalQuota, usedQuota);
		return quota;
	}	

}
