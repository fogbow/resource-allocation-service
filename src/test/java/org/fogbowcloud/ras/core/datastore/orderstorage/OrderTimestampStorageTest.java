package org.fogbowcloud.ras.core.datastore.orderstorage;

import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.datastore.commands.OrderTimestampTableAttributes;
import org.fogbowcloud.ras.core.datastore.commands.TimestampSQLCommands;
import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.OrderState;
import org.fogbowcloud.ras.core.models.orders.UserData;
import org.fogbowcloud.ras.core.models.orders.VolumeOrder;
import org.fogbowcloud.ras.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.attachment.v2.CreateAttachmentRequest.Attachment;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@PrepareForTest(PropertiesHolder.class)
@RunWith(PowerMockRunner.class)
public class OrderTimestampStorageTest {

    private static String databaseFile = "orderStorageTest.sqlite3";
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
                "requestingMember", "providingMember", "fake-instance-name", 8, 1024,
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

    // Adding orders of different types and checking BD and usage
    @Test
    public void testAddOrderOfDifferentTypes() throws SQLException {
    	
    	// set up
    	ComputeOrder computeOrder = new ComputeOrder();
    	computeOrder.setActualAllocation(new ComputeAllocation(2, 4, 1));
    	computeOrder.setOrderStateInTestMode(OrderState.FULFILLED);
    	computeOrder.setId("fake-id-1");
    	
    	NetworkOrder networkOrder = new NetworkOrder();
    	networkOrder.setOrderStateInTestMode(OrderState.OPEN);
    	networkOrder.setId("fake-id-2");
    	
    	AttachmentOrder attachmentOrder = new AttachmentOrder();
    	attachmentOrder.setOrderStateInTestMode(OrderState.SPAWNING);
    	attachmentOrder.setId("fake-id-3");
    	
    	VolumeOrder volumeOrder = new VolumeOrder(new FederationUserToken(), 
    			"reqMem", "provMember", 50, "volume-test");
    	volumeOrder.setOrderStateInTestMode(OrderState.CLOSED);
    	volumeOrder.setId("fake-id-4");
    	
    	// exercise
    	orderStorage.addOrder(computeOrder);
    	orderStorage.addOrder(networkOrder);
    	orderStorage.addOrder(attachmentOrder);
    	orderStorage.addOrder(volumeOrder);
    	
    	// verify 1
    	ResultSet resultSet = makeRequestByOrderId("fake-id-1");
    	Assert.assertEquals(resultSet.getString(OrderTimestampTableAttributes.ORDER_STATE), "FULFILLED");
    	Assert.assertEquals(resultSet.getString(OrderTimestampTableAttributes.RESOURCE_TYPE), "COMPUTE");
    	Assert.assertEquals(resultSet.getString(OrderTimestampTableAttributes.SPEC), "2/4");
    	
    	// verify 2
    	resultSet = makeRequestByOrderId("fake-id-2");
    	Assert.assertEquals(resultSet.getString(OrderTimestampTableAttributes.ORDER_STATE), "OPEN");
    	Assert.assertEquals(resultSet.getString(OrderTimestampTableAttributes.RESOURCE_TYPE), "NETWORK");
    	Assert.assertEquals(resultSet.getString(OrderTimestampTableAttributes.SPEC), "");
    	
    	// verify 3
    	resultSet = makeRequestByOrderId("fake-id-3");
    	Assert.assertEquals(resultSet.getString(OrderTimestampTableAttributes.ORDER_STATE), "SPAWNING");
    	Assert.assertEquals(resultSet.getString(OrderTimestampTableAttributes.RESOURCE_TYPE), "ATTACHMENT");
    	Assert.assertEquals(resultSet.getString(OrderTimestampTableAttributes.SPEC), "");
    	
    	// verify 4
    	resultSet = makeRequestByOrderId("fake-id-4");
    	Assert.assertEquals(resultSet.getString(OrderTimestampTableAttributes.ORDER_STATE), "CLOSED");
    	Assert.assertEquals(resultSet.getString(OrderTimestampTableAttributes.RESOURCE_TYPE), "VOLUME");
    	Assert.assertEquals(resultSet.getString(OrderTimestampTableAttributes.SPEC), "50");
    	
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
    
    private ResultSet makeRequestByOrderId(String orderId) throws SQLException {
    	PreparedStatement selectMemberStatement = null;

        Connection connection = null;

        ResultSet rs = null;
        
        try {
            connection = orderStorage.getConnection();
            connection.setAutoCommit(false);

            selectMemberStatement = connection
                    .prepareStatement(TimestampSQLCommands.SELECT_TIMESTAMP_BY_ORDER_ID_SQL);

            selectMemberStatement.setString(1, orderId);

            rs = selectMemberStatement.executeQuery();

        } catch (SQLException e) {
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e1) {
                e1.printStackTrace();
                System.out.println("Couldn't rollback transaction.");
            }

        }
        
        return rs;
    }

}
