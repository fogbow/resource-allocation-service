package org.fogbowcloud.manager.core;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.instanceprovider.LocalInstanceProvider;
import org.fogbowcloud.manager.core.instanceprovider.RemoteInstanceProvider;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.manager.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.manager.constants.Operation;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.services.AAAController;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ApplicationFacadeTest extends BaseUnitTests {

	private ApplicationFacade application;
	private AAAController aaaController;
	private OrderController orderController;

	@Before
	public void setUp() throws UnauthorizedException {
		this.aaaController = Mockito.mock(AAAController.class);
		Properties properties = new Properties();
		properties.setProperty(ConfigurationConstants.XMPP_ID_KEY, BaseUnitTests.LOCAL_MEMBER_ID);
		this.orderController = Mockito.spy(new OrderController(properties, Mockito.mock(LocalInstanceProvider.class),
				Mockito.mock(RemoteInstanceProvider.class)));
		this.application = ApplicationFacade.getInstance();
		this.application.setAAAController(this.aaaController);
		this.application.setOrderController(this.orderController);
	}

	@Test
	public void testDeleteComputeOrder() throws Exception {
		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Order order = createComputeOrder();
		this.orderController.activateOrder(order, order.getFederationToken());

		Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		this.application.deleteCompute(order.getId(), order.getFederationToken().getAccessId());

		Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
	}

	@Test
	public void testDeleteComputeOrderUnathenticated() throws Exception {
		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

		Order order = createComputeOrder();
		this.orderController.activateOrder(order, order.getFederationToken());

		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString(), Mockito.any(Token.User.class),
				Mockito.any(OrderType.class));

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		try {
			this.application.deleteCompute(order.getId(), order.getFederationToken().getAccessId());
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
				.getFederationToken(Mockito.anyString());

		this.orderController.activateOrder(order, order.getFederationToken());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		try {
			this.application.deleteCompute(order.getId(), order.getFederationToken().getAccessId());
			Assert.fail();
		} catch (UnauthenticatedException e) {
			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	@Test
	public void testDeleteComputeOrderTokenUnauthorized() throws Exception {
		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Order order = createComputeOrder();
		Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).getFederationToken(Mockito.anyString());

		this.orderController.activateOrder(order, order.getFederationToken());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		try {
			this.application.deleteCompute(order.getId(), order.getFederationToken().getAccessId());
			Assert.fail();
		} catch (UnauthorizedException e) {
			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	@Test(expected = OrderManagementException.class)
	public void testDeleteComputeOrderNullGet() throws Exception {
		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Order order = createComputeOrder();
		Mockito.doReturn(null).when(this.orderController).getOrder(Mockito.anyString(), Mockito.any(Token.User.class),
				Mockito.any(OrderType.class));

		Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		this.application.deleteCompute(order.getId(), order.getFederationToken().getAccessId());
	}

	@Test
	public void testDeleteComputeOrderUnauthorizedOperation() throws Exception {
		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Order order = createComputeOrder();
		this.orderController.activateOrder(order, order.getFederationToken());

		Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(Token.class),
				Mockito.any(Operation.class), Mockito.any(Order.class));

		Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.any(Order.class));

		try {
			this.application.deleteCompute(order.getId(), order.getFederationToken().getAccessId());
			Assert.fail();
		} catch (UnauthorizedException e) {
			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	@Test
	public void testCreateComputeOrder() throws Exception {
		ComputeOrder order = createComputeOrder();

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		Assert.assertNull(order.getOrderState());

		this.application.createCompute(order, order.getFederationToken().getAccessId());

		Assert.assertEquals(OrderState.OPEN, order.getOrderState());
	}

	@Test
	public void testCreateComputeOrderUnauthenticated() throws Exception {
		ComputeOrder order = createComputeOrder();

		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		try {
			this.application.createCompute(order, order.getFederationToken().getAccessId());
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
				.getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		Assert.assertNull(order.getOrderState());

		try {
			this.application.createCompute(order, order.getFederationToken().getAccessId());
			Assert.fail();
		} catch (UnauthenticatedException e) {
			Assert.assertNull(order.getOrderState());
		}
	}

	@Test
	public void testCreateComputeOrderTokenUnauthorized() throws Exception {
		ComputeOrder order = createComputeOrder();

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		Assert.assertNull(order.getOrderState());

		try {
			this.application.createCompute(order, order.getFederationToken().getAccessId());
			Assert.fail();
		} catch (UnauthorizedException e) {
			Assert.assertNull(order.getOrderState());
		}
	}

	@Test
	public void testCreateComputeOrderUnauthorizedOperation() throws Exception {
		ComputeOrder order = createComputeOrder();

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(Token.class),
				Mockito.any(Operation.class), Mockito.any(OrderType.class));

		Assert.assertNull(order.getOrderState());

		try {
			this.application.createCompute(order, order.getFederationToken().getAccessId());
			Assert.fail();
		} catch (UnauthorizedException e) {
			Assert.assertNull(order.getOrderState());
		}
	}

	@Test(expected = OrderManagementException.class)
	public void testCreateNullComputeOrder() throws Exception {
		ComputeOrder order = createComputeOrder();

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(OrderType.class));

		Assert.assertNull(order.getOrderState());

		this.application.createCompute(null, order.getFederationToken().getAccessId());
	}

	@Test
	public void testGetComputeOrder() throws Exception {
		ComputeOrder order = createComputeOrder();

		this.orderController.activateOrder(order, order.getFederationToken());

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(OrderType.class));

		ComputeOrder actualOrder = this.application.getCompute(order.getId(), order.getFederationToken().getAccessId());

		Assert.assertSame(order, actualOrder);
	}

	@Test(expected = UnauthenticatedException.class)
	public void testGetComputeOrderUnauthenticated() throws Exception {
		ComputeOrder order = createComputeOrder();

		this.orderController.activateOrder(order, order.getFederationToken());

		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		this.application.getCompute(order.getId(), order.getFederationToken().getAccessId());
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetComputeOrderTokenUnauthorized() throws Exception {
		ComputeOrder order = createComputeOrder();

		this.orderController.activateOrder(order, order.getFederationToken());

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		this.application.getCompute(order.getId(), order.getFederationToken().getAccessId());
	}

	@Test(expected = UnauthenticatedException.class)
	public void testGetComputeOrderTokenUnauthenticated() throws Exception {
		ComputeOrder order = createComputeOrder();

		this.orderController.activateOrder(order, order.getFederationToken());

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
				.getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(Order.class));

		this.application.getCompute(order.getId(), order.getFederationToken().getAccessId());
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetComputeOrderUnauthorizedOperation() throws Exception {
		ComputeOrder order = createComputeOrder();

		this.orderController.activateOrder(order, order.getFederationToken());

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(Token.class),
				Mockito.any(Operation.class), Mockito.any(Order.class));

		this.application.getCompute(order.getId(), order.getFederationToken().getAccessId());
	}

	@Test
	public void testGetAllComputes() throws OrderManagementException, UnauthenticatedException, UnauthorizedException {
		ComputeOrder order = createComputeOrder();

		this.orderController.activateOrder(order, order.getFederationToken());

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(OrderType.class));

		List<ComputeOrder> allComputes = this.application.getAllComputes(order.getFederationToken().getAccessId());

		Assert.assertEquals(1, allComputes.size());

		Assert.assertSame(order, allComputes.get(0));
	}

	@Test
	public void testGetAllComputesEmpty()
			throws OrderManagementException, UnauthenticatedException, UnauthorizedException {
		ComputeOrder order = createComputeOrder();

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(OrderType.class));

		List<ComputeOrder> allComputes = this.application.getAllComputes(order.getFederationToken().getAccessId());

		Assert.assertEquals(0, allComputes.size());
	}

	@Test(expected = UnauthenticatedException.class)
	public void testGetAllComputesUnauthenticated()
			throws OrderManagementException, UnauthenticatedException, UnauthorizedException {
		ComputeOrder order = createComputeOrder();

		this.orderController.activateOrder(order, order.getFederationToken());

		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(OrderType.class));

		this.application.getAllComputes(order.getFederationToken().getAccessId());
	}

	@Test(expected = UnauthenticatedException.class)
	public void testGetAllComputesTokenUnauthenticated()
			throws OrderManagementException, UnauthenticatedException, UnauthorizedException {
		ComputeOrder order = createComputeOrder();

		this.orderController.activateOrder(order, order.getFederationToken());

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
				.getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(OrderType.class));

		this.application.getAllComputes(order.getFederationToken().getAccessId());
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetAllComputesTokenUnauthorized()
			throws OrderManagementException, UnauthenticatedException, UnauthorizedException {
		ComputeOrder order = createComputeOrder();

		this.orderController.activateOrder(order, order.getFederationToken());

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(OrderType.class));

		this.application.getAllComputes(order.getFederationToken().getAccessId());
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetAllComputesOperationUnauthorized()
			throws OrderManagementException, UnauthenticatedException, UnauthorizedException {
		ComputeOrder order = createComputeOrder();

		this.orderController.activateOrder(order, order.getFederationToken());

		Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

		Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

		Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
				Mockito.any(OrderType.class));

		this.application.getAllComputes(order.getFederationToken().getAccessId());
	}

	@Test
    public void testCreateVolumeOrder() throws Exception {
        VolumeOrder order = createVolumeOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Assert.assertNull(order.getOrderState());

        this.application.createVolume(order, order.getFederationToken().getAccessId());

        Assert.assertEquals(OrderState.OPEN, order.getOrderState());
    }
	
	@Test
    public void testCreateVolumeOrderUnauthenticated() throws Exception {
        VolumeOrder order = createVolumeOrder();

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        try {
            this.application.createVolume(order, order.getFederationToken().getAccessId());
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
                .getFederationToken(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Assert.assertNull(order.getOrderState());

        try {
            this.application.createVolume(order, order.getFederationToken().getAccessId());
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertNull(order.getOrderState());
        }
    }
	
	@Test
    public void testCreateVolumeOrderTokenUnauthorized() throws Exception {
        VolumeOrder order = createVolumeOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Assert.assertNull(order.getOrderState());

        try {
            this.application.createVolume(order, order.getFederationToken().getAccessId());
            Assert.fail();
        } catch (UnauthorizedException e) {
            Assert.assertNull(order.getOrderState());
        }
    }
	
	@Test
    public void testCreateVolumeOrderUnauthorizedOperation() throws Exception {
        VolumeOrder order = createVolumeOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(Token.class),
                Mockito.any(Operation.class), Mockito.any(OrderType.class));

        Assert.assertNull(order.getOrderState());

        try {
            this.application.createVolume(order, order.getFederationToken().getAccessId());
            Assert.fail();
        } catch (UnauthorizedException e) {
            Assert.assertNull(order.getOrderState());
        }
    }
	
	@Test(expected = OrderManagementException.class)
    public void testCreateNullVolumeOrder() throws Exception {
        VolumeOrder order = createVolumeOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(OrderType.class));

        Assert.assertNull(order.getOrderState());

        this.application.createVolume(null, order.getFederationToken().getAccessId());
    }
	
	@Test
    public void testGetVolumeOrder() throws Exception {
        VolumeOrder order = createVolumeOrder();

        this.orderController.activateOrder(order, order.getFederationToken());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(OrderType.class));

        VolumeOrder actualOrder = this.application.getVolume(order.getId(), order.getFederationToken().getAccessId());

        Assert.assertSame(order, actualOrder);
    }
	
	@Test(expected = UnauthenticatedException.class)
    public void testGetVolumeOrderUnauthenticated() throws Exception {
        ComputeOrder order = createComputeOrder();

        this.orderController.activateOrder(order, order.getFederationToken());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        this.application.getVolume(order.getId(), order.getFederationToken().getAccessId());
    }
	
	@Test(expected = UnauthorizedException.class)
    public void testGetVolumeOrderTokenUnauthorized() throws Exception {
        VolumeOrder order = createVolumeOrder();

        this.orderController.activateOrder(order, order.getFederationToken());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        this.application.getVolume(order.getId(), order.getFederationToken().getAccessId());
    }
	
	@Test(expected = UnauthenticatedException.class)
    public void testGetVolumeOrderTokenUnauthenticated() throws Exception {
        VolumeOrder order = createVolumeOrder();

        this.orderController.activateOrder(order, order.getFederationToken());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
                .getFederationToken(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        this.application.getVolume(order.getId(), order.getFederationToken().getAccessId());
    }
	
	@Test(expected = UnauthorizedException.class)
    public void testGetVolumeOrderUnauthorizedOperation() throws Exception {
        VolumeOrder order = createVolumeOrder();

        this.orderController.activateOrder(order, order.getFederationToken());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(Token.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        this.application.getVolume(order.getId(), order.getFederationToken().getAccessId());
    }
	
	@Test
    public void testGetAllVolumes() throws OrderManagementException, UnauthenticatedException, UnauthorizedException {
        VolumeOrder order = createVolumeOrder();

        this.orderController.activateOrder(order, order.getFederationToken());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(OrderType.class));

        List<VolumeOrder> allVolumes = this.application.getAllVolumes(order.getFederationToken().getAccessId());

        Assert.assertEquals(1, allVolumes.size());

        Assert.assertSame(order, allVolumes.get(0));
    }
	
	@Test
    public void testGetAllVolumesEmpty()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException {
        VolumeOrder order = createVolumeOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(OrderType.class));

        List<VolumeOrder> allVolumes = this.application.getAllVolumes(order.getFederationToken().getAccessId());

        Assert.assertEquals(0, allVolumes.size());
    }
	
	@Test(expected = UnauthenticatedException.class)
    public void testGetAllVolumesTokenUnauthenticated()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException {
        VolumeOrder order = createVolumeOrder();

        this.orderController.activateOrder(order, order.getFederationToken());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
                .getFederationToken(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(OrderType.class));

        this.application.getAllVolumes(order.getFederationToken().getAccessId());
    }
	
	@Test(expected = UnauthorizedException.class)
    public void testGetAllVolumesTokenUnauthorized()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException {
        VolumeOrder order = createVolumeOrder();

        this.orderController.activateOrder(order, order.getFederationToken());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(OrderType.class));

        this.application.getAllVolumes(order.getFederationToken().getAccessId());
    }
	
	@Test(expected = UnauthorizedException.class)
    public void testGetAllVolumesOperationUnauthorized()
            throws OrderManagementException, UnauthenticatedException, UnauthorizedException {
        VolumeOrder order = createVolumeOrder();

        this.orderController.activateOrder(order, order.getFederationToken());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(OrderType.class));

        this.application.getAllVolumes(order.getFederationToken().getAccessId());
    }
	
	@Test
    public void testDeleteVolumeOrder() throws Exception {
        VolumeOrder order = createVolumeOrder();
        
        this.orderController.activateOrder(order, order.getFederationToken());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());
        
        Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        this.application.deleteVolume(order.getId(), order.getFederationToken().getAccessId());

        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }
	
	@Test
    public void testDeleteVolumeOrderUnathenticated() throws Exception {
        VolumeOrder order = createVolumeOrder();
        
        this.orderController.activateOrder(order, order.getFederationToken());
        
        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString(), Mockito.any(Token.User.class),
                Mockito.any(OrderType.class));

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        try {
            this.application.deleteVolume(order.getId(), order.getFederationToken().getAccessId());
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }
	
	@Test
    public void testDeleteVolumeOrderTokenUnathenticated() throws Exception {
        VolumeOrder order = createVolumeOrder();
        
        this.orderController.activateOrder(order, order.getFederationToken());
        
        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());
        
        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));
        
        Mockito.doThrow(new UnauthenticatedException()).when(this.aaaController)
                .getFederationToken(Mockito.anyString());

        try {
            this.application.deleteVolume(order.getId(), order.getFederationToken().getAccessId());
            Assert.fail();
        } catch (UnauthenticatedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }
	
	@Test
    public void testDeleteVolumeOrderTokenUnauthorized() throws Exception {
        VolumeOrder order = createVolumeOrder();
        
        this.orderController.activateOrder(order, order.getFederationToken());
        
        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());
        
        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));
        
        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).getFederationToken(Mockito.anyString());

        try {
            this.application.deleteVolume(order.getId(), order.getFederationToken().getAccessId());
            Assert.fail();
        } catch (UnauthorizedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }
	
	@Test(expected = OrderManagementException.class)
    public void testDeleteVolumeOrderNullGet() throws Exception {
	    VolumeOrder order = createVolumeOrder();
	    
	    Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());
        
        Mockito.doReturn(null).when(this.orderController).getOrder(Mockito.anyString(), Mockito.any(Token.User.class),
                Mockito.any(OrderType.class));

        Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(Token.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        this.application.deleteVolume(order.getId(), order.getFederationToken().getAccessId());
    }

    @Test
    public void testDeleteVolumeOrderUnauthorizedOperation() throws Exception {
        VolumeOrder order = createVolumeOrder();
        
        this.orderController.activateOrder(order, order.getFederationToken());
        
        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());        

        Mockito.doReturn(order.getFederationToken()).when(this.aaaController).getFederationToken(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedException()).when(this.aaaController).authorize(Mockito.any(Token.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.any(Order.class));

        try {
            this.application.deleteVolume(order.getId(), order.getFederationToken().getAccessId());
            Assert.fail();
        } catch (UnauthorizedException e) {
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }
	
	private ComputeOrder createComputeOrder() {
		Token federationToken = createToken();
		ComputeOrder order = new ComputeOrder(federationToken, "fake-member-id", "fake-member-id", 2, 2, 30,
				"fake-image-name", new UserData(), "fake-public-key");
		return order;
	}
	
	private VolumeOrder createVolumeOrder() {
        Token federationToken = createToken();
        VolumeOrder volumeOrder = new VolumeOrder(federationToken, "fake-member-id", "fake-member-id", 1);
        return volumeOrder;
    }
	
	private Token createToken() {
	    Token.User user = new Token.User("fake-user-id", "fake-user-name");
        Token federationToken = new Token("fake-accessId", user, new Date(), new HashMap<String, String>());
	    return federationToken;
	}

}
