package org.fogbowcloud.manager.core.datastore;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.*;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.plugins.cloud.util.CloudInitUserDataBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ComputeOrderStorageTest extends DatabaseManagerTest {

    // test case: Tests if a new compute order is added properly in the database.
    @Test(expected = UnexpectedException.class)
    public void testAddExistingComputeOrder() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        Order computeOrder = new ComputeOrder(federationUser,
                "requestingMember", "providingMember", 8, 1024,
                30, "fake_image_name", new UserData("extraUserDataFile",
                CloudInitUserDataBuilder.FileType.CLOUD_CONFIG), "fake_public_key", null);
        computeOrder.setOrderStateInTestMode(OrderState.OPEN);

        // exercise
        databaseManager.add(computeOrder);

        databaseManager.add(computeOrder);
    }

    // test case: Tests if a new compute order is added properly in the database.
    @Test
    public void testAddComputeOrder() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        Order computeOrder = new ComputeOrder(federationUser,
                "requestingMember", "providingMember", 8, 1024,
                30, "fake_image_name", new UserData("extraUserDataFile",
                CloudInitUserDataBuilder.FileType.CLOUD_CONFIG), "fake_public_key", null);
        computeOrder.setOrderStateInTestMode(OrderState.OPEN);

        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.OPEN);

        // verify
        Assert.assertEquals(0, getListSize(synchronizedDoublyLinkedList));

        // exercise
        databaseManager.add(computeOrder);

        synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.OPEN);

        // verify
        Assert.assertEquals(1, getListSize(synchronizedDoublyLinkedList));
    }

    // test case: Tests if a stored compute order is updated properly in the database.
    @Test
    public void testUpdateComputeOrderState() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        ComputeOrder computeOrder = new ComputeOrder("id", federationUser,
                "requestingMember", "providingMember", 8, 1024,
                30, "fake_image_name", new UserData("extraUserDataFile",
                CloudInitUserDataBuilder.FileType.CLOUD_CONFIG), "fake_public_key", null);
        computeOrder.setOrderStateInTestMode(OrderState.OPEN);

        databaseManager.add(computeOrder);

        computeOrder.setOrderStateInTestMode(OrderState.FULFILLED);
        computeOrder.setActualAllocation(new ComputeAllocation(10, 10,10));

        // exercise
        databaseManager.update(computeOrder);

        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.FULFILLED);

        ComputeOrder result = (ComputeOrder) synchronizedDoublyLinkedList.getNext();

        // verify
        Assert.assertEquals(result.getOrderState(), OrderState.FULFILLED);
        Assert.assertEquals(result.getActualAllocation().getRam(), 10);
        Assert.assertEquals(result.getActualAllocation().getvCPU(), 10);
        Assert.assertEquals(result.getActualAllocation().getInstances(), 10);
    }

    // test case: Try to update a nonexisting order.
    @Test(expected = UnexpectedException.class)
    public void testUpdateNonexistingComputeOrder() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        ComputeOrder computeOrder = new ComputeOrder("id", federationUser,
                "requestingMember", "providingMember", 8, 1024,
                30, "fake_image_name", new UserData("extraUserDataFile",
                CloudInitUserDataBuilder.FileType.CLOUD_CONFIG), "fake_public_key", null);
        computeOrder.setOrderStateInTestMode(OrderState.FULFILLED);

        // exercise
        databaseManager.update(computeOrder);
    }

    // test case: If a closed order do not have an instance id, it should not be recovered.
    @Test
    public void testGetClosedOrderWithoutInstanceId() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        ComputeOrder computeOrder = new ComputeOrder("id", federationUser,
                "requestingMember", "providingMember", 8, 1024,
                30, "fake_image_name", new UserData("extraUserDataFile",
                CloudInitUserDataBuilder.FileType.CLOUD_CONFIG), "fake_public_key", null);
        computeOrder.setOrderStateInTestMode(OrderState.CLOSED);

        databaseManager.add(computeOrder);

        // verify
        Assert.assertNull(computeOrder.getInstanceId());

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

        ComputeOrder computeOrder = new ComputeOrder("id", federationUser,
                "requestingMember", "providingMember", 8, 1024,
                30, "fake_image_name", new UserData("extraUserDataFile",
                CloudInitUserDataBuilder.FileType.CLOUD_CONFIG), "fake_public_key", null);
        computeOrder.setOrderStateInTestMode(OrderState.CLOSED);
        computeOrder.setInstanceId("instanceId");

        databaseManager.add(computeOrder);

        // verify
        Assert.assertNotNull(computeOrder.getInstanceId());

        // exercise
        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.CLOSED);

        // verify
        Assert.assertEquals(1, getListSize(synchronizedDoublyLinkedList));
    }
}
