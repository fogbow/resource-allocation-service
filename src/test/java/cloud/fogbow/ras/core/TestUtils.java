package cloud.fogbow.ras.core;

import java.util.ArrayList;

import org.mockito.BDDMockito;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;

public class TestUtils {
    
    public static final int CPU_VALUE = 8;
    public static final int DISK_VALUE = 30;
    public static final int MEMORY_VALUE = 1024;
    public static final int RUN_ONCE = 1;

    public static final long DEFAULT_SLEEP_TIME = 500;
    
    public static final String DEFAULT_CLOUD_NAME = "default";
    public static final String FAKE_ADDRESS = "fake-address";
    public static final String FAKE_COMPUTE_ID = "fake-compute-id";
    public static final String FAKE_DEVICE = "fake-device";
    public static final String FAKE_GATEWAY = "fake-gateway";
    public static final String FAKE_IMAGE_ID = "fake-image-id";
    public static final String FAKE_INSTANCE_ID = "fake-instance-id";
    public static final String FAKE_INSTANCE_NAME = "fake-instance-name";
    public static final String FAKE_ORDER_ID = "fake-order-id";
    public static final String FAKE_ORDER_NAME = "fake-order-name";
    public static final String FAKE_PUBLIC_KEY= "fake-public-key";
    public static final String FAKE_REMOTE_MEMBER_ID = "fake-intercomponent-member";
    public static final String FAKE_SECURITY_RULE_ID = "fake-security-rule-id";
    public static final String FAKE_USER_ID = "fake-user-id";
    public static final String FAKE_USER_NAME = "fake-user-name";
    public static final String FAKE_VOLUME_ID = "fake-volume-id";
    public static final String LOCAL_MEMBER_ID =
            PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);
    
    public void cleanList(ChainedList<Order> list) {
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

    public String getLocalMemberId() {
        return LOCAL_MEMBER_ID;
    }

    public Order createLocalOrder(String requestingMember) {
        String providingMember = requestingMember;
        return createComputeOrder(requestingMember, providingMember);
    }

    public Order createRemoteOrder(String requestingMember) {
        String providingMember = FAKE_REMOTE_MEMBER_ID;
        return createComputeOrder(requestingMember, providingMember);
    }

    public ComputeOrder createLocalComputeOrder() {
        return createComputeOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);
    }
    
    public ComputeOrder createComputeOrder(String requestingMember, String providingMember) {
        ComputeOrder computeOrder =
                new ComputeOrder(
                        createSystemUser(),
                        requestingMember,
                        providingMember,
                        DEFAULT_CLOUD_NAME, 
                        FAKE_INSTANCE_NAME,
                        CPU_VALUE,
                        MEMORY_VALUE,
                        DISK_VALUE,
                        FAKE_IMAGE_ID,
                        mockUserData(),
                        FAKE_PUBLIC_KEY,
                        null);

        return computeOrder;
    }
    
    public NetworkOrder createLocalNetworkOrder() {
        return createNetworkOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);
    }

    public NetworkOrder createNetworkOrder(String requestingMember, String providingMember) {
        NetworkOrder networkOrder = 
                new NetworkOrder(
                        createSystemUser(), 
                        requestingMember, 
                        requestingMember,
                        DEFAULT_CLOUD_NAME, 
                        FAKE_INSTANCE_NAME, 
                        FAKE_GATEWAY, 
                        FAKE_ADDRESS, 
                        NetworkAllocationMode.STATIC);

        return networkOrder;
    }
    
    public VolumeOrder createLocalVolumeOrder() {
        return createVolumeOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);
    }

    public VolumeOrder createVolumeOrder(String requestingMember, String providingMember) {
        SystemUser systemUser = createSystemUser();

        VolumeOrder volumeOrder =
                new VolumeOrder(
                        systemUser,
                        providingMember,
                        requestingMember,
                        DEFAULT_CLOUD_NAME,
                        FAKE_ORDER_NAME,
                        DISK_VALUE);

        return volumeOrder;
    }

    public AttachmentOrder createLocalAttachmentOrder(ComputeOrder computeOrder, VolumeOrder volumeOrder) {
        SystemUser systemUser = Mockito.mock(SystemUser.class);
        String computeId = computeOrder.getId();
        String volumeId = volumeOrder.getId();

        AttachmentOrder attachmentOrder =
                new AttachmentOrder(
                        systemUser,
                        LOCAL_MEMBER_ID,
                        LOCAL_MEMBER_ID,
                        DEFAULT_CLOUD_NAME,
                        computeId,
                        volumeId,
                        FAKE_DEVICE);

        return attachmentOrder;
    }

    public PublicIpOrder createLocalPublicIpOrder(String computeOrderId) {
        SystemUser systemUser = createSystemUser();
        PublicIpOrder publicIpOrder =
                new PublicIpOrder(
                        systemUser,
                        LOCAL_MEMBER_ID,
                        LOCAL_MEMBER_ID,
                        DEFAULT_CLOUD_NAME,
                        computeOrderId);

        return publicIpOrder;
    }

    public ArrayList<UserData> mockUserData() {
        ArrayList<UserData> userDataScripts = new ArrayList<>();
        UserData userDataScript = Mockito.mock(UserData.class);
        userDataScripts.add(userDataScript);
        return userDataScripts;
    }
    
    public SystemUser createSystemUser() {
        SystemUser systemUser = new SystemUser(FAKE_USER_ID, FAKE_USER_NAME, LOCAL_MEMBER_ID);
        return systemUser;
    }

    /*
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
    
    /*
     * Simulates instance of a LocalCloudConnector since its creation via CloudConnectorFactory.
     */
    public LocalCloudConnector mockLocalCloudConnectorFromFactory() {
        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        LocalCloudConnector localCloudConnector = Mockito.mock(LocalCloudConnector.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(localCloudConnector);
        
        return localCloudConnector;
    }

}
