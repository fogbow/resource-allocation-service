package cloud.fogbow.ras.core;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.DependencyDetectedException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.RequestStillBeingDispatchedException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.InstanceStatus;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;

@PrepareForTest({ CloudConnectorFactory.class, DatabaseManager.class })
public class OrderControllerTest extends BaseUnitTests {

    private static final String AVAILABLE_STATE = "available";
    private static final String FAKE_IP_ADDRESS = "0.0.0.0";
    private static final String INVALID_ORDER_ID = "invalid-order-id";
    
    private OrderController ordersController;
    private LocalCloudConnector localCloudConnector;
    private Map<String, Order> activeOrdersMap;
    private ChainedList<Order> openOrdersList;
    private ChainedList<Order> pendingOrdersList;
    private ChainedList<Order> spawningOrdersList;
    private ChainedList<Order> fulfilledOrdersList;
    private ChainedList<Order> failedAfterSuccessfulRequestOrdersList;
    private ChainedList<Order> failedOnRequestOrdersList;
    private ChainedList<Order> closedOrdersList;

    @Before
    public void setUp() throws UnexpectedException {
        // mocking database to return empty instances of SynchronizedDoublyLinkedList.
        this.testUtils.mockReadOrdersFromDataBase();

        // setting up the attributes.
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        this.openOrdersList = sharedOrderHolders.getOpenOrdersList();
        this.pendingOrdersList = sharedOrderHolders.getPendingOrdersList();
        this.spawningOrdersList = sharedOrderHolders.getSpawningOrdersList();
        this.fulfilledOrdersList = sharedOrderHolders.getFulfilledOrdersList();
        this.failedAfterSuccessfulRequestOrdersList = sharedOrderHolders.getFailedAfterSuccessfulRequestOrdersList();
        this.closedOrdersList = sharedOrderHolders.getClosedOrdersList();
        this.ordersController = Mockito.spy(new OrderController());
        this.localCloudConnector = this.testUtils.mockLocalCloudConnectorFromFactory();
    }

    // test case: when try to delete an Order closed, it must raise an InstanceNotFoundException.
    @Test(expected = InstanceNotFoundException.class) // verify
    public void testDeleteClosedOrderThrowsInstanceNotFoundException()
            throws Exception {

        // set up
        String orderId = setupOrder(OrderState.CLOSED);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // exercise
        this.ordersController.deleteOrder(computeOrder);
    }

    // test case: Checks if the getInstancesStatusmethod returns exactly the same
    // list of instances that were added on the lists.
    @Test
    public void testGetAllInstancesStatus() throws Exception {
        // set up
        SystemUser systemUser = this.testUtils.createSystemUser();

        ComputeOrder computeOrderFulfilled = this.testUtils.createLocalComputeOrder();
        computeOrderFulfilled.setSystemUser(systemUser);
        computeOrderFulfilled.setOrderState(OrderState.FULFILLED);

        ComputeOrder computeOrderFailed = this.testUtils.createLocalComputeOrder();
        computeOrderFailed.setSystemUser(systemUser);
        computeOrderFailed.setOrderState(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);

        this.activeOrdersMap.put(computeOrderFulfilled.getId(), computeOrderFulfilled);
        this.fulfilledOrdersList.addItem(computeOrderFulfilled);

        this.activeOrdersMap.put(computeOrderFailed.getId(), computeOrderFailed);
        this.failedAfterSuccessfulRequestOrdersList.addItem(computeOrderFailed);

        InstanceStatus statusOrderFulfilled = createInstanceStatus(computeOrderFulfilled);
        InstanceStatus statusOrderFailed = createInstanceStatus(computeOrderFailed);

        // exercise
        List<InstanceStatus> instances = this.ordersController.getInstancesStatus(systemUser, ResourceType.COMPUTE);

        // verify
        Assert.assertTrue(instances.contains(statusOrderFulfilled));
        Assert.assertTrue(instances.contains(statusOrderFailed));
        Assert.assertEquals(2, instances.size());
    }

    // test case: Checks if the getOrder method returns exactly the same order that
    // were added on the list.
    @Test
    public void testGetOrder() throws Exception {
        // set up
        String orderId = setupOrder(OrderState.OPEN);

        // exercise
        ComputeOrder computeOrder = (ComputeOrder) this.ordersController.getOrder(orderId);

        // verify
        Assert.assertSame(computeOrder, this.openOrdersList.getNext());
    }

    // test case: Get a not active Order, must throw InstanceNotFoundException.
    @Test(expected = InstanceNotFoundException.class) // verify
    public void testGetInactiveOrder() throws InstanceNotFoundException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        String orderId = order.getId();

        // verify
        Assert.assertNull(this.activeOrdersMap.get(orderId));

        // exercise
        this.ordersController.getOrder(orderId);
    }

    // test case: Getting order with a null systemUser must throw InstanceNotFoundException.
    @Test(expected = InstanceNotFoundException.class)
    public void testGetInvalidOrder() throws FogbowException {
        // exercise
        this.ordersController.getOrder(INVALID_ORDER_ID);
    }

    //test case: Checks if attempt to activate null order throws UnexpectedException
    @Test(expected = UnexpectedException.class)
    public void testActivateNullOrder() throws FogbowException {
        // set up
        Order order = null;

        // exercise
        this.ordersController.activateOrder(order);
    }

    // test case: Attempt to activate the same order more than once must throw
    // UnexpectedException and only one order will be placed in the active order map.
    @Test
    public void testActivateOrderTwice() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        String expected = String.format(Messages.Exception.REQUEST_ID_ALREADY_ACTIVATED, order.getId());

        // exercise
        this.ordersController.activateOrder(order);
        try {
            this.ordersController.activateOrder(order);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
            Assert.assertEquals(1, this.activeOrdersMap.size());
        }
    }

    // test case: Attempt to deactivate the same order more than
    // once must throw UnexpectedException
    @Test
    public void testDeactivateOrderTwice() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        String expected = String.format(Messages.Exception.UNABLE_TO_REMOVE_INACTIVE_REQUEST, order.getId());

        this.ordersController.activateOrder(order);
        
        // exercise
        this.ordersController.deactivateOrder(order);
        try {
            this.ordersController.deactivateOrder(order);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: Checks if deactivateOrder changes it's state to DEACTIVATED
    @Test
    public void testDeactivateOrderSuccess() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());

        this.ordersController.activateOrder(order);
        Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        
        // exercise
        this.ordersController.deactivateOrder(order);

        // verify
        Assert.assertEquals(OrderState.DEACTIVATED, order.getOrderState());
    }

    // test case: Creates an order with dependencies and check if the order id
    // will be inserted into dependencies.
    @Test
    public void testCreateOrderWithDependencies() throws FogbowException {
        // set up
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        VolumeOrder volumeOrder = this.testUtils.createLocalVolumeOrder();

        this.ordersController.activateOrder(computeOrder);
        this.ordersController.activateOrder(volumeOrder);

        AttachmentOrder attachmentOrder = this.testUtils.createLocalAttachmentOrder(computeOrder, volumeOrder);

        // exercise
        this.ordersController.activateOrder(attachmentOrder);

        // verify
        Assert.assertTrue(this.ordersController.hasOrderDependencies(computeOrder.getId()));
    }

    // test case: Creates an order with dependencies and attempt to delete
    // it must throw an DependencyDetectedException.
    @Test(expected = DependencyDetectedException.class) // verify
    public void testDeleteOrderWithoutRemovingDependenciesFirst() throws FogbowException {
        // set up
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        VolumeOrder volumeOrder = this.testUtils.createLocalVolumeOrder();

        this.ordersController.activateOrder(computeOrder);
        this.ordersController.activateOrder(volumeOrder);

        AttachmentOrder attachmentOrder = this.testUtils.createLocalAttachmentOrder(computeOrder, volumeOrder);
        this.ordersController.activateOrder(attachmentOrder);

        // exercise
        this.ordersController.deleteOrder(volumeOrder);
    }

    // test case: Creates an order with dependencies and attempt to delete
    // them in correct order must not throw any exceptions.
    @Test
    public void testDeleteOrderWithDependencies() throws FogbowException {
        // set up
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        VolumeOrder volumeOrder = this.testUtils.createLocalVolumeOrder();

        this.ordersController.activateOrder(computeOrder);
        Assert.assertSame(computeOrder, this.openOrdersList.getNext());
        
        this.ordersController.activateOrder(volumeOrder);
        Assert.assertSame(volumeOrder, this.openOrdersList.getNext());

        AttachmentOrder attachmentOrder = this.testUtils.createLocalAttachmentOrder(computeOrder, volumeOrder);
        this.ordersController.activateOrder(attachmentOrder);
        Assert.assertSame(attachmentOrder, this.openOrdersList.getNext());
        
        Assert.assertNull(this.closedOrdersList.getNext());

        // exercise
        this.ordersController.deleteOrder(attachmentOrder);
        this.ordersController.deleteOrder(volumeOrder);
        this.ordersController.deleteOrder(computeOrder);
        
        // verify
        Assert.assertNull(this.openOrdersList.getNext());
        
        AttachmentOrder testAttachmentOrder = (AttachmentOrder) this.closedOrdersList.getNext();
        Assert.assertEquals(OrderState.CLOSED, testAttachmentOrder.getOrderState());
        
        VolumeOrder testVolumeOrder = (VolumeOrder) this.closedOrdersList.getNext();
        Assert.assertEquals(OrderState.CLOSED, testVolumeOrder.getOrderState());
        
        ComputeOrder testComputeOrder = (ComputeOrder) this.closedOrdersList.getNext();
        Assert.assertEquals(OrderState.CLOSED, testComputeOrder.getOrderState());
    }

    // test case: When invoking the deleteOrder method, it must call the
    // deleteInstance method on the Local Cloud Connector to remove the 
    // order instance and change its order state to CLOSED.
    @Test
    public void testDeleteOrderWithInstanceRunning() throws FogbowException {
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        computeOrder.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.ordersController.activateOrder(computeOrder);

        // exercise
        this.ordersController.deleteOrder(computeOrder);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(1)).deleteInstance(Mockito.any());
        Assert.assertEquals(OrderState.CLOSED, computeOrder.getOrderState());
    }

    // test case: Checks if given an OPEN order getResourceInstance() throws
    // RequestStillBeingDispatchedException.
    @Test(expected = RequestStillBeingDispatchedException.class)
    public void testGetResourceInstanceOfOpenOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createRemoteOrder(this.testUtils.getLocalMemberId());
        order.setOrderState(OrderState.OPEN);

        // exercise
        this.ordersController.getResourceInstance(order);
    }

    // test case: Checks if given an order in the getResourceInstance method returns its instance.
    @Test
    public void testGetResourceInstance() throws Exception {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setOrderState(OrderState.FULFILLED);
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);

        this.fulfilledOrdersList.addItem(order);
        this.activeOrdersMap.put(order.getId(), order);

        ComputeInstance computeInstance = new ComputeInstance(TestUtils.FAKE_INSTANCE_ID);
        computeInstance.setState(InstanceState.READY);

        Mockito.doReturn(computeInstance).when(this.localCloudConnector)
                .getInstance(Mockito.any(Order.class));

        // exercise
        this.ordersController.getResourceInstance(order);

        // verify
        Mockito.verify(this.ordersController, Mockito.times(1)).getCloudConnector(Mockito.eq(order));
        Mockito.verify(this.localCloudConnector, Mockito.times(1)).getInstance(Mockito.eq(order));
    }

    // test case: Checks if given an attachment order in the getResourceInstance method returns its instance.
    @Test
    public void testGetAttachmentInstance() throws Exception {
        // set up
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        VolumeOrder volumeOrder = this.testUtils.createLocalVolumeOrder();
        AttachmentOrder attachmentOrder = this.testUtils.createLocalAttachmentOrder(computeOrder, volumeOrder);

        this.ordersController.activateOrder(computeOrder);
        this.ordersController.activateOrder(volumeOrder);
        this.ordersController.activateOrder(attachmentOrder);

        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        computeOrder.setInstanceId(instanceId);
        volumeOrder.setInstanceId(instanceId);

        attachmentOrder.setInstanceId(instanceId);
        String cloudState = AVAILABLE_STATE;
        String device = TestUtils.FAKE_DEVICE;
        AttachmentInstance attachmentInstance =
                new AttachmentInstance(
                        instanceId,
                        cloudState,
                        computeOrder.getInstanceId(),
                        volumeOrder.getInstanceId(),
                        device);

        Mockito.doReturn(attachmentInstance).when(this.localCloudConnector).getInstance(Mockito.eq(attachmentOrder));

        // exercise
        this.ordersController.getResourceInstance(attachmentOrder);

        // verify
        Mockito.verify(this.ordersController, Mockito.times(1)).getCloudConnector(Mockito.eq(attachmentOrder));
        Mockito.verify(this.localCloudConnector, Mockito.times(1)).getInstance(Mockito.eq(attachmentOrder));
    }

    // test case: Checks if given an public ip order in the getResourceInstance method returns its instance.
    @Test
    public void testGetPublicIpInstance() throws Exception {
        // set up
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        PublicIpOrder publicIpOrder = this.testUtils.createLocalPublicIpOrder(computeOrder.getId());

        this.ordersController.activateOrder(computeOrder);
        this.ordersController.activateOrder(publicIpOrder);

        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        computeOrder.setInstanceId(instanceId);
        publicIpOrder.setInstanceId(instanceId);

        String cloudState = AVAILABLE_STATE;
        String ip = FAKE_IP_ADDRESS;
        PublicIpInstance publicIpInstance =
                new PublicIpInstance(
                        instanceId,
                        cloudState,
                        ip);

        Mockito.doReturn(publicIpInstance).when(this.localCloudConnector).getInstance(Mockito.eq(publicIpOrder));

        // exercise
        this.ordersController.getResourceInstance(publicIpOrder);

        // verify
        Mockito.verify(this.ordersController, Mockito.times(1)).getCloudConnector(Mockito.eq(publicIpOrder));
        Mockito.verify(this.localCloudConnector, Mockito.times(1)).getInstance(Mockito.eq(publicIpOrder));
    }

    // test case: Checks if given an remote order in the getResourceInstance method returns its instance.
    @Test public void testRemoteGetResourceInstance() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        order.setOrderState(OrderState.FULFILLED);

        this.fulfilledOrdersList.addItem(order);
        this.activeOrdersMap.put(order.getId(), order);

        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        OrderInstance orderInstance = new ComputeInstance(instanceId);
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(instanceId);

        Mockito.doReturn(orderInstance).when(this.localCloudConnector)
                .getInstance(Mockito.any(Order.class));

        // exercise
        this.ordersController.getResourceInstance(order);

        // verify
        Mockito.verify(this.ordersController, Mockito.times(1)).getCloudConnector(Mockito.eq(order));
        Mockito.verify(this.localCloudConnector, Mockito.times(1)).getInstance(Mockito.eq(order));
    }

    // test case: Requesting a null order must return a UnexpectedException.
    @Test(expected = UnexpectedException.class) // verify
    public void testGetResourceInstanceNullOrder() throws Exception {
        // exercise
        this.ordersController.getResourceInstance(null);
    }

    // test case: Tests if the getUserAllocation method returns the
    // ComputeAllocation properly.
    @Test
    public void testGetUserAllocationToComputeResourceType() throws UnexpectedException {
        // set up
        SystemUser systemUser = this.testUtils.createSystemUser();

        ComputeOrder computeOrder1 = createFulfilledComputeOrder(systemUser);
        computeOrder1.setActualAllocation(new ComputeAllocation(1, 2, 3));

        ComputeOrder computeOrder2 = createFulfilledComputeOrder(systemUser);
        computeOrder2.setActualAllocation(new ComputeAllocation(3, 2, 1));

        this.activeOrdersMap.put(computeOrder1.getId(), computeOrder1);
        this.activeOrdersMap.put(computeOrder2.getId(), computeOrder2);

        this.fulfilledOrdersList.addItem(computeOrder1);
        this.fulfilledOrdersList.addItem(computeOrder2);

        int expectedValue = 4;

        // exercise
        ComputeAllocation allocation = (ComputeAllocation) this.ordersController
                .getUserAllocation(TestUtils.LOCAL_MEMBER_ID, systemUser, ResourceType.COMPUTE);

        // verify
        Assert.assertEquals(expectedValue, allocation.getInstances());
        Assert.assertEquals(expectedValue, allocation.getRam());
        Assert.assertEquals(expectedValue, allocation.getvCPU());
    }
    
    // test case: Tests if the getUserAllocation method throws an Exception for an
    // Order with the ResourceType not implemented.
    @Test(expected = UnexpectedException.class)
    public void testGetUserAllocationWithInvalidResourceType() throws UnexpectedException {
        // set up
        SystemUser systemUser = this.testUtils.createSystemUser();
        NetworkOrder networkOrder = createFulfilledNetworkOrder(systemUser);

        this.fulfilledOrdersList.addItem(networkOrder);
        this.activeOrdersMap.put(networkOrder.getId(), networkOrder);

        // exercise
        this.ordersController.getUserAllocation(TestUtils.LOCAL_MEMBER_ID, systemUser, ResourceType.GENERIC_RESOURCE);
    }

    // test case: Checks if deleting a failed order, this one will be moved to the closed orders
    // list.
    @Test
    public void testDeleteOrderStateFailed()
            throws Exception {
        // set up
        String orderId = setupOrder(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // verify
        Assert.assertNotNull(this.failedAfterSuccessfulRequestOrdersList.getNext());
        Assert.assertNull(this.closedOrdersList.getNext());

        // exercise
        this.ordersController.deleteOrder(computeOrder);

        // verify
        Order order = this.closedOrdersList.getNext();
        this.failedAfterSuccessfulRequestOrdersList.resetPointer();

        Assert.assertNull(this.failedAfterSuccessfulRequestOrdersList.getNext());
        Assert.assertNotNull(order);
        Assert.assertEquals(computeOrder, order);
        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: Checks if deleting a fulfilled order, this one will be moved to the closed orders
    // list.
    @Test
    public void testDeleteOrderStateFulfilled()
            throws Exception {
        // set up
        String orderId = setupOrder(OrderState.FULFILLED);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // verify
        Assert.assertNotNull(this.fulfilledOrdersList.getNext());
        Assert.assertNull(this.closedOrdersList.getNext());

        // exercise
        this.ordersController.deleteOrder(computeOrder);

        // verify
        Order order = this.closedOrdersList.getNext();

        Assert.assertNull(this.fulfilledOrdersList.getNext());
        Assert.assertNotNull(order);
        Assert.assertEquals(computeOrder, order);
        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: Checks if deleting a spawning order, this one will be moved to the closed orders
    // list.
    @Test
    public void testDeleteOrderStateSpawning()
            throws Exception {
        // set up
        String orderId = setupOrder(OrderState.SPAWNING);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // verify
        Assert.assertNotNull(this.spawningOrdersList.getNext());
        Assert.assertNull(this.closedOrdersList.getNext());

        // exercise
        this.ordersController.deleteOrder(computeOrder);

        // verify
        Order order = this.closedOrdersList.getNext();

        Assert.assertNull(this.spawningOrdersList.getNext());
        Assert.assertNotNull(order);
        Assert.assertEquals(computeOrder, order);
        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: Checks if deleting a pending order, this one will be moved to the closed orders
    // list.
    @Test
    public void testDeleteOrderStatePending()
            throws Exception {
        // set up
        String orderId = setupOrder(OrderState.PENDING);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // verify
        Assert.assertNotNull(this.pendingOrdersList.getNext());
        Assert.assertNull(this.closedOrdersList.getNext());

        // exercise
        this.ordersController.deleteOrder(computeOrder);

        // verify
        Order order = this.closedOrdersList.getNext();
        Assert.assertNull(this.pendingOrdersList.getNext());

        Assert.assertNotNull(order);
        Assert.assertEquals(computeOrder, order);
        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: Checks if deleting a open order, this one will be moved to the closed orders list.
    @Test
    public void testDeleteOrderStateOpen()
            throws Exception {
        // set up
        String orderId = setupOrder(OrderState.OPEN);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // verify
        Assert.assertNotNull(this.openOrdersList.getNext());
        Assert.assertNull(this.closedOrdersList.getNext());

        // exercise
        this.ordersController.deleteOrder(computeOrder);

        // verify
        Order order = this.closedOrdersList.getNext();

        Assert.assertNull(this.openOrdersList.getNext());
        Assert.assertNotNull(order);
        Assert.assertEquals(computeOrder, order);
        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: Deleting a null order must return a UnexpectedException.
    @Test(expected = UnexpectedException.class) // verify
    public void testDeleteNullOrder()
            throws Exception {
        // exercise
        this.ordersController.deleteOrder(null);
    }

    // test case: Getting an order with a nonexistent id must throw an InstanceNotFoundException
    @Test(expected = InstanceNotFoundException.class) // verify
    public void testGetOrderWithInvalidId() throws InstanceNotFoundException {
        // exercise
        this.ordersController.getOrder(INVALID_ORDER_ID);
    }
    
    private NetworkOrder createFulfilledNetworkOrder(SystemUser systemUser) throws UnexpectedException {
        NetworkOrder networkOrder = new NetworkOrder();
        networkOrder.setSystemUser(systemUser);
        networkOrder.setRequester(TestUtils.LOCAL_MEMBER_ID);
        networkOrder.setProvider(TestUtils.LOCAL_MEMBER_ID);
        networkOrder.setOrderState(OrderState.FULFILLED);
        return networkOrder;
    }
    
    private ComputeOrder createFulfilledComputeOrder(SystemUser systemUser) throws UnexpectedException {
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setSystemUser(systemUser);
        computeOrder.setRequester(TestUtils.LOCAL_MEMBER_ID);
        computeOrder.setProvider(TestUtils.LOCAL_MEMBER_ID);
        computeOrder.setOrderState(OrderState.FULFILLED);
        return computeOrder;
    }
    
    private InstanceStatus createInstanceStatus(ComputeOrder computeOrder) throws InstanceNotFoundException {
        return new InstanceStatus(computeOrder.getId(),
                computeOrder.getProvider(), computeOrder.getCloudName(),
                InstanceStatus.mapInstanceStateFromOrderState(computeOrder.getOrderState()));
    }

    private String setupOrder(OrderState orderState) throws UnexpectedException {
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        computeOrder.setOrderState(orderState);

        String orderId = computeOrder.getId();

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
        case FAILED_AFTER_SUCCESSFUL_REQUEST:
            this.failedAfterSuccessfulRequestOrdersList.addItem(computeOrder);
            break;
        case FAILED_ON_REQUEST:
            this.failedOnRequestOrdersList.addItem(computeOrder);
            break;
        case CLOSED:
            this.closedOrdersList.addItem(computeOrder);
        default:
            break;
        }

        return orderId;
    }

}


