package cloud.fogbow.ras.core;


import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;

/*
 * This class is intended to reuse code components to assist other unit test classes 
 * but does not contemplate performing any tests. The @Ignore annotation is being used 
 * in this context to prevent it from being initialized as a test class.
 */
@Ignore
@RunWith(PowerMockRunner.class)
public class BaseUnitTests {
    
    protected static final Logger LOGGER = Logger.getLogger(BaseUnitTests.class);
    
    protected TestUtils testUtils;
    protected SharedOrderHolders sharedOrderHolders;
    
    private final List<OrderState> orderStatesToIgnore =
            Arrays.asList(
                    OrderState.CLOSED,
                    OrderState.PAUSED,
                    OrderState.PAUSING,
                    OrderState.RESUMING,
                    OrderState.HIBERNATING,
                    OrderState.HIBERNATED,
                    OrderState.STOPPING,
                    OrderState.STOPPED
            );

    @Before
    public void setup() throws FogbowException {
        this.testUtils = new TestUtils();
    }
    
    /*
     * Clears the orders from the lists on the SharedOrderHolders instance.
     */
    @After
    public void tearDown() throws InternalServerErrorException {
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        
        for (OrderState state : OrderState.values()) {
            if (!orderStatesToIgnore.contains(state)) {
                SynchronizedDoublyLinkedList<Order> ordersList = sharedOrderHolders.getOrdersList(state);
                this.testUtils.cleanList(ordersList);
            }
        }

        Map<String, Order> activeOrderMap = sharedOrderHolders.getActiveOrdersMap();
        activeOrderMap.clear();
    }
}
