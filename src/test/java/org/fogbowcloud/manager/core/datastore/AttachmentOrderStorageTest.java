package org.fogbowcloud.manager.core.datastore;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AttachmentOrderStorageTest extends DatabaseManagerTest {

    // test case: Tests if a new attachment order is added properly in the database.
    @Test
    public void testAddAttachmentOrder() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        Order attachmentOrder = new AttachmentOrder(federationUser, "requestingMember",
                "providingMember", "source", "target", "device");
        attachmentOrder.setOrderStateInTestMode(OrderState.OPEN);

        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.OPEN);

        // verify
        Assert.assertEquals(0, getListSize(synchronizedDoublyLinkedList));

        // exercise
        databaseManager.add(attachmentOrder);

        synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.OPEN);

        // verify
        Assert.assertEquals(1, getListSize(synchronizedDoublyLinkedList));
    }

    // test case: Try to add the same attachment order twice
    @Test(expected = UnexpectedException.class)
    public void testAddExistingAttachmentOrder() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        Order attachmentOrder = new AttachmentOrder(federationUser, "requestingMember",
                "providingMember", "source", "target", "device");
        attachmentOrder.setOrderStateInTestMode(OrderState.OPEN);

        // exercise
        databaseManager.add(attachmentOrder);

        // add the same order twice
        databaseManager.add(attachmentOrder);
    }

    // test case: Tests if a stored attachment order is updated properly in the database.
    @Test
    public void testUpdateAttachmentOrderState() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        Order attachmentOrder = new AttachmentOrder(federationUser, "requestingMember",
                "providingMember", "source", "target", "device");
        attachmentOrder.setOrderStateInTestMode(OrderState.OPEN);

        databaseManager.add(attachmentOrder);

        attachmentOrder.setOrderStateInTestMode(OrderState.FULFILLED);

        // exercise
        databaseManager.update(attachmentOrder);

        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.FULFILLED);

        AttachmentOrder result = (AttachmentOrder) synchronizedDoublyLinkedList.getNext();

        // verify
        Assert.assertEquals(OrderState.FULFILLED, result.getOrderState());
        Assert.assertEquals("source", result.getSource());
        Assert.assertEquals("target", result.getTarget());
        Assert.assertEquals("device", result.getDevice());
    }

    // test case: Try to update a nonexisting order.
    @Test(expected = UnexpectedException.class)
    public void testUpdateNonexistingAttachmentOrder() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        Order attachmentOrder = new AttachmentOrder(federationUser, "requestingMember",
                "providingMember", "source", "target", "device");
        attachmentOrder.setOrderStateInTestMode(OrderState.FULFILLED);

        // exercise
        databaseManager.update(attachmentOrder);
    }

    // test case: If a closed order do not have an instance id, it should not be recovered.
    @Test
    public void testGetClosedOrderWithoutInstanceId() throws InvalidParameterException, UnexpectedException {
        // set up
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "fake-name");
        FederationUser federationUser = new FederationUser("fake-user", attributes);

        Order attachmentOrder = new AttachmentOrder(federationUser, "requestingMember",
                "providingMember", "source", "target", "device");
        attachmentOrder.setOrderStateInTestMode(OrderState.CLOSED);

        databaseManager.add(attachmentOrder);

        // verify
        Assert.assertNull(attachmentOrder.getInstanceId());

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

        Order attachmentOrder = new AttachmentOrder(federationUser, "requestingMember",
                "providingMember", "source", "target", "device");
        attachmentOrder.setOrderStateInTestMode(OrderState.CLOSED);
        attachmentOrder.setInstanceId("instanceId");

        databaseManager.add(attachmentOrder);

        // verify
        Assert.assertNotNull(attachmentOrder.getInstanceId());

        // exercise
        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList =
                databaseManager.readActiveOrders(OrderState.CLOSED);

        // verify
        Assert.assertEquals(1, getListSize(synchronizedDoublyLinkedList));
    }
}
