package cloud.fogbow.ras.core;

import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.api.http.response.*;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.*;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatabaseManager.class, CloudConnectorFactory.class})
public class OrderControllerTest extends BaseUnitTests {

    //FIXME: many of bellow tests, e.g testGetResourceInstance are too complicated. The problem is
    //they are setting up the scenario for the getOrder method (which they use). This set up
    //include changing the content of fulfilledOrdersList and activeOrdersMap.put
    //A simpler way of doing these tests is to spy the behaviour of the getOrder method.

    private OrderController ordersController;
    private Map<String, Order> activeOrdersMap;
    private ChainedList<Order> openOrdersList;
    private ChainedList<Order> pendingOrdersList;
    private ChainedList<Order> spawningOrdersList;
    private ChainedList<Order> fulfilledOrdersList;
    private ChainedList<Order> failedAfterSuccessfulRequestOrdersList;
    private ChainedList<Order> failedOnRequestOrdersList;
    private ChainedList<Order> closedOrdersList;
    private String localMember = BaseUnitTests.LOCAL_MEMBER_ID;
    private LocalCloudConnector localCloudConnector;

    @Before
    public void setUp() throws UnexpectedException {
        PowerMockito.mockStatic(CloudConnectorFactory.class);
        // mocking database to return empty instances of SynchronizedDoublyLinkedList.
        super.mockReadOrdersFromDataBase();

        this.ordersController = new OrderController();

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.localCloudConnector = Mockito.mock(LocalCloudConnector.class);

        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(localCloudConnector);

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);
        // setting up the attributes.
        this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        this.openOrdersList = sharedOrderHolders.getOpenOrdersList();
        this.pendingOrdersList = sharedOrderHolders.getPendingOrdersList();
        this.spawningOrdersList = sharedOrderHolders.getSpawningOrdersList();
        this.fulfilledOrdersList = sharedOrderHolders.getFulfilledOrdersList();
        this.failedAfterSuccessfulRequestOrdersList = sharedOrderHolders.getFailedAfterSuccessfulRequestOrdersList();
        this.closedOrdersList = sharedOrderHolders.getClosedOrdersList();
    }

    // test case: when try to delete an Order closed, it must raise an InstanceNotFoundException.
    @Test(expected = InstanceNotFoundException.class) // verify
    public void testDeleteClosedOrderThrowsInstanceNotFoundException()
            throws Exception {

        // set up
        String orderId = getComputeOrderCreationId(OrderState.CLOSED);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // exercise
        this.ordersController.deleteOrder(computeOrder);
    }

    // test case: Checks if getInstancesStatus() returns exactly the same list of instances that
    // were added on the lists.
    @Test
    public void testGetAllInstancesStatus() throws InstanceNotFoundException {
        // set up
        SystemUser systemUser = new SystemUser("fake-id", "fake-user", "fake-token-provider"
        );

        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setSystemUser(systemUser);
        computeOrder.setRequester(this.localMember);
        computeOrder.setProvider(this.localMember);
        computeOrder.setOrderStateInTestMode(OrderState.FULFILLED);

        ComputeOrder computeOrder2 = new ComputeOrder();
        computeOrder2.setSystemUser(systemUser);
        computeOrder2.setRequester(this.localMember);
        computeOrder2.setProvider(this.localMember);
        computeOrder2.setOrderStateInTestMode(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);

        this.activeOrdersMap.put(computeOrder.getId(), computeOrder);
        this.fulfilledOrdersList.addItem(computeOrder);

        this.activeOrdersMap.put(computeOrder2.getId(), computeOrder2);
        this.failedAfterSuccessfulRequestOrdersList.addItem(computeOrder2);

        InstanceStatus statusOrder = new InstanceStatus(computeOrder.getId(), computeOrder.getProvider(),
                computeOrder.getCloudName(), InstanceStatus.mapInstanceStateFromOrderState(computeOrder.getOrderState()));
        InstanceStatus statusOrder2 = new InstanceStatus(computeOrder2.getId(), computeOrder2.getProvider(),
                computeOrder.getCloudName(), InstanceStatus.mapInstanceStateFromOrderState(computeOrder2.getOrderState()));

        // exercise
        List<InstanceStatus> instances = this.ordersController.getInstancesStatus(systemUser,
                ResourceType.COMPUTE);

        // verify
        Assert.assertTrue(instances.contains(statusOrder));
        Assert.assertTrue(instances.contains(statusOrder2));
        Assert.assertEquals(2, instances.size());
    }

    // test case: Checks if getOrder() returns exactly the same order that
    // were added on the list.
    @Test
    public void testGetOrder() throws UnexpectedException, FogbowException {
        // set up
        String orderId = getComputeOrderCreationId(OrderState.OPEN);

        SystemUser systemUser = new SystemUser("fake-id", "fake-user", "fake-token-provider"
        );

        // exercise
        ComputeOrder computeOrder = (ComputeOrder) this.ordersController.getOrder(orderId);

        // verify
        Assert.assertEquals(computeOrder, this.openOrdersList.getNext());
    }

    // test case: Get a not active Order, must throw InstanceNotFoundException.
    @Test(expected = InstanceNotFoundException.class) // verify
    public void testGetInactiveOrder() throws InstanceNotFoundException {
        // set up
        Order order = createOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);
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
        this.ordersController.getOrder("invalid-order-id");
    }

    //test case: Checks if attempt to activate null order throws UnexpectedException
    @Test(expected = UnexpectedException.class)
    public void testActivateNullOrder() throws FogbowException {
        // set up
        Order order = null;

        // exercise
        this.ordersController.activateOrder(order);
    }

    // test case: Attempt to activate the same order more than
    // once must throw UnexpectedException
    @Test(expected = UnexpectedException.class)
    public void testActivateOrderTwice() throws FogbowException {
        // set up
        Order order = createOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);

        // exercise
        this.ordersController.activateOrder(order);
        this.ordersController.activateOrder(order);
    }

    // test case: Attempt to deactivate the same order more than
    // once must throw UnexpectedException
    @Test(expected = UnexpectedException.class)
    public void testDeactivateOrderTwice() throws FogbowException {
        // set up
        Order order = createOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);

        // exercise
        this.ordersController.activateOrder(order);
        this.ordersController.deactivateOrder(order);
        this.ordersController.deactivateOrder(order);
    }

    // test case: Checks if deactivateOrder changes it's state to DEACTIVATED
    @Test
    public void testDeactivateOrderSuccess() throws FogbowException {
        // set up
        Order order = createOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);

        // exercise
        this.ordersController.activateOrder(order);
        this.ordersController.deactivateOrder(order);

        // verify
        Assert.assertEquals(order.getOrderState(), OrderState.DEACTIVATED);
    }

    // test case: Creates an order with dependencies and check if the order id
    // will be inserted into dependencies.
    @Test
    public void testCreateOrderWithDependencies() throws FogbowException {
        // set up
        ComputeOrder computeOrder = createLocalComputeOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);
        VolumeOrder volumeOrder1 = createLocalVolumeOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);
        VolumeOrder volumeOrder2 = createLocalVolumeOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);

        this.ordersController.activateOrder(computeOrder);
        this.ordersController.activateOrder(volumeOrder1);
        this.ordersController.activateOrder(volumeOrder2);

        AttachmentOrder attachmentOrder1 = createLocalAttachmentOrder(computeOrder, volumeOrder1);
        AttachmentOrder attachmentOrder2 = createLocalAttachmentOrder(computeOrder, volumeOrder2);

        // exercise
        this.ordersController.activateOrder(attachmentOrder1);
        this.ordersController.activateOrder(attachmentOrder2);

        // verify
        Assert.assertTrue(this.orderHasDependencies(computeOrder.getId()));
    }

    // test case: Creates an order with dependencies and attempt to delete
    // it must throw an DependencyDetectedException.
    @Test(expected = DependencyDetectedException.class)
    public void testDeleteOrderWithoutRemovingDependenciesFirst() throws FogbowException {
        // set up
        ComputeOrder computeOrder = createLocalComputeOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);
        VolumeOrder volumeOrder = createLocalVolumeOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);

        this.ordersController.activateOrder(computeOrder);
        this.ordersController.activateOrder(volumeOrder);

        AttachmentOrder attachmentOrder = createLocalAttachmentOrder(computeOrder, volumeOrder);
        this.ordersController.activateOrder(attachmentOrder);

        // exercise / verify
        this.ordersController.deleteOrder(volumeOrder);
    }

    // test case: Creates an order with dependencies and attempt to delete
    // them in correct order must not throw any exceptions.
    @Test
    public void testDeleteOrderWithDependencies() throws FogbowException {
        // set up
        ComputeOrder computeOrder = createLocalComputeOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);
        VolumeOrder volumeOrder = createLocalVolumeOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);

        this.ordersController.activateOrder(computeOrder);
        this.ordersController.activateOrder(volumeOrder);

        AttachmentOrder attachmentOrder = createLocalAttachmentOrder(computeOrder, volumeOrder);
        this.ordersController.activateOrder(attachmentOrder);

        // exercise / verify
        this.ordersController.deleteOrder(attachmentOrder);
        this.ordersController.deleteOrder(computeOrder);
        this.ordersController.deleteOrder(volumeOrder);
    }

    // test case: Checks if given an OPEN order getResourceInstance() throws
    // RequestStillBeingDispatchedException.
    @Test(expected = RequestStillBeingDispatchedException.class)
    public void testGetResourceInstanceOfOpenOrder() throws FogbowException {
        // set up
        Order order = createOrder(LOCAL_MEMBER_ID, REMOTE_MEMBER_ID);
        order.setOrderStateInTestMode(OrderState.OPEN);

        // exercise
        this.ordersController.getResourceInstance(order);
    }

    // test case: Checks if given an order getResourceInstance() returns its instance.
    @Test
    public void testGetResourceInstance() throws Exception {
        // set up
        Order order = createOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);
        order.setOrderStateInTestMode(OrderState.FULFILLED);

        this.fulfilledOrdersList.addItem(order);
        this.activeOrdersMap.put(order.getId(), order);

        String instanceId = "instanceid";
        OrderInstance orderInstance = Mockito.spy(new ComputeInstance(instanceId));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(instanceId);

        Mockito.doReturn(orderInstance).when(localCloudConnector)
                .getInstance(Mockito.any(Order.class));

        // exercise
        Instance instance = this.ordersController.getResourceInstance(order);

        // verify
        Assert.assertEquals(orderInstance, instance);
    }

    // test case: Checks if given an remote order getResourceInstance() returns its instance.
    @Test public void testRemoteGetResourceInstance() throws FogbowException {
        // set up
        Order order = createOrder(LOCAL_MEMBER_ID, REMOTE_MEMBER_ID);
        order.setOrderStateInTestMode(OrderState.FULFILLED);

        this.fulfilledOrdersList.addItem(order);
        this.activeOrdersMap.put(order.getId(), order);

        String instanceId = "instanceid";
        OrderInstance orderInstance = Mockito.spy(new ComputeInstance(instanceId));
        orderInstance.setState(InstanceState.READY);
        order.setInstanceId(instanceId);

        Mockito.doReturn(orderInstance).when(localCloudConnector)
                .getInstance(Mockito.any(Order.class));

        // exercise
        Instance instance = this.ordersController.getResourceInstance(order);

        // verify
        Assert.assertEquals(orderInstance, instance);
    }

    // test case: Requesting a null order must return a UnexpectedException.
    @Test(expected = UnexpectedException.class) // verify
    public void testGetResourceInstanceNullOrder() throws Exception {
        // exercise
        this.ordersController.getResourceInstance(null);
    }

    // test case: Tests if getUserAllocation() returns the ComputeAllocation properly.
    @Test
    public void testGetUserAllocation() throws UnexpectedException, InvalidParameterException {
        // set up
        SystemUser systemUser = new SystemUser("fake-id", "fake-user", "fake-token-provider"
        );;
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setSystemUser(systemUser);
        computeOrder.setRequester(this.localMember);
        computeOrder.setProvider(this.localMember);
        computeOrder.setOrderStateInTestMode(OrderState.FULFILLED);

        computeOrder.setActualAllocation(new ComputeAllocation(1, 2, 3, 4));

        this.activeOrdersMap.put(computeOrder.getId(), computeOrder);
        this.fulfilledOrdersList.addItem(computeOrder);

        // exercise
        ComputeAllocation allocation = (ComputeAllocation) this.ordersController.getUserAllocation(
                this.localMember, systemUser, ResourceType.COMPUTE);

        // verify
        Assert.assertEquals(computeOrder.getActualAllocation().getInstances(),
                allocation.getInstances());
        Assert.assertEquals(computeOrder.getActualAllocation().getRam(), allocation.getRam());
        Assert.assertEquals(computeOrder.getActualAllocation().getvCPU(), allocation.getvCPU());
        Assert.assertEquals(computeOrder.getActualAllocation().getDisk(), allocation.getDisk());
    }


    // test case: Tests if getUserAllocation() throws UnexpectedException when there is no order
    // with the ResourceType specified.
    @Test(expected = UnexpectedException.class)
    public void testGetUserAllocationWithInvalidInstanceType()
            throws UnexpectedException, InvalidParameterException {
        // set up
        Map<String, String> attributes = new HashMap<>();
        attributes.put(FogbowConstants.PROVIDER_ID_KEY, "fake-token-provider");
        attributes.put(FogbowConstants.USER_ID_KEY, "fake-id");
        attributes.put(FogbowConstants.USER_NAME_KEY, "fake-user");
        attributes.put(FogbowConstants.TOKEN_VALUE_KEY, "token-value");
        SystemUser systemUser = new SystemUser("fake-id", "fake-user", "fake-token-provider"
        );;
        NetworkOrder networkOrder = new NetworkOrder();
        networkOrder.setSystemUser(systemUser);
        networkOrder.setRequester(this.localMember);
        networkOrder.setProvider(this.localMember);
        networkOrder.setOrderStateInTestMode(OrderState.FULFILLED);

        this.fulfilledOrdersList.addItem(networkOrder);
        this.activeOrdersMap.put(networkOrder.getId(), networkOrder);

        // exercise
        this.ordersController.getUserAllocation(this.localMember, systemUser, ResourceType.NETWORK);
    }

    // test case: Checks if deleting a failed order, this one will be moved to the closed orders
    // list.
    @Test
    public void testDeleteOrderStateFailed()
            throws Exception {
        // set up
        String orderId = getComputeOrderCreationId(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
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
        String orderId = getComputeOrderCreationId(OrderState.FULFILLED);
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
        String orderId = getComputeOrderCreationId(OrderState.SPAWNING);
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
        String orderId = getComputeOrderCreationId(OrderState.PENDING);
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
        String orderId = getComputeOrderCreationId(OrderState.OPEN);
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

    // test case: Getting an order with an unexisting id must throw an InstanceNotFoundException
    @Test(expected = InstanceNotFoundException.class)
    public void testGetOrderWithInvalidId() throws InstanceNotFoundException {
        this.ordersController.getOrder("invalid-order-id");
    }

    private String getComputeOrderCreationId(OrderState orderState) throws InvalidParameterException, UnexpectedException {
        SystemUser systemUser = new SystemUser("fake-id", "fake-user", "fake-token-provider");

        ComputeOrder computeOrder = Mockito.spy(new ComputeOrder());
        computeOrder.setSystemUser(systemUser);
        computeOrder.setRequester(this.localMember);
        computeOrder.setProvider(this.localMember);
        computeOrder.setOrderStateInTestMode(orderState);

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

    private boolean orderHasDependencies(String orderId) {
        boolean ret = false;

        try {
            this.ordersController.checkOrderDependencies(orderId);
        } catch (DependencyDetectedException e) {
            ret = true;
        }

        return ret;
    }
}

