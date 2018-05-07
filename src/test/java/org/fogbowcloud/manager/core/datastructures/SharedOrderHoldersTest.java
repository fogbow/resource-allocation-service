package org.fogbowcloud.manager.core.datastructures;

import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.token.Token;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class SharedOrderHoldersTest {

	private SharedOrderHolders instanceOne;
	private SharedOrderHolders instanceTwo;

	@Before
	public void initialize() {
		this.instanceOne = SharedOrderHolders.getInstance();
		this.instanceTwo = SharedOrderHolders.getInstance();
	}

	@Test
	public void testGetSameListReference() {
		SynchronizedDoublyLinkedList listFromInstanceOne = instanceOne.getOpenOrdersList();
		SynchronizedDoublyLinkedList listFromInstanceTwo = instanceTwo.getOpenOrdersList();
		assertEquals(listFromInstanceOne, listFromInstanceTwo);

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
		Token localToken = Mockito.mock(Token.class);
		Token federationToken = Mockito.mock(Token.class);
		UserData userData = Mockito.mock(UserData.class);
		String imageName = "fake-image-name";
		String requestingMember = String.valueOf("local-member");
		String providingMember = String.valueOf("local-member");
		String publicKey = "fake-public-key";
		Order order = new ComputeOrder(orderId, localToken, federationToken, requestingMember, providingMember, 8, 1024,
				30, imageName, userData, publicKey);
		return order;
	}

}