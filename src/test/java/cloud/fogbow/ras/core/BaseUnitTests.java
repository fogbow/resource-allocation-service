package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.*;
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

    public static final String LOCAL_MEMBER_ID =
            PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);

    public static final String FAKE_CLOUD_NAME = "fake-cloud";
    public static final String FAKE_DEVICE = "fake-device";
    public static final String FAKE_ID = "fake-id";
    public static final String FAKE_IMAGE_NAME = "fake-image-name";
    public static final String FAKE_INSTANCE_NAME = "fake-instance-name";
    public static final String FAKE_NAME = "fake-name";
    public static final String FAKE_PUBLIC_KEY= "fake-public-key";
    public static final String FAKE_USER = "fake-user";
    public static final int FAKE_VOLUME_SIZE = 42;
    public static final String REMOTE_MEMBER_ID = "remote-member-id";
    public static final String RESOURCES_PATH_TEST = "src/test/resources/private";


    /**
     * Clears the orders from the lists on the SharedOrderHolders instance.
     */
    @After
    public void tearDown() {
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        for (OrderState state : OrderState.values()) {
            if (!state.equals(OrderState.DEACTIVATED)) {
                SynchronizedDoublyLinkedList<Order> ordersList = sharedOrderHolders.getOrdersList(state);
                cleanList(ordersList);
            }
        }

        Map<String, Order> activeOrderMap = sharedOrderHolders.getActiveOrdersMap();
        activeOrderMap.clear();
    }

    protected void cleanList(ChainedList<Order> list) {
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

    protected boolean isEmpty(ChainedList<Order> list) {
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
        String providingMember = REMOTE_MEMBER_ID;
        return createOrder(requestingMember, providingMember);
    }

    protected Order createOrder(String requestingMember, String providingMember) {
        return createLocalComputeOrder(requestingMember, providingMember);
    }

    protected ComputeOrder createLocalComputeOrder(String requestingMember, String providingMember) {
        SystemUser systemUser = Mockito.mock(SystemUser.class);
        String imageName = "fake-image-name";
        String publicKey = "fake-public-key";
        String instanceName = "fake-instance-name";

        ComputeOrder localOrder =
                new ComputeOrder(
                        systemUser,
                        requestingMember,
                        providingMember,
                        "default", instanceName,
                        8,
                        1024,
                        30,
                        imageName,
                        null,
                        publicKey,
                        null);

        return localOrder;
    }

    protected VolumeOrder createLocalVolumeOrder(String requestingMember, String providingMember) {
        SystemUser systemUser = Mockito.mock(SystemUser.class);

        VolumeOrder volumeOrder =
                new VolumeOrder(
                        systemUser,
                        providingMember,
                        requestingMember,
                        FAKE_CLOUD_NAME,
                        FAKE_NAME,
                        FAKE_VOLUME_SIZE);

        return volumeOrder;
    }

    protected AttachmentOrder createLocalAttachmentOrder(ComputeOrder computeOrder, VolumeOrder volumeOrder) {
        SystemUser systemUser = Mockito.mock(SystemUser.class);
        String computeId = computeOrder.getId();
        String volumeId = volumeOrder.getId();

        String device = FAKE_DEVICE;

        AttachmentOrder attachmentOrder =
                new AttachmentOrder(
                        systemUser,
                        LOCAL_MEMBER_ID,
                        LOCAL_MEMBER_ID,
                        FAKE_CLOUD_NAME,
                        computeId,
                        volumeId,
                        device);

        return attachmentOrder;
    }

    protected PublicIpOrder createLocalPublicIpOrder(String computeOrderId) {
        SystemUser systemUser = Mockito.mock(SystemUser.class);
        String cloudName = "fake-cloudio";
        PublicIpOrder publicIpOrder =
                new PublicIpOrder(
                        systemUser,
                        LOCAL_MEMBER_ID,
                        LOCAL_MEMBER_ID,
                        cloudName,
                        computeOrderId);

        return publicIpOrder;
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
        Mockito.when(databaseManager.readActiveOrders(OrderState.OPEN)).thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(databaseManager.readActiveOrders(OrderState.FULFILLED)).thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(databaseManager.readActiveOrders(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST)).thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(databaseManager.readActiveOrders(OrderState.CLOSED)).thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(databaseManager.readActiveOrders(OrderState.PENDING)).thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(databaseManager.readActiveOrders(OrderState.SPAWNING)).thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(databaseManager.readActiveOrders(OrderState.FAILED_ON_REQUEST)).thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(databaseManager.readActiveOrders(OrderState.UNABLE_TO_CHECK_STATUS)).thenReturn(new SynchronizedDoublyLinkedList<>());

        Mockito.doNothing().when(databaseManager).add(Matchers.any(Order.class));
        Mockito.doNothing().when(databaseManager).update(Matchers.any(Order.class));

        PowerMockito.mockStatic(DatabaseManager.class);
        BDDMockito.given(DatabaseManager.getInstance()).willReturn(databaseManager);
    }
}
