package org.fogbowcloud.manager.core.datastore;

import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.plugins.cloud.util.CloudInitUserDataBuilder;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertiesHolder.class)
public class DatabaseManagerTest {

    private static final String DATABASE_PATH = "/tmp/fogbow_history_test.db";
    private String DATABASE_URL = "jdbc:sqlite:" + DATABASE_PATH;

    /**
     * Cleaning all the enviromnent before running the tests.
     */
    @BeforeClass
    public static void init() throws NoSuchFieldException, IllegalAccessException {
        resetDatabaseManagerInstance();
        cleanEnviromnent();
    }

    @Before
    public void setUp() {
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        when(propertiesHolder.getProperty(anyString())).thenReturn(DATABASE_URL);

        PowerMockito.mockStatic(PropertiesHolder.class);
        given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
    }

    /**
     * Remove database file and reset database manager.
     */
    @After
    public void tearDown() throws NoSuchFieldException, IllegalAccessException {
        resetDatabaseManagerInstance();
        cleanEnviromnent();
    }

    @Test(expected = Error.class)
    public void testInitializeWithErrorDataStore() {
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        when(propertiesHolder.getProperty(anyString())).thenReturn("invalid_url");

        PowerMockito.mockStatic(PropertiesHolder.class);
        given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);

        // It should raise an exception since the database path is an invalid one
        DatabaseManager.getInstance();
    }

    @Test
    public void testAddComputeOrder() throws UnexpectedException {
        DatabaseManager databaseManager = DatabaseManager.getInstance();

        // Check if compute order table is empty
        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.OPEN);

        Assert.assertEquals(0, getListSize(synchronizedDoublyLinkedList));

        // Create a new compute order
        Order computeOrder = new ComputeOrder(new FederationUser("fed-id", new HashMap<>()),
                "requestingMember", "providingMember", 8, 1024,
                30, "fake_image_name", new UserData("extraUserDataFile",
                CloudInitUserDataBuilder.FileType.CLOUD_CONFIG), "fake_public_key", null);
        computeOrder.setOrderState(OrderState.OPEN);

        // Add compute order into database
        databaseManager.add(computeOrder);

        // Check if compute order table has the new order
        synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.OPEN);

        Assert.assertEquals(1, getListSize(synchronizedDoublyLinkedList));
    }

    @Test
    public void testUpdateComputeOrderState() throws UnexpectedException {
        DatabaseManager databaseManager = DatabaseManager.getInstance();

        ComputeOrder computeOrder = new ComputeOrder("id", new FederationUser("fed-id", new HashMap<>()),
                "requestingMember", "providingMember", 8, 1024,
                30, "fake_image_name", new UserData("extraUserDataFile",
                CloudInitUserDataBuilder.FileType.CLOUD_CONFIG), "fake_public_key", null);
        computeOrder.setOrderState(OrderState.OPEN);

        databaseManager.add(computeOrder);

        computeOrder.setOrderState(OrderState.FULFILLED);
        computeOrder.setActualAllocation(new ComputeAllocation(10, 10,10));

        databaseManager.update(computeOrder);

        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.FULFILLED);

        ComputeOrder result = (ComputeOrder) synchronizedDoublyLinkedList.getNext();

        Assert.assertEquals(result.getOrderState(), OrderState.FULFILLED);
        Assert.assertEquals(result.getActualAllocation().getRam(), 10);
        Assert.assertEquals(result.getActualAllocation().getvCPU(), 10);
        Assert.assertEquals(result.getActualAllocation().getInstances(), 10);
    }

    @Test
    public void testAddNetworkOrder() throws UnexpectedException {
        DatabaseManager databaseManager = DatabaseManager.getInstance();

        // Check if network order table is empty
        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.OPEN);

        Assert.assertEquals(0, getListSize(synchronizedDoublyLinkedList));

        // Create a new network order
        Order networkOrder = new NetworkOrder(new FederationUser("fed-id", new HashMap<>()),
                "requestingMember", "providingMember", "gateway",
                "address", NetworkAllocationMode.STATIC);
        networkOrder.setOrderState(OrderState.OPEN);

        // Add network order into database
        databaseManager.add(networkOrder);

        // Check if network order table has the new order
        synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.OPEN);

        Assert.assertEquals(1, getListSize(synchronizedDoublyLinkedList));
    }

    @Test
    public void testUpdateNetworkOrderState() throws UnexpectedException {
        DatabaseManager databaseManager = DatabaseManager.getInstance();

        Order networkOrder = new NetworkOrder(new FederationUser("fed-id", new HashMap<>()),
                "requestingMember", "providingMember", "gateway",
                "address", NetworkAllocationMode.STATIC);
        networkOrder.setOrderState(OrderState.OPEN);

        databaseManager.add(networkOrder);

        networkOrder.setOrderState(OrderState.FULFILLED);

        databaseManager.update(networkOrder);

        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.FULFILLED);

        Order result = synchronizedDoublyLinkedList.getNext();

        Assert.assertEquals(result.getOrderState(), OrderState.FULFILLED);
    }

    @Test
    public void testAddVolumeOrder() throws UnexpectedException {
        DatabaseManager databaseManager = DatabaseManager.getInstance();

        // Check if volume order table is empty
        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.OPEN);

        Assert.assertEquals(0, getListSize(synchronizedDoublyLinkedList));

        // Create a new volume order
        Order volumeOrder = new VolumeOrder(new FederationUser("fed-id", new HashMap<>()),
                "requestingMember", "providingMember", 0, "volume-name");
        volumeOrder.setOrderState(OrderState.OPEN);

        // Add volume order into database
        databaseManager.add(volumeOrder);

        // Check if volume order table has the new order
        synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.OPEN);

        Assert.assertEquals(1, getListSize(synchronizedDoublyLinkedList));
    }

    @Test
    public void testUpdateVolumeOrderState() throws UnexpectedException {
        DatabaseManager databaseManager = DatabaseManager.getInstance();

        Order volumeOrder = new VolumeOrder(new FederationUser("fed-id", new HashMap<>()),
                "requestingMember", "providingMember", 0, "volume-name");
        volumeOrder.setOrderState(OrderState.OPEN);

        databaseManager.add(volumeOrder);

        volumeOrder.setOrderState(OrderState.FULFILLED);

        databaseManager.update(volumeOrder);

        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.FULFILLED);

        Order result = synchronizedDoublyLinkedList.getNext();

        Assert.assertEquals(result.getOrderState(), OrderState.FULFILLED);
    }

    /**
     * If a closed order do not have an instance id, it should not be recovered.
     */
    @Test
    public void testGetClosedOrderWithoutInstanceId() throws UnexpectedException {
        DatabaseManager databaseManager = DatabaseManager.getInstance();

        Order volumeOrder = new VolumeOrder(new FederationUser("fed-id", new HashMap<>()),
                "requestingMember", "providingMember", 0, "volume-name");
        volumeOrder.setOrderState(OrderState.CLOSED);

        databaseManager.add(volumeOrder);

        Assert.assertEquals(volumeOrder.getInstanceId(), null);

        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.CLOSED);

        Assert.assertEquals(0, getListSize(synchronizedDoublyLinkedList));
    }

    @Test
    public void testGetClosedOrderWithInstanceId() throws UnexpectedException {
        DatabaseManager databaseManager = DatabaseManager.getInstance();

        VolumeOrder volumeOrder = new VolumeOrder(new FederationUser("fed-id", new HashMap<>()),
                "requestingMember", "providingMember", 0, "volume-name");
        volumeOrder.setOrderState(OrderState.CLOSED);
        volumeOrder.setInstanceId("instanceId");

        databaseManager.add(volumeOrder);

        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.CLOSED);

        Assert.assertEquals(1, getListSize(synchronizedDoublyLinkedList));
    }

    /**
     * Returns size of synchronizedDoublyLinkedList.
     */
    private int getListSize(SynchronizedDoublyLinkedList synchronizedDoublyLinkedList) {
        int listSize = 0;

        while (synchronizedDoublyLinkedList.getNext() != null) {
            listSize++;
        }

        return listSize;
    }

    private static void resetDatabaseManagerInstance() throws NoSuchFieldException, IllegalAccessException {
        Field instance = DatabaseManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    private static void cleanEnviromnent() {
        File folder = new File(DATABASE_PATH);
        File[] listFiles = folder.listFiles();

        if (listFiles != null) {
            for (File file : listFiles) {
                if (file != null) {
                    file.delete();
                    file.deleteOnExit();
                }
            }
        }

        folder.delete();
        folder.deleteOnExit();
    }
}
