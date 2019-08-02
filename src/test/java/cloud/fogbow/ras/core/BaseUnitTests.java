package cloud.fogbow.ras.core;

import java.util.ArrayList;
import java.util.Map;

import org.junit.After;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;

/*
 * This class is intended to reuse code components to assist other unit test classes 
 * but does not contemplate performing any tests. The @Ignore annotation is being used 
 * in this context to prevent it from being initialized as a test class.
 */
@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest({ CloudConnectorFactory.class, DatabaseManager.class })
public class BaseUnitTests {

    protected static final int CPU_VALUE = 8;
    protected static final int DISK_VALUE = 30;
    protected static final int MEMORY_VALUE = 1024;

    protected static final long DEFAULT_SLEEP_TIME = 500;
    
    protected static final String DEFAULT_CLOUD_NAME = "default";
    protected static final String FAKE_COMPUTE_ID = "fake-compute-id";
    protected static final String FAKE_DEVICE = "fake-device";
    protected static final String FAKE_IMAGE_ID = "fake-image-id";
    protected static final String FAKE_INSTANCE_ID = "fake-instance-id";
    protected static final String FAKE_INSTANCE_NAME = "fake-instance-name";
    protected static final String FAKE_ORDER_NAME = "fake-order-name";
    protected static final String FAKE_PUBLIC_KEY= "fake-public-key";
    protected static final String FAKE_REMOTE_MEMBER_ID = "fake-intercomponent-member";
    protected static final String FAKE_SECURITY_RULE_ID = "fake-security-rule-id";
    protected static final String FAKE_USER_ID = "fake-user-id";
    protected static final String FAKE_USER_NAME = "fake-user-name";
    protected static final String FAKE_VOLUME_ID = "fake-volume-id";
    protected static final String LOCAL_MEMBER_ID =
            PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);
    
    protected LocalCloudConnector localCloudConnector;
    
    protected AttachmentInstance attachmentInstance;
    protected ComputeInstance computeInstance;
    protected ImageInstance imageInstance;
    protected NetworkInstance networkInstance;
    protected SecurityRuleInstance securityRuleInstance;
    protected VolumeInstance volumeInstance;

    /*
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
        Order<?> order = null;
        do {
            order = list.getNext();
            if (order != null) {
                list.removeItem(order);
            }
        } while (order != null);
        list.resetPointer();
    }

    protected String getLocalMemberId() {
        return LOCAL_MEMBER_ID;
    }

    protected Order<?> createLocalOrder(String requestingMember) {
        String providingMember = requestingMember;
        return createComputeOrder(requestingMember, providingMember);
    }

    protected Order<?> createRemoteOrder(String requestingMember) {
        String providingMember = FAKE_REMOTE_MEMBER_ID;
        return createComputeOrder(requestingMember, providingMember);
    }

    protected ComputeOrder createLocalComputeOrder() {
        return createComputeOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);
    }
    
    protected ComputeOrder createComputeOrder(String requestingMember, String providingMember) {
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
    
    protected VolumeOrder createLocalVolumeOrder() {
        return createVolumeOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);
    }

    protected VolumeOrder createVolumeOrder(String requestingMember, String providingMember) {
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

    protected AttachmentOrder createLocalAttachmentOrder(ComputeOrder computeOrder, VolumeOrder volumeOrder) {
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

    protected PublicIpOrder createLocalPublicIpOrder(String computeOrderId) {
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

    protected ArrayList<UserData> mockUserData() {
        ArrayList<UserData> userDataScripts = new ArrayList<>();
        UserData userDataScript = Mockito.mock(UserData.class);
        userDataScripts.add(userDataScript);
        return userDataScripts;
    }
    
    protected SystemUser createSystemUser() {
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
    public void mockLocalCloudConnectorFromFactory() {
        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        this.localCloudConnector = Mockito.mock(LocalCloudConnector.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(this.localCloudConnector);
    }
    
    /*
     * Simulates the instance of an attachment by setting the instance ID return of
     * the resources involved.
     */
    public void mockAttachmentInstance() {
        this.attachmentInstance = Mockito.mock(AttachmentInstance.class);
        Mockito.when(this.attachmentInstance.getId()).thenReturn(FAKE_INSTANCE_ID);
        Mockito.when(this.attachmentInstance.getComputeId()).thenReturn(FAKE_COMPUTE_ID);
        Mockito.when(this.attachmentInstance.getVolumeId()).thenReturn(FAKE_VOLUME_ID);
    }
    
    /*
     * Simulates the instance of a compute by setting the instance ID return of this
     * resource.
     */
    public void mockComputeInstance() {
        this.computeInstance = Mockito.mock(ComputeInstance.class);
        Mockito.when(this.computeInstance.getId()).thenReturn(FAKE_INSTANCE_ID);
    }
    
    /*
     * Simulates the instance of an image by setting the instance ID return of this
     * resource.
     */
    public void mockImageInstance() {
        this.imageInstance = Mockito.mock(ImageInstance.class);
        Mockito.when(this.imageInstance.getId()).thenReturn(FAKE_IMAGE_ID);
    }
    
    /*
     * Simulates the instance of a network by setting the instance ID return of this resource.
     */
    public void mockNetworkInstance() {
        this.networkInstance = Mockito.mock(NetworkInstance.class);
        Mockito.when(this.networkInstance.getId()).thenReturn(FAKE_INSTANCE_ID);
    }
    
    /*
     * Simulates the instance of a security rule by setting the instance ID return
     * of this resource.
     */
    public void mockSecurityRuleInstance() {
        this.securityRuleInstance = Mockito.mock(SecurityRuleInstance.class);
        Mockito.when(this.securityRuleInstance.getId()).thenReturn(FAKE_SECURITY_RULE_ID);
    }
    
    /*
     * Simulates the instance of a volume by setting the instance ID return of this
     * resource.
     */
    public void mockVolumeInstance() {
        this.volumeInstance = Mockito.mock(VolumeInstance.class);
        Mockito.when(this.volumeInstance.getId()).thenReturn(FAKE_INSTANCE_ID);
    }
    
}
