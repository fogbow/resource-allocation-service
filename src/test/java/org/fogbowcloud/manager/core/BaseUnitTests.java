package org.fogbowcloud.manager.core;

import java.util.Map;

import org.fogbowcloud.manager.core.datastore.DatabaseManager;
import org.fogbowcloud.manager.core.models.linkedlists.ChainedList;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.junit.After;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

//@RunWith(PowerMockRunner.class)
@PrepareForTest(DatabaseManager.class)
public class BaseUnitTests {

    public static final String LOCAL_MEMBER_ID = "fake-localidentity-member";

    @After
    public void tearDown() {
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        for (OrderState state : OrderState.values()) {
            SynchronizedDoublyLinkedList ordersList = sharedOrderHolders.getOrdersList(state);
            cleanList(ordersList);
        }
        Map<String, Order> activeOrderMap = sharedOrderHolders.getActiveOrdersMap();
        activeOrderMap.clear();
    }

    protected void cleanList(ChainedList list) {
        list.resetPointer();
        Order order = null;
        do {
            order = list.getNext();
            if (order != null) {
                list.removeItem(order);
            }
        } while (order != null);
        list.resetPointer();
    }

    protected boolean isEmpty(ChainedList list) {
        list.resetPointer();
        return list.getNext() == null;
    }

    protected String getLocalMemberId() {
        return LOCAL_MEMBER_ID;
    }

    protected Order createLocalOrder(String requestingMember) {
        String providingMember = requestingMember;
        return createOrder(requestingMember, providingMember);
    }

    protected Order createRemoteOrder(String requestingMember) {
        String providingMember = "fake-intercomponent-member";
        return createOrder(requestingMember, providingMember);
    }

    private Order createOrder(String requestingMember, String providingMember) {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        UserData userData = Mockito.mock(UserData.class);
        String imageName = "fake-image-name";
        String publicKey = "fake-public-key";

        Order localOrder =
                new ComputeOrder(
                        federationUser,
                        requestingMember,
                        providingMember,
                        8,
                        1024,
                        30,
                        imageName,
                        userData,
                        publicKey,
                        null);
        return localOrder;
    }

    public void mockDB() {
        DatabaseManager databaseManager = Mockito.mock(DatabaseManager.class);
        when(databaseManager.readActiveOrders(OrderState.OPEN)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.SPAWNING)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.FAILED)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.FULFILLED)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.PENDING)).thenReturn(new SynchronizedDoublyLinkedList());
        when(databaseManager.readActiveOrders(OrderState.CLOSED)).thenReturn(new SynchronizedDoublyLinkedList());

        doNothing().when(databaseManager).add(any(Order.class));
        doNothing().when(databaseManager).update(any(Order.class));

        PowerMockito.mockStatic(DatabaseManager.class);
        given(DatabaseManager.getInstance()).willReturn(databaseManager);
    }
}
