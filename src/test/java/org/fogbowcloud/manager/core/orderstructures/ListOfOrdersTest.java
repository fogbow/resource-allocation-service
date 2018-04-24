package org.fogbowcloud.manager.core.orderstructures;

import org.fogbowcloud.manager.core.models.linkedList.Node;
import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class ListOfOrdersTest {

    private ListOfOpenOrders instanceOne;
    private ListOfOpenOrders instanceTwo;

    @Before
    public void initialize() {
        this.instanceOne = ListOfOpenOrders.getInstance();
        this.instanceTwo = ListOfOpenOrders.getInstance();
    }

    @Test
    public void testGetSameListReference() {

        SynchronizedDoublyLinkedList listFromInstanceOne = instanceOne.getOrdersList();
        SynchronizedDoublyLinkedList listFromInstanceTwo = instanceTwo.getOrdersList();

        Order orderOne = createOrder("one");
        listFromInstanceOne.addItem(orderOne);

        assertEquals(listFromInstanceOne.getCurrent(), listFromInstanceTwo.getCurrent());
        assertEquals(orderOne, listFromInstanceOne.getCurrent().getOrder());
        assertEquals(orderOne, listFromInstanceTwo.getCurrent().getOrder());

        Order orderTwo = createOrder("two");
        listFromInstanceTwo.addItem(orderTwo);

        assertEquals(listFromInstanceOne.getCurrent().getNext(), listFromInstanceTwo.getCurrent().getNext());
        assertEquals(orderTwo, listFromInstanceOne.getCurrent().getNext().getOrder());
        assertEquals(orderTwo, listFromInstanceTwo.getCurrent().getNext().getOrder());

    }

    private Order createOrder(String orderId) {
        Order order = new ComputeOrder();
        order.setId(orderId);
        return order;
    }

}
