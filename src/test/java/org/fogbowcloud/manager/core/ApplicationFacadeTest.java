package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.datastore.DatabaseManager;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.manager.core.models.instances.*;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DatabaseManager.class)
public class ApplicationFacadeTest extends BaseUnitTests {

    private static final String FAKE_ORDER_ID = "fake-order-id";
    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FEDERATION_TOKEN_VALUE = "federation_token-value";
    private static final String FAKE_USER = "fake-user";
    private static final String FAKE_MEMBER_ID = "fake-member-id";
    private static final String FAKE_GATEWAY = "fake-gateway";
    private static final String FAKE_ADDRESS = "fake-address";
    private static final String FAKE_VOLUME_NAME = "fake-volume-name";
    private static final String FAKE_IMAGE_NAME = "fake-image-name";
    private static final String FAKE_PUBLIC_KEY = "fake-public-key";
    private static final String FAKE_SOURCE_ID = "fake-source-id";
    private static final String FAKE_TARGET_ID = "fake-target-id";
    private static final String FAKE_DEVICE_MOUNT_POINT = "fake-device-mount-point";

    private ApplicationFacade application;
    private AaController aaaController;
    private OrderController orderController;
    private Map<String, Order> activeOrdersMap;

    @Before
    public void setUp() throws UnauthorizedRequestException {
        this.aaaController = Mockito.mock(AaController.class);

        HomeDir.getInstance().setPath("src/test/resources/private");
        super.mockReadOrdersFromDataBase(); //TODO
//        DatabaseManager databaseManager = Mockito.mock(DatabaseManager.class);
//        Mockito.when(databaseManager.readActiveOrders(OrderState.OPEN))
//                .thenReturn(new SynchronizedDoublyLinkedList());
//        Mockito.when(databaseManager.readActiveOrders(OrderState.SPAWNING))
//                .thenReturn(new SynchronizedDoublyLinkedList());
//        Mockito.when(databaseManager.readActiveOrders(OrderState.FAILED))
//                .thenReturn(new SynchronizedDoublyLinkedList());
//        Mockito.when(databaseManager.readActiveOrders(OrderState.FULFILLED))
//                .thenReturn(new SynchronizedDoublyLinkedList());
//        Mockito.when(databaseManager.readActiveOrders(OrderState.PENDING))
//                .thenReturn(new SynchronizedDoublyLinkedList());
//        Mockito.when(databaseManager.readActiveOrders(OrderState.CLOSED))
//                .thenReturn(new SynchronizedDoublyLinkedList());
//
//        Mockito.doNothing().when(databaseManager).add(Matchers.any(Order.class));
//        Mockito.doNothing().when(databaseManager).update(Matchers.any(Order.class));
//
//        PowerMockito.mockStatic(DatabaseManager.class);
//        BDDMockito.given(DatabaseManager.getInstance()).willReturn(databaseManager);

        this.orderController = Mockito.spy(new OrderController());
        this.application = ApplicationFacade.getInstance();
        this.application.setAaController(this.aaaController);
        this.application.setOrderController(this.orderController);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
    }

    // test case: When calling the method changeNetworkOrderIdsToNetworInstanceIds(), it must change
    // the collection of OrderIDs by NetworkInstacesIDs.
    @Test
    public void changeNetworkOrderIdsToNetworInstanceIdsTest() {

        // set up
        NetworkOrder networkOrder = Mockito.mock(NetworkOrder.class);

        Mockito.doReturn(FAKE_ORDER_ID).when(networkOrder).getId();
        Mockito.doReturn(FAKE_INSTANCE_ID).when(networkOrder).getInstanceId();

        this.activeOrdersMap.put(networkOrder.getId(), networkOrder);

        List<String> networkIdList = new ArrayList<>();
        networkIdList.add(FAKE_ORDER_ID);
        
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setNetworksId(networkIdList);
        
        List<String> expectedList = new ArrayList<>();
        expectedList.add(FAKE_INSTANCE_ID);

        // exercise
        this.application.changeNetworkOrderIdsToNetworInstanceIds(computeOrder);

        // verify
        Assert.assertEquals(expectedList, computeOrder.getNetworksId());
    }

    // test case: When calling the method deleteCompute(), the Order passed per parameter must
    // return its state to Closed.
    @Test
    public void testDeleteComputeOrder() throws Exception {

        // set up
        Order order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);
        
        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(FEDERATION_TOKEN_VALUE);

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.deleteCompute(order.getId(), FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.aaaController, Mockito.times(1)).authenticate(FEDERATION_TOKEN_VALUE);
        Mockito.verify(this.aaaController, Mockito.times(1)).authorize(Mockito.any(FederationUser.class), Mockito.any(Operation.class), Mockito.any(Order.class));
        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: When try calling the deleteCompute() method without user authentication, it must
    // throw UnauthenticatedUserException and the Order remains in the same state.
    @Test
    public void testDeleteComputeOrderUnathenticated() throws Exception {

        // set up
        Order order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString(),
                Mockito.any(FederationUser.class), Mockito.any(InstanceType.class));

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        try {
            // exercise
            this.application.deleteCompute(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteCompute() method without a token authentication, it
    // must throw UnauthenticatedUserException and the Order remains in the same state.
    @Test
    public void testDeleteComputeOrderTokenUnathenticated() throws Exception {

        // set up
        Order order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteCompute(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteCompute() method with a Federation User without
    // authentication, it must throw UnauthenticatedUserException and the Order remains in the same
    // state.
    @Test
    public void testDeleteComputeOrderWithFederationUserUnauthenticated() throws Exception {

        // set up
        Order order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteCompute(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteCompute() method performing an operation without
    // authorization, it must throw UnauthorizedRequestException and the Order remains in the same
    // state.
    @Test
    public void testDeleteComputeOrderUnauthorizedOperation() throws Exception {

        // set up
        Order order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.any(Order.class));

        try {
            // exercise
            this.application.deleteCompute(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When calling the createCompute() method the Order passed by parameter must return
    // set to Open OrderState after its activation.
    @Test
    public void testCreateComputeOrder() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.createCompute(order, FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertEquals(OrderState.OPEN, order.getOrderState());
    }

    // test case: When try calling the createCompute() method without
    // authentication, it must throw UnauthenticatedUserException.
    @Test
    public void testCreateComputeOrderUnauthenticated() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.createCompute(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createCompute() method without a token authentication, it
    // must throw UnauthenticatedUserException.
    @Test
    public void testCreateComputeOrderTokenUnauthenticated() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.createCompute(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createCompute() method with a Federation User without
    // authentication, it must throw UnauthenticatedUserException.
    @Test
    public void testCreateComputeOrderWithFederationUserUnauthenticated() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.createCompute(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createCompute() method performing an operation without
    // authorization, it must throw UnauthorizedRequestException.
    @Test
    public void testCreateComputeOrderUnauthorizedOperation() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        try {
            // exercise
            this.application.createCompute(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When calling the getCompute() method, it must return the ComputeInstance of the
    // OrderID passed per parameter.
    @Test
    public void testGetComputeOrder() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        ComputeInstance computeInstanceExcepted = new ComputeInstance("");
        Mockito.doReturn(computeInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order));

        // exercise
        ComputeInstance computeInstance =
                this.application.getCompute(order.getId(), FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertSame(computeInstanceExcepted, computeInstance);
    }

    // test case: When calling the getCompute() method without authentication, it must
    // expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetComputeOrderUnauthenticated() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.getCompute(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getCompute() method with a Federation User without
    // authentication, it must expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetComputeOrderWithFederationUserUnauthenticated() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.getCompute(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getCompute() method without a token authentication, it must
    // expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetComputeOrderTokenUnauthenticated() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.getCompute(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getCompute() method performing an operation without
    // authorization, it must expected a UnauthorizedRequestException.
    @Test
    // verify
    (expected = UnauthorizedRequestException.class)
    public void testGetComputeOrderUnauthorizedOperation() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        // exercise
        this.application.getCompute(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAllComputes() method, it must return a list of the
    // ComputeInstance.
    @Test
    public void testGetAllComputes() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        List<ComputeInstance> allComputesInstances = this.application.getAllComputes("");

        // verify
        Assert.assertEquals(1, allComputesInstances.size());
        Assert.assertEquals(order.getInstanceId(), allComputesInstances.get(0).getId());
    }

    // test case: When calling the getAllComputes() method without entering a valid token value
    // federation, it must return a empty list.
    @Test
    public void testGetAllComputesEmpty() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        List<ComputeInstance> allComputesInstances =
                this.application.getAllComputes(Mockito.anyString());

        // verify
        Assert.assertEquals(0, allComputesInstances.size());
    }

    // test case: When calling the getAllComputes() method without user authentication, it must
    // expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetAllComputesUnauthenticated() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        this.application.getAllComputes(Mockito.anyString());
    }

    // test case: When calling the getAllComputes() method without a token authentication, it must
    // expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetAllComputesTokenUnauthenticated() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        this.application.getAllComputes(Mockito.anyString());
    }

    // test case: When calling the getAllComputes() method with a Federation User without
    // authentication, it must expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetAllComputesWithFederationUserUnauthenticated() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        this.application.getAllComputes(Mockito.anyString());
    }

    // test case: When calling the getAllComputes() method performing an operation without
    // authorization, it must expected a UnauthorizedRequestException.
    @Test
    // verify
    (expected = UnauthorizedRequestException.class)
    public void testGetAllComputesOperationUnauthorized() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        // exercise
        this.application.getAllComputes(Mockito.anyString());
    }

    // test case: When calling the createVolume() method the Order passed by parameter must return
    // set to Open OrderState after its activation.
    @Test
    public void testCreateVolumeOrder() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.createVolume(order, FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertEquals(OrderState.OPEN, order.getOrderState());
    }

    // test case: When try calling the createVolume() method without
    // authentication, it must throw UnauthenticatedUserException.
    @Test
    public void testCreateVolumeOrderUnauthenticated() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.createVolume(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createVolume() method without a token authentication, it
    // must throw UnauthenticatedUserException.
    @Test
    public void testCreateVolumeOrderTokenUnauthenticated() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.createVolume(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createVolume() method with a Federation User without
    // authentication, it must throw UnauthenticatedUserException.
    @Test
    public void testCreateVolumeOrderWithFederationUserUnauthenticated() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.createVolume(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createVolume() method performing an operation without
    // authorization, it must throw UnauthorizedRequestException.
    @Test
    public void testCreateVolumeOrderUnauthorizedOperation() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        try {
            // exercise
            this.application.createVolume(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When calling the getVolume() method, it must return the VolumeInstance of the
    // OrderID passed per parameter.
    @Test
    public void testGetVolumeOrder() throws Exception {

        // set up
        VolumeOrder volumeOrder = createVolumeOrder();
        OrderStateTransitioner.activateOrder(volumeOrder);

        Mockito.doReturn(volumeOrder.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        VolumeInstance volumeInstance = this.application.getVolume(volumeOrder.getId(), "");

        // verify
        Assert.assertSame(volumeOrder.getInstanceId(), volumeInstance.getId());
    }

    // test case: When calling the getVolume() method without authentication, it must
    // expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetVolumeOrderUnauthenticated() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.getVolume(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getVolume() method with a Federation User without
    // authentication, it must expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetVolumeOrderWithFederationUserUnauthenticated() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.getVolume(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getVolume() method without a token authentication, it must
    // expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetVolumeOrderTokenUnauthenticated() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.getVolume(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getVolume() method performing an operation without
    // authorization, it must expected a UnauthorizedRequestException.
    @Test
    // verify
    (expected = UnauthorizedRequestException.class)
    public void testGetVolumeOrderUnauthorizedOperation() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        // exercise
        this.application.getVolume(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAllVolume() method, it must return a list of the
    // VolumeInstance.
    @Test
    public void testGetAllVolumes() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        List<VolumeInstance> volumeInstances =
                this.application.getAllVolumes(FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertEquals(1, volumeInstances.size());
        Assert.assertSame(order.getInstanceId(), volumeInstances.get(0).getId());
    }

    // test case: When calling the getAllVolumes() method without entering a valid token value
    // federation, it must return a empty list.
    @Test
    public void testGetAllVolumesEmpty() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        List<VolumeInstance> allVolumes = this.application.getAllVolumes(FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertEquals(0, allVolumes.size());
    }

    // test case: When calling the getAllVolumes() method without a token authentication, it must
    // expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetAllVolumesTokenUnauthenticated() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        this.application.getAllVolumes(FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAllVolumes() method with a Federation User without
    // authentication, it must expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetAllVolumesWithFederationUserUnauthenticated() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        this.application.getAllVolumes(Mockito.anyString());
    }

    // test case: When calling the getAllVolumes() method performing an operation without
    // authorization, it must expected a UnauthorizedRequestException.
    @Test
    // verify
    (expected = UnauthorizedRequestException.class)
    public void testGetAllVolumesOperationUnauthorized() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        // exercise
        this.application.getAllVolumes(FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the method deleteVolume(), the Order passed per parameter must return
    // its state to Closed.
    @Test
    public void testDeleteVolumeOrder() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.deleteVolume(order.getId(), FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: When try calling the deleteVolume() method without user authentication, it must
    // throw UnauthenticatedUserException and the Order remains in the same state.
    @Test
    public void testDeleteVolumeOrderUnathenticated() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString(),
                Mockito.any(FederationUser.class), Mockito.any(InstanceType.class));

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.deleteVolume(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteVolume() method without a token authentication, it
    // must throw UnauthenticatedUserException and the Order remains in the same state.
    @Test
    public void testDeleteVolumeOrderTokenUnathenticated() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteVolume(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteVolume() method with a Federation User without
    // authentication, it must throw UnauthenticatedUserException and the Order remains in the same
    // state.
    @Test
    public void testDeleteVolumeOrderWithFederationUserUnauthenticated() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteVolume(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteVolume() method performing an operation without
    // authorization, it must throw UnauthorizedRequestException and the Order remains in the same
    // state.
    @Test
    public void testDeleteVolumeOrderUnauthorizedOperation() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.any(Order.class));

        try {
            // exercise
            this.application.deleteVolume(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When calling the createNetwork() method the Order passed by parameter must return
    // set to Open OrderState after its activation.
    @Test
    public void testCreateNetworkOrder() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.createNetwork(order, FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertEquals(OrderState.OPEN, order.getOrderState());
    }

    // test case: When try calling the createNetwork() method without
    // authentication, it must throw UnauthenticatedUserException.
    @Test
    public void testCreateNetworkOrderUnauthenticated() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.createNetwork(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createNetWork() method without a token authentication, it
    // must throw UnauthenticatedUserException.
    @Test
    public void testCreateNetworkOrderTokenUnauthenticated() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.createNetwork(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createNetwork() method with a Federation User without
    // authentication, it must throw UnauthenticatedUserException.
    @Test
    public void testCreateNetworkOrderWithFederationUserUnauthenticated() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.createNetwork(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createNetwork() method performing an operation without
    // authorization, it must throw UnauthorizedRequestException.
    @Test
    public void testCreateNetworkOrderUnauthorizedOperation() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        try {
            // exercise
            this.application.createNetwork(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When calling the getNetwork() method, it must return the NetworkInstance of the
    // OrderID passed per parameter.
    @Test
    public void testGetNetworkOrder() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        NetworkInstance networkInstanceExcepted = new NetworkInstance("");
        Mockito.doReturn(networkInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order));

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        NetworkInstance actualInstance =
                this.application.getNetwork(order.getId(), FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertSame(networkInstanceExcepted, actualInstance);
    }

    // test case: When calling the getNetwork() method without authentication, it must
    // expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetNetworkOrderUnauthenticated() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.getNetwork(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getNetwork() method with a Federation User without
    // authentication, it must expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetNetworkOrderWithFederationUserUnauthenticated() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.getNetwork(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getNetwork() method without a token authentication, it must
    // expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetNetworkOrderTokenUnauthenticated() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.getNetwork(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getNetwork() method performing an operation without
    // authorization, it must expected a UnauthorizedRequestException.
    @Test
    // verify
    (expected = UnauthorizedRequestException.class)
    public void testGetNetworkOrderUnauthorizedOperation() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        // exercise
        this.application.getNetwork(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAllNetworks() method, it must return a list of the
    // NetworkInstance.
    @Test
    public void testGetAllNetworks() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        List<NetworkInstance> allNetworks = this.application.getAllNetworks("");

        // verify
        Assert.assertEquals(1, allNetworks.size());
        Assert.assertSame(order.getInstanceId(), allNetworks.get(0).getId());
    }

    // test case: When calling the getAllNetworks() method without entering a valid token value
    // federation, it must return a empty list.
    @Test
    public void testGetAllNetworksEmpty() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        List<NetworkInstance> allNetworks = this.application.getAllNetworks(FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertEquals(0, allNetworks.size());
    }

    // test case: When calling the getAllNetworks() method without a token authentication, it must
    // expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetAllNetworksTokenUnauthenticated() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        this.application.getAllNetworks(FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAllNetworks() method with a Federation User without
    // authentication, it must expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetAllNetworksWithFederationUserUnauthenticated() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        this.application.getAllNetworks(FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAllNetworks() method performing an operation without
    // authorization, it must expected a UnauthorizedRequestException.
    @Test
    // verify
    (expected = UnauthorizedRequestException.class)
    public void testGetAllNetworksOperationUnauthorized() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        // exercise
        this.application.getAllNetworks(FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the method deleteNetwork(), the Order passed per parameter must
    // return its state to Closed.
    @Test
    public void testDeleteNetworkOrder() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.deleteNetwork(order.getId(), FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: When try calling the deleteNetwork() method without user authentication, it must
    // throw UnauthenticatedUserException and the Order remains in the same state.
    @Test
    public void testDeleteNetworkOrderUnathenticated() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString(),
                Mockito.any(FederationUser.class), Mockito.any(InstanceType.class));

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.deleteNetwork(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteNetwork() method without a token authentication, it
    // must throw UnauthenticatedUserException and the Order remains in the same state.
    @Test
    public void testDeleteNetworkOrderTokenUnathenticated() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteNetwork(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteNetwork() method with a Federation User without
    // authentication, it must throw UnauthenticatedUserException and the Order remains in the same
    // state.
    @Test
    public void testDeleteNetworkOrderWithFederationUserUnauthenticated() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteNetwork(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteNetwork() method performing an operation without
    // authorization, it must throw UnauthorizedRequestException and the Order remains in the same
    // state.
    @Test
    public void testDeleteNetworkOrderUnauthorizedOperation() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.any(Order.class));

        try {
            // exercise
            this.application.deleteNetwork(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the createAttachment() method without
    // authentication, it must throw UnauthenticatedUserException.
    @Test
    public void testCreateAttachmentOrderUnauthenticated() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.createAttachment(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createAttachment() method without a token authentication, it
    // must throw UnauthenticatedUserException.
    @Test
    public void testCreateAttachmentOrderTokenUnauthenticated() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.createAttachment(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createAttachment() method with a Federation User without
    // authentication, it must throw UnauthenticatedUserException.
    @Test
    public void testCreateAttachmentOrderWithFederationUserUnauthenticated() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.createAttachment(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createAttachment() method performing an operation without
    // authorization, it must throw UnauthorizedRequestException.
    @Test
    public void testCreateAttachmentOrderUnauthorizedOperation() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        try {
            // exercise
            this.application.createAttachment(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When calling the getAttachment() method, it must return the AttachmentInstance of
    // the
    // OrderID passed per parameter.
    @Test
    public void testGetAttachmentOrder() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        AttachmentInstance actualInstance =
                this.application.getAttachment(order.getId(), FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertNotNull(actualInstance);
    }

    // test case: When calling the getAttachment() method without authentication, it must
    // expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetAttachmentOrderUnauthenticated() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.getAttachment(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAttachment() method with a Federation User without
    // authentication, it must expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetAttachmentOrderWithFederationUserUnauthenticated() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.getAttachment(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAttachment() method without a token authentication, it must
    // expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetAttachmentOrderTokenUnauthenticated() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.getAttachment(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAttachment() method performing an operation without
    // authorization, it must expected a UnauthorizedRequestException.
    @Test
    // verify
    (expected = UnauthorizedRequestException.class)
    public void testGetAttachmentOrderUnauthorizedOperation() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        // exercise
        this.application.getAttachment(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAllAttachments() method, it must return a list of the
    // AttachmentInstance.
    @Test
    public void testGetAllAttachments() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        List<AttachmentInstance> allAttachments =
                this.application.getAllAttachments(FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertEquals(1, allAttachments.size());
    }

    // test case: When calling the getAllAttachments() method without entering a valid token value
    // federation, it must return a empty list.
    @Test
    public void testGetAllAttachmentEmpty() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        List<AttachmentInstance> allAttachments =
                this.application.getAllAttachments(FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertEquals(0, allAttachments.size());
    }

    // test case: When calling the getAllAttachments() method without a token authentication, it
    // must
    // expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetAllAttachmentTokenUnauthenticated() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        this.application.getAllAttachments(FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAllAttachments() method with a Federation User without
    // authentication, it must expected a UnauthenticatedUserException.
    @Test
    // verify
    (expected = UnauthenticatedUserException.class)
    public void testGetAllAttachmentsWithFederationUserUnauthenticated() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(InstanceType.class));

        // exercise
        this.application.getAllAttachments(FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAllAttachments() method performing an operation without
    // authorization, it must expected a UnauthorizedRequestException.
    @Test
    // verify
    (expected = UnauthorizedRequestException.class)
    public void testGetAllAttachmentOperationUnauthorized() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(InstanceType.class));

        // exercise
        this.application.getAllAttachments(FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the method deleteAttachment(), the Order passed per parameter must
    // return its state to Closed.
    @Test
    public void testDeleteAttachmentOrder() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        // exercise
        this.application.deleteAttachment(order.getId(), FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: When try calling the deleteAttachment() method without user authentication, it
    // must throw UnauthenticatedUserException and the Order remains in the same state.
    @Test
    public void testDeleteAttachmentOrderUnathenticated() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();

        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString(),
                Mockito.any(FederationUser.class), Mockito.any(InstanceType.class));

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.deleteAttachment(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteAttachment() method without a token authentication, it
    // must throw UnauthenticatedUserException and the Order remains in the same state.
    @Test
    public void testDeleteAttachmentOrderTokenUnathenticated() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteAttachment(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteAttachment() method with a Federation User without
    // authentication, it must throw UnauthenticatedUserException and the Order remains in the same
    // state.
    @Test
    public void testDeleteAttachmentOrderWithFederationUserUnauthenticated() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(Order.class));

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteAttachment(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteAttachment() method performing an operation without
    // authorization, it must throw UnauthorizedRequestException and the Order remains in the same
    // state.
    @Test
    public void testDeleteAttachmentOrderUnauthorizedOperation() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(Order.class));

        Mockito.doNothing().when(this.orderController).deleteOrder(Mockito.any(Order.class));

        try {
            // exercise
            this.application.deleteAttachment(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    private NetworkOrder createNetworkOrder() throws Exception {
        FederationUser federationUser = new FederationUser(FAKE_USER, new HashMap<>());
        NetworkOrder order = new NetworkOrder(federationUser, FAKE_MEMBER_ID, FAKE_MEMBER_ID,
                FAKE_GATEWAY, FAKE_ADDRESS, NetworkAllocationMode.STATIC);

        NetworkInstance networtkInstanceExcepted = new NetworkInstance(order.getId());
        Mockito.doReturn(networtkInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order));
        order.setInstanceId(networtkInstanceExcepted.getId());

        return order;
    }

    private VolumeOrder createVolumeOrder() throws Exception {
        FederationUser federationUser = new FederationUser(FAKE_USER, new HashMap<>());
        VolumeOrder order = new VolumeOrder(federationUser, FAKE_MEMBER_ID, FAKE_MEMBER_ID, 1,
                FAKE_VOLUME_NAME);

        VolumeInstance volumeInstanceExcepted = new VolumeInstance(order.getId());
        Mockito.doReturn(volumeInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order));
        order.setInstanceId(volumeInstanceExcepted.getId());

        return order;
    }

    private ComputeOrder createComputeOrder() throws Exception {
        FederationUser federationUser = new FederationUser(FAKE_USER, new HashMap<>());

        ComputeOrder order = new ComputeOrder(federationUser, FAKE_MEMBER_ID, FAKE_MEMBER_ID, 2, 2,
                30, FAKE_IMAGE_NAME, new UserData(), FAKE_PUBLIC_KEY, null);

        ComputeInstance computeInstanceExcepted = new ComputeInstance(order.getId());
        Mockito.doReturn(computeInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order));
        order.setInstanceId(computeInstanceExcepted.getId());

        return order;
    }

    private AttachmentOrder createAttachmentOrder() throws Exception {
        FederationUser federationUser = new FederationUser(FAKE_USER, new HashMap<>());

        ComputeOrder computeOrder = new ComputeOrder();
        ComputeInstance computeInstance = new ComputeInstance(FAKE_SOURCE_ID);
        computeOrder.setInstanceId(computeInstance.getId());
        this.activeOrdersMap.put(computeOrder.getId(), computeOrder);
        String sourceId = computeOrder.getId();

        VolumeOrder volumeOrder = new VolumeOrder();
        VolumeInstance volumeInstance = new VolumeInstance(FAKE_TARGET_ID);
        volumeOrder.setInstanceId(volumeInstance.getId());
        this.activeOrdersMap.put(volumeOrder.getId(), volumeOrder);
        String targetId = volumeOrder.getId();

        AttachmentOrder order = new AttachmentOrder(federationUser, FAKE_MEMBER_ID, FAKE_MEMBER_ID,
                sourceId, targetId, FAKE_DEVICE_MOUNT_POINT);

        AttachmentInstance attachmentInstanceExcepted = new AttachmentInstance(order.getId());

        Mockito.doReturn(attachmentInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order));
        order.setInstanceId(attachmentInstanceExcepted.getId());

        return order;
    }

}
