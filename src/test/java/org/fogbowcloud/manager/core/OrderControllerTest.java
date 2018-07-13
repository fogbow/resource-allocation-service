package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.cloudconnector.LocalCloudConnector;
import org.fogbowcloud.manager.core.datastore.DatabaseManager;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.linkedlists.ChainedList;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatabaseManager.class, CloudConnectorFactory.class})
public class OrderControllerTest extends BaseUnitTests {

    private OrderController ordersController;

    private Map<String, Order> activeOrdersMap;
    private ChainedList openOrdersList;
    private ChainedList pendingOrdersList;
    private ChainedList spawningOrdersList;
    private ChainedList fulfilledOrdersList;
    private ChainedList failedOrdersList;
    private ChainedList closedOrdersList;
    private String localMember = BaseUnitTests.LOCAL_MEMBER_ID;

    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");

        DatabaseManager databaseManager = Mockito.mock(DatabaseManager.class);
        when(databaseManager.readActiveOrders(OrderState.OPEN)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.SPAWNING)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.FAILED)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.FULFILLED)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.PENDING)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.CLOSED)).thenReturn(new SynchronizedDoublyLinkedList());

        doNothing().when(databaseManager).add(any(Order.class));
        doNothing().when(databaseManager).update(any(Order.class));

        PowerMockito.mockStatic(DatabaseManager.class);
        given(DatabaseManager.getInstance()).willReturn(databaseManager);

        this.ordersController = new OrderController();

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();

        this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        this.openOrdersList = sharedOrderHolders.getOpenOrdersList();
        this.pendingOrdersList = sharedOrderHolders.getPendingOrdersList();
        this.spawningOrdersList = sharedOrderHolders.getSpawningOrdersList();
        this.fulfilledOrdersList = sharedOrderHolders.getFulfilledOrdersList();
        this.failedOrdersList = sharedOrderHolders.getFailedOrdersList();
        this.closedOrdersList = sharedOrderHolders.getClosedOrdersList();
    }

    @Test
    public void testNewOrderRequest() throws UnexpectedException {
        ComputeOrder computeOrder = new ComputeOrder();
        @SuppressWarnings("unused")
        FederationUser federationUser = new FederationUser("fake-id", null);
        OrderStateTransitioner.activateOrder(computeOrder);
    }

    /**
     * There is no matching method in the 'OrdersController' class
     * @throws UnexpectedException 
     */
    @Test(expected = UnexpectedException.class)
    public void testFailedNewOrderRequestOrderIsNull() throws UnexpectedException {
    	Order order = null;
    	@SuppressWarnings("unused")
    	FederationUser federationUser = new FederationUser("fake-id", null);
    	OrderStateTransitioner.activateOrder(order);
    }

    @Test(expected = FogbowManagerException.class)
    public void testDeleteOrderStateClosed() throws UnexpectedException, OrderNotFoundException {
        String orderId = getComputeOrderCreationId(OrderState.CLOSED);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        this.ordersController.deleteOrder(computeOrder);
    }

    @Test
    public void testGetAllOrders() throws UnexpectedException {
        FederationUser federationUser = new FederationUser("fake-id", null);
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setFederationUser(federationUser);
        computeOrder.setRequestingMember(this.localMember);
        computeOrder.setProvidingMember(this.localMember);
        computeOrder.setOrderState(OrderState.OPEN);

        this.openOrdersList.addItem(computeOrder);

        this.ordersController.getAllOrders(federationUser, InstanceType.COMPUTE);

        Assert.assertEquals(computeOrder, this.openOrdersList.getNext());
    }

    @Test
    public void testGetOrder() throws UnexpectedException, FogbowManagerException {
        String orderId = getComputeOrderCreationId(OrderState.OPEN);
        FederationUser federationUser = new FederationUser("fake-id", null);

        ComputeOrder computeOrder = (ComputeOrder) this.ordersController.getOrder(
                orderId, federationUser, InstanceType.COMPUTE);
        Assert.assertEquals(computeOrder, this.openOrdersList.getNext());
    }

    @Test(expected = InstanceNotFoundException.class)
    public void testGetInvalidOrder() throws FogbowManagerException {
        this.ordersController.getOrder("invalid-order-id", null, InstanceType.COMPUTE);
    }

    @Test(expected = InstanceNotFoundException.class)
    public void testGetOrderWithInvalidInstanceType() throws FogbowManagerException, UnexpectedException {
        String orderId = getComputeOrderCreationId(OrderState.OPEN);

        this.ordersController.getOrder(orderId, null, InstanceType.NETWORK);
    }

    @Test(expected = UnauthorizedRequestException.class)
    public void testGetOrderWithInvalidFedUser() throws FogbowManagerException, UnexpectedException {
        String orderId = getComputeOrderCreationId(OrderState.OPEN);

        FederationUser federationUser = new FederationUser("another-id", null);

        this.ordersController.getOrder(orderId, federationUser, InstanceType.COMPUTE);
    }

    @Test
    public void testGetResourceInstance() throws Exception {
        LocalCloudConnector localCloudConnector = Mockito.mock(LocalCloudConnector.class);

        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
        when(cloudConnectorFactory.getCloudConnector(anyString())).thenReturn(localCloudConnector);

        Order order = createLocalOrder();
        order.setOrderState(OrderState.FULFILLED);

        this.fulfilledOrdersList.addItem(order);

        String instanceId = "instanceid";
        Instance orderInstance = Mockito.spy(new ComputeInstance(instanceId));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(instanceId);

        doReturn(orderInstance).when(localCloudConnector).getInstance(Mockito.any(Order.class));

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        Instance instance = this.ordersController.getResourceInstance(order);
        Assert.assertEquals(instance, orderInstance);
    }

    @Test
    public void testGetUserAllocation() throws UnexpectedException {
        FederationUser federationUser = new FederationUser("fake-id", null);
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setFederationUser(federationUser);
        computeOrder.setRequestingMember(this.localMember);
        computeOrder.setProvidingMember(this.localMember);
        computeOrder.setOrderState(OrderState.FULFILLED);

        computeOrder.setActualAllocation(new ComputeAllocation(1, 2, 3));

        this.fulfilledOrdersList.addItem(computeOrder);
        this.activeOrdersMap.put(computeOrder.getId(), computeOrder);

        ComputeAllocation allocation = (ComputeAllocation) this.ordersController.getUserAllocation(
                this.localMember, federationUser, InstanceType.COMPUTE);

        Assert.assertEquals(allocation.getInstances(), computeOrder.getActualAllocation().getInstances());
        Assert.assertEquals(allocation.getRam(), computeOrder.getActualAllocation().getRam());
        Assert.assertEquals(allocation.getvCPU(), computeOrder.getActualAllocation().getvCPU());
    }

    @Test(expected = UnexpectedException.class)
    public void testGetUserAllocationInvalidInstanceType() throws UnexpectedException {
        FederationUser federationUser = new FederationUser("fake-id", null);
        NetworkOrder networkOrder = new NetworkOrder();
        networkOrder.setFederationUser(federationUser);
        networkOrder.setRequestingMember(this.localMember);
        networkOrder.setProvidingMember(this.localMember);
        networkOrder.setOrderState(OrderState.FULFILLED);

        this.fulfilledOrdersList.addItem(networkOrder);
        this.activeOrdersMap.put(networkOrder.getId(), networkOrder);

        this.ordersController.getUserAllocation(this.localMember, federationUser, InstanceType.NETWORK);
    }

    @Test
    public void testDeleteOrderStateFailed() throws UnexpectedException, OrderNotFoundException {
        String orderId = getComputeOrderCreationId(OrderState.FAILED);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        Assert.assertNull(this.closedOrdersList.getNext());

        this.ordersController.deleteOrder(computeOrder);

        Order test = this.closedOrdersList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
    }

    @Test
    public void testDeleteOrderStateFulfilled() throws UnexpectedException, OrderNotFoundException {
        String orderId = getComputeOrderCreationId(OrderState.FULFILLED);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        Assert.assertNull(this.closedOrdersList.getNext());

        this.ordersController.deleteOrder(computeOrder);

        Order test = this.closedOrdersList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
    }

    @Test
    public void testDeleteOrderStateSpawning() throws UnexpectedException, OrderNotFoundException {
        String orderId = getComputeOrderCreationId(OrderState.SPAWNING);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        Assert.assertNull(this.closedOrdersList.getNext());

        this.ordersController.deleteOrder(computeOrder);

        Order test = this.closedOrdersList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
    }

    @Test
    public void testDeleteOrderStatePending() throws UnexpectedException, OrderNotFoundException {
        String orderId = getComputeOrderCreationId(OrderState.PENDING);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        Assert.assertNull(this.closedOrdersList.getNext());

        this.ordersController.deleteOrder(computeOrder);

        Order test = this.closedOrdersList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
    }

    @Test
    public void testDeleteOrderStateOpen() throws UnexpectedException, OrderNotFoundException {
        String orderId = getComputeOrderCreationId(OrderState.OPEN);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        Assert.assertNull(this.closedOrdersList.getNext());

        this.ordersController.deleteOrder(computeOrder);

        Order test = this.closedOrdersList.getNext();
        Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
    }

    @Test(expected = FogbowManagerException.class)
    public void testDeleteNullOrder() throws UnexpectedException, OrderNotFoundException {
        this.ordersController.deleteOrder(null);
    }

    private String getComputeOrderCreationId(OrderState orderState) throws UnexpectedException {
        String orderId = null;

        FederationUser federationUser = new FederationUser("fake-id", null);
        ComputeOrder computeOrder = Mockito.spy(new ComputeOrder());
        computeOrder.setFederationUser(federationUser);
        computeOrder.setRequestingMember(this.localMember);
        computeOrder.setProvidingMember(this.localMember);
        computeOrder.setOrderState(orderState);

        orderId = computeOrder.getId();

        this.activeOrdersMap.put(orderId, computeOrder);

        switch (orderState) {
            case OPEN:
                this.openOrdersList.addItem(computeOrder);
                break;
            case PENDING:
                this.pendingOrdersList.addItem(computeOrder);
                break;
            case SPAWNING:
                this.spawningOrdersList.addItem(computeOrder);
                break;
            case FULFILLED:
                this.fulfilledOrdersList.addItem(computeOrder);
                break;
            case FAILED:
                this.failedOrdersList.addItem(computeOrder);
                break;
            case CLOSED:
                this.closedOrdersList.addItem(computeOrder);
        }

        return orderId;
    }

    private Order createLocalOrder() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        UserData userData = Mockito.mock(UserData.class);
        String imageName = "fake-image-name";
        String requestingMember = "";
        String providingMember = "";
        String publicKey = "fake-public-key";

        Order localOrder =
                new ComputeOrder(
                        federationUser,
                        requestingMember,
                        providingMember,
                        8,
                        1024,
                        30,
                        imageName,
                        userData,
                        publicKey,
                        null);

        return localOrder;
    }
}
