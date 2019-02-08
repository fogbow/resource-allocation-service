package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.OrderStateTransitioner;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.cloudconnector.RemoteCloudConnector;
import cloud.fogbow.ras.core.models.linkedlists.ChainedList;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertNull;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CloudConnectorFactory.class)
public class ClosedProcessorTest extends BaseUnitTests {

    private ClosedProcessor closedProcessor;

    @SuppressWarnings("unused")
    private RemoteCloudConnector remoteCloudConnector;
    private LocalCloudConnector localCloudConnector;

    @SuppressWarnings("unused")
    private Properties properties;
    private Thread thread;

    @Before
    public void setUp() throws UnexpectedException {
        mockReadOrdersFromDataBase();
        this.properties = new Properties();

        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        properties = propertiesHolder.getProperties();

        this.localCloudConnector = Mockito.mock(LocalCloudConnector.class);
        this.remoteCloudConnector = Mockito.mock(RemoteCloudConnector.class);
        this.closedProcessor = Mockito.spy(new ClosedProcessor(
                ConfigurationPropertyDefaults.CLOSED_ORDERS_SLEEP_TIME));
    }

    @Override
    public void tearDown() {
        if (this.thread != null) {
            this.thread.interrupt();
            this.thread = null;
        }
        super.tearDown();
    }

    //test case: the closed processor remove closed orders from the closed orders list
    @Test
    public void testProcessClosedLocalOrder() throws Exception {

        //set up
        String instanceId = "fake-id";

        Order localOrder = createLocalOrder(getLocalMemberId());
        localOrder.setInstanceId(instanceId);

        OrderStateTransitioner.activateOrder(localOrder);
        OrderStateTransitioner.transition(localOrder, OrderState.CLOSED);

        //exercise
        this.thread = new Thread(this.closedProcessor);
        this.thread.start();
        Thread.sleep(500);

        //verify
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        ChainedList closedOrders = sharedOrderHolders.getClosedOrdersList();
        Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        assertNull(activeOrdersMap.get(localOrder.getId()));

        closedOrders.resetPointer();
        assertNull(closedOrders.getNext());

        //TODO: it would be good to verity that the OrderStateTransitioner.deactivateOrder was called. However,
        //it is static thus, hard to mock
    }
}
