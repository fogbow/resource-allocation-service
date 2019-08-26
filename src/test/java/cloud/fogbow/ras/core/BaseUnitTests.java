package cloud.fogbow.ras.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
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
@PrepareForTest({SharedOrderHolders.class})
public class BaseUnitTests {
    
    protected static final Logger LOGGER = Logger.getLogger(BaseUnitTests.class);
    
    protected TestUtils testUtils;
    protected SharedOrderHolders sharedOrderHolders;

    @Before
    public void setup() {
        this.testUtils = new TestUtils();
    }
    
    /*
     * Clears the orders from the lists on the SharedOrderHolders instance.
     */
    @After
    public void tearDown() {
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        for (OrderState state : OrderState.values()) {
            if (!state.equals(OrderState.DEACTIVATED)) {
                SynchronizedDoublyLinkedList<Order> ordersList = sharedOrderHolders.getOrdersList(state);
                this.testUtils.cleanList(ordersList);
            }
        }

        Map<String, Order> activeOrderMap = sharedOrderHolders.getActiveOrdersMap();
        activeOrderMap.clear();
    }

    protected void mockSharedOrdersHolder() {
        this.sharedOrderHolders = Mockito.mock(SharedOrderHolders.class);
        PowerMockito.mockStatic(SharedOrderHolders.class);
        BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(this.sharedOrderHolders);
        Mockito.when(this.sharedOrderHolders.getOrdersList(Mockito.any(OrderState.class)))
                .thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(this.sharedOrderHolders.getActiveOrdersMap()).thenReturn(new HashMap<>());
    }
}
