package org.fogbowcloud.ras.core.datastore.orderstorage;

import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.OrderState;
import org.fogbowcloud.ras.core.models.orders.UserData;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.interoperability.util.CloudInitUserDataBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@PrepareForTest(PropertiesHolder.class)
@RunWith(PowerMockRunner.class)
public class OrderTimestampStorageTest {

    private static String databaseFile = "orderStoreTest.sqlite3";
    private static String databaseURL = "jdbc:sqlite:" + databaseFile;
    private static final String FAKE_USER_ID = "fake-user-id";


    private OrderTimestampStorage orderStorage;

    private Order orderTest;

    @Before
    public void setUp() throws SQLException {

        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(Mockito.anyString())).thenReturn(databaseURL);

        PowerMockito.mockStatic(PropertiesHolder.class);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);

        orderStorage = new OrderTimestampStorage();

        FederationUserToken federationUserToken = new FederationUserToken("fake-token-provider",
                "token-value", "fake-id", "fake-user");

        orderTest = new ComputeOrder(federationUserToken,
                "requestingMember", "providingMember", 8, 1024,
                30, "fake_image_name", new UserData("extraUserDataFile",
                CloudInitUserDataBuilder.FileType.CLOUD_CONFIG), "fake_public_key", null);
        orderTest.setOrderStateInTestMode(OrderState.OPEN);
        orderTest.setId(FAKE_USER_ID);
    }

    @After
    public void tearDown() {
        new File(databaseFile).delete();
    }

    // test case: Adding new order to database and checking its state
    @Test
    public void testAddOrder() throws SQLException {

        // exercise
        orderStorage.addOrder(orderTest);
        Map<String, List<String>> result = orderStorage.selectOrderById(FAKE_USER_ID);

        // verify
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(1, result.get(FAKE_USER_ID).size());
        Assert.assertEquals("OPEN", result.get(FAKE_USER_ID).get(0));
    }

    // test case: Adding the same order to database with two different states.
    @Test
    public void testAddOrderStateChange() throws SQLException {

        // exercise
        orderStorage.addOrder(orderTest);
        orderTest.setOrderStateInTestMode(OrderState.PENDING);
        orderStorage.addOrder(orderTest);

        Map<String, List<String>> result = orderStorage.selectOrderById(FAKE_USER_ID);

        // verify
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(2, result.get(FAKE_USER_ID).size());
        Assert.assertEquals("OPEN", result.get(FAKE_USER_ID).get(0));
        Assert.assertEquals("PENDING", result.get(FAKE_USER_ID).get(1));
    }

    // test case: Adding the order with the same state twice and checking the exception
    @Test(expected = SQLException.class)
    public void testAddOrderWithSameState() throws SQLException {

        // exercise
        orderStorage.addOrder(orderTest);
        orderStorage.addOrder(orderTest);
    }

}
