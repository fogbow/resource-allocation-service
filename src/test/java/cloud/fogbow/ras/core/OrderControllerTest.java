package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.api.http.response.*;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.api.http.response.quotas.allocation.NetworkAllocation;
import cloud.fogbow.ras.api.http.response.quotas.allocation.PublicIpAllocation;
import cloud.fogbow.ras.api.http.response.quotas.allocation.VolumeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.cloudconnector.RemoteCloudConnector;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.*;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@PrepareForTest({CloudConnectorFactory.class, DatabaseManager.class, OrderStateTransitioner.class})
public class OrderControllerTest extends BaseUnitTests {

    private static final String AVAILABLE_STATE = "available";
    private static final String FAKE_IP_ADDRESS = "0.0.0.0";
    private static final String INVALID_ORDER_ID = "invalid-order-id";
    private static final int INSTANCES_LAUNCH_NUMBER = 1;

    private OrderController ordersController;
    private LocalCloudConnector localCloudConnector;
    private Map<String, Order> activeOrdersMap;
    private ChainedList<Order> openOrdersList;
    private ChainedList<Order> pendingOrdersList;
    private ChainedList<Order> spawningOrdersList;
    private ChainedList<Order> fulfilledOrdersList;
    private ChainedList<Order> failedAfterSuccessfulRequestOrdersList;
    private ChainedList<Order> failedOnRequestOrdersList;
    private ChainedList<Order> checkingDeletionOrdersList;
    private ChainedList<Order> assignedForDeletionOrdersList;

    private LoggerAssert loggerTestChecking = new LoggerAssert(OrderController.class);

    @Rule
    private ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws UnexpectedException {
        // mocking database to return empty instances of SynchronizedDoublyLinkedList.
        this.testUtils.mockReadOrdersFromDataBase();

        // setting up the attributes.
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        this.openOrdersList = sharedOrderHolders.getOpenOrdersList();
        this.pendingOrdersList = sharedOrderHolders.getRemoteProviderOrdersList();
        this.spawningOrdersList = sharedOrderHolders.getSpawningOrdersList();
        this.fulfilledOrdersList = sharedOrderHolders.getFulfilledOrdersList();
        this.failedAfterSuccessfulRequestOrdersList = sharedOrderHolders.getFailedAfterSuccessfulRequestOrdersList();
        this.failedOnRequestOrdersList = sharedOrderHolders.getFailedOnRequestOrdersList();
        this.checkingDeletionOrdersList = sharedOrderHolders.getCheckingDeletionOrdersList();
        this.assignedForDeletionOrdersList = sharedOrderHolders.getAssignedForDeletionOrdersList();
        this.ordersController = Mockito.spy(new OrderController());
        this.localCloudConnector = this.testUtils.mockLocalCloudConnectorFromFactory();
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

    // test case: Attempt to close the same order more than
    // once must throw UnexpectedException
    @Test
    public void testCloseOrderTwice() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());
        String expected = String.format(Messages.Exception.UNABLE_TO_REMOVE_INACTIVE_REQUEST, order.getId());

        this.ordersController.activateOrder(order);

        // exercise
        this.ordersController.closeOrder(order);
        try {
            this.ordersController.closeOrder(order);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: Checks if closeOrder changes the order state to CLOSED
    @Test
    public void testCloseOrderSuccess() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalOrder(this.testUtils.getLocalMemberId());

        this.ordersController.activateOrder(order);
        Assert.assertEquals(OrderState.OPEN, order.getOrderState());

        // exercise
        this.ordersController.closeOrder(order);

        // verify
        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: Checks if closeOrder nofities a remote RAS.
    @Test
    public void testCloseOrderRemoteSuccessfully() throws FogbowException {
        // set up
        Order remoteOrder = this.testUtils.createLocalOrderWithRemoteRequester(TestUtils.LOCAL_MEMBER_ID);

        this.ordersController.activateOrder(remoteOrder);
        Assert.assertEquals(OrderState.OPEN, remoteOrder.getOrderState());

        // exercise
        this.ordersController.closeOrder(remoteOrder);

        // verify
        PowerMockito.verifyStatic();
        this.ordersController.notifyRequesterToCloseOrder(Mockito.eq(remoteOrder));
    }

    // test case: Checks if closeOrder logs a warning message when occurs an exception.
    @Test
    public void testCloseOrderRemoteFail() throws FogbowException {
        // set up
        Order remoteOrder = this.testUtils.createRemoteOrder(TestUtils.ANY_VALUE);

        this.ordersController.activateOrder(remoteOrder);
        Assert.assertEquals(OrderState.OPEN, remoteOrder.getOrderState());

        PowerMockito.mockStatic(OrderStateTransitioner.class);
        PowerMockito.doThrow(new RemoteCommunicationException()).when(OrderStateTransitioner.class);
        this.ordersController.notifyRequesterToCloseOrder(Mockito.eq(remoteOrder));

        String warnMessageException = String.format(
                Messages.Warn.UNABLE_TO_NOTIFY_REQUESTING_PROVIDER, remoteOrder.getRequester(), remoteOrder.getId());

        // exercise
        this.ordersController.closeOrder(remoteOrder);

        // verify
        this.loggerTestChecking.assertEqualsInOrder(Level.INFO, Messages.Info.ACTIVATING_NEW_REQUEST)
                .assertEqualsInOrder(Level.WARN, warnMessageException);
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

    // test case: Creates an order with dependencies and attempts to delete
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

        Assert.assertNull(this.checkingDeletionOrdersList.getNext());

        // exercise part 1
        this.ordersController.deleteOrder(attachmentOrder);

        // Verify part 1
        AttachmentOrder testAttachmentOrder = (AttachmentOrder) this.assignedForDeletionOrdersList.getNext();
        Assert.assertEquals(OrderState.ASSIGNED_FOR_DELETION, testAttachmentOrder.getOrderState());

        // Simulating processors; Delete the attachment and remove the dependencies.
        this.ordersController.updateOrderDependencies(attachmentOrder, Operation.DELETE);

        // exercise part 2
        this.ordersController.deleteOrder(volumeOrder);
        this.ordersController.deleteOrder(computeOrder);

        // verify part 2
        Assert.assertNull(this.openOrdersList.getNext());

        VolumeOrder testVolumeOrder = (VolumeOrder) this.assignedForDeletionOrdersList.getNext();
        Assert.assertEquals(OrderState.ASSIGNED_FOR_DELETION, testVolumeOrder.getOrderState());

        ComputeOrder testComputeOrder = (ComputeOrder) this.assignedForDeletionOrdersList.getNext();
        Assert.assertEquals(OrderState.ASSIGNED_FOR_DELETION, testComputeOrder.getOrderState());
    }

    // test case: When invoking the deleteOrder method, it must change the order state to ASSIGNED_FOR_DELETION.
    @Test
    public void testDeleteOrderWithInstanceRunning() throws FogbowException {
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        computeOrder.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        this.ordersController.activateOrder(computeOrder);

        // exercise
        this.ordersController.deleteOrder(computeOrder);

        // verify
        Assert.assertEquals(OrderState.ASSIGNED_FOR_DELETION, computeOrder.getOrderState());
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

    // test case: Checks if given an SELECTED order getResourceInstance() throws
    // RequestStillBeingDispatchedException.
    @Test(expected = RequestStillBeingDispatchedException.class)
    public void testGetResourceInstanceOfSelectedOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createRemoteOrder(this.testUtils.getLocalMemberId());
        order.setOrderState(OrderState.SELECTED);

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

        Mockito.doReturn(computeInstance).when(this.localCloudConnector).getInstance(Mockito.any(Order.class));

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
    @Test
    public void testRemoteGetResourceInstance() throws FogbowException {
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
        computeOrder1.setActualAllocation(new ComputeAllocation(1, 2, 2048, 4));

        ComputeOrder computeOrder2 = createFulfilledComputeOrder(systemUser);
        computeOrder2.setActualAllocation(new ComputeAllocation(1, 4, 4096, 8));

        this.activeOrdersMap.put(computeOrder1.getId(), computeOrder1);
        this.activeOrdersMap.put(computeOrder2.getId(), computeOrder2);

        this.fulfilledOrdersList.addItem(computeOrder1);
        this.fulfilledOrdersList.addItem(computeOrder2);

        int expectedCpuValue = 6;
        int expectedMemoryValue = 6144;
        int expectedInstancesValue = 2;
        int expectedDiskValue = 12;

        List<ComputeOrder> orders = new ArrayList<>();
        orders.add(computeOrder1);
        orders.add(computeOrder2);

        ComputeAllocation expectedAllocation = new ComputeAllocation(expectedInstancesValue, expectedCpuValue, expectedMemoryValue, expectedDiskValue);
        Mockito.doReturn(expectedAllocation).when(this.ordersController).getUserComputeAllocation(Mockito.eq(orders));
        Mockito.doReturn(orders).when(this.ordersController).castOrders(Mockito.anyListOf(Order.class));
        // exercise
        ComputeAllocation allocation = (ComputeAllocation) this.ordersController
                .getUserAllocation(TestUtils.LOCAL_MEMBER_ID, TestUtils.DEFAULT_CLOUD_NAME, systemUser, ResourceType.COMPUTE);

        // verify
        Mockito.verify(this.ordersController, Mockito.times(TestUtils.RUN_ONCE)).getUserComputeAllocation(Mockito.eq(orders));
        Mockito.verify(this.ordersController, Mockito.times(TestUtils.RUN_ONCE)).castOrders(Mockito.anyListOf(Order.class));

        Assert.assertEquals(expectedCpuValue, allocation.getvCPU());
        Assert.assertEquals(expectedMemoryValue, allocation.getRam());
        Assert.assertEquals(expectedInstancesValue, allocation.getInstances());
        Assert.assertEquals(expectedDiskValue, allocation.getDisk());
    }

    // test case: Tests if the getUserAllocation method returns the
    // VolumeAllocation properly.
    @Test
    public void testGetUserAllocationToVolumeResourceType() throws UnexpectedException {
        // set up
        SystemUser systemUser = this.testUtils.createSystemUser();

        VolumeOrder volumeOrder1 = createFulfilledVolumeOrder(systemUser);
        VolumeAllocation volumeAllocation1 = new VolumeAllocation(2);
        volumeOrder1.setActualAllocation(volumeAllocation1);

        VolumeOrder volumeOrder2 = createFulfilledVolumeOrder(systemUser);
        VolumeAllocation volumeAllocation2 = new VolumeAllocation(5);
        volumeOrder2.setActualAllocation(volumeAllocation2);

        this.activeOrdersMap.put(volumeOrder1.getId(), volumeOrder1);
        this.activeOrdersMap.put(volumeOrder2.getId(), volumeOrder2);

        this.fulfilledOrdersList.addItem(volumeOrder1);
        this.fulfilledOrdersList.addItem(volumeOrder2);

        List<VolumeOrder> orders = new ArrayList<>();
        orders.add(volumeOrder1);
        orders.add(volumeOrder2);

        int expectedValue = volumeAllocation1.getStorage() + volumeAllocation2.getStorage();
        VolumeAllocation expectedAllocation = new VolumeAllocation(expectedValue);

        Mockito.doReturn(orders).when(this.ordersController).castOrders(Mockito.anyListOf(Order.class));
        Mockito.doReturn(expectedAllocation).when(this.ordersController).getUserVolumeAllocation(Mockito.eq(orders));

        // exercise
        VolumeAllocation allocation = (VolumeAllocation) this.ordersController
                .getUserAllocation(TestUtils.LOCAL_MEMBER_ID, TestUtils.DEFAULT_CLOUD_NAME, systemUser, ResourceType.VOLUME);

        // verify
        Mockito.verify(this.ordersController, Mockito.times(TestUtils.RUN_ONCE)).getUserVolumeAllocation(Mockito.eq(orders));
        Mockito.verify(this.ordersController, Mockito.times(TestUtils.RUN_ONCE)).castOrders(Mockito.anyListOf(Order.class));

        Assert.assertEquals(expectedValue, allocation.getStorage());
    }

    // test case: Tests if the getUserAllocation method returns the
    // NetworkAllocation properly
    @Test
    public void testGetUserAllocationToNetworkResourceType() throws UnexpectedException {
        // set up
        SystemUser systemUser = this.testUtils.createSystemUser();

        NetworkOrder networkOrder1 = createFulfilledNetworkOrder(systemUser);
        NetworkOrder networkOrder2 = createFulfilledNetworkOrder(systemUser);

        this.activeOrdersMap.put(networkOrder1.getId(), networkOrder1);
        this.activeOrdersMap.put(networkOrder2.getId(), networkOrder2);

        this.fulfilledOrdersList.addItem(networkOrder1);
        this.fulfilledOrdersList.addItem(networkOrder2);

        List<NetworkOrder> orders = new ArrayList<>();
        orders.add(networkOrder1);
        orders.add(networkOrder2);

        int expectedValue = orders.size();
        NetworkAllocation expectedAllocation = new NetworkAllocation(expectedValue);

        Mockito.doReturn(orders).when(this.ordersController).castOrders(Mockito.anyListOf(Order.class));
        Mockito.doReturn(expectedAllocation).when(this.ordersController).getUserNetworkAllocation(Mockito.eq(orders));

        // exercise
        NetworkAllocation allocation = (NetworkAllocation) this.ordersController
                .getUserAllocation(TestUtils.LOCAL_MEMBER_ID, TestUtils.DEFAULT_CLOUD_NAME, systemUser, ResourceType.NETWORK);

        // verify
        Mockito.verify(this.ordersController, Mockito.times(TestUtils.RUN_ONCE)).getUserNetworkAllocation(Mockito.eq(orders));
        Mockito.verify(this.ordersController, Mockito.times(TestUtils.RUN_ONCE)).castOrders(Mockito.anyListOf(Order.class));

        Assert.assertEquals(expectedValue, allocation.getInstances());
    }

    // test case: Tests if the getUserAllocation method returns the
    // PublicIpAllocation properly
    @Test
    public void testGetUserAllocationToPublicIpResourceType() throws UnexpectedException {
        // set up
        SystemUser systemUser = this.testUtils.createSystemUser();

        PublicIpOrder publicIpOrder1 = createFulfilledPublicIpOrder(systemUser);
        PublicIpOrder publicIpOrder2 = createFulfilledPublicIpOrder(systemUser);

        this.activeOrdersMap.put(publicIpOrder1.getId(), publicIpOrder1);
        this.activeOrdersMap.put(publicIpOrder2.getId(), publicIpOrder2);

        this.fulfilledOrdersList.addItem(publicIpOrder1);
        this.fulfilledOrdersList.addItem(publicIpOrder2);

        List<PublicIpOrder> orders = new ArrayList<>();
        orders.add(publicIpOrder1);
        orders.add(publicIpOrder2);

        int expectedValue = orders.size();
        PublicIpAllocation expectedAllocation = new PublicIpAllocation(expectedValue);

        Mockito.doReturn(orders).when(this.ordersController).castOrders(Mockito.anyListOf(Order.class));
        Mockito.doReturn(expectedAllocation).when(this.ordersController).getUserPublicIpAllocation(Mockito.eq(orders));

        // exercise
        PublicIpAllocation allocation = (PublicIpAllocation) this.ordersController
                .getUserAllocation(TestUtils.LOCAL_MEMBER_ID, TestUtils.DEFAULT_CLOUD_NAME, systemUser, ResourceType.PUBLIC_IP);

        // verify
        Mockito.verify(this.ordersController, Mockito.times(TestUtils.RUN_ONCE)).getUserPublicIpAllocation(Mockito.eq(orders));
        Mockito.verify(this.ordersController, Mockito.times(TestUtils.RUN_ONCE)).castOrders(Mockito.anyListOf(Order.class));

        Assert.assertEquals(expectedValue, allocation.getInstances());
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
        this.ordersController.getUserAllocation(TestUtils.LOCAL_MEMBER_ID, TestUtils.DEFAULT_CLOUD_NAME, systemUser,
                ResourceType.INVALID_RESOURCE);
    }

    // test case: Tests if the castOrders method casts a list of orders (Order.class) to
    // a list of orders which the type is inferred by the return type (it should be a order's subclass)
    @Test
    public void testCastOrders() throws UnexpectedException {
        // set up
        SystemUser systemUser = this.testUtils.createSystemUser();

        List<Order> ordersToCastCompute = new ArrayList<>();
        ordersToCastCompute.add(createFulfilledComputeOrder(systemUser));

        List<Order> ordersToCastVolume = new ArrayList<>();
        ordersToCastCompute.add(createFulfilledVolumeOrder(systemUser));

        List<Order> ordersToCastNetwork = new ArrayList<>();
        ordersToCastCompute.add(createFulfilledNetworkOrder(systemUser));

        List<Order> ordersToCastPublicIp = new ArrayList<>();
        ordersToCastCompute.add(createFulfilledPublicIpOrder(systemUser));

        // exercise
        List<ComputeOrder> computeOrders = this.ordersController.castOrders(ordersToCastCompute);
        List<VolumeOrder> volumeOrders = this.ordersController.castOrders(ordersToCastVolume);
        List<PublicIpOrder> publicIpOrders = this.ordersController.castOrders(ordersToCastPublicIp);
        List<NetworkOrder> networkOrders = this.ordersController.castOrders(ordersToCastNetwork);

        // verify
        Assert.assertEquals(ordersToCastCompute.size(), computeOrders.size());
        Assert.assertEquals(ordersToCastVolume.size(), volumeOrders.size());
        Assert.assertEquals(ordersToCastNetwork.size(), networkOrders.size());
        Assert.assertEquals(ordersToCastPublicIp.size(), publicIpOrders.size());
    }

    // test case: Tests if the getUserComputeAllocation method builds a ComputeAllocation with
    // the total allocation of all orders passed by
    @Test
    public void testGetUserComputeAllocation() throws UnexpectedException {
        // set up
        List<ComputeOrder> orders = new ArrayList<>();
        SystemUser systemUser = this.testUtils.createSystemUser();

        int numberOfInstances = 3;
        int expectedDisk = numberOfInstances * TestUtils.DISK_VALUE;
        int expectedRam = numberOfInstances * TestUtils.MEMORY_VALUE;
        int expectedCores = numberOfInstances * TestUtils.CPU_VALUE;

        ComputeOrder order;
        for (int i = 0; i < numberOfInstances; i++) {
            order = createFulfilledComputeOrderWithAllocation(systemUser);
            orders.add(order);
        }

        // exercise
        ComputeAllocation allocation = this.ordersController.getUserComputeAllocation(orders);

        // verify
        Assert.assertEquals(numberOfInstances, allocation.getInstances());
        Assert.assertEquals(expectedDisk, allocation.getDisk());
        Assert.assertEquals(expectedCores, allocation.getvCPU());
        Assert.assertEquals(expectedRam, allocation.getRam());
    }

    // test case: Tests if the getUserVolumeAllocation method builds a VolumeAllocation with
    // the total allocation of all orders passed by
    @Test
    public void testGetUserVolumeAllocation() throws UnexpectedException {
        // set up
        List<VolumeOrder> orders = new ArrayList<>();
        SystemUser systemUser = this.testUtils.createSystemUser();

        int numberOfInstances = 3;
        int expectedStorage = numberOfInstances * TestUtils.DISK_VALUE;

        VolumeOrder order;
        for (int i = 0; i < numberOfInstances; i++) {
            order = createFulfilledVolumeOrderWithAllocation(systemUser);
            orders.add(order);
        }

        // exercise
        VolumeAllocation allocation = this.ordersController.getUserVolumeAllocation(orders);

        // verify
        Assert.assertEquals(expectedStorage, allocation.getStorage());
        Assert.assertEquals(numberOfInstances, allocation.getInstances());
    }

    // test case: Tests if the getUserNetworkAllocation method builds a NetworkAllocation with
    // the total allocation of all orders passed by
    @Test
    public void testGetUserNetworkAllocation() throws UnexpectedException {
        // set up
        List<NetworkOrder> orders = new ArrayList<>();
        SystemUser systemUser = this.testUtils.createSystemUser();

        int numberOfInstances = 3;

        NetworkOrder order;
        for (int i = 0; i < numberOfInstances; i++) {
            order = createFulfilledNetworkOrder(systemUser);
            orders.add(order);
        }

        // exercise
        NetworkAllocation allocation = this.ordersController.getUserNetworkAllocation(orders);

        // verify
        Assert.assertEquals(numberOfInstances, allocation.getInstances());
    }

    // test case: Tests if the getUserNetworkAllocation method builds a NetworkAllocation with
    // the total allocation of all orders passed by
    @Test
    public void testGetUserPublicIpllocation() throws UnexpectedException {
        // set up
        List<PublicIpOrder> orders = new ArrayList<>();
        SystemUser systemUser = this.testUtils.createSystemUser();

        int numberOfInstances = 3;

        PublicIpOrder order;
        for (int i = 0; i < numberOfInstances; i++) {
            order = createFulfilledPublicIpOrder(systemUser);
            orders.add(order);
        }

        // exercise
        PublicIpAllocation allocation = this.ordersController.getUserPublicIpAllocation(orders);

        // verify
        Assert.assertEquals(numberOfInstances, allocation.getInstances());
    }

    // test case: Checks if deleting a failed order, this one will be moved to the assignedForDeletion orders
    // list.
    @Test
    public void testDeleteOrderStateFailed()
            throws Exception {
        // set up
        String orderId = setupOrder(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // verify
        Assert.assertNotNull(this.failedAfterSuccessfulRequestOrdersList.getNext());
        Assert.assertNull(this.assignedForDeletionOrdersList.getNext());

        // exercise
        this.ordersController.deleteOrder(computeOrder);

        // verify
        Order order = this.assignedForDeletionOrdersList.getNext();
        this.failedAfterSuccessfulRequestOrdersList.resetPointer();

        Assert.assertNull(this.failedAfterSuccessfulRequestOrdersList.getNext());
        Assert.assertNotNull(order);
        Assert.assertEquals(computeOrder, order);
        Assert.assertEquals(OrderState.ASSIGNED_FOR_DELETION, order.getOrderState());
    }

    // test case: Checks if deleting a failed_on_request order,
    // this one will be moved to the assignedForDeletion orders list.
    @Test
    public void testDeleteOrderStateFailedOnRequest()
            throws Exception {
        // set up
        String orderId = setupOrder(OrderState.FAILED_ON_REQUEST);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // verify
        Assert.assertNotNull(this.failedOnRequestOrdersList.getNext());
        Assert.assertNull(this.assignedForDeletionOrdersList.getNext());

        // exercise
        this.ordersController.deleteOrder(computeOrder);

        // verify
        Order order = this.assignedForDeletionOrdersList.getNext();
        this.failedOnRequestOrdersList.resetPointer();

        Assert.assertNull(this.failedOnRequestOrdersList.getNext());
        Assert.assertNotNull(order);
        Assert.assertEquals(computeOrder, order);
        Assert.assertEquals(OrderState.ASSIGNED_FOR_DELETION, order.getOrderState());
    }

    // test case: Checks if deleting a fulfilled order, this one will be moved to the assignedForDeletion orders
    // list.
    @Test
    public void testDeleteOrderStateFulfilled()
            throws Exception {
        // set up
        String orderId = setupOrder(OrderState.FULFILLED);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // verify
        Assert.assertNotNull(this.fulfilledOrdersList.getNext());
        Assert.assertNull(this.assignedForDeletionOrdersList.getNext());

        // exercise
        this.ordersController.deleteOrder(computeOrder);

        // verify
        Order order = this.assignedForDeletionOrdersList.getNext();

        Assert.assertNull(this.fulfilledOrdersList.getNext());
        Assert.assertNotNull(order);
        Assert.assertEquals(computeOrder, order);
        Assert.assertEquals(OrderState.ASSIGNED_FOR_DELETION, order.getOrderState());
    }

    // test case: Checks if deleting a spawning order, this one will be moved to the assignedForDeletion orders
    // list.
    @Test
    public void testDeleteOrderStateSpawning()
            throws Exception {
        // set up
        String orderId = setupOrder(OrderState.SPAWNING);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // verify
        Assert.assertNotNull(this.spawningOrdersList.getNext());
        Assert.assertNull(this.assignedForDeletionOrdersList.getNext());

        // exercise
        this.ordersController.deleteOrder(computeOrder);

        // verify
        Order order = this.assignedForDeletionOrdersList.getNext();

        Assert.assertNull(this.spawningOrdersList.getNext());
        Assert.assertNotNull(order);
        Assert.assertEquals(computeOrder, order);
        Assert.assertEquals(OrderState.ASSIGNED_FOR_DELETION, order.getOrderState());
    }

    // test case: Checks if deleting a pending order, this one will be moved to the assignedForDeletion orders
    // list.
    @Test
    public void testDeleteOrderStatePending()
            throws Exception {
        // set up
        String orderId = setupOrder(OrderState.PENDING);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // verify
        Assert.assertNotNull(this.pendingOrdersList.getNext());
        Assert.assertNull(this.assignedForDeletionOrdersList.getNext());

        // exercise
        this.ordersController.deleteOrder(computeOrder);

        // verify
        Order order = this.assignedForDeletionOrdersList.getNext();
        Assert.assertNull(this.pendingOrdersList.getNext());

        Assert.assertNotNull(order);
        Assert.assertEquals(computeOrder, order);
        Assert.assertEquals(OrderState.ASSIGNED_FOR_DELETION, order.getOrderState());
    }

    // test case: when try to delete an order in state checking_deletion,
    // it must raise an OnGoingOperationException.
    @Test
    public void testDeleteOrderStateCheckingDeletion()
            throws Exception {

        // set up
        String orderId = setupOrder(OrderState.CHECKING_DELETION);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // verify
        this.expectedException.expect(OnGoingOperationException.class);
        this.expectedException.expectMessage(Messages.Error.DELETE_OPERATION_ALREADY_ONGOING);

        // exercise
        this.ordersController.deleteOrder(computeOrder);
    }

    // test case: when try to delete an order in state assigned_for_delection,
    // it must raise an OnGoingOperationException.
    @Test
    public void testDeleteOrderStateAssignedForDeletion()
            throws Exception {

        // set up
        String orderId = setupOrder(OrderState.ASSIGNED_FOR_DELETION);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // verify
        this.expectedException.expect(OnGoingOperationException.class);
        this.expectedException.expectMessage(Messages.Error.DELETE_OPERATION_ALREADY_ONGOING);

        // exercise
        this.ordersController.deleteOrder(computeOrder);
    }

    // test case: when try to delete an order in state selected,
    // it must log a warning message.
    @Test
    public void testDeleteOrderStateSelected() throws Exception {

        // set up
        String orderId = setupOrder(OrderState.SELECTED);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // verify
        this.expectedException.expectMessage(String.format(Messages.Warn.REMOVING_ORDER_IN_SELECT_STATE_S, computeOrder.toString()));

        // exercise
        this.ordersController.deleteOrder(computeOrder);
    }

    // test case: Checks if deleting an open order, this one will be moved to the assignedForDeletion orders list.
    @Test
    public void testDeleteOrderStateOpen()
            throws Exception {
        // set up
        String orderId = setupOrder(OrderState.OPEN);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);

        // verify
        Assert.assertNotNull(this.openOrdersList.getNext());
        Assert.assertNull(this.assignedForDeletionOrdersList.getNext());

        // exercise
        this.ordersController.deleteOrder(computeOrder);

        // verify
        Order order = this.assignedForDeletionOrdersList.getNext();

        Assert.assertNull(this.openOrdersList.getNext());
        Assert.assertNotNull(order);
        Assert.assertEquals(computeOrder, order);
        Assert.assertEquals(OrderState.ASSIGNED_FOR_DELETION, order.getOrderState());
    }

    // test case: Checks if deleting an open order, this one will be moved to the assignedForDeletion orders list
    // even though a deleteInstance method has thrown an FogbowException.
    @Test
    public void testDeleteOrderStateOpenAndOccurAnException()
            throws Exception {
        // set up
        String orderId = setupOrder(OrderState.OPEN);
        ComputeOrder computeOrder = (ComputeOrder) this.activeOrdersMap.get(orderId);
        computeOrder.setProvider(TestUtils.ANY_VALUE);

        FogbowException fogbowExceptionExpected = new FogbowException();
        RemoteCloudConnector removeCloudConnector = Mockito.mock(RemoteCloudConnector.class);
        Mockito.doThrow(fogbowExceptionExpected).when(removeCloudConnector).deleteInstance(Mockito.eq(computeOrder));

        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(
                Mockito.eq(computeOrder.getProvider()), Mockito.eq(computeOrder.getCloudName())))
                .thenReturn(removeCloudConnector);
        PowerMockito.mockStatic(CloudConnectorFactory.class);
        BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        // verify
        Assert.assertNotNull(this.openOrdersList.getNext());
        Assert.assertNull(this.assignedForDeletionOrdersList.getNext());

        // exercise
        try {
            this.ordersController.deleteOrder(computeOrder);
            Assert.fail();
        } catch (FogbowException e) {
            Assert.assertEquals(fogbowExceptionExpected.getCause(), e.getCause());
            Assert.assertEquals(fogbowExceptionExpected.getMessage(), e.getMessage());
        }

        // verify
        Order order = this.assignedForDeletionOrdersList.getNext();
        Assert.assertNull(this.openOrdersList.getNext());
        Assert.assertNotNull(order);
        Assert.assertEquals(computeOrder, order);
        Assert.assertEquals(OrderState.ASSIGNED_FOR_DELETION, order.getOrderState());
        Mockito.verify(removeCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .deleteInstance(Mockito.eq(computeOrder));
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

    // test case: When calling the updateAllOrdersDependencies method,
    // it must verify if It performs the updateOrderDependencies to each local active order.
    @Test
    public void testUpdateAllOrdersDependencies() throws UnexpectedException {
        // set up
        Order orderRemoteActive = Mockito.spy(this.testUtils.createRemoteOrder(TestUtils.ANY_VALUE));
        ComputeOrder orderLocalNonActive = Mockito.spy(this.testUtils.createLocalComputeOrder());
        ComputeOrder orderLocalActive = Mockito.spy(this.testUtils.createLocalComputeOrder());

        this.activeOrdersMap.put(orderLocalActive.getId(), orderLocalActive);
        this.fulfilledOrdersList.addItem(orderLocalActive);

        // verify
        this.ordersController.updateAllOrdersDependencies();

        // exercise
        Mockito.verify(orderLocalActive, Mockito.times(TestUtils.RUN_ONCE))
                .isRequesterLocal(TestUtils.LOCAL_MEMBER_ID);
        Mockito.verify(this.ordersController, Mockito.times(TestUtils.RUN_ONCE))
                .updateOrderDependencies(Mockito.eq(orderLocalActive), Mockito.eq(Operation.CREATE));
        Mockito.verify(this.ordersController, Mockito.times(TestUtils.NEVER_RUN))
                .updateOrderDependencies(Mockito.eq(orderLocalNonActive), Mockito.eq(Operation.CREATE));
        Mockito.verify(this.ordersController, Mockito.times(TestUtils.NEVER_RUN))
                .updateOrderDependencies(Mockito.eq(orderRemoteActive), Mockito.eq(Operation.CREATE));
    }

    private PublicIpOrder createFulfilledPublicIpOrder(SystemUser systemUser) throws UnexpectedException {
        PublicIpOrder publicIpOrder = new PublicIpOrder();
        publicIpOrder.setSystemUser(systemUser);
        publicIpOrder.setRequester(TestUtils.LOCAL_MEMBER_ID);
        publicIpOrder.setProvider(TestUtils.LOCAL_MEMBER_ID);
        publicIpOrder.setOrderState(OrderState.FULFILLED);
        publicIpOrder.setCloudName(TestUtils.DEFAULT_CLOUD_NAME);
        return publicIpOrder;
    }

    private VolumeOrder createFulfilledVolumeOrder(SystemUser systemUser) throws UnexpectedException {
        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setSystemUser(systemUser);
        volumeOrder.setRequester(TestUtils.LOCAL_MEMBER_ID);
        volumeOrder.setProvider(TestUtils.LOCAL_MEMBER_ID);
        volumeOrder.setOrderState(OrderState.FULFILLED);
        volumeOrder.setCloudName(TestUtils.DEFAULT_CLOUD_NAME);
        return volumeOrder;
    }

    private NetworkOrder createFulfilledNetworkOrder(SystemUser systemUser) throws UnexpectedException {
        NetworkOrder networkOrder = new NetworkOrder();
        networkOrder.setSystemUser(systemUser);
        networkOrder.setRequester(TestUtils.LOCAL_MEMBER_ID);
        networkOrder.setProvider(TestUtils.LOCAL_MEMBER_ID);
        networkOrder.setOrderState(OrderState.FULFILLED);
        networkOrder.setCloudName(TestUtils.DEFAULT_CLOUD_NAME);
        return networkOrder;
    }

    private ComputeOrder createFulfilledComputeOrder(SystemUser systemUser) throws UnexpectedException {
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setSystemUser(systemUser);
        computeOrder.setRequester(TestUtils.LOCAL_MEMBER_ID);
        computeOrder.setProvider(TestUtils.LOCAL_MEMBER_ID);
        computeOrder.setOrderState(OrderState.FULFILLED);
        computeOrder.setCloudName(TestUtils.DEFAULT_CLOUD_NAME);
        return computeOrder;
    }

    private ComputeOrder createFulfilledComputeOrderWithAllocation(SystemUser systemUser) throws UnexpectedException {
        ComputeOrder order = createFulfilledComputeOrder(systemUser);
        ComputeAllocation computeAllocation = new ComputeAllocation(
                INSTANCES_LAUNCH_NUMBER, TestUtils.CPU_VALUE, TestUtils.MEMORY_VALUE, TestUtils.DISK_VALUE);
        order.setActualAllocation(computeAllocation);
        return order;
    }

    private VolumeOrder createFulfilledVolumeOrderWithAllocation(SystemUser systemUser) throws UnexpectedException {
        VolumeOrder order = createFulfilledVolumeOrder(systemUser);
        VolumeAllocation volumeAllocation = new VolumeAllocation(TestUtils.DISK_VALUE);
        order.setActualAllocation(volumeAllocation);
        return order;
    }

    private InstanceStatus createInstanceStatus(ComputeOrder computeOrder) throws UnexpectedException {
        return new InstanceStatus(computeOrder.getId(),
                computeOrder.getProvider(), computeOrder.getCloudName(),
                InstanceStatus.mapInstanceStateFromOrderState(computeOrder.getOrderState(), false, false));
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
            case CHECKING_DELETION:
                this.checkingDeletionOrdersList.addItem(computeOrder);
            default:
                break;
        }

        return orderId;
    }

}


