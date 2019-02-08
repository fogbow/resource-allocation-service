package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.linkedlists.ChainedList;
import cloud.fogbow.ras.core.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.junit.After;
import org.mockito.BDDMockito;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.ArrayList;
import java.util.Map;

@PrepareForTest(DatabaseManager.class)
public class BaseUnitTests {

    public static final String LOCAL_MEMBER_ID = "fake-localidentity-member";

    /**
     * Clears the orders from the lists on the SharedOrderHolders instance.
     */
    @After
    public void tearDown() {
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        for (OrderState state : OrderState.values()) {
            if (!state.equals(OrderState.DEACTIVATED)) {
                SynchronizedDoublyLinkedList ordersList = sharedOrderHolders.getOrdersList(state);
                cleanList(ordersList);
            }
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

    protected Order createOrder(String requestingMember, String providingMember) {
        FederationUser federationUser = Mockito.mock(FederationUser.class);

        ArrayList<UserData> userDataScripts = mockUserData();

        String imageName = "fake-image-name";
        String publicKey = "fake-public-key";
        String instanceName = "fake-instance-name";

        Order localOrder =
                new ComputeOrder(
                        federationUser,
                        requestingMember,
                        providingMember,
                        "default", instanceName,
                        8,
                        1024,
                        30,
                        imageName,
                        userDataScripts,
                        publicKey,
                        null);
        return localOrder;
    }

    protected ArrayList<UserData> mockUserData() {
        ArrayList<UserData> userDataScripts = new ArrayList<>();
        UserData userDataScript = Mockito.mock(UserData.class);
        userDataScripts.add(userDataScript);
        return userDataScripts;
    }

    /**
     * Mocks the behavior of the database as if there was no order in any state.
     */
    public void mockReadOrdersFromDataBase() throws UnexpectedException {
        DatabaseManager databaseManager = Mockito.mock(DatabaseManager.class);
        Mockito.when(databaseManager.readActiveOrders(OrderState.OPEN)).thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.SPAWNING)).thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.FAILED_AFTER_SUCCESSUL_REQUEST)).thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.FAILED_ON_REQUEST)).thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.FULFILLED)).thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.PENDING)).thenReturn(new SynchronizedDoublyLinkedList());
        Mockito.when(databaseManager.readActiveOrders(OrderState.CLOSED)).thenReturn(new SynchronizedDoublyLinkedList());

        Mockito.doNothing().when(databaseManager).add(Matchers.any(Order.class));
        Mockito.doNothing().when(databaseManager).update(Matchers.any(Order.class));

        PowerMockito.mockStatic(DatabaseManager.class);
        BDDMockito.given(DatabaseManager.getInstance()).willReturn(databaseManager);
    }
}
