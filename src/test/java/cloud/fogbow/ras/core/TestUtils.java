package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.common.util.CloudInitUserDataBuilder;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.cloudconnector.RemoteCloudConnector;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.datastore.services.RecoveryService;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.*;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import com.google.gson.Gson;
import org.apache.http.client.HttpResponseException;
import org.mockito.BDDMockito;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestUtils {

    public static final int CPU_VALUE = 8;
    public static final int DISK_VALUE = 30;
    public static final int ERROR_STATUS_CODE = 500;
    public static final int MEMORY_VALUE = 1024;
    public static final int RUN_ONCE = 1;
    public static final int NEVER_RUN = 0;
    public static final int RUN_TWICE = 2;
    public static final int RUN_THRICE = 3;
    public static final int RUN_FOUR_TIMES = 4;
    public static final int RUN_FIVE_TIMES = 5;
    public static final int RUN_SIX_TIMES = 6;

    public static final long DEFAULT_SLEEP_TIME = 500;
    
    public static final String ANY_VALUE = "anything";
    public static final String CREATE_SECURITY_GROUP_METHOD = "createSecurityGroup";
    public static final String CREATE_TAGS_REQUEST_METHOD = "createTagsRequest";
    public static final String DEFAULT_CLOUD_NAME = "default";
    public static final String DO_AUTHORIZE_SECURITY_GROUP_INGRESS_METHOD = "doAuthorizeSecurityGroupIngress";
    public static final String DO_DELETE_SECURITY_GROUP_METHOD = "doDeleteSecurityGroup";
    public static final String DO_DESCRIBE_IMAGES_REQUEST_METHOD = "doDescribeImagesRequest";
    public static final String DO_DESCRIBE_INSTANCE_BY_ID_METHOD = "doDescribeInstanceById";
    public static final String DO_DESCRIBE_INSTANCES_METHOD = "doDescribeInstances";
    public static final String EMPTY_STRING = "";
    public static final String FAKE_ADDRESS = "fake-address";
    public static final String FAKE_CIDR = "10.0.0.19/8";
    public static final String FAKE_COMPUTE_ID = "fake-compute-id";
    public static final String FAKE_DEVICE = "fake-device";
    public static final String FAKE_FLAVOR_ID = "fake-flavor-id";
    public static final String FAKE_GATEWAY = "fake-gateway";
    public static final String FAKE_IMAGE_ID = "fake-image-id";
    public static final String FAKE_INSTANCE_ID = "fake-instance-id";
    public static final String FAKE_INSTANCE_NAME = "fake-instance-name";
    public static final String FAKE_NETWORK_ID = "fake-network-id";
    public static final String FAKE_ORDER_ID = "fake-order-id";
    public static final String FAKE_ORDER_NAME = "fake-order-name";
    public static final String FAKE_PROJECT_ID = "fake-project-id";
    public static final String FAKE_PUBLIC_KEY= "fake-public-key";
    public static final String FAKE_REMOTE_MEMBER_ID = "fake-intercomponent-member";
    public static final String FAKE_SECURITY_GROUP_ID = "fake-security-group-id";
    public static final String FAKE_SECURITY_RULE_ID = "fake-security-rule-id";
    public static final String FAKE_TAG = "fake-tag";
    public static final String FAKE_TOKEN_VALUE = "fake-token-value";
    public static final String FAKE_USER_DATA = "fake-user-data";
    public static final String FAKE_USER_ID = "fake-user-id";
    public static final String FAKE_USER_NAME = "fake-user-name";
    public static final String FAKE_VOLUME_ID = "fake-volume-id";
    public static final String FROM_JSON_METHOD = "fromJson";
    public static final String GET_ADDRESS_BY_ID_METHOD = "getAddressById";
    public static final String GET_GROUP_ID_FROM_METHOD = "getGroupIdFrom";
    public static final String GET_NETWORK_POOL_BY_USER_METHOD = "getNetworkPoolByUser";
    public static final String DEFAULT_CIDR = "0.0.0.0/0";
    public static final String TCP_PROTOCOL = "tcp";
    public static final String UDP_PROTOCOL = "udp";
    public static final String ICMP_PROTOCOL = "icmp";
    public static final String GET_SUBNET_BY_ID_METHOD = "getSubnetById";
    public static final String JSON_MALFORMED = "{anything:}";
    public static final String LOCAL_MEMBER_ID = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
    public static final String MAP_METHOD = "map";
    public static final String MESSAGE_STATUS_CODE = "Internal server error.";

    public void cleanList(ChainedList<Order> list) throws InternalServerErrorException {
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

    public Order createLocalOrderWithRemoteRequester(String providingMember) {
        String requestingMember = FAKE_REMOTE_MEMBER_ID;
        return createComputeOrder(requestingMember, providingMember);
    }

    public ComputeOrder createLocalComputeOrder() {
        return createComputeOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);
    }
    
    public ComputeOrder createLocalComputeOrder(List<String> networkOrderIds) {
        return createComputeOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID, networkOrderIds);
    }
    
    public ComputeOrder createComputeOrder(String requestingMember, String providingMember) {
        return createComputeOrder(requestingMember, providingMember, null);
    }
    
    public ComputeOrder createComputeOrder(String requestingMember, String providingMember,
            List<String> networkOrderIds) {

        ComputeOrder computeOrder = new ComputeOrder(createSystemUser(), 
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
                networkOrderIds);

        return computeOrder;
    }
    
    public NetworkOrder createLocalNetworkOrder() {
        return createNetworkOrder(LOCAL_MEMBER_ID, LOCAL_MEMBER_ID);
    }

    public NetworkOrder createNetworkOrder(String requestingMember, String providingMember) {
        NetworkOrder networkOrder = new NetworkOrder(createSystemUser(), 
                requestingMember, 
                providingMember,
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
        VolumeOrder volumeOrder = new VolumeOrder(createSystemUser(), 
                providingMember, 
                requestingMember,
                DEFAULT_CLOUD_NAME, 
                FAKE_ORDER_NAME, 
                DISK_VALUE);

        return volumeOrder;
    }

    public AttachmentOrder createLocalAttachmentOrder(ComputeOrder computeOrder, VolumeOrder volumeOrder) {
        return createAttachmentOrder(computeOrder, volumeOrder, FAKE_DEVICE);
    }

    public AttachmentOrder createAttachmentOrder(ComputeOrder computeOrder, VolumeOrder volumeOrder, String device) {
        String computeOrderId = computeOrder.getId();
        String volumeOrderId = volumeOrder.getId();
        
        AttachmentOrder attachmentOrder = new AttachmentOrder(createSystemUser(), 
                LOCAL_MEMBER_ID, 
                LOCAL_MEMBER_ID,
                DEFAULT_CLOUD_NAME, 
                computeOrderId, 
                volumeOrderId, 
                device);

        return attachmentOrder;
    }

    public PublicIpOrder createLocalPublicIpOrder(String computeOrderId) {
        PublicIpOrder publicIpOrder = new PublicIpOrder(createSystemUser(), 
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

    public void mockReadOrdersFromDataBase() throws InternalServerErrorException {
                mockReadOrdersFromDataBase(new SynchronizedDoublyLinkedList<>(), new SynchronizedDoublyLinkedList<>(),
                        new SynchronizedDoublyLinkedList<>(), new SynchronizedDoublyLinkedList<>(),
                        new SynchronizedDoublyLinkedList<>(), new SynchronizedDoublyLinkedList<>(),
                        new SynchronizedDoublyLinkedList<>(), new SynchronizedDoublyLinkedList<>(),
                        new SynchronizedDoublyLinkedList<>(), new SynchronizedDoublyLinkedList<>(),
                        new SynchronizedDoublyLinkedList<>(), new SynchronizedDoublyLinkedList<>(),
                        new SynchronizedDoublyLinkedList<>(), new SynchronizedDoublyLinkedList<>(),
                        new SynchronizedDoublyLinkedList<>(), new SynchronizedDoublyLinkedList<>(), 
                        new SynchronizedDoublyLinkedList<>());
    }

    /*
     * Mocks the behavior of the database as if there was no order in any state.
     */
    public void mockReadOrdersFromDataBase(SynchronizedDoublyLinkedList<Order> openList,
                                           SynchronizedDoublyLinkedList<Order> selectedList,
                                           SynchronizedDoublyLinkedList<Order> fulfilledList,
                                           SynchronizedDoublyLinkedList<Order> failedAfterSuccessRequestList,
                                           SynchronizedDoublyLinkedList<Order> checkingDeletionList,
                                           SynchronizedDoublyLinkedList<Order> pendingList,
                                           SynchronizedDoublyLinkedList<Order> spawningList,
                                           SynchronizedDoublyLinkedList<Order> failedOnRequestList,
                                           SynchronizedDoublyLinkedList<Order> unableToCheckRequestList,
                                           SynchronizedDoublyLinkedList<Order> assignedForDeletionRequestList,
                                           SynchronizedDoublyLinkedList<Order> pausingRequestList,
                                           SynchronizedDoublyLinkedList<Order> resumingRequestList,
                                           SynchronizedDoublyLinkedList<Order> hibernatingRequestList,
                                           SynchronizedDoublyLinkedList<Order> hibernatedRequestList,
                                           SynchronizedDoublyLinkedList<Order> pausedRequestList,
                                           SynchronizedDoublyLinkedList<Order> stoppingRequestList,
                                           SynchronizedDoublyLinkedList<Order> stoppedRequestList)
            throws InternalServerErrorException {

        DatabaseManager databaseManager = Mockito.mock(DatabaseManager.class);
        Mockito.when(databaseManager.readActiveOrders(OrderState.OPEN)).thenReturn(openList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.SELECTED)).thenReturn(selectedList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.FULFILLED)).thenReturn(fulfilledList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST)).thenReturn(failedAfterSuccessRequestList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.CHECKING_DELETION)).thenReturn(checkingDeletionList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.PENDING)).thenReturn(pendingList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.SPAWNING)).thenReturn(spawningList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.FAILED_ON_REQUEST)).thenReturn(failedOnRequestList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.UNABLE_TO_CHECK_STATUS)).thenReturn(unableToCheckRequestList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.ASSIGNED_FOR_DELETION)).thenReturn(assignedForDeletionRequestList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.PAUSING)).thenReturn(pausingRequestList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.RESUMING)).thenReturn(resumingRequestList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.HIBERNATING)).thenReturn(hibernatingRequestList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.HIBERNATED)).thenReturn(hibernatedRequestList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.STOPPING)).thenReturn(stoppingRequestList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.STOPPED)).thenReturn(stoppedRequestList);
        Mockito.when(databaseManager.readActiveOrders(OrderState.PAUSED)).thenReturn(pausedRequestList);

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

    /*
     * Simulates instance of a LocalCloudConnector since its creation via CloudConnectorFactory.
     */
    public RemoteCloudConnector mockRemoteCloudConnectorFromFactory() {
        CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);

        PowerMockito.mockStatic(CloudConnectorFactory.class);
        BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

        RemoteCloudConnector remoteCloudConnector = Mockito.mock(RemoteCloudConnector.class);
        Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(remoteCloudConnector);

        return remoteCloudConnector;
    }

    public List<Order> populateFedNetDbWithState(OrderState state, int size, RecoveryService service) throws InternalServerErrorException {
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Order order = createComputeOrder(FAKE_REMOTE_MEMBER_ID, FAKE_REMOTE_MEMBER_ID);
            order.setOrderState(state);
            orders.add(order);
            service.save(order);
        }
        return orders;
    }

    public InternalServerErrorException getInternalServerErrorException() {
        return new InternalServerErrorException(MESSAGE_STATUS_CODE);
    }
    
    /*
     * Create fake user data for testing.
     */
    public ArrayList<UserData> createUserDataList() {
        UserData[] userDataArray = new UserData[]{
                new UserData(FAKE_USER_DATA, CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, FAKE_TAG)};

        return new ArrayList<>(Arrays.asList(userDataArray));
    }

    public Ec2Client getAwsMockedClient() throws FogbowException {
        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
        return client;
    }
    
    /*
     * Create fake OpenStack user for testing.
     */
    public OpenStackV3User createOpenStackUser() {
        String userId = TestUtils.FAKE_USER_ID;
        String userName = TestUtils.FAKE_USER_NAME;
        String tokenValue = TestUtils.FAKE_TOKEN_VALUE;
        String projectId = TestUtils.FAKE_PROJECT_ID;
        return new OpenStackV3User(userId, userName, tokenValue, projectId);
    }
    
    /*
     * Simulates instance of a ApplicationFacade
     */
    public ApplicationFacade mockApplicationFacade() {
        ApplicationFacade facade = Mockito.mock(ApplicationFacade.class);
        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(facade);
        return facade;
    }
    
    /*
     * Create an available quota for testing.
     */
    public ResourceQuota createAvailableQuota() {
        ResourceAllocation totalQuota = createTotalQuota();
        ResourceAllocation usedQuota = createUsedQuota();
        return new ResourceQuota(totalQuota, usedQuota);
    }

    public ResourceAllocation createTotalQuota() {
        int totalInstances = 100;
        int totalvCPU = 8;
        int tolalRam = 16384;
        int totalDisk = 30;
        int totalNetworks = 15;
        int totalPublicIps = 5;
        int totalVolumes = 200;
        
        ResourceAllocation totalQuota = ResourceAllocation.builder()
                .instances(totalInstances)
                .vCPU(totalvCPU)
                .ram(tolalRam)
                .storage(totalDisk)
                .networks(totalNetworks)
                .volumes(totalVolumes)
                .publicIps(totalPublicIps)
                .build();
        
        return totalQuota;
    }
    
    public ResourceAllocation createUsedQuota() {
        int usedInstances = 1;
        int usedvCPU = 2;
        int usedRam = 8192;
        int usedDisk = 8;
        int usedNetworks = 1;
        int usedPublicIps = 1;
        int usedVolumes = 2;

        ResourceAllocation usedQuota =  ResourceAllocation.builder()
                .instances(usedInstances)
                .vCPU(usedvCPU)
                .ram(usedRam)
                .storage(usedDisk)
                .volumes(usedVolumes)
                .networks(usedNetworks)
                .publicIps(usedPublicIps)
                .build();
        
        return usedQuota;
    }

    /*
     * Transforms the content of an object into its JSON 
     * representation in string format.
     */
    public String getResponseContent(Object content) {
        return new Gson().toJson(content);
    }
}
