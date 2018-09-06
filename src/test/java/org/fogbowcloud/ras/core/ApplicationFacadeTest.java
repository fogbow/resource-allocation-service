package org.fogbowcloud.ras.core;

import org.fogbowcloud.ras.core.constants.Operation;
import org.fogbowcloud.ras.core.datastore.DatabaseManager;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.ras.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.AttachmentInstance;
import org.fogbowcloud.ras.core.models.instances.ComputeInstance;
import org.fogbowcloud.ras.core.models.instances.NetworkInstance;
import org.fogbowcloud.ras.core.models.instances.VolumeInstance;
import org.fogbowcloud.ras.core.models.orders.*;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DatabaseManager.class)
public class ApplicationFacadeTest extends BaseUnitTests {

    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_INSTANCE_NAME = "fake-instance-name";
    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_FEDERATION_TOKEN_VALUE = "federation-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USER_NAME = "fake-user-name";
    private static final String FAKE_MEMBER_ID = "fake-member-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_GATEWAY = "fake-gateway";
    private static final String FAKE_ADDRESS = "fake-address";
    private static final String FAKE_VOLUME_NAME = "fake-volume-name";
    private static final String FAKE_IMAGE_NAME = "fake-image-name";
    private static final String FAKE_PUBLIC_KEY = "fake-public-key";
    private static final String FAKE_SOURCE_ID = "fake-source-id";
    private static final String FAKE_TARGET_ID = "fake-target-id";
    private static final String FAKE_DEVICE_MOUNT_POINT = "fake-device-mount-point";

    private ApplicationFacade application;
    private AaaController aaaController;
    private OrderController orderController;
    private Map<String, Order> activeOrdersMap;

    @Before
    public void setUp() throws UnexpectedException {
        this.aaaController = Mockito.mock(AaaController.class);

        super.mockReadOrdersFromDataBase();

        this.orderController = Mockito.spy(new OrderController());
        this.application = ApplicationFacade.getInstance();
        this.application.setAaaController(this.aaaController);
        this.application.setOrderController(this.orderController);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
    }

    // test case: When calling the method deleteCompute(), the Order passed as parameter must
    // have its state changed to Closed.
    @Test
    public void testDeleteComputeOrder() throws Exception {

        // set up
        Order order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        // exercise
        this.application.deleteCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.aaaController, Mockito.times(1)).authenticateAndAuthorize(
                Mockito.any(FederationUserToken.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class), Mockito.any(Order.class));

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
                .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));
        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                            Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }


    // test case: Check if deleteCompute is properly forwarding the exception thrown by
    // getFederationUserToken, in this case the Order must remain in the same state.
    @Test
    public void testDeleteComputeOrderWithUnauthenticatedUserExceptionInGetFederationUser() throws Exception {

        // set up
        Order order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.any(FederationUserToken.class));

        Mockito.doThrow(new InvalidParameterException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);

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

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.deleteCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).
                    authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                            Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

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

        Mockito.doNothing().when(this.aaaController).authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        // exercise
        this.application.createCompute(order, FAKE_FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.aaaController, Mockito.times(1)).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class));

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
                .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class));
        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.createCompute(order, FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                            Mockito.any(Operation.class), Mockito.any(ResourceType.class));

            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: Check if createCompute is properly forwarding the exception thrown by
    // getFederationUserToken.
    @Test
    public void testCreateComputeOrderWithUnauthenticatedUserExceptionInGetFederationUser() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.any(FederationUserToken.class));

        Mockito.doThrow(new InvalidParameterException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.createCompute(order, FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);

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

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        try {
            // exercise
            this.application.createCompute(order, FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).
                    authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                            Mockito.any(Operation.class), Mockito.any(ResourceType.class));

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

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.any(FederationUserToken.class));

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        ComputeInstance computeInstanceExcepted = new ComputeInstance(FAKE_INSTANCE_ID);

        Mockito.doReturn(computeInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order.getId()));

        // exercise
        ComputeInstance computeInstance =
                this.application.getCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.orderController, Mockito.times(1))
                .getResourceInstance(Mockito.eq(order.getId()));

        Assert.assertSame(computeInstanceExcepted, computeInstance);
    }

    // test case: When calling the getCompute() method without authentication, it must
    // throw a UnauthenticatedUserException.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetComputeOrderWithoutAuthentication() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        // exercise
        this.application.getCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
    }

    // test case: Check if getCompute is properly forwarding the exception thrown by
    // getFederationUserToken.
    @Test(expected = InvalidParameterException.class) // verify
    public void testGetComputeOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        Mockito.doThrow(new InvalidParameterException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        // exercise
        this.application.getCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getCompute() method with an operation not authorized, it must
    // expected a UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class) // verify
    public void testGetComputeOrderWithOperationNotAuthorized() throws Exception {

        // set up
        ComputeOrder order = createComputeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        // exercise
        this.application.getCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the createVolume() method the new Order passed by parameter without
    // state, it must return set to Open OrderState after its activation.
    @Test
    public void testCreateVolumeOrder() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        // verifying that the created order is null
        Assert.assertNull(order.getOrderState());

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        this.application.createVolume(order, FAKE_FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.aaaController, Mockito.times(1)).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class), Mockito.any(Operation.class),
                        Mockito.any(ResourceType.class));

        Assert.assertEquals(OrderState.OPEN, order.getOrderState());
    }

    // test case: When try calling the createVolume() method without
    // authentication, it must throw UnauthenticatedUserException.
    @Test
    public void testCreateVolumeOrderWithoutAuthentication() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        try {
            // exercise
            this.application.createVolume(order, FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);

            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: Check if createVolume is properly forwarding the exception thrown by
    // getFederationUserToken.
    @Test
    public void testCreateVolumeOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.any(FederationUserToken.class));

        Mockito.doThrow(new InvalidParameterException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.createVolume(order, FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);

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

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class), Mockito.any(Operation.class),
                        Mockito.any(ResourceType.class));

        try {
            // exercise
            this.application.createVolume(order, FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).
                    authenticateAndAuthorize(Mockito.any(FederationUserToken.class), Mockito.any(Operation.class),
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

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.any(FederationUserToken.class));

        Mockito.doReturn(volumeOrder.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        VolumeInstance volumeInstance = this.application.getVolume(volumeOrder.getId(), "");

        // verify
        Assert.assertSame(volumeOrder.getInstanceId(), volumeInstance.getId());
    }

    // test case: When calling the getVolume() method without authentication, it must
    // throw a UnauthenticatedUserException.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetVolumeOrderWithoutAuthentication() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        // exercise
        this.application.getVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
    }

    // test case: Check if getVolume is properly forwarding the exception thrown by
    // getFederationUserToken.
    @Test(expected = InvalidParameterException.class) // verify
    public void testGetVolumeOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.any(FederationUserToken.class));

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new InvalidParameterException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        // exercise
        this.application.getVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getVolume() method with operation not authorized, it must
    // expected a UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class) // verify
    public void testGetVolumeOrderWithOperationNotAuthorized() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        // exercise
        this.application.getVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the method deleteVolume(), the Order passed per parameter must return
    // its state to Closed.
    @Test
    public void testDeleteVolumeOrder() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        // exercise
        this.application.deleteVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.aaaController, Mockito.times(1)).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: When try calling the deleteVolume() method without user authentication, it must
    // throw UnauthenticatedUserException and the Order remains in the same state.
    @Test
    public void testDeleteVolumeOrderWithoutAuthentication() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.deleteVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                            Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: Check if DeleteVolume is properly forwarding the exception thrown by
    // getFederationUserToken, in this case the Order must remains in the same state.
    @Test
    public void testDeleteVolumeOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        VolumeOrder order = createVolumeOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.any(FederationUserToken.class));

        Mockito.doThrow(new InvalidParameterException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);

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

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.deleteVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).
                    authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                            Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

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

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        this.application.createNetwork(order, FAKE_FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.aaaController, Mockito.times(1)).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class), Mockito.any(Operation.class),
                        Mockito.any(ResourceType.class));

        Assert.assertEquals(OrderState.OPEN, order.getOrderState());
    }

    // test case: When try calling the createNetwork() method without
    // authentication, it must throw UnauthenticatedUserException.
    @Test
    public void testCreateNetworkOrderWithoutAuthentication() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        try {
            // exercise
            this.application.createNetwork(order, FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                            Mockito.any(Operation.class), Mockito.any(ResourceType.class));

            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: Check if createNetwork is properly forwarding the exception thrown by
    // getFederationUserToken.
    @Test
    public void testCreateNetworkOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        Assert.assertNull(order.getOrderState());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.any(FederationUserToken.class));

        Mockito.doThrow(new InvalidParameterException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.createNetwork(order, FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);

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

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class), Mockito.any(Operation.class),
                        Mockito.any(ResourceType.class));

        try {
            // exercise
            this.application.createNetwork(order, FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).
                    authenticateAndAuthorize(Mockito.any(FederationUserToken.class), Mockito.any(Operation.class),
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

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        NetworkInstance networkInstanceExcepted = new NetworkInstance("");
        Mockito.doReturn(networkInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order.getId()));

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.any(FederationUserToken.class));

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        NetworkInstance actualInstance =
                this.application.getNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.orderController, Mockito.times(1))
                .getResourceInstance(Mockito.eq(order.getId()));

        Assert.assertSame(networkInstanceExcepted, actualInstance);
    }

    // test case: When calling the getNetwork() method without authentication, it must
    // expected a UnauthenticatedUserException.
    @Test(expected = UnauthenticatedUserException.class) // verify
    public void testGetNetworkOrderWithoutAuthentication() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        // exercise
        this.application.getNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
    }

    // test case: Check if getNetwork is properly forwarding the exception thrown by
    // getFederationUserToken.
    @Test(expected = InvalidParameterException.class) // verify
    public void testGetNetworkOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.any(FederationUserToken.class));

        Mockito.doThrow(new InvalidParameterException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        // exercise
        this.application.getNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getNetwork() method with an operation not authorized, it must
    // expected a UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class) // verify
    public void testGetNetworkOrderWithOperationNotAuthorized() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        // exercise
        this.application.getNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the method deleteNetwork(), the Order passed per parameter must
    // return its state to Closed.
    @Test
    public void testDeleteNetworkOrder() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        // exercise
        this.application.deleteNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.aaaController, Mockito.times(1)).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: When try calling the deleteNetwork() method without user authentication, it must
    // throw UnauthenticatedUserException and the Order remains in the same state.
    @Test
    public void testDeleteNetworkOrderWithoutAuthentication() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.deleteNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                            Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: Check if deleteNetwork is properly forwarding the exception thrown by
    // getFederationUserToken, in this case the Order must remains in the same state.
    @Test
    public void testDeleteNetworkOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        NetworkOrder order = createNetworkOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.any(FederationUserToken.class));

        Mockito.doThrow(new InvalidParameterException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);

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

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.deleteNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).
                    authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                            Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: When try calling the createAttachment() method without
    // authentication, it must throw UnauthenticatedUserException.
    @Test
    public void testCreateAttachmentOrderWithoutAuthentication() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        try {
            // exercise
            this.application.createAttachment(order, FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                            Mockito.any(Operation.class), Mockito.any(ResourceType.class));

            Assert.assertNull(order.getOrderState());
        }
    }

    // test case: Check if createAttachment is properly forwarding the exception thrown by
    // getFederationUserToken.
    @Test
    public void testCreateAttachmentOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.any(FederationUserToken.class));

        Mockito.doThrow(new InvalidParameterException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.createAttachment(order, FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);

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

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class), Mockito.any(Operation.class),
                        Mockito.any(ResourceType.class));

        try {
            // exercise
            this.application.createAttachment(order, FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).
                    authenticateAndAuthorize(Mockito.any(FederationUserToken.class), Mockito.any(Operation.class),
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

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.any(FederationUserToken.class));

        Mockito.doNothing().when(this.aaaController).authorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class));

        // exercise
        AttachmentInstance actualInstance =
                this.application.getAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

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

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        // exercise
        this.application.getAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
    }

    // test case: Check if getAttachment is properly forwarding the exception thrown by
    // getFederationUserToken.
    @Test(expected = InvalidParameterException.class) // verify
    public void testGetAttachmentOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        Mockito.doThrow(new InvalidParameterException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        // exercise
        this.application.getAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the getAttachment() method performing an operation without
    // authorization, it must expected a UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class) // verify
    public void testGetAttachmentOrderWithOperationNotAuthorized() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        // exercise
        this.application.getAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
    }

    // test case: When calling the method deleteAttachment(), the Order passed per parameter must
    // return its state to Closed.
    @Test
    public void testDeleteAttachmentOrder() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doNothing().when(this.aaaController).authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        // exercise
        this.application.deleteAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

        // verify
        Mockito.verify(this.aaaController, Mockito.times(1)).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: When try calling the deleteAttachment() method without user authentication, it
    // must throw UnauthenticatedUserException and the Order remains in the same state.
    @Test
    public void testDeleteAttachmentOrderWithoutAuthentication() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
                .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.deleteAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthenticatedUserException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                            Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    // test case: Check if deleteAttachment is properly forwarding the exception thrown by
    // getFederationUserToken, in this case the Order must remains in the same state.
    @Test
    public void testDeleteAttachmentOrderWhenGetFederationUserThrowsAnException() throws Exception {

        // set up
        AttachmentOrder order = createAttachmentOrder();
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaaController).authenticate(Mockito.any(FederationUserToken.class));

        Mockito.doThrow(new InvalidParameterException()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        try {
            // exercise
            this.application.deleteAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1))
                    .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);
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

        Mockito.doReturn(order.getFederationUserToken()).when(this.aaaController)
                .getFederationUser(Mockito.anyString());

        Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
                authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                        Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

        try {
            // exercise
            this.application.deleteAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
            Assert.fail();
        } catch (UnauthorizedRequestException e) {
            // verify
            Mockito.verify(this.aaaController, Mockito.times(1)).
                    authenticateAndAuthorize(Mockito.any(FederationUserToken.class),
                            Mockito.any(Operation.class), Mockito.any(ResourceType.class), Mockito.any(Order.class));

            Assert.assertEquals(OrderState.OPEN, order.getOrderState());
        }
    }

    private NetworkOrder createNetworkOrder() throws Exception {
        FederationUserToken federationUserToken = new FederationUserToken(FAKE_TOKEN_PROVIDER, FAKE_FEDERATION_TOKEN_VALUE,
                FAKE_USER_ID, FAKE_USER_NAME);
        NetworkOrder order = new NetworkOrder(federationUserToken, FAKE_MEMBER_ID, FAKE_MEMBER_ID,
                FAKE_NAME, FAKE_GATEWAY, FAKE_ADDRESS, NetworkAllocationMode.STATIC);

        NetworkInstance networtkInstanceExcepted = new NetworkInstance(order.getId());
        Mockito.doReturn(networtkInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order.getId()));
        order.setInstanceId(networtkInstanceExcepted.getId());

        return order;
    }

    private VolumeOrder createVolumeOrder() throws Exception {
        FederationUserToken federationUserToken = new FederationUserToken(FAKE_TOKEN_PROVIDER, FAKE_FEDERATION_TOKEN_VALUE,
                FAKE_USER_ID, FAKE_USER_NAME);
        VolumeOrder order = new VolumeOrder(federationUserToken, FAKE_MEMBER_ID, FAKE_MEMBER_ID, 1,
                FAKE_VOLUME_NAME);

        VolumeInstance volumeInstanceExcepted = new VolumeInstance(order.getId());
        Mockito.doReturn(volumeInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order.getId()));
        order.setInstanceId(volumeInstanceExcepted.getId());

        return order;
    }

    private ComputeOrder createComputeOrder() throws Exception {
        FederationUserToken federationUserToken = new FederationUserToken(FAKE_TOKEN_PROVIDER, FAKE_FEDERATION_TOKEN_VALUE,
                FAKE_USER_ID, FAKE_USER_NAME);
        ComputeOrder order = new ComputeOrder(federationUserToken, FAKE_MEMBER_ID, FAKE_MEMBER_ID, FAKE_INSTANCE_NAME, 2, 2,
                30, FAKE_IMAGE_NAME, new UserData(), FAKE_PUBLIC_KEY, null);

        ComputeInstance computeInstanceExcepted = new ComputeInstance(order.getId());

        Mockito.doReturn(computeInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order.getId()));

        order.setInstanceId(computeInstanceExcepted.getId());

        return order;
    }

    private AttachmentOrder createAttachmentOrder() throws Exception {
        FederationUserToken federationUserToken = new FederationUserToken(FAKE_TOKEN_PROVIDER, FAKE_FEDERATION_TOKEN_VALUE,
                FAKE_USER_ID, FAKE_USER_NAME);

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

        AttachmentOrder order = new AttachmentOrder(federationUserToken, FAKE_MEMBER_ID, FAKE_MEMBER_ID,
                sourceId, targetId, FAKE_DEVICE_MOUNT_POINT);

        AttachmentInstance attachmentInstanceExcepted = new AttachmentInstance(order.getId());

        Mockito.doReturn(attachmentInstanceExcepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order.getId()));
        order.setInstanceId(attachmentInstanceExcepted.getId());

        return order;
    }
}
