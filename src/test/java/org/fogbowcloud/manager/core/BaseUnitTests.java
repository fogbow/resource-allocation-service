package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.models.linkedlist.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.token.Token;
import org.junit.After;
import org.mockito.Mockito;

public class BaseUnitTests {

	public static final String LOCAL_MEMBER_ID = "fake-local-member";

	@After
	public void tearDown() {
		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		for (OrderState state : OrderState.values()) {
			SynchronizedDoublyLinkedList ordersList = sharedOrderHolders.getOrdersList(state);
			cleanList(ordersList);
		}
	}

	protected void cleanList(ChainedList list) {
		list.resetPointer();
		Order order = null;
		do {
			order = list.getNext();
			if (order != null) {
				list.removeItem(order);
			}
		} while (order != null);
		list.resetPointer();
	}

	protected boolean isEmpty(ChainedList list) {
		list.resetPointer();
		return list.getNext() == null;
	}

	protected String getLocalMemberId() {
		return LOCAL_MEMBER_ID;
	}

	protected Order createLocalOrder(String requestingMember) {
		String providingMember = requestingMember;
		return createOrder(requestingMember, providingMember);
	}

	protected Order createRemoteOrder(String requestingMember) {
		String providingMember = "fake-remote-member";
		return createOrder(requestingMember, providingMember);
	}

	private Order createOrder(String requestingMember, String providingMember) {
		Token localToken = Mockito.mock(Token.class);
		Token federationToken = Mockito.mock(Token.class);
		UserData userData = Mockito.mock(UserData.class);
		String imageName = "fake-image-name";
		String publicKey = "fake-public-key";

		Order localOrder = new ComputeOrder(localToken, federationToken, requestingMember, providingMember, 8, 1024, 30,
				imageName, userData, publicKey);
		return localOrder;
	}
}
