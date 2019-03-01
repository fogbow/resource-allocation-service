package cloud.fogbow.ras.core.intercomponent;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationController;
import cloud.fogbow.common.util.connectivity.GenericRequestResponse;
import cloud.fogbow.ras.core.*;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.intercomponent.xmpp.Event;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.api.http.response.Image;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.Instance;
import cloud.fogbow.ras.core.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.Quota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.api.http.response.securityrules.SecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
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
    private static final String FAKE_LOCAL_IDENTITY_MEMBER = "fake-localidentity-member";
	private static final String FAKE_OWNER_USER_ID_VALUE = "fake-owner-user-id";
    private static final String FAKE_REQUESTER_USER_ID_VALUE = "fake-requester-user-id";
    private static final String FAKE_REQUESTING_MEMBER_ID = "fake-requesting-member-id";
    private static final String FAKE_URL = "https://www.foo.bar";
	private static final String ID_KEY = "id";
	private static final String FAKE_RULE_ID = "fake-rule-id";

	private RemoteFacade facade;
    private OrderController orderController;

    @Before
    public void setUp() throws UnexpectedException {
        super.mockReadOrdersFromDataBase();
        this.orderController = Mockito.spy(new OrderController());
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
		FederationUser federationUser = createFederationUser();
		AuthorizationController authorization = mockAuthorizationController(federationUser);
		
		String cloudName = DEFAULT_CLOUD_NAME;
		String provider = FAKE_LOCAL_IDENTITY_MEMBER;
		Order order = spyComputeOrder(federationUser, cloudName, provider);

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.activateOrder(FAKE_REQUESTING_MEMBER_ID, order);

		// verify
		String operation = Operation.CREATE.getValue();
		String resourceType = ResourceType.COMPUTE.getValue();
		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.eq(cloudName),
				Mockito.eq(operation), Mockito.eq(resourceType));
		
		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}
	
	// test case: When calling the getResourceInstance method and the instance of the
	// requested order is not found, it must throw an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testRemoteGetResourceInstanceThrowsInstanceNotFoundException() throws Exception {
		// set up
		FederationUser federationUser = createFederationUser();
		Order order = new ComputeOrder();

		// exercise
		this.facade.getResourceInstance(FAKE_REQUESTING_MEMBER_ID, order.getId(), federationUser, order.getType());
	}

	// test case: When calling the getResourceInstance method, it must return an
	// Instance of the Order ID passed per parameter.
	@Test
	public void testRemoteGetResourceInstanceSuccessfully() throws Exception {
		// set up
		FederationUser federationUser = createFederationUser();
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		String cloudName = DEFAULT_CLOUD_NAME;
		String provider = FAKE_LOCAL_IDENTITY_MEMBER;
		Order order = spyComputeOrder(federationUser, cloudName, provider);
		OrderStateTransitioner.activateOrder(order);

		Instance exceptedInstance = new ComputeInstance(FAKE_INSTANCE_ID);
		Mockito.doReturn(exceptedInstance).when(this.orderController).getResourceInstance(Mockito.anyString());

		// exercise
		Instance instance = this.facade.getResourceInstance(FAKE_REQUESTING_MEMBER_ID, order.getId(), federationUser,
				order.getType());

		// verify
		String operation = Operation.GET.getValue();
		String resourceType = ResourceType.COMPUTE.getValue();
		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.eq(cloudName),
				Mockito.eq(operation), Mockito.eq(resourceType));

		Mockito.verify(this.orderController, Mockito.times(1)).getResourceInstance(Mockito.eq(order.getId()));

		Assert.assertSame(exceptedInstance, instance);
	}	

	// test case: When calling the deleteOrder method and the instance of the
	// requested order is not found, it must throw an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testRemoteDeleteOrderThrowsInstanceNotFoundException() throws Exception {
		// set up
		FederationUser federationUser = createFederationUser();

		AuthorizationController authorization = Mockito.mock(AuthorizationController.class);
		Mockito.doNothing().when(authorization).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		this.facade.setAuthorizationController(authorization);

		String cloudName = DEFAULT_CLOUD_NAME;
		String provider = FAKE_LOCAL_IDENTITY_MEMBER;
		Order order = spyComputeOrder(federationUser, cloudName, provider);

		CloudConnectorFactory factory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(factory);

		Mockito.doThrow(new InstanceNotFoundException()).when(cloudConnector).deleteInstance(Mockito.eq(order));

		// exercise
		this.facade.deleteOrder(FAKE_REQUESTING_MEMBER_ID, order.getId(), federationUser, order.getType());
	}

	// test case: When calling the deleteOrder method with an Order passed per
	// parameter, it must return its OrderState to Closed.
	@Test
	public void testRemoteDeleteOrderSuccessfully() throws Exception {
		// set up
		FederationUser federationUser = createFederationUser();
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		String cloudName = DEFAULT_CLOUD_NAME;
		String provider = FAKE_LOCAL_IDENTITY_MEMBER;
		Order order = spyComputeOrder(federationUser, cloudName, provider);
		OrderStateTransitioner.activateOrder(order);

		// checking that the order has a state and is not null
		Assert.assertNotNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.CLOSED;

		// exercise
		this.facade.deleteOrder(FAKE_REQUESTING_MEMBER_ID, order.getId(), federationUser, order.getType());

		// verify
		String operation = Operation.DELETE.getValue();
		String resourceType = ResourceType.COMPUTE.getValue();
		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.eq(cloudName),
				Mockito.eq(operation), Mockito.eq(resourceType));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}
	
	// test case: When calling the getUserQuota method with valid parameters, it
	// must return the User Quota from that.
	@Test
	public void testRemoteGetUserQuotaSuccessfully() throws Exception {
		// set up
		FederationUser federationUser = createFederationUser();
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		String cloudName = DEFAULT_CLOUD_NAME;
		ResourceType resourceType = ResourceType.COMPUTE;

		CloudConnectorFactory factory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(factory);

		Quota expectedQuota = createComputeQuota();
		Mockito.doReturn(expectedQuota).when(cloudConnector).getUserQuota(federationUser, resourceType);

		// exercise
		Quota quota = this.facade.getUserQuota(FAKE_REQUESTING_MEMBER_ID, cloudName, federationUser, resourceType);

		// verify
		String operation = Operation.GET_USER_QUOTA.getValue();
		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.eq(cloudName),
				Mockito.eq(operation), Mockito.eq(resourceType.getValue()));

		Mockito.verify(cloudConnector, Mockito.times(1)).getUserQuota(Mockito.eq(federationUser),
				Mockito.eq(resourceType));

		Assert.assertSame(expectedQuota, quota);
		Assert.assertEquals(expectedQuota.getTotalQuota(), quota.getTotalQuota());
		Assert.assertEquals(expectedQuota.getUsedQuota(), quota.getUsedQuota());
	}	
	
	// test case: Verifies generic request behavior inside Remote Facade, i.e. it
	// needs to authorize the request, and also get the correct cloud connector,
	// before passing a generic request.
	@Test
	public void testGenericRequestSuccessfully() throws Exception {
		// set up
		FederationUser federationUser = createFederationUser();
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		HttpMethod method = HttpMethod.GET;
		String url = FAKE_URL;
		HashMap<String, String> headers = new HashMap<>();
		HashMap<String, String> body = new HashMap<>();
		GenericRequest genericRequest = new GenericRequest(method, url, body, headers);

		String responseContent = FAKE_CONTENT;
		GenericRequestResponse expectedResponse = new GenericRequestResponse(responseContent);

		CloudConnectorFactory factory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(factory);

		Mockito.when(cloudConnector.genericRequest(Mockito.eq(genericRequest), Mockito.eq(federationUser)))
				.thenReturn(expectedResponse);

		String cloudName = DEFAULT_CLOUD_NAME;

		// exercise
		GenericRequestResponse genericRequestResponse = facade.genericRequest(FAKE_REQUESTING_MEMBER_ID, cloudName,
				genericRequest, federationUser);

		// verify
		String operation = Operation.GENERIC_REQUEST.getValue();
		String resourceType = ResourceType.GENERIC_RESOURCE.getValue();
		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.eq(cloudName),
				Mockito.eq(operation), Mockito.eq(resourceType));

		Mockito.verify(cloudConnector, Mockito.times(1)).genericRequest(Mockito.eq(genericRequest),
				Mockito.eq(federationUser));

		Assert.assertEquals(expectedResponse, genericRequestResponse);
	}

	// test case: Verifies getImage method behavior inside Remote Facade, i.e. it
	// needs to authorize the request, and also get the correct cloud connector,
	// before getting an image.
	@Test
	public void testRemoteGetImageSuccessfully() throws Exception {
		// set up
		FederationUser federationUser = createFederationUser();
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		Image image = Mockito.mock(Image.class);
		Mockito.when(cloudConnector.getImage(Mockito.anyString(), Mockito.eq(federationUser))).thenReturn(image);

		String cloudName = DEFAULT_CLOUD_NAME;
		String imageId = FAKE_IMAGE_ID;

		// exercise
		this.facade.getImage(FAKE_REQUESTING_MEMBER_ID, cloudName, imageId, federationUser);

		// verify
		String operation = Operation.GET.getValue();
		String resourceType = ResourceType.IMAGE.getValue();
		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.eq(cloudName),
				Mockito.eq(operation), Mockito.eq(resourceType));

		Mockito.verify(cloudConnector, Mockito.times(1)).getImage(Mockito.anyString(), Mockito.eq(federationUser));
	}

	// test case: Verifies getAllImage method behavior inside Remote Facade, i.e. it
	// needs to authorize the request, and also get the correct cloud connector,
	// before getting all available images.
	@Test
	public void testRemoteGetAllImagesSuccessfully() throws Exception {
		// set up
		FederationUser federationUser = createFederationUser();
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		Map<String, String> images = new HashMap<>();
		Mockito.when(cloudConnector.getAllImages(Mockito.eq(federationUser))).thenReturn(images);

		String cloudName = DEFAULT_CLOUD_NAME;

		// exercise
		this.facade.getAllImages(FAKE_REQUESTING_MEMBER_ID, cloudName, federationUser);

		// verify
		String operation = Operation.GET_ALL.getValue();
		String resourceType = ResourceType.IMAGE.getValue();
		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.eq(cloudName),
				Mockito.eq(operation), Mockito.eq(resourceType));

		Mockito.verify(cloudConnector, Mockito.times(1)).getAllImages(Mockito.eq(federationUser));
	}
	
	// test case: Verifies getCloudNames method behavior inside Remote Facade, i.e.
	// it needs to authorize the request, and also get the correct cloud connector,
	// before getting a list of cloud names.
	@Test
	public void testGetCloudNamesSuccessfully() throws Exception {
		// set up
		FederationUser federationUser = createFederationUser();
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		List<String> cloudNames = new ArrayList<>();

		CloudListController cloudListController = Mockito.mock(CloudListController.class);
		Mockito.doReturn(cloudNames).when(cloudListController).getCloudNames();
		this.facade.setCloudListController(cloudListController);

		// exercise
		this.facade.getCloudNames(FAKE_REQUESTING_MEMBER_ID, federationUser);

		// verify
		String operation = Operation.GET.getValue();
		String resourceType = ResourceType.CLOUD_NAMES.getValue();
		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.eq(operation),
				Mockito.eq(resourceType));

		Mockito.verify(cloudListController, Mockito.times(1)).getCloudNames();
	}
	
	// test case: Verifies createSecurityRule method behavior inside Remote Facade,
	// i.e. it needs to authorize the request, and make the call the corresponding
	// method in the SecurityRuleController class, responsible for delegating to the
	// cloud connector the create rule request.
	@Test
	public void testCreateSecurityRuleSuccessfully() throws Exception {
		// set up
		FederationUser federationUser = createFederationUser();
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		String cloudName = DEFAULT_CLOUD_NAME;
		String provider = FAKE_LOCAL_IDENTITY_MEMBER;
		Order order = spyComputeOrder(federationUser, cloudName, provider);
		OrderStateTransitioner.activateOrder(order);

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);

		SecurityRuleController securityRuleController = Mockito.mock(SecurityRuleController.class);
		Mockito.when(securityRuleController.createSecurityRule(Mockito.eq(order), Mockito.eq(securityRule),
				Mockito.eq(federationUser))).thenReturn(FAKE_INSTANCE_ID);
		this.facade.setSecurityRuleController(securityRuleController);

		// exercise
		this.facade.createSecurityRule(FAKE_REQUESTING_MEMBER_ID, order.getId(), securityRule, federationUser);

		// verify
		String operation = Operation.CREATE.getValue();
		String resourceType = ResourceType.SECURITY_RULE.getValue();
		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.eq(cloudName),
				Mockito.eq(operation), Mockito.eq(resourceType));

		Mockito.verify(securityRuleController, Mockito.times(1)).createSecurityRule(Mockito.eq(order),
				Mockito.eq(securityRule), Mockito.eq(federationUser));
	}
	
	// test case: Verifies getAllSecurityRules method behavior inside Remote Facade,
	// i.e. it needs to authorize the request, and make the call the corresponding
	// method in the SecurityRuleController class, responsible for delegating to the
	// cloud connector the all rules request.
	@Test
	public void testGetAllSecurityRulesSuccessfully() throws Exception {
		// set up
		FederationUser federationUser = createFederationUser();
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		String cloudName = DEFAULT_CLOUD_NAME;
		String provider = FAKE_LOCAL_IDENTITY_MEMBER;
		Order order = spyComputeOrder(federationUser, cloudName, provider);
		OrderStateTransitioner.activateOrder(order);

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);
		List<SecurityRule> expectedSecurityRules = new ArrayList<>();
		expectedSecurityRules.add(securityRule);

		SecurityRuleController securityRuleController = Mockito.mock(SecurityRuleController.class);
		Mockito.when(securityRuleController.getAllSecurityRules(Mockito.eq(order), Mockito.eq(federationUser)))
				.thenReturn(expectedSecurityRules);
		this.facade.setSecurityRuleController(securityRuleController);

		// exercise
		List<SecurityRule> securityRules = this.facade.getAllSecurityRules(FAKE_REQUESTING_MEMBER_ID, order.getId(),
				federationUser);

		// verify
		String operation = Operation.GET_ALL.getValue();
		String resourceType = ResourceType.SECURITY_RULE.getValue();
		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.eq(cloudName),
				Mockito.eq(operation), Mockito.eq(resourceType));

		Mockito.verify(securityRuleController, Mockito.times(1)).getAllSecurityRules(Mockito.eq(order),
				Mockito.eq(federationUser));

		Assert.assertSame(expectedSecurityRules, securityRules);
	}
	
	// case test: Verifies deleteSecurityRule method behavior inside Remote Facade,
	// i.e. it needs to authorize the request, and make the call the corresponding
	// method in the SecurityRuleController class, responsible for delegating to the
	// cloud connector the removing of the rule.
	@Test
	public void testdeleteSecurityRuleSuccessfully() throws Exception {
		// set up
		FederationUser federationUser = createFederationUser();
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		String cloudName = DEFAULT_CLOUD_NAME;
		String provider = FAKE_LOCAL_IDENTITY_MEMBER;
		Order order = spyComputeOrder(federationUser, cloudName, provider);
		OrderStateTransitioner.activateOrder(order);

		SecurityRuleController securityRuleController = Mockito.mock(SecurityRuleController.class);
		Mockito.doNothing().when(securityRuleController).deleteSecurityRule(Mockito.anyString(), Mockito.eq(cloudName),
				Mockito.anyString(), Mockito.eq(federationUser));
		this.facade.setSecurityRuleController(securityRuleController);

		String ruleId = FAKE_RULE_ID;

		// exercise
		this.facade.deleteSecurityRule(FAKE_REQUESTING_MEMBER_ID, cloudName, ruleId, federationUser);

		// verify
		String operation = Operation.DELETE.getValue();
		String resourceType = ResourceType.SECURITY_RULE.getValue();
		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.eq(cloudName),
				Mockito.eq(operation), Mockito.eq(resourceType));

		Mockito.verify(securityRuleController, Mockito.times(1)).deleteSecurityRule(Mockito.anyString(),
				Mockito.eq(cloudName), Mockito.anyString(), Mockito.eq(federationUser));
	}

	// test case: When calling the authorizeOrder method with a different resource
	// type of the order, it must throw a InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testAuthorizeOrderThrowsInstanceNotFoundException() throws Exception {
		// set up
		FederationUser federationUser = null;
		String cloudName = null;
		Operation operation = null;
		ResourceType resourceType = ResourceType.VOLUME;
		ComputeOrder order = new ComputeOrder();

		// exercise
		this.facade.authorizeOrder(federationUser, cloudName, operation, resourceType, order);
	}

	// test case: When calling the authorizeOrder method with a federation user
	// different of the order requester, it must throw an
	// UnauthorizedRequestException.
	@Test(expected = UnauthorizedRequestException.class) // verify
	public void testAuthorizeOrderThrowsUnauthorizedRequestException() throws Exception {
		// set up
		FederationUser ownerUser = new FederationUser(null, FAKE_OWNER_USER_ID_VALUE,
				FAKE_OWNER_USER_ID_VALUE, null, new HashMap<>());

		FederationUser requesterUser = new FederationUser(null, FAKE_REQUESTER_USER_ID_VALUE,
				FAKE_REQUESTER_USER_ID_VALUE, null, new HashMap<>());

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
		Event event = Event.INSTANCE_FULFILLED;

		String signallingMember = LOCAL_MEMBER_ID;
		String requester = FAKE_REQUESTING_MEMBER_ID;
		String provider = signallingMember;

		Order remoteOrder = new ComputeOrder();
		remoteOrder.setRequester(requester);
		remoteOrder.setProvider(provider);
		remoteOrder.setOrderState(OrderState.PENDING);

		Mockito.doReturn(remoteOrder).when(this.orderController).getOrder(Mockito.eq(remoteOrder.getId()));

		IQ response = Mockito.mock(IQ.class);
		PacketSender packetSender = Mockito.mock(PacketSender.class); 
		PowerMockito.mockStatic(PacketSenderHolder.class);
		Mockito.when(PacketSenderHolder.getPacketSender()).thenReturn(packetSender);
		Mockito.when(packetSender.syncSendPacket(Mockito.any(IQ.class))).thenReturn(response);
		
		SharedOrderHolders ordersHolder = SharedOrderHolders.getInstance();
		SynchronizedDoublyLinkedList origin = ordersHolder.getOrdersList(OrderState.PENDING);
		origin.addItem(remoteOrder);
		
		// exercise
		this.facade.handleRemoteEvent(signallingMember, event, remoteOrder);

		// verify
		OrderState expectedOrderState = OrderState.FULFILLED;
		Assert.assertEquals(expectedOrderState, remoteOrder.getOrderState());
	}
	
	// test case: When calling the RemoteHandleRemoteEvent method from a pending
	// remote order, it must return its OrderState FAILED_AFTER_SUCCESSUL_REQUEST,
	// based on the match of the event passed by parameter.
	@Test
	public void testRemoteHandleRemoteEventWithInstanceFailed() throws Exception {
		// set up
		Event event = Event.INSTANCE_FAILED;

		String signallingMember = LOCAL_MEMBER_ID;
		String requester = FAKE_REQUESTING_MEMBER_ID;
		String provider = signallingMember;

		Order remoteOrder = new ComputeOrder();
		remoteOrder.setRequester(requester);
		remoteOrder.setProvider(provider);
		remoteOrder.setOrderState(OrderState.PENDING);

		Mockito.doReturn(remoteOrder).when(this.orderController).getOrder(Mockito.eq(remoteOrder.getId()));

		IQ iqResponse = Mockito.mock(IQ.class);
		PacketSender packetSender = Mockito.mock(PacketSender.class);
		PowerMockito.mockStatic(PacketSenderHolder.class);
		Mockito.when(PacketSenderHolder.getPacketSender()).thenReturn(packetSender);
		Mockito.when(packetSender.syncSendPacket(Mockito.any(IQ.class))).thenReturn(iqResponse);

		SharedOrderHolders ordersHolder = SharedOrderHolders.getInstance();
		SynchronizedDoublyLinkedList origin = ordersHolder.getOrdersList(OrderState.PENDING);
		origin.addItem(remoteOrder);

		// exercise
		this.facade.handleRemoteEvent(signallingMember, event, remoteOrder);

		// verify
		OrderState expectedOrderState = OrderState.FAILED_AFTER_SUCCESSUL_REQUEST;
		Assert.assertEquals(expectedOrderState, remoteOrder.getOrderState());
	}
	
	// test case: When calling the handleRemoteEvent method with a provider
	// different from the ordering provider, it must throw an UnexpectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testRemoteHandleRemoteEvent() throws Exception {
		// set up
		Event event = Event.INSTANCE_FAILED;

		String signallingMember = FAKE_REQUESTING_MEMBER_ID;
		String requester = signallingMember;
		String provider = LOCAL_MEMBER_ID;

		Order remoteOrder = new ComputeOrder();
		remoteOrder.setRequester(requester);
		remoteOrder.setProvider(provider);
		remoteOrder.setOrderState(OrderState.PENDING);

		Mockito.doReturn(remoteOrder).when(this.orderController).getOrder(Mockito.eq(remoteOrder.getId()));

		// exercise
		this.facade.handleRemoteEvent(signallingMember, event, remoteOrder);
	}
	
	private AuthorizationController mockAuthorizationController(FederationUser federationUser)
			throws UnexpectedException, UnauthorizedRequestException {
		AuthorizationController authorization = Mockito.mock(AuthorizationController.class);
		Mockito.doNothing().when(authorization).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		this.facade.setAuthorizationController(authorization);
		return authorization;
	}

	private Order spyComputeOrder(FederationUser federationUser, String cloudName, String provider) throws UnexpectedException {
		Order order = Mockito.spy(new ComputeOrder());
		order.setFederationUser(federationUser);
		order.setRequester(FAKE_REQUESTING_MEMBER_ID);
		order.setProvider(provider);
		order.setCloudName(cloudName);
		return order;
	}

	private FederationUser createFederationUser() {
		return new FederationUser(null, FAKE_REQUESTER_USER_ID_VALUE,
				FAKE_REQUESTER_USER_ID_VALUE, null, new HashMap<>());
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
