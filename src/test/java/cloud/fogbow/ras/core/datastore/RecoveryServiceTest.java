package cloud.fogbow.ras.core.datastore;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.datastore.orderstorage.OrderRepository;
import cloud.fogbow.ras.core.datastore.services.RecoveryService;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.*;
import cloud.fogbow.common.util.CloudInitUserDataBuilder;
import org.aspectj.weaver.ast.Or;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.*;

@PowerMockIgnore({"javax.management.*"})
@PrepareForTest({DatabaseManager.class, PropertiesHolder.class})
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@SpringBootTest
public class RecoveryServiceTest extends BaseUnitTests {

    private static final String DATABASE_PATH = "/tmp/fogbow_history_test.db";
    private String DATABASE_URL = "jdbc:sqlite:" + DATABASE_PATH;

    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_ID_1 = "fake-id-1";
    private static final String FAKE_REQUESTING_MEMBER = "fake-requesting-member";
    private static final String FAKE_PROVIDING_MEMBER = "fake-providing-member";
    private static final String FAKE_INSTANCE_NAME = "fake-instance-name";
    private static final int FAKE_CPU_AMOUNT = 8;
    private static final int FAKE_RAM_AMOUNT = 1024;
    private static final int FAKE_DISK_AMOUNT = 30;
    private static final String FAKE_IMAGE_NAME = "fake-image-name";
    private static final String FAKE_PUBLIC_KEY = "fake-public-key";
    private static final String FAKE_USER_DATA_FILE = "extraUserDataFile";
    private static final String FAKE_CLOUD_NAME = "fake-cloud";
    private static final String FAKE_NETWORK_ID = "fake-network";

    private static final String PROVIDER_KEY = "provider";
    private static final String ID_KEY = "id";
    private static final String NAME_KEY = "name";
    private static final String TOKEN_KEY = "token";
    private static final Integer ORDERS_AMOUNT = 3;

    public static final ArrayList<UserData> FAKE_USER_DATA = new ArrayList<UserData>(Arrays.asList(
            new UserData[] { new UserData(FAKE_USER_DATA_FILE, CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, "fake-tag")}));

    public static final List<String> FAKE_NETWORK_IDS = new ArrayList<>(Arrays.asList(
            new String[] { (FAKE_NETWORK_ID) }));

    @Resource
    private RecoveryService recoveryService;

    @Resource
    private OrderRepository orderRepository;

    private Order computeOrder;

    @Before
    public void setUp() throws UnexpectedException {

        // mocking databaseManager
        DatabaseManager databaseManager = Mockito.mock(DatabaseManager.class);
        Mockito.when(databaseManager.readActiveOrders(OrderState.OPEN)).thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(databaseManager.readActiveOrders(OrderState.SPAWNING)).thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(databaseManager.readActiveOrders(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST)).thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(databaseManager.readActiveOrders(OrderState.FULFILLED)).thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(databaseManager.readActiveOrders(OrderState.UNABLE_TO_CHECK_STATUS)).thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(databaseManager.readActiveOrders(OrderState.PENDING)).thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(databaseManager.readActiveOrders(OrderState.CLOSED)).thenReturn(new SynchronizedDoublyLinkedList<>());
        PowerMockito.mockStatic(DatabaseManager.class);
        BDDMockito.given(DatabaseManager.getInstance()).willReturn(databaseManager);

        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(Mockito.anyString())).thenReturn(DATABASE_URL);

        PowerMockito.mockStatic(PropertiesHolder.class);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
    }

    // Delete all orders in database
    @After
    public void deleteDB() {

        for (Order order : orderRepository.findAll()) {
            orderRepository.delete(order);
        }
    }

    // test case: All lists of orders must be empty
    @Test
    public void testReadActiveOrdersWhenIsEmpty() {

        // exercise
        List<Order> openorders = recoveryService.readActiveOrders(OrderState.OPEN);
        List<Order> closedOrders = recoveryService.readActiveOrders(OrderState.CLOSED);
        List<Order> pendingOrders = recoveryService.readActiveOrders(OrderState.PENDING);
        List<Order> spawningOrders = recoveryService.readActiveOrders(OrderState.SPAWNING);
        List<Order> fulfilledOrders = recoveryService.readActiveOrders(OrderState.FULFILLED);
        List<Order> unableToCheckStatus = recoveryService.readActiveOrders(OrderState.UNABLE_TO_CHECK_STATUS);
        List<Order> failedOrders = recoveryService.readActiveOrders(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
        List<Order> closedorders = recoveryService.readActiveOrders(OrderState.CLOSED);
        List<Order> deactivatedOrders = recoveryService.readActiveOrders(OrderState.DEACTIVATED);

        // verify
        Assert.assertTrue(openorders.isEmpty());
        Assert.assertTrue(closedOrders.isEmpty());
        Assert.assertTrue(pendingOrders.isEmpty());
        Assert.assertTrue(spawningOrders.isEmpty());
        Assert.assertTrue(fulfilledOrders.isEmpty());
        Assert.assertTrue(unableToCheckStatus.isEmpty());
        Assert.assertTrue(failedOrders.isEmpty());
        Assert.assertTrue(closedorders.isEmpty());
        Assert.assertTrue(deactivatedOrders.isEmpty());
    }

    // test case: Adding a new compute order to database and checking with a query.
    @Test
    public void testAddComputeOrder() throws UnexpectedException {
        // set up
        Map<String, String> attributes = new HashMap<>();
        attributes.put(PROVIDER_KEY, FAKE_TOKEN_PROVIDER);
        attributes.put(ID_KEY, FAKE_ID_1);
        attributes.put(NAME_KEY, FAKE_INSTANCE_NAME);
        attributes.put(TOKEN_KEY, FAKE_TOKEN_VALUE);
        SystemUser systemUser = new SystemUser(FAKE_ID_1, FAKE_INSTANCE_NAME, FAKE_TOKEN_PROVIDER);

        computeOrder = new ComputeOrder(
                FAKE_ID_1, systemUser, FAKE_REQUESTING_MEMBER, FAKE_PROVIDING_MEMBER,
                FAKE_CLOUD_NAME, FAKE_INSTANCE_NAME, FAKE_CPU_AMOUNT, FAKE_RAM_AMOUNT, FAKE_DISK_AMOUNT,
                FAKE_IMAGE_NAME, FAKE_USER_DATA, FAKE_PUBLIC_KEY, FAKE_NETWORK_IDS);
        computeOrder.setOrderStateInTestMode(OrderState.OPEN);

        // exercise
        recoveryService.save(computeOrder);
        List<Order> orders = recoveryService.readActiveOrders(OrderState.OPEN);

        // verify
        Assert.assertEquals(1, orders.size());
        Assert.assertEquals(computeOrder, orders.get(0));
    }

    // test case: Adding a new open compute order to database and checking that there is one element in open state
    // and there is no elements in pending state. After set the order state to pending, check reverse situation.
    @Test
    public void testAddComputeOrderSettingState() throws UnexpectedException {
        // set up
        SystemUser systemUser = new SystemUser(FAKE_ID_1, FAKE_INSTANCE_NAME, FAKE_TOKEN_PROVIDER);

        computeOrder = new ComputeOrder(
                FAKE_ID_1, systemUser, FAKE_REQUESTING_MEMBER, FAKE_PROVIDING_MEMBER,
                FAKE_CLOUD_NAME, FAKE_INSTANCE_NAME, FAKE_CPU_AMOUNT, FAKE_RAM_AMOUNT, FAKE_DISK_AMOUNT,
                FAKE_IMAGE_NAME, FAKE_USER_DATA, FAKE_PUBLIC_KEY, FAKE_NETWORK_IDS);
        computeOrder.setOrderStateInTestMode(OrderState.OPEN);

        // exercise
        recoveryService.save(computeOrder);
        List<Order> openOrders = recoveryService.readActiveOrders(OrderState.OPEN);
        List<Order> closedOrders = recoveryService.readActiveOrders(OrderState.PENDING);

        // verify
        Assert.assertEquals(1, openOrders.size());
        Assert.assertTrue(closedOrders.isEmpty());
        Assert.assertEquals(computeOrder, openOrders.get(0));

        // set up
        computeOrder.setOrderStateInTestMode(OrderState.PENDING);

        // exercise
        recoveryService.update(computeOrder);
        openOrders = recoveryService.readActiveOrders(OrderState.OPEN);
        closedOrders = recoveryService.readActiveOrders(OrderState.PENDING);

        // verify
        Assert.assertTrue(openOrders.isEmpty());
        Assert.assertEquals(1, closedOrders.size());
        Assert.assertEquals(computeOrder, closedOrders.get(0));
    }

    //// test case: Adding orders of all types and checking the method readOrders
    @Test
    public void testAddOrdersOfAllTypes() throws UnexpectedException {
        // set up
        SystemUser systemUser = new SystemUser(FAKE_ID_1, FAKE_INSTANCE_NAME, FAKE_TOKEN_PROVIDER);

        computeOrder = new ComputeOrder(
                FAKE_ID_1, systemUser, FAKE_REQUESTING_MEMBER, FAKE_PROVIDING_MEMBER,
                FAKE_CLOUD_NAME, FAKE_INSTANCE_NAME, FAKE_CPU_AMOUNT, FAKE_RAM_AMOUNT, FAKE_DISK_AMOUNT,
                FAKE_IMAGE_NAME, FAKE_USER_DATA, FAKE_PUBLIC_KEY, FAKE_NETWORK_IDS);
        computeOrder.setOrderStateInTestMode(OrderState.OPEN);

        // creating attachment order with open state
        Order attachmentOrder = new AttachmentOrder(systemUser, FAKE_REQUESTING_MEMBER, FAKE_PROVIDING_MEMBER,
                FAKE_CLOUD_NAME, FAKE_ID_1, FAKE_ID_1, null);
        attachmentOrder.setOrderStateInTestMode(OrderState.OPEN);

        // creating network order with fulfilled state
        Order networkOrder = new NetworkOrder(systemUser, FAKE_REQUESTING_MEMBER, FAKE_PROVIDING_MEMBER,
                FAKE_CLOUD_NAME, null, null, null, null);
        networkOrder.setOrderStateInTestMode(OrderState.FULFILLED);

        // creating volume order with fulfilled state
        Order volumeOrder = new VolumeOrder(systemUser, FAKE_REQUESTING_MEMBER, FAKE_PROVIDING_MEMBER,
                FAKE_CLOUD_NAME, null, 0);
        volumeOrder.setOrderStateInTestMode(OrderState.FULFILLED);

        // exercise
        recoveryService.save(computeOrder);
        recoveryService.save(attachmentOrder);
        recoveryService.save(networkOrder);
        recoveryService.save(volumeOrder);
        List<Order> openOrders = recoveryService.readActiveOrders(OrderState.OPEN);
        List<Order> fulfilledOrders = recoveryService.readActiveOrders(OrderState.FULFILLED);

        // verify
        Assert.assertEquals(2, openOrders.size());
        Assert.assertEquals(2, fulfilledOrders.size());
    }

    // test case: Adding the same order twice and checking the exception
    @Test(expected = UnexpectedException.class)
    public void testSaveExistentOrder() throws UnexpectedException {
        // set up
        SystemUser systemUser = new SystemUser(FAKE_ID_1, FAKE_INSTANCE_NAME, FAKE_TOKEN_PROVIDER);

        computeOrder = new ComputeOrder(
                FAKE_ID_1, systemUser, FAKE_REQUESTING_MEMBER, FAKE_PROVIDING_MEMBER,
                FAKE_CLOUD_NAME, FAKE_INSTANCE_NAME, FAKE_CPU_AMOUNT, FAKE_RAM_AMOUNT, FAKE_DISK_AMOUNT,
                FAKE_IMAGE_NAME, FAKE_USER_DATA, FAKE_PUBLIC_KEY, FAKE_NETWORK_IDS);
        computeOrder.setOrderStateInTestMode(OrderState.OPEN);

        // exercise
        recoveryService.save(computeOrder);
        recoveryService.save(computeOrder);
    }

    // test case: Call the update method of a non-existent order and checking the exception
    @Test(expected = UnexpectedException.class)
    public void testUpdateNonExistentOrder() throws UnexpectedException {
        // set up
        SystemUser systemUser = new SystemUser(FAKE_ID_1, FAKE_INSTANCE_NAME, FAKE_TOKEN_PROVIDER);

        computeOrder = new ComputeOrder(
                FAKE_ID_1, systemUser, FAKE_REQUESTING_MEMBER, FAKE_PROVIDING_MEMBER,
                FAKE_CLOUD_NAME, FAKE_INSTANCE_NAME, FAKE_CPU_AMOUNT, FAKE_RAM_AMOUNT, FAKE_DISK_AMOUNT,
                FAKE_IMAGE_NAME, FAKE_USER_DATA, FAKE_PUBLIC_KEY, FAKE_NETWORK_IDS);
        computeOrder.setOrderStateInTestMode(OrderState.OPEN);

        // exercise
        recoveryService.update(computeOrder);
    }

    //test case: test if the save operation works as expected by saving some objects and comparing the list returned by db.
    @Test
    public void testSaveOperation() throws UnexpectedException{
        // setup //exercise
        List<Order> expectedFulfilledOrders = populateFedNetDbWithState(OrderState.FULFILLED, ORDERS_AMOUNT, recoveryService);
        List<Order> expectedOpenedOrders = populateFedNetDbWithState(OrderState.OPEN, ORDERS_AMOUNT, recoveryService);
        List<Order> expectedClosedOrders = populateFedNetDbWithState(OrderState.CLOSED, ORDERS_AMOUNT, recoveryService);
        List<Order> expectedDeactivatedOrders = populateFedNetDbWithState(OrderState.DEACTIVATED, ORDERS_AMOUNT, recoveryService);
        List<Order> expectedFailedAfterSuccessfulRequestOrders = populateFedNetDbWithState(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST, ORDERS_AMOUNT, recoveryService);
        List<Order> expectedFailedOrders = populateFedNetDbWithState(OrderState.FAILED_ON_REQUEST, ORDERS_AMOUNT, recoveryService);
        List<Order> expectedSpawningOrders = populateFedNetDbWithState(OrderState.SPAWNING, ORDERS_AMOUNT, recoveryService);

        //verify
        Assert.assertEquals(expectedFulfilledOrders, recoveryService.readActiveOrders(OrderState.FULFILLED));
        Assert.assertEquals(expectedOpenedOrders, recoveryService.readActiveOrders(OrderState.OPEN));
        Assert.assertEquals(expectedClosedOrders, recoveryService.readActiveOrders(OrderState.CLOSED));
        Assert.assertEquals(expectedDeactivatedOrders, recoveryService.readActiveOrders(OrderState.DEACTIVATED));
        Assert.assertEquals(expectedFailedAfterSuccessfulRequestOrders, recoveryService.readActiveOrders(OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST));
        Assert.assertEquals(expectedFailedOrders, recoveryService.readActiveOrders(OrderState.FAILED_ON_REQUEST));
        Assert.assertEquals(expectedSpawningOrders, recoveryService.readActiveOrders(OrderState.SPAWNING));
    }
}
