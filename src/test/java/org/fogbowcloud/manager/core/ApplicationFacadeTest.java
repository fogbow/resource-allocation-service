package org.fogbowcloud.manager.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.NetworkAllocation;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.AaController;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class ApplicationFacadeTest extends BaseUnitTests {

	private ApplicationFacade application;
	private AaController aaaController;
	private OrderController orderController;
    private Map<String, Order> activeOrdersMap;

	@Before
	public void setUp() throws UnauthorizedException {
		this.aaaController = Mockito.mock(AaController.class);
		
		@SuppressWarnings("unused")
        Properties properties = new Properties();
		
		HomeDir.getInstance().setPath("src/test/resources/private");
		PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
		properties = propertiesHolder.getProperties();
		
		this.orderController = Mockito.spy(new OrderController(""));
		this.application = Mockito.spy(ApplicationFacade.getInstance());
		this.application.setAaController(this.aaaController);
		this.application.setOrderController(this.orderController);
		
		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
	}

	@Test
	public void testDeleteComputeOrder() throws Exception {
		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Order order = createComputeOrder();
		OrderStateTransitioner.activateOrder(order);

		Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
				Mockito.any(Operation.class), Mockito.any(Order.class));

		String federationTokenValue = "";
		this.application.deleteCompute(order.getId(), federationTokenValue);

		Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
	}

	@Test
	public void testDeleteComputeOrderUnathenticated() throws Exception {
		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

		Order order = createComputeOrder();
		OrderStateTransitioner.activateOrder(order);

		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString(),
				Mockito.any(FederationUser.class), Mockito.any(InstanceType.class));

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
				Mockito.any(Operation.class), Mockito.any(Order.class));

		try {
			String federationTokenValue = "";
			this.application.deleteCompute(order.getId(), federationTokenValue);
			Assert.fail();
		} catch (UnauthenticatedException e) {
			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	@Test
	public void testDeleteComputeOrderTokenUnathenticated() throws Exception {
		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Order order = createComputeOrder();
		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
				.getFederationUser(Mockito.anyString());

		OrderStateTransitioner.activateOrder(order);

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
				Mockito.any(Operation.class), Mockito.any(Order.class));

		try {
			String federationTokenValue = "";
			this.application.deleteCompute(order.getId(), federationTokenValue);
			Assert.fail();
		} catch (UnauthenticatedException e) {
			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	@Test
	public void testDeleteComputeOrderWithFederationUserUnauthenticated() throws Exception {
		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Order order = createComputeOrder();
		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

		OrderStateTransitioner.activateOrder(order);

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
				Mockito.any(Operation.class), Mockito.any(Order.class));

		try {
			String federationTokenValue = "";
			this.application.deleteCompute(order.getId(), federationTokenValue);
			Assert.fail();
		} catch (UnauthenticatedException e) {
			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	
	@Test(expected = OrderManagementException.class)
	public void testDeleteComputeOrderNullGet() throws Exception {
		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Order order = createComputeOrder();
		Mockito.doReturn(null).when(this.orderController).getOrder(Mockito.anyString(),
				Mockito.any(FederationUser.class), Mockito.any(InstanceType.class));

		Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
				Mockito.any(Operation.class), Mockito.any(Order.class));

		String federationTokenValue = "";
		this.application.deleteCompute(order.getId(), federationTokenValue);
	}

	@Test
	public void testDeleteComputeOrderUnauthorizedOperation() throws Exception {
		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Order order = createComputeOrder();
		OrderStateTransitioner.activateOrder(order);

		Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(
				Mockito.any(FederationUser.class), Mockito.any(Operation.class), Mockito.any(Order.class));

		Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.any(Order.class));

		try {
			this.application.deleteCompute(order.getId(), "");
			Assert.fail();
		} catch (UnauthorizedException e) {
			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	@Test
	public void testCreateComputeOrder() throws Exception {
		ComputeOrder order = createComputeOrder();

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
				Mockito.any(Operation.class), Mockito.any(Order.class));

		Assert.assertNull(order.getOrderState());

		String federationTokenValue = "";
		this.application.createCompute(order, federationTokenValue);

		Assert.assertEquals(OrderState.OPEN, order.getOrderState());
	}

	@Test
	public void testCreateComputeOrderUnauthenticated() throws Exception {
		ComputeOrder order = createComputeOrder();

		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		try {
			this.application.createCompute(order, "");
			Assert.fail();
		} catch (UnauthenticatedException e) {
			Assert.assertNull(order.getOrderState());
		}
	}

	@Test
	public void testCreateComputeOrderTokenUnauthenticated() throws Exception {
		ComputeOrder order = createComputeOrder();

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
				.getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		Assert.assertNull(order.getOrderState());

		try {
			String federationTokenValue = "";
			this.application.createCompute(order, federationTokenValue);
			Assert.fail();
		} catch (UnauthenticatedException e) {
			Assert.assertNull(order.getOrderState());
		}
	}

	@Test
	public void testCreateComputeOrderWithFederationUserUnauthenticated() throws Exception {
		ComputeOrder order = createComputeOrder();

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		Assert.assertNull(order.getOrderState());

		try {
			String federationTokenValue = "";
			this.application.createCompute(order, federationTokenValue);
			Assert.fail();
		} catch (UnauthenticatedException e) {
			Assert.assertNull(order.getOrderState());
		}
	}

	/**
     * Method 'isAuthorized(federationUser, operation, order)' 
     * in 'DefaultAuthorizationPlugin' class, 
     * is set to always return true
     */
	@Ignore
	@Test
	public void testCreateComputeOrderUnauthorizedOperation() throws Exception {
		ComputeOrder order = createComputeOrder();

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(FederationUser.class),
				Mockito.any(Operation.class), Mockito.any(InstanceType.class));

		Assert.assertNull(order.getOrderState());

		try {
			String federationTokenValue = "";
			this.application.createCompute(order, federationTokenValue);
			Assert.fail();
		} catch (UnauthorizedException e) {
			Assert.assertNull(order.getOrderState());
		}
	}

	/**
     * This test attempts to throw OrderManagementException, which occurs when sending a 'null'
     * request, something that should not be tested in this class, since the request here must go
     * through several checks before this.
     */
    @Ignore
	@Test(expected = OrderManagementException.class)
	public void testCreateNullComputeOrder() throws Exception {
		ComputeOrder order = createComputeOrder();

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
				Mockito.any(InstanceType.class));

		Assert.assertNull(order.getOrderState());

		String federationTokenValue = "";
		this.application.createCompute(order, federationTokenValue);
	}

	
	@Test
	public void testGetComputeOrder() throws Exception {
		ComputeOrder order = createComputeOrder();

		OrderStateTransitioner.activateOrder(order);

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
				Mockito.any(InstanceType.class));

		ComputeInstance computeInstanceExcepted = new ComputeInstance("");
		Mockito.doReturn(computeInstanceExcepted).when(this.orderController).getResourceInstance(Mockito.eq(order));
		
		String federationTokenValue = "";
		ComputeInstance computeInstance = this.application.getCompute(order.getId(), federationTokenValue);

		Assert.assertSame(computeInstanceExcepted, computeInstance);
	}

	@Test(expected = UnauthenticatedException.class)
	public void testGetComputeOrderUnauthenticated() throws Exception {
		ComputeOrder order = createComputeOrder();

		OrderStateTransitioner.activateOrder(order);

		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		this.application.getCompute(order.getId(), "");
	}

	@Test(expected = UnauthenticatedException.class)
	public void testGetComputeOrderWithFederationUserUnauthenticated() throws Exception {
		ComputeOrder order = createComputeOrder();

		OrderStateTransitioner.activateOrder(order);

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		String federationTokenValue = "";
		this.application.getCompute(order.getId(), federationTokenValue);
	}

	@Test(expected = UnauthenticatedException.class)
	public void testGetComputeOrderTokenUnauthenticated() throws Exception {
		ComputeOrder order = createComputeOrder();

		OrderStateTransitioner.activateOrder(order);

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
				.getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		this.application.getCompute(order.getId(), "");
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetComputeOrderUnauthorizedOperation() throws Exception {
		ComputeOrder order = createComputeOrder();

		OrderStateTransitioner.activateOrder(order);

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(FederationUser.class),
				Mockito.any(Operation.class), Mockito.any(Order.class));

		String federationTokenValue = "";
		this.application.getCompute(order.getId(), federationTokenValue);
	}

	
	@Test
	public void testGetAllComputes() throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
		ComputeOrder order = createComputeOrder();

		OrderStateTransitioner.activateOrder(order);

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
				Mockito.any(InstanceType.class));
		
		List<ComputeInstance> allComputesInstances = this.application.getAllComputes("");

		Assert.assertEquals(1, allComputesInstances.size());

		Assert.assertEquals(order.getInstanceId(), allComputesInstances.get(0).getId());
	}

	@Test
	public void testGetAllComputesEmpty()
			throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
		ComputeOrder order = createComputeOrder();

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
				Mockito.any(InstanceType.class));

		List<ComputeInstance> allComputesInstances = this.application.getAllComputes(Mockito.anyString());

		Assert.assertEquals(0, allComputesInstances.size());
	}

	@Test(expected = UnauthenticatedException.class)
	public void testGetAllComputesUnauthenticated()
			throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
		ComputeOrder order = createComputeOrder();

		OrderStateTransitioner.activateOrder(order);

		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
				Mockito.any(InstanceType.class));

		this.application.getAllComputes(Mockito.anyString());
	}

	@Test(expected = UnauthenticatedException.class)
	public void testGetAllComputesTokenUnauthenticated()
			throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
		ComputeOrder order = createComputeOrder();

		OrderStateTransitioner.activateOrder(order);

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
				.getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
				Mockito.any(InstanceType.class));

		this.application.getAllComputes(Mockito.anyString());
	}

	@Test(expected = UnauthenticatedException.class)
	public void testGetAllComputesWithFederationUserUnauthenticated()
			throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
		ComputeOrder order = createComputeOrder();

		OrderStateTransitioner.activateOrder(order);

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
				Mockito.any(InstanceType.class));

		this.application.getAllComputes(Mockito.anyString());
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetAllComputesOperationUnauthorized()
			throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
		ComputeOrder order = createComputeOrder();

		OrderStateTransitioner.activateOrder(order);

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

		Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
				Mockito.any(InstanceType.class));

		this.application.getAllComputes(Mockito.anyString());
	}

	@Test
    public void testCreateVolumeOrder() throws Exception {
        VolumeOrder order = createVolumeOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Assert.assertNull(order.getOrderState());

        String federationTokenValue = "";
        this.application.createVolume(order, federationTokenValue);

        Assert.assertEquals(OrderState.OPEN, order.getOrderState());
    }

	@Test
    public void testCreateVolumeOrderUnauthenticated() throws Exception {
        VolumeOrder order = createVolumeOrder();

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        try {
        	String federationTokenValue = "";
            this.application.createVolume(order, federationTokenValue);
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertNull(order.getOrderState());
        }
    }

	@Test
    public void testCreateVolumeOrderTokenUnauthenticated() throws Exception {
        VolumeOrder order = createVolumeOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Assert.assertNull(order.getOrderState());

        try {
        	String federationTokenValue = "";
            this.application.createVolume(order, federationTokenValue);
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertNull(order.getOrderState());
        }
    }

	@Test
    public void testCreateVolumeOrderWithFederationUserUnauthenticated() throws Exception {
        VolumeOrder order = createVolumeOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Assert.assertNull(order.getOrderState());

        try {
        	String federationTokenValue = "";
            this.application.createVolume(order, federationTokenValue);
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertNull(order.getOrderState());
        }
    }

	/**
     * Method 'isAuthorized(federationUser, operation, order)' 
     * in 'DefaultAuthorizationPlugin' class, 
     * is set to always return true
     */
	@Ignore
	@Test
    public void testCreateVolumeOrderUnauthorizedOperation() throws Exception {
        VolumeOrder order = createVolumeOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        Assert.assertNull(order.getOrderState());

        try {
        	String federationTokenValue = "";
            this.application.createVolume(order, federationTokenValue);
            Assert.fail();
        } catch (UnauthorizedException e) {
            Assert.assertNull(order.getOrderState());
        }
    }

	/**
     * This test attempts to throw OrderManagementException, which occurs when sending a 'null'
     * request, something that should not be tested in this class, since the request here must go
     * through several checks before this.
     */
    @Ignore
	@Test(expected = OrderManagementException.class)
    public void testCreateNullVolumeOrder() throws Exception {
        VolumeOrder order = createVolumeOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        Assert.assertNull(order.getOrderState());

        String federationTokenValue = "";
        this.application.createVolume(order, federationTokenValue);
    }

	
	@Test
    public void testGetVolumeOrder() throws Exception {
        VolumeOrder volumeOrder = createVolumeOrder();

		OrderStateTransitioner.activateOrder(volumeOrder);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(volumeOrder.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        VolumeInstance volumeInstance = this.application.getVolume(volumeOrder.getId(), "");

        Assert.assertSame(volumeOrder.getInstanceId(), volumeInstance.getId());
    }

	@Test(expected = UnauthenticatedException.class)
    public void testGetVolumeOrderUnauthenticated() throws Exception {
        VolumeOrder order = createVolumeOrder();

		OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        String federationTokenValue = "";
        this.application.getVolume(order.getId(), federationTokenValue);
    }

	@Test(expected = UnauthenticatedException.class)
    public void testGetVolumeOrderWithFederationUserUnauthenticated() throws Exception {
        VolumeOrder order = createVolumeOrder();

		OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        String federationTokenValue = "";
        this.application.getVolume(order.getId(), federationTokenValue);
    }

	@Test(expected = UnauthenticatedException.class)
    public void testGetVolumeOrderTokenUnauthenticated() throws Exception {
        VolumeOrder order = createVolumeOrder();

		OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        String federationTokenValue = "";
        this.application.getVolume(order.getId(), federationTokenValue);
    }

	@Test(expected = UnauthorizedException.class)
    public void testGetVolumeOrderUnauthorizedOperation() throws Exception {
        VolumeOrder order = createVolumeOrder();

		OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        String federationTokenValue = "";
        this.application.getVolume(order.getId(), federationTokenValue);
    }

	@Test
    public void testGetAllVolumes() throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        VolumeOrder order = createVolumeOrder();

		OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        String federationTokenValue = "";
        List<VolumeInstance> volumeInstances = this.application.getAllVolumes(federationTokenValue);

        Assert.assertEquals(1, volumeInstances.size());

        Assert.assertSame(order.getInstanceId(), volumeInstances.get(0).getId());
    }

	@Test
    public void testGetAllVolumesEmpty()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        VolumeOrder order = createVolumeOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        String federationTokenValue = "";
        List<VolumeInstance> allVolumes = this.application.getAllVolumes(federationTokenValue);

        Assert.assertEquals(0, allVolumes.size());
    }

	@Test(expected = UnauthenticatedException.class)
    public void testGetAllVolumesTokenUnauthenticated()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        VolumeOrder order = createVolumeOrder();

		OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        String federationTokenValue = "";
        this.application.getAllVolumes(federationTokenValue);
    }

	@Test(expected = UnauthenticatedException.class)
    public void testGetAllVolumesFederationUserUnauthenticated()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        VolumeOrder order = createVolumeOrder();

		OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        this.application.getAllVolumes("");
    }

	@Test(expected = UnauthorizedException.class)
    public void testGetAllVolumesOperationUnauthorized()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        VolumeOrder order = createVolumeOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        String federationTokenValue = "";
        this.application.getAllVolumes(federationTokenValue);
    }

	@Test
    public void testDeleteVolumeOrder() throws Exception {
        VolumeOrder order = createVolumeOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        String federationTokenValue = "";
        this.application.deleteVolume(order.getId(), federationTokenValue);

        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

	@Test
    public void testDeleteVolumeOrderUnathenticated() throws Exception {
        VolumeOrder order = createVolumeOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString(), Mockito.any(FederationUser.class),
                Mockito.any(InstanceType.class));

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        try {
        	String federationTokenValue = "";
            this.application.deleteVolume(order.getId(), federationTokenValue);
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

	@Test
    public void testDeleteVolumeOrderTokenUnathenticated() throws Exception {
        VolumeOrder order = createVolumeOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
        	String federationTokenValue = "";
            this.application.deleteVolume(order.getId(), federationTokenValue);
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

	@Test
    public void testDeleteVolumeOrderWithFederationUserUnauthenticated() throws Exception {
        VolumeOrder order = createVolumeOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

        try {
        	String federationTokenValue = "";
            this.application.deleteVolume(order.getId(), federationTokenValue);
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

	
	@Test(expected = OrderManagementException.class)
    public void testDeleteVolumeOrderNullGet() throws Exception {
	    VolumeOrder order = createVolumeOrder();

	    Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(null).when(this.orderController).getOrder(Mockito.anyString(), Mockito.any(FederationUser.class),
                Mockito.any(InstanceType.class));

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        String federationTokenValue = "";
        this.application.deleteVolume(order.getId(), federationTokenValue);
    }

    @Test
    public void testDeleteVolumeOrderUnauthorizedOperation() throws Exception {
        VolumeOrder order = createVolumeOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.any(Order.class));

        try {
        	String federationTokenValue = "";
            this.application.deleteVolume(order.getId(), federationTokenValue);
            Assert.fail();
        } catch (UnauthorizedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    @Test
    public void testCreateNetworkOrder() throws Exception {
        NetworkOrder order = createNetworkOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Assert.assertNull(order.getOrderState());

        String federationTokenValue = "";
        this.application.createNetwork(order, federationTokenValue);

        Assert.assertEquals(OrderState.OPEN, order.getOrderState());
    }

    @Test
    public void testCreateNetworkOrderUnauthenticated() throws Exception {
        NetworkOrder order = createNetworkOrder();

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        try {
        	String federationTokenValue = "";
            this.application.createNetwork(order, federationTokenValue);
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertNull(order.getOrderState());
        }
    }

    @Test
    public void testCreateNetworkOrderTokenUnauthenticated() throws Exception {
        NetworkOrder order = createNetworkOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Assert.assertNull(order.getOrderState());

        try {
        	String federationTokenValue = "";
            this.application.createNetwork(order, federationTokenValue);
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertNull(order.getOrderState());
        }
    }

    @Test
    public void testCreateNetworkOrderWithFederationUserUnauthenticated() throws Exception {
        NetworkOrder order = createNetworkOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Assert.assertNull(order.getOrderState());

        try {
        	String federationTokenValue = "";
            this.application.createNetwork(order, federationTokenValue);
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertNull(order.getOrderState());
        }
    }

    /**
     * Method 'isAuthorized(federationUser, operation, order)' 
     * in 'DefaultAuthorizationPlugin' class, 
     * is set to always return true
     */
    @Ignore
    @Test
    public void testCreateNetworkOrderUnauthorizedOperation() throws Exception {
        NetworkOrder order = createNetworkOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        Assert.assertNull(order.getOrderState());

        try {
        	String federationTokenValue = "";
            this.application.createNetwork(order, federationTokenValue);
            Assert.fail();
        } catch (UnauthorizedException e) {
            Assert.assertNull(order.getOrderState());
        }
    }

    /**
     * This test attempts to throw OrderManagementException, which occurs when sending a 'null'
     * request, something that should not be tested in this class, since the request here must go
     * through several checks before this.
     */
    @Ignore
    @Test(expected = OrderManagementException.class)
    public void testCreateNullNetworkOrder() throws Exception {
        NetworkOrder order = createNetworkOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        Assert.assertNull(order.getOrderState());

        String federationTokenValue = "";
        this.application.createNetwork(order, federationTokenValue);
    }

    
    @Test
    public void testGetNetworkOrder() throws Exception {
        NetworkOrder order = createNetworkOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

		NetworkInstance networkInstanceExcepted = new NetworkInstance("");
		Mockito.doReturn(networkInstanceExcepted).when(this.orderController).getResourceInstance(Mockito.eq(order));
        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        String federationTokenValue = "";
        NetworkInstance actualInstance = this.application.getNetwork(order.getId(), federationTokenValue);

        Assert.assertSame(networkInstanceExcepted, actualInstance);
    }

    @Test(expected = UnauthenticatedException.class)
    public void testGetNetworkOrderUnauthenticated() throws Exception {
        NetworkOrder order = createNetworkOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        String federationTokenValue = "";
        this.application.getNetwork(order.getId(), federationTokenValue);
    }

    @Test(expected = UnauthenticatedException.class)
    public void testGetNetworkOrderWithFederationUserUnauthenticatedException() throws Exception {
        NetworkOrder order = createNetworkOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        String federationTokenValue = "";
        this.application.getNetwork(order.getId(), federationTokenValue);
    }

    @Test(expected = UnauthenticatedException.class)
    public void testGetNetworkOrderTokenUnauthenticated() throws Exception {
        NetworkOrder order = createNetworkOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        String federationTokenValue = "";
        this.application.getNetwork(order.getId(), federationTokenValue);
    }

    @Test(expected = UnauthorizedException.class)
    public void testGetNetworkOrderUnauthorizedOperation() throws Exception {
        NetworkOrder order = createNetworkOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        String federationTokenValue = "";
        this.application.getNetwork(order.getId(), federationTokenValue);
    }

    @Test
    public void testGetAllNetworks() throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        NetworkOrder order = createNetworkOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        List<NetworkInstance> allNetworks = this.application.getAllNetworks("");

        Assert.assertEquals(1, allNetworks.size());

        Assert.assertSame(order.getInstanceId(), allNetworks.get(0).getId());
    }

    @Test
    public void testGetAllNetworksEmpty()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        NetworkOrder order = createNetworkOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        String federationTokenValue = "";
        List<NetworkInstance> allNetworks = this.application.getAllNetworks(federationTokenValue);

        Assert.assertEquals(0, allNetworks.size());
    }

    @Test(expected = UnauthenticatedException.class)
    public void testGetAllNetworksTokenUnauthenticated()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        NetworkOrder order = createNetworkOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        String federationTokenValue = "";
        this.application.getAllNetworks(federationTokenValue);
    }

    @Test(expected = UnauthenticatedException.class)
    public void testGetAllNetworksWithFederationUserUnauthenticated()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        NetworkOrder order = createNetworkOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        String federationTokenValue = "";
        this.application.getAllNetworks(federationTokenValue);
    }

    @Test(expected = UnauthorizedException.class)
    public void testGetAllNetworksOperationUnauthorized()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        NetworkOrder order = createNetworkOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        String federationTokenValue = "";
        this.application.getAllNetworks(federationTokenValue);
    }

    @Test
    public void testDeleteNetworkOrder() throws Exception {
        NetworkOrder order = createNetworkOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        String federationTokenValue = "";
        this.application.deleteNetwork(order.getId(), federationTokenValue);

        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    @Test
    public void testDeleteNetworkOrderUnathenticated() throws Exception {
        NetworkOrder order = createNetworkOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString(), Mockito.any(FederationUser.class),
                Mockito.any(InstanceType.class));

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        try {
        	String federationTokenValue = "";
            this.application.deleteNetwork(order.getId(), federationTokenValue);
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    @Test
    public void testDeleteNetworkOrderTokenUnathenticated() throws Exception {
        NetworkOrder order = createNetworkOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
        	String federationTokenValue = "";
            this.application.deleteNetwork(order.getId(), federationTokenValue);
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    @Test
    public void testDeleteNetworkOrderWithFederationUserUnauthenticated() throws Exception {
        NetworkOrder order = createNetworkOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

        try {
        	String federationTokenValue = "";
            this.application.deleteNetwork(order.getId(), federationTokenValue);
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    @Test(expected = OrderManagementException.class)
    public void testDeleteNetworkOrderNullGet() throws Exception {
        NetworkOrder order = createNetworkOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(null).when(this.orderController).getOrder(Mockito.anyString(), Mockito.any(FederationUser.class),
                Mockito.any(InstanceType.class));

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        String federationTokenValue = "";
        this.application.deleteNetwork(order.getId(), federationTokenValue);
    }

    @Test
    public void testDeleteNetworkOrderUnauthorizedOperation() throws Exception {
        NetworkOrder order = createNetworkOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.any(Order.class));

        try {
        	String federationTokenValue = "";
            this.application.deleteNetwork(order.getId(), federationTokenValue);
            Assert.fail();
        } catch (UnauthorizedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

	private NetworkOrder createNetworkOrder() throws PropertyNotSpecifiedException, TokenCreationException, RequestException, UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
	    FederationUser federationUser = new FederationUser("fake-user", new HashMap<>());
        NetworkOrder order = new NetworkOrder(federationUser, "fake-member-id", "fake-member-id", "fake-gateway", "fake-address", NetworkAllocation.STATIC);
        
		NetworkInstance networtkInstanceExcepted = new NetworkInstance(order.getId());
		Mockito.doReturn(networtkInstanceExcepted).when(this.orderController).getResourceInstance(Mockito.eq(order));
		order.setInstanceId(networtkInstanceExcepted.getId());
        
        return order;
    }

    private VolumeOrder createVolumeOrder() throws PropertyNotSpecifiedException, TokenCreationException, RequestException, UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        FederationUser federationUser = new FederationUser("fake-user", new HashMap<>());
        VolumeOrder order = new VolumeOrder(federationUser, "fake-member-id", "fake-member-id", 1);
        
		VolumeInstance volumeInstanceExcepted = new VolumeInstance(order.getId());
		Mockito.doReturn(volumeInstanceExcepted).when(this.orderController).getResourceInstance(Mockito.eq(order));
		order.setInstanceId(volumeInstanceExcepted.getId());
        
        return order;
    }

    private ComputeOrder createComputeOrder() throws PropertyNotSpecifiedException, TokenCreationException, RequestException, UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
		FederationUser federationUser = new FederationUser("fake-user", new HashMap<>());
		
		ComputeOrder order = new ComputeOrder(federationUser, "fake-member-id", "fake-member-id", 2, 2, 30,
				"fake-image-name", new UserData(), "fake-public-key", null);
		
		ComputeInstance computeInstanceExcepted = new ComputeInstance(order.getId());
		Mockito.doReturn(computeInstanceExcepted).when(this.orderController).getResourceInstance(Mockito.eq(order));
		order.setInstanceId(computeInstanceExcepted.getId());
		
		return order;
	}
    
    @Test
    public void testCreateAttachmentOrderUnauthenticated() throws Exception {
        AttachmentOrder order = createAttachmentOrder();

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        try {
            this.application.createAttachment(order, "");
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertNull(order.getOrderState());
        }
    }

    @Test
    public void testCreateAttachmentOrderTokenUnauthenticated() throws Exception {
        AttachmentOrder order = createAttachmentOrder();
        
        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));
        
        try {
            this.application.createAttachment(order, "");
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertNull(order.getOrderState());
        }
    }
    
    @Test
    public void testCreateAttachmentOrderWithFederationUserUnauthenticated() throws Exception {
        AttachmentOrder order = createAttachmentOrder();
        
        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        

        try {
            this.application.createAttachment(order, "");
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertNull(order.getOrderState());
        }
    }
    
    /**
     * Method 'isAuthorized(federationUser, operation, order)' 
     * in 'DefaultAuthorizationPlugin' class, 
     * is set to always return true
     */
    @Ignore
    @Test
    public void testCreateAttachmentOrderUnauthorizedOperation() throws Exception {
        AttachmentOrder order = createAttachmentOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        Assert.assertNull(order.getOrderState());

        try {
            this.application.createAttachment(order, "");
            Assert.fail();
        } catch (UnauthorizedException e) {
            Assert.assertNull(order.getOrderState());
        }
    }

    /**
     * This test attempts to throw OrderManagementException, which occurs when sending a 'null'
     * request, something that should not be tested in this class, since the request here must go
     * through several checks before this.
     */
    @Ignore
    @Test(expected = OrderManagementException.class)
    public void testCreateNullAttachmentOrder() throws Exception {
        AttachmentOrder order = createAttachmentOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        Assert.assertNull(order.getOrderState());

        this.application.createAttachment(order, "");
    }

    @Test
    public void testGetAttachmentOrder() throws Exception {
        AttachmentOrder order = createAttachmentOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());
        
        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        AttachmentInstance actualInstance = this.application.getAttachment(order.getId(), "");
        
        Assert.assertNotNull(actualInstance);
    }

    @Test(expected = UnauthenticatedException.class)
    public void testGetAttachmentOrderUnauthenticated() throws Exception {
        AttachmentOrder order = createAttachmentOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        this.application.getAttachment(order.getId(), "");
    }
    
    @Test(expected = UnauthenticatedException.class)
    public void testGetAttachmentOrderWithFederationUserUnauthenticated() throws Exception {
        AttachmentOrder order = createAttachmentOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        this.application.getAttachment(order.getId(), "");
    }

    @Test(expected = UnauthenticatedException.class)
    public void testGetAttachmentOrderTokenUnauthenticated() throws Exception {
        AttachmentOrder order = createAttachmentOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        this.application.getAttachment(order.getId(), "");
    }

    @Test(expected = UnauthorizedException.class)
    public void testGetAttachmentOrderUnauthorizedOperation() throws Exception {
        AttachmentOrder order = createAttachmentOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        this.application.getAttachment(order.getId(), "");
    }

    @Test
    public void testGetAllAttachments() throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        AttachmentOrder order = createAttachmentOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        List<AttachmentInstance> allAttachments = this.application.getAllAttachments("");

        Assert.assertEquals(1, allAttachments.size());
    }
    
    @Test
    public void testGetAllAttachmentEmpty()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        AttachmentOrder order = createAttachmentOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        List<AttachmentInstance> allAttachments = this.application.getAllAttachments("");

        Assert.assertEquals(0, allAttachments.size());
    }

    @Test(expected = UnauthenticatedException.class)
    public void testGetAllAttachmentTokenUnauthenticated()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        AttachmentOrder order = createAttachmentOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        this.application.getAllAttachments("");
    }

    @Test(expected = UnauthenticatedException.class)
    public void testGetAllAttachmentsFederationUserUnauthenticated()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        AttachmentOrder order = createAttachmentOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        this.application.getAllAttachments("");
    }

    @Test(expected = UnauthorizedException.class)
    public void testGetAllAttachmentOperationUnauthorized()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException, RequestException, TokenCreationException, PropertyNotSpecifiedException, InstanceNotFoundException, RemoteRequestException {
        AttachmentOrder order = createAttachmentOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        this.application.getAllAttachments("");
    }
    
    @Test
    public void testDeleteAttachmentOrder() throws Exception {
        AttachmentOrder order = createAttachmentOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        this.application.deleteAttachment(order.getId(), "");

        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    @Test
    public void testDeleteAttachmentOrderUnathenticated() throws Exception {
        AttachmentOrder order = createAttachmentOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString(), Mockito.any(FederationUser.class),
                Mockito.any(InstanceType.class));

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        try {
            this.application.deleteAttachment(order.getId(), "");
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    @Test
    public void testDeleteAttachmentOrderTokenUnathenticated() throws Exception {
        AttachmentOrder order = createAttachmentOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            this.application.deleteAttachment(order.getId(), "");
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    @Test
    public void testDeleteAttachmentOrderFederationUserUnauthenticated() throws Exception {
        AttachmentOrder order = createAttachmentOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).getFederationUser(Mockito.anyString());

        try {
            this.application.deleteAttachment(order.getId(), "");
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    
    @Test(expected = OrderManagementException.class)
    public void testDeleteAttachmentOrderNullGet() throws Exception {
        AttachmentOrder order = createAttachmentOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(null).when(this.orderController).getOrder(Mockito.anyString(),
                Mockito.any(FederationUser.class), Mockito.any(InstanceType.class));

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        String federationTokenValue = "";
        this.application.deleteAttachment(order.getId(), federationTokenValue);
    }

    @Test
    public void testDeleteAttachmentOrderUnauthorizedOperation() throws Exception {
        AttachmentOrder order = createAttachmentOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController).getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.any(Order.class));

        try {
            this.application.deleteAttachment(order.getId(), "");
            Assert.fail();
        } catch (UnauthorizedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }
    
    private AttachmentOrder createAttachmentOrder()
            throws PropertyNotSpecifiedException, TokenCreationException, RequestException,
            UnauthorizedException, InstanceNotFoundException, RemoteRequestException {
        
        FederationUser federationUser = new FederationUser("fake-user", new HashMap<>());
        
        ComputeOrder computeOrder = new ComputeOrder();
        ComputeInstance computeInstance = new ComputeInstance("fake-source-id");
        computeOrder.setInstanceId(computeInstance.getId());
        this.activeOrdersMap.put(computeOrder.getId(), computeOrder);
        String sourceId = computeOrder.getId();
        
        VolumeOrder volumeOrder = new VolumeOrder();
        VolumeInstance volumeInstance = new VolumeInstance("fake-target-id");
        volumeOrder.setInstanceId(volumeInstance.getId());
        this.activeOrdersMap.put(volumeOrder.getId(), volumeOrder);
        String targetId = volumeOrder.getId();

        AttachmentOrder order = new AttachmentOrder(federationUser, "fake-member-id",
                "fake-member-id", sourceId, targetId, "fake-device-mount-point");

        AttachmentInstance attachmentInstanceExcepted = new AttachmentInstance(order.getId());
        
        Mockito.doReturn(attachmentInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order));
        order.setInstanceId(attachmentInstanceExcepted.getId());

        return order;
    }

}
