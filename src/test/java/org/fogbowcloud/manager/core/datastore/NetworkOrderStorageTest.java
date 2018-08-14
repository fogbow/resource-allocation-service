package org.fogbowcloud.manager.core.datastore;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.plugins.cloud.util.CloudInitUserDataBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class NetworkOrderStorageTest extends DatabaseManagerTest {

    // test case: Tests if a new network order is added properly in the database.
    @Test(expected = UnexpectedException.class)
    public void testAddExistingNetworkOrder() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        Order networkOrder = new NetworkOrder(federationUser,
                "requestingMember", "providingMember", "gateway",
                "address", NetworkAllocationMode.STATIC);
        networkOrder.setOrderStateInTestMode(OrderState.OPEN);

        // exercise
        databaseManager.add(networkOrder);

        databaseManager.add(networkOrder);
    }

    // test case: Tests if a new network order is added properly in the database.
    @Test
    public void testAddNetworkOrder() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        Order networkOrder = new NetworkOrder(federationUser,
                "requestingMember", "providingMember", "gateway",
                "address", NetworkAllocationMode.STATIC);
        networkOrder.setOrderStateInTestMode(OrderState.OPEN);

        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.OPEN);

        // verify
        Assert.assertEquals(0, getListSize(synchronizedDoublyLinkedList));

        // exercise
        databaseManager.add(networkOrder);

        synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.OPEN);

        // verify
        Assert.assertEquals(1, getListSize(synchronizedDoublyLinkedList));
    }

    // test case: Tests if a stored network order is updated properly in the database.
    @Test
    public void testUpdateNetworkOrderState() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        Order networkOrder = new NetworkOrder(federationUser,
                "requestingMember", "providingMember", "gateway",
                "address", NetworkAllocationMode.STATIC);
        networkOrder.setOrderStateInTestMode(OrderState.OPEN);

        databaseManager.add(networkOrder);

        networkOrder.setOrderStateInTestMode(OrderState.FULFILLED);

        // exercise
        databaseManager.update(networkOrder);

        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.FULFILLED);

        Order result = synchronizedDoublyLinkedList.getNext();

        // verify
        Assert.assertEquals(result.getOrderState(), OrderState.FULFILLED);
    }

    // test case: Try to update a nonexisting order.
    @Test(expected = UnexpectedException.class)
    public void testUpdateNonexistingNetworkOrder() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        Order networkOrder = new NetworkOrder(federationUser,
                "requestingMember", "providingMember", "gateway",
                "address", NetworkAllocationMode.STATIC);
        networkOrder.setOrderStateInTestMode(OrderState.FULFILLED);

        // exercise
        databaseManager.update(networkOrder);
    }

    // test case: If a closed order do not have an instance id, it should not be recovered.
    @Test
    public void testGetClosedOrderWithoutInstanceId() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        Order networkOrder = new NetworkOrder(federationUser,
                "requestingMember", "providingMember", "gateway",
                "address", NetworkAllocationMode.STATIC);
        networkOrder.setOrderStateInTestMode(OrderState.CLOSED);

        databaseManager.add(networkOrder);

        // verify
        Assert.assertNull(networkOrder.getInstanceId());

        // exercise
        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.CLOSED);

        // verify
        Assert.assertEquals(0, getListSize(synchronizedDoublyLinkedList));
    }

    // test case: If a closed order has an instance id, it should be recovered.
    @Test
    public void testGetClosedOrderWithInstanceId() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        Order networkOrder = new NetworkOrder(federationUser,
                "requestingMember", "providingMember", "gateway",
                "address", NetworkAllocationMode.STATIC);
        networkOrder.setOrderStateInTestMode(OrderState.CLOSED);
        networkOrder.setInstanceId("instanceId");

        databaseManager.add(networkOrder);

        // verify
        Assert.assertNotNull(networkOrder.getInstanceId());

        // exercise
        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.CLOSED);

        // verify
        Assert.assertEquals(1, getListSize(synchronizedDoublyLinkedList));
    }
}
