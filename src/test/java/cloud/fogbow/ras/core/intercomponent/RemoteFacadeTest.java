package cloud.fogbow.ras.core.intercomponent;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationController;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.OrderController;
import cloud.fogbow.ras.core.OrderStateTransitioner;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.instances.ComputeInstance;
import cloud.fogbow.ras.core.models.instances.Instance;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.models.quotas.ComputeQuota;
import cloud.fogbow.ras.core.models.quotas.Quota;
import cloud.fogbow.ras.core.models.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudConnectorFactory.class, DatabaseManager.class, PacketSenderHolder.class})
public class RemoteFacadeTest extends BaseUnitTests {

	private static final String DEFAULT_CLOUD_NAME = "default";
    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_LOCAL_IDENTITY_MEMBER = "fake-localidentity-member";
    private static final String FAKE_REQUESTER_USER_ID_VALUE = "fake-requester-user-id";
    private static final String FAKE_REQUESTING_MEMBER_ID = "fake-requesting-member-id";
	private static final String ID_KEY = "id";

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
	public void testRemoteGetUserQuota() throws Exception {
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
		Map<String, String> attributes = new HashMap<>();
		attributes.put(ID_KEY, FAKE_REQUESTER_USER_ID_VALUE);
		FederationUser federationUser = new FederationUser(attributes);
		return federationUser;
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
    
    // test case: Verifies generic request behavior inside Remote Facade, i.e. it needs to authenticate and authorize the request, and also get the correct cloud connector, before passing a generic request.
    @Ignore
    @Test
    public void testGenericRequest() throws Exception {
        // set up
    	String cloudName = DEFAULT_CLOUD_NAME;
    	FederationUser federationUser = createFederationUser();
    	
    	GenericRequest genericRequest = Mockito.mock(GenericRequest.class);
    	
        // exercise
        facade.genericRequest(FAKE_REQUESTING_MEMBER_ID, cloudName, genericRequest, federationUser);

        // verify

    }

    @Ignore
    @Test
    public void testRemoteGetImage() {
        // TODO implement test
    }

    @Ignore
    @Test
    public void testRemoteGetAllImages() {
        // TODO implement test
    }

    @Ignore
    @Test
    public void testRemoteHandleRemoteEvent() {
        // TODO implement test
    }

}
