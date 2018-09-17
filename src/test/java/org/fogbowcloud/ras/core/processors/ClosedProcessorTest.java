package org.fogbowcloud.ras.core.processors;

import org.fogbowcloud.ras.core.BaseUnitTests;
import org.fogbowcloud.ras.core.OrderStateTransitioner;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.SharedOrderHolders;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.ras.core.cloudconnector.LocalCloudConnector;
import org.fogbowcloud.ras.core.cloudconnector.RemoteCloudConnector;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.linkedlists.ChainedList;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.OrderState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
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
                DefaultConfigurationConstants.CLOSED_ORDERS_SLEEP_TIME));
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
