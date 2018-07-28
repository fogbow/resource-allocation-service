package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.datastore.DatabaseManager;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.instances.*;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DatabaseManager.class)
public class ApplicationFacadeTest extends BaseUnitTests {

    private static final String FAKE_ORDER_ID = "fake-order-id";
    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FEDERATION_TOKEN_VALUE = "federation-token-value";
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
        super.mockReadOrdersFromDataBase();

        this.orderController = Mockito.spy(new OrderController());
        this.application = ApplicationFacade.getInstance();
        this.application.setAaController(this.aaaController);
        this.application.setOrderController(this.orderController);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
    }

    // test case: When calling the method changeNetworkOrderIdsToNetworkInstanceIds(), it must change
    // the collection of OrderIDs by NetworkInstancesIDs.
    @Test
    public void testChangeNetworkOrderIdsToNetworkInstanceIds() {

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
        //FIXME: I think we cannot mock the return of the computerOrder.getNetworkId. This is the behaviour we want to
        // check. One option is to not use a mock for it or to use and to check we called the set method
    }

    // test case: When calling the method deleteCompute(), the Order passed per parameter must
    // return its state to Closed.
    @Test
    public void testDeleteComputeOrder() throws Exception {

        // set up
        Order order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(FEDERATION_TOKEN_VALUE);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        this.application.deleteCompute(order.getId(), FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.aaaController, Mockito.times(1)).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
        //FIXME: we are missing verifies
    }

    // test case: When try calling the deleteCompute() method without authentication, it must
    // throw UnauthenticatedUserException and the Order remains in the same state.
    @Test
    public void testDeleteComputeOrderWithoutAuthentication() throws Exception {

        //FIXME: improve both the name of the test and the case. I think we are testing how be behave when
        //the authentication method throws an exception not "WithoutAuthentication"

        // set up
        Order order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        try {
            // exercise
            this.application.deleteCompute(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }


    // test case: Check if deleteCompute is properly forwarding the exception thrown by
    // getFederationUser, in this case the Order must remains in the same state.
    @Test
    public void testDeleteComputeOrderWithUnauthenticatedUserExceptionInGetFederationUser() throws Exception {

        // set up
        Order order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteCompute(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteCompute() method with an operation not authorized, it
    // must throw UnauthorizedRequestException and the Order remains in the same state.
    @Test
    public void testDeleteComputeOrderWithOperationNotAuthorizedException() throws Exception {

        // set up
        Order order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        try {
            // exercise
            this.application.deleteCompute(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).authorize(
                    Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                    Mockito.any(ResourceType.class));

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When calling the createCompute() method the new Order passed by parameter without
    // state, it must return set to Open OrderState after its activation.
    @Test
    public void testCreateComputeOrder() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        // verifying that the created order is null
        Assert.assertNull(order.getOrderState());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        this.application.createCompute(order, FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.aaaController, Mockito.times(1)).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        //FIXME: we are missing the assert before the exercise
        Assert.assertEquals(OrderState.OPEN, order.getOrderState());
    }

    // test case: When try calling the createCompute() method without
    // authentication, it must throw UnauthenticatedUserException.
    @Test
    public void testCreateComputeOrderWithUnauthenticatedUserException() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        try {
            // exercise
            this.application.createCompute(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);

            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: Check if createCompute is properly forwarding the exception thrown by
    // getFederationUser.
    @Test
    public void testCreateComputeOrderWithUnauthenticatedUserExceptionInGetFederationUser() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.createCompute(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);

            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createCompute() method with an operation not authorized, it
    // must throw UnauthorizedRequestException.
    @Test
    public void testCreateComputeOrderWithOperationNotAuthorizedException() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        try {
            // exercise
            this.application.createCompute(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).authorize(
                    Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                    Mockito.any(ResourceType.class));

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

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        ComputeInstance computeInstanceExcepted = new ComputeInstance(FAKE_INSTANCE_ID);

        Mockito.doReturn(computeInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order));

        // exercise
        ComputeInstance computeInstance =
                this.application.getCompute(order.getId(), FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.orderController, Mockito.times(1))
                .getResourceInstance(Mockito.eq(order));

        Assert.assertSame(computeInstanceExcepted, computeInstance);
    }

    // test case: When calling the getCompute() method without authentication, it must
    // expected a UnauthenticatedUserException.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetComputeOrderWithoutAuthentication() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        // exercise
        this.application.getCompute(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: Check if getCompute is properly forwarding the exception thrown by
    // getFederationUser.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetComputeOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);


        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        // exercise
        this.application.getCompute(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getCompute() method with an operation not authorized, it must
    // expected a UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class) // verify
    public void testGetComputeOrderWithOperationNotAuthorized() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

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

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        List<ComputeInstance> allComputesInstances =
                this.application.getAllComputes(FEDERATION_TOKEN_VALUE);

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

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        List<ComputeInstance> allComputesInstances =
                this.application.getAllComputes(FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertEquals(0, allComputesInstances.size());
    }

    // test case: When calling the getAllComputes() method without user authentication, it must
    // expected a UnauthenticatedUserException.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetAllComputesWithoutAuthentication() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        // exercise
        this.application.getAllComputes(Mockito.anyString());
    }

    // test case: Check if getAllCompute is properly forwarding the exception thrown by
    // getFederationUser.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetAllComputesWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        // exercise
        this.application.getAllComputes(Mockito.anyString());
    }

    // test case: When calling the getAllComputes() method with an operation not authorized, it
    // must expected a UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class) // verify
    public void testGetAllComputesWithOperationNotAuthorized() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        // exercise
        this.application.getAllComputes(Mockito.anyString());
    }

    // test case: When calling the createVolume() method the new Order passed by parameter without
    // state, it must return set to Open OrderState after its activation.
    @Test
    public void testCreateVolumeOrder() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        // verifying that the created order is null
        Assert.assertNull(order.getOrderState());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        this.application.createVolume(order, FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.aaaController, Mockito.times(1)).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        Assert.assertEquals(OrderState.OPEN, order.getOrderState());
    }

    // test case: When try calling the createVolume() method without
    // authentication, it must throw UnauthenticatedUserException.
    @Test
    public void testCreateVolumeOrderWithoutAuthentication() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        try {
            // exercise
            this.application.createVolume(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);

            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: Check if createVolume is properly forwarding the exception thrown by
    // getFederationUser.
    @Test
    public void testCreateVolumeOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.createVolume(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);

            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createVolume() method with operation not authorized, it must
    // throw UnauthorizedRequestException.
    @Test
    public void testCreateVolumeOrderWithOperationNotAuthorized() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        try {
            // exercise
            this.application.createVolume(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).authorize(
                    Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                    Mockito.any(ResourceType.class));

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

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(volumeOrder.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        VolumeInstance volumeInstance = this.application.getVolume(volumeOrder.getId(), "");

        // verify
        Assert.assertSame(volumeOrder.getInstanceId(), volumeInstance.getId());
    }

    // test case: When calling the getVolume() method without authentication, it must
    // expected a UnauthenticatedUserException.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetVolumeOrderWithoutAuthentication() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        // exercise
        this.application.getVolume(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: Check if getVolume is properly forwarding the exception thrown by
    // getFederationUser.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetVolumeOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        // exercise
        this.application.getVolume(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getVolume() method with operation not authorized, it must
    // expected a UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class) // verify
    public void testGetVolumeOrderWithOperationNotAuthorized() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

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
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

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

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        List<VolumeInstance> allVolumes = this.application.getAllVolumes(FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertEquals(0, allVolumes.size());
    }
    
    // test case: When calling the getAllVolumes() method without user authentication, it must
    // expected a UnauthenticatedUserException.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetAllVolumesWithoutAuthentication() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        // exercise
        this.application.getAllVolumes(Mockito.anyString());
    }

    // test case: Check if getAllVolume is properly forwarding the exception thrown by
    // getFederationUser.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetAllVolumesWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        // exercise
        this.application.getAllVolumes(Mockito.anyString());
    }

    // test case: When calling the getAllVolumes() method with operation not authorized, it must
    // expected a UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class) // verify
    public void testGetAllVolumesWithOperationNotAuthorized() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

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
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        this.application.deleteVolume(order.getId(), FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.aaaController, Mockito.times(1)).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: When try calling the deleteVolume() method without user authentication, it must
    // throw UnauthenticatedUserException and the Order remains in the same state.
    @Test
    public void testDeleteVolumeOrderWithoutAuthentication() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        try {
            // exercise
            this.application.deleteVolume(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: Check if DeleteVolume is properly forwarding the exception thrown by
    // getFederationUser, in this case the Order must remains in the same state.
    @Test
    public void testDeleteVolumeOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteVolume(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteVolume() method with operation not authorized, it must
    // throw UnauthorizedRequestException and the Order remains in the same state.
    @Test
    public void testDeleteVolumeOrderWithOperationNotAuthorized() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        try {
            // exercise
            this.application.deleteVolume(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).authorize(
                    Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                    Mockito.any(ResourceType.class));

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When calling the createNetwork() method the new Order passed by parameter without
    // state, it must return set to Open OrderState after its activation.
    @Test
    public void testCreateNetworkOrder() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        // verifying that the created order is null
        Assert.assertNull(order.getOrderState());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        this.application.createNetwork(order, FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.aaaController, Mockito.times(1)).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        Assert.assertEquals(OrderState.OPEN, order.getOrderState());
    }

    // test case: When try calling the createNetwork() method without
    // authentication, it must throw UnauthenticatedUserException.
    @Test
    public void testCreateNetworkOrderWithoutAuthentication() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        try {
            // exercise
            this.application.createNetwork(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);

            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: Check if createNetwork is properly forwarding the exception thrown by
    // getFederationUser.
    @Test
    public void testCreateNetworkOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.createNetwork(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);

            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createNetwork() method with an operation not authorized, it
    // must throw UnauthorizedRequestException.
    @Test
    public void testCreateNetworkOrderWithOperationNotAuthorized() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        try {
            // exercise
            this.application.createNetwork(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).authorize(
                    Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                    Mockito.any(ResourceType.class));

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
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        NetworkInstance actualInstance =
                this.application.getNetwork(order.getId(), FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.orderController, Mockito.times(1))
                .getResourceInstance(Mockito.eq(order));

        Assert.assertSame(networkInstanceExcepted, actualInstance);
    }

    // test case: When calling the getNetwork() method without authentication, it must
    // expected a UnauthenticatedUserException.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetNetworkOrderWithoutAuthentication() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        // exercise
        this.application.getNetwork(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: Check if getNetwork is properly forwarding the exception thrown by
    // getFederationUser.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetNetworkOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        // exercise
        this.application.getNetwork(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getNetwork() method with an operation not authorized, it must
    // expected a UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class) // verify
    public void testGetNetworkOrderWithOperationNotAuthorized() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

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

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

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

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        List<NetworkInstance> allNetworks = this.application.getAllNetworks(FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertEquals(0, allNetworks.size());
    }

    // test case: Check if getAllNetworks is properly forwarding the exception thrown by
    // getFederationUser.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetAllNetworksWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        // exercise
        this.application.getAllNetworks(FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAllNetworks() method with an operation not authorized, it must
    // expected a UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class) // verify
    public void testGetAllNetworksWithOperationNotAuthorized() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

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

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUser.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        this.application.deleteNetwork(order.getId(), FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.aaaController, Mockito.times(1)).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: When try calling the deleteNetwork() method without user authentication, it must
    // throw UnauthenticatedUserException and the Order remains in the same state.
    @Test
    public void testDeleteNetworkOrderWithoutAuthentication() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        try {
            // exercise
            this.application.deleteNetwork(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: Check if deleteNetwork is properly forwarding the exception thrown by
    // getFederationUser, in this case the Order must remains in the same state.
    @Test
    public void testDeleteNetworkOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteNetwork(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteNetwork() method performing an operation without
    // authorization, it must throw UnauthorizedRequestException and the Order remains in the same
    // state.
    @Test
    public void testDeleteNetworkOrderWithOperationNotAuthorized() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        try {
            // exercise
            this.application.deleteNetwork(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).authorize(
                    Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                    Mockito.any(ResourceType.class));

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the createAttachment() method without
    // authentication, it must throw UnauthenticatedUserException.
    @Test
    public void testCreateAttachmentOrderWithoutAuthentication() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        try {
            // exercise
            this.application.createAttachment(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);

            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: Check if createAttachment is properly forwarding the exception thrown by
    // getFederationUser.
    @Test
    public void testCreateAttachmentOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.createAttachment(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);

            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: When try calling the createAttachment() method with an operation not authorized,
    // it
    // must throw UnauthorizedRequestException.
    @Test
    public void testCreateAttachmentOrderWithOperationNotAuthorized() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        try {
            // exercise
            this.application.createAttachment(order, FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).authorize(
                    Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                    Mockito.any(ResourceType.class));

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
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

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
    public void testGetAttachmentOrderWithoutAuthentication() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        // exercise
        this.application.getAttachment(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: Check if getAttachment is properly forwarding the exception thrown by
    // getFederationUser.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetAttachmentOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        // exercise
        this.application.getAttachment(order.getId(), FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAttachment() method performing an operation without
    // authorization, it must expected a UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class) // verify
    public void testGetAttachmentOrderWithOperationNotAuthorized() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

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
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

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
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        List<AttachmentInstance> allAttachments =
                this.application.getAllAttachments(FEDERATION_TOKEN_VALUE);

        // verify
        Assert.assertEquals(0, allAttachments.size());
    }

    // test case: Check if getAllAttachment is properly forwarding the exception thrown by
    // getFederationUser.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetAllAttachmentsWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        // exercise
        this.application.getAllAttachments(FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAllAttachments() method with an operation not authorized, it
    // must expected a UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class) // verify
    public void testGetAllAttachmentWithOperationNotAuthorized() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

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
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        this.application.deleteAttachment(order.getId(), FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.aaaController, Mockito.times(1)).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: When try calling the deleteAttachment() method without user authentication, it
    // must throw UnauthenticatedUserException and the Order remains in the same state.
    @Test
    public void testDeleteAttachmentOrderWithoutAuthentication() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticate(Mockito.anyString());

        try {
            // exercise
            this.application.deleteAttachment(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: Check if deleteAttachment is properly forwarding the exception thrown by
    // getFederationUser, in this case the Order must remains in the same state.
    @Test
    public void testDeleteAttachmentOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteAttachment(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticate(FEDERATION_TOKEN_VALUE);
            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the deleteAttachment() method with operation not authorized, it
    // must throw UnauthorizedRequestException and the Order remains in the same state.
    @Test
    public void testDeleteAttachmentOrderWithOperationNotAuthorized() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.anyString());

        Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).authorize(
                Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        try {
            // exercise
            this.application.deleteAttachment(order.getId(), FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).authorize(
                    Mockito.any(FederationUser.class), Mockito.any(Operation.class),
                    Mockito.any(ResourceType.class));

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    private NetworkOrder createNetworkOrder() throws Exception {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser(FAKE_USER, attributes);
        NetworkOrder order = new NetworkOrder(federationUser, FAKE_MEMBER_ID, FAKE_MEMBER_ID,
                FAKE_GATEWAY, FAKE_ADDRESS, NetworkAllocationMode.STATIC);

        NetworkInstance networtkInstanceExcepted = new NetworkInstance(order.getId());
        Mockito.doReturn(networtkInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order));
        order.setInstanceId(networtkInstanceExcepted.getId());

        return order;
    }

    private VolumeOrder createVolumeOrder() throws Exception {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser(FAKE_USER, attributes);
        VolumeOrder order = new VolumeOrder(federationUser, FAKE_MEMBER_ID, FAKE_MEMBER_ID, 1,
                FAKE_VOLUME_NAME);

        VolumeInstance volumeInstanceExcepted = new VolumeInstance(order.getId());
        Mockito.doReturn(volumeInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order));
        order.setInstanceId(volumeInstanceExcepted.getId());

        return order;
    }

    private ComputeOrder createComputeOrder() throws Exception {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser(FAKE_USER, attributes);

        ComputeOrder order = new ComputeOrder(federationUser, FAKE_MEMBER_ID, FAKE_MEMBER_ID, 2, 2,
                30, FAKE_IMAGE_NAME, new UserData(), FAKE_PUBLIC_KEY, null);

        ComputeInstance computeInstanceExcepted = new ComputeInstance(order.getId());

        Mockito.doReturn(computeInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order));

        order.setInstanceId(computeInstanceExcepted.getId());

        return order;
    }

    private AttachmentOrder createAttachmentOrder() throws Exception {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser(FAKE_USER, attributes);

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
