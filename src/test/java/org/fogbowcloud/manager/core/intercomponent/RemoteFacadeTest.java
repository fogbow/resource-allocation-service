package org.fogbowcloud.manager.core.intercomponent;

import org.fogbowcloud.manager.core.*;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.cloudconnector.RemoteCloudConnector;
import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.datastore.DatabaseManager;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.intercomponent.xmpp.PacketSenderHolder;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.quotas.Quota;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatabaseManager.class, CloudConnectorFactory.class, PacketSenderHolder.class})
public class RemoteFacadeTest extends BaseUnitTests {

    private static final String TEST_PATH = "src/test/resources/private";
    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String REMOTE_MEMBER_ID = "fake-intercomponent-member";

    private AaController aaController;
    private OrderController orderController;
    private RemoteFacade remoteFacade;
    private CloudConnector cloudConnector;

    @Before
    public void setUp() throws UnexpectedException {
        HomeDir.getInstance().setPath(TEST_PATH);
        super.mockReadOrdersFromDataBase();

        this.orderController = Mockito.spy(new OrderController());
        this.aaController = Mockito.mock(AaController.class);

        this.remoteFacade = RemoteFacade.getInstance();
        this.remoteFacade.setOrderController(this.orderController);
        this.remoteFacade.setAaController(this.aaController);

        this.cloudConnector = Mockito.spy(new RemoteCloudConnector(REMOTE_MEMBER_ID));
    }

    // test case: When calling the activateOrder method a new Order without state passed by
    // parameter, it must return to Open OrderState after its activation.
    @Test
    public void testRemoteActivateOrder() throws FogbowManagerException, UnexpectedException {
        // set up
        FederationUserToken federationUser = createFederationUser();
        Order order = createOrder(federationUser);
        Assert.assertNull(order.getOrderState());

        Mockito.doNothing().when(this.aaController).authorize(Mockito.eq(federationUser),
                Mockito.eq(Operation.CREATE), Mockito.eq(ResourceType.COMPUTE));

        // exercise
        this.remoteFacade.activateOrder(order);

        // verify
        Assert.assertEquals(OrderState.OPEN, order.getOrderState());
    }

    // test case: When calling the getResourceInstance method, it must return an Instance of the
    // OrderID passed per parameter.
    @Test
    @Ignore
    public void testRemoteGetResourceInstance() throws Exception {
        // set up
        FederationUserToken federationUser = createFederationUser();
        Order order = createOrder(federationUser);

        Mockito.doNothing().when(this.aaController).authorize(Mockito.eq(federationUser),
                Mockito.eq(Operation.CREATE), Mockito.eq(ResourceType.COMPUTE));

        Instance excepted = new ComputeInstance(FAKE_INSTANCE_ID);

        Mockito.doReturn(excepted).when(this.orderController)
                .getResourceInstance(Mockito.eq(order.getId()));

        // exercise
        Instance instance = this.remoteFacade.getResourceInstance(order.getId(), federationUser,
                ResourceType.COMPUTE);

        // verify
        Mockito.verify(this.orderController, Mockito.times(1))
                .getResourceInstance(Mockito.eq(order.getId()));

        Assert.assertSame(excepted, instance);
    }

    // test case: When calling the deleteOrder method with an Order passed per parameter, it must
    // return its OrderState to Closed.
    @Test
    @Ignore
    public void testRemoteDeleteOrder() throws UnexpectedException, FogbowManagerException {
        // set up
        FederationUserToken federationUser = createFederationUser();
        Order order = createOrder(federationUser);
        OrderStateTransitioner.activateOrder(order);

        Mockito.doNothing().when(this.aaController).authorize(Mockito.eq(federationUser),
                Mockito.eq(Operation.CREATE), Mockito.eq(ResourceType.COMPUTE));

        // exercise
        this.remoteFacade.deleteOrder(order.getId(), federationUser, ResourceType.COMPUTE);

        // verify
        Mockito.verify(this.aaController, Mockito.times(1)).authorize(
                Mockito.any(FederationUserToken.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
    }

    // test case: When calling the getUserQuota method with valid parameters, it must return the
    // User Quota from that.
    @Test
    @Ignore
    public void testRemoteGetUserQuota() throws Exception {
        // set up
        FederationUserToken federationUser = createFederationUser();

        Mockito.doNothing().when(this.aaController).authorize(Mockito.eq(federationUser),
                Mockito.eq(Operation.CREATE), Mockito.eq(ResourceType.COMPUTE));

        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        Mockito.when(cloudConnectorFactory.getCloudConnector(REMOTE_MEMBER_ID))
                .thenReturn(this.cloudConnector);

        ComputeAllocation totalQuota = new ComputeAllocation(8, 2048, 2);
        ComputeAllocation usedQuota = new ComputeAllocation(4, 1024, 1);

        Quota quota = new ComputeQuota(totalQuota, usedQuota);

        Mockito.doReturn(quota).when(this.cloudConnector).getUserQuota(Mockito.eq(federationUser),
                Mockito.eq(ResourceType.COMPUTE));

        // exercise
        Quota expected = this.remoteFacade.getUserQuota(REMOTE_MEMBER_ID, federationUser,
                ResourceType.COMPUTE);

        // verify
        Mockito.verify(this.aaController, Mockito.times(1)).authorize(
                Mockito.any(FederationUserToken.class), Mockito.any(Operation.class),
                Mockito.any(ResourceType.class));

        Mockito.verify(this.cloudConnector, Mockito.times(1))
                .getUserQuota(Mockito.eq(federationUser), Mockito.eq(ResourceType.COMPUTE));

        Assert.assertNotNull(expected);
        Assert.assertEquals(expected.getTotalQuota(), quota.getTotalQuota());
        Assert.assertEquals(expected.getUsedQuota(), quota.getUsedQuota());
    }

    @Test
    public void testRemoteGetImage() {
        // TODO implement test
    }

    @Test
    public void testRemoteGetAllImages() {
        // TODO implement test
    }

    @Test
    public void testRemoteHandleRemoteEvent() {
        // TODO implement test
    }

    private Order createOrder(FederationUserToken token) {
        return new ComputeOrder(token, "fake-requesting-member", "fake-providing-member",
                -1, -1, -1, "fake-image-id", null, "fake-public-key", new ArrayList<String>());
    }

    private FederationUserToken createFederationUser() {
        return new FederationUserToken("fake-token-provider", "fake-token", "fake-id", "fogbow");
    }

}
