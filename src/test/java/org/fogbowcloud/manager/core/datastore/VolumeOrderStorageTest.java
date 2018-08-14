package org.fogbowcloud.manager.core.datastore;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class VolumeOrderStorageTest extends DatabaseManagerTest {

    // test case: Tests if a new volume order is added properly in the database.
    @Test(expected = UnexpectedException.class)
    public void testAddExistingVolumeOrder() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        FederationUserToken federationUserToken = new FederationUserToken("fake-token-provider",
                "fake-federation-token-value", "fake-user-id", "fake-user-name");

        Order volumeOrder = new VolumeOrder(federationUserToken,
                "requestingMember", "providingMember", 0, "volume-name");
        volumeOrder.setOrderStateInTestMode(OrderState.OPEN);

        // exercise
        databaseManager.add(volumeOrder);

        databaseManager.add(volumeOrder);
    }

    // test case: Tests if a new volume order is added properly in the database.
    @Test
    public void testAddVolumeOrder() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        FederationUserToken federationUserToken = new FederationUserToken("fake-token-provider",
                "fake-federation-token-value", "fake-user-id", "fake-user-name");

        Order volumeOrder = new VolumeOrder(federationUserToken,
                "requestingMember", "providingMember", 0, "volume-name");
        volumeOrder.setOrderStateInTestMode(OrderState.OPEN);

        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.OPEN);

        // verify
        Assert.assertEquals(0, getListSize(synchronizedDoublyLinkedList));

        // exercise
        databaseManager.add(volumeOrder);

        synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.OPEN);

        // verify
        Assert.assertEquals(1, getListSize(synchronizedDoublyLinkedList));
    }

    // test case: Tests if a stored volume order is updated properly in the database.
    @Test
    public void testUpdateVolumeOrderState() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        FederationUserToken federationUserToken = new FederationUserToken("fake-token-provider",
                "fake-federation-token-value", "fake-user-id", "fake-user-name");

        Order volumeOrder = new VolumeOrder(federationUserToken,
                "requestingMember", "providingMember", 0, "volume-name");
        volumeOrder.setOrderStateInTestMode(OrderState.OPEN);

        databaseManager.add(volumeOrder);

        volumeOrder.setOrderStateInTestMode(OrderState.FULFILLED);

        // exercise
        databaseManager.update(volumeOrder);

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
        FederationUserToken federationUserToken = new FederationUserToken("fake-token-provider",
                "fake-federation-token-value", "fake-user-id", "fake-user-name");

        Order volumeOrder = new VolumeOrder(federationUserToken,
                "requestingMember", "providingMember", 0, "volume-name");
        volumeOrder.setOrderStateInTestMode(OrderState.FULFILLED);

        // exercise
        databaseManager.update(volumeOrder);
    }

    // test case: If a closed order do not have an instance id, it should not be recovered.
    @Test
    public void testGetClosedOrderWithoutInstanceId() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        FederationUserToken federationUserToken = new FederationUserToken("fake-token-provider",
                "fake-federation-token-value", "fake-user-id", "fake-user-name");

        Order volumeOrder = new VolumeOrder(federationUserToken,
                "requestingMember", "providingMember", 0, "volume-name");
        volumeOrder.setOrderStateInTestMode(OrderState.CLOSED);

        databaseManager.add(volumeOrder);

        // verify
        Assert.assertNull(volumeOrder.getInstanceId());

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
        FederationUserToken federationUserToken = new FederationUserToken("fake-token-provider",
                "fake-federation-token-value", "fake-user-id", "fake-user-name");

        VolumeOrder volumeOrder = new VolumeOrder(federationUserToken,
                "requestingMember", "providingMember", 0, "volume-name");
        volumeOrder.setOrderStateInTestMode(OrderState.CLOSED);
        volumeOrder.setInstanceId("instanceId");

        databaseManager.add(volumeOrder);

        // verify
        Assert.assertNotNull(volumeOrder.getInstanceId());

        // exercise
        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.CLOSED);

        // verify
        Assert.assertEquals(1, getListSize(synchronizedDoublyLinkedList));
    }
}
