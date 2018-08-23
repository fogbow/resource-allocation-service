package org.fogbowcloud.manager.core.datastore;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.datastore.orderstorage.RecoveryService;
import org.fogbowcloud.manager.core.datastore.orderstorage.OrderTimestampStorage;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;

public class DatabaseManager implements StableStorage {

	private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class);
	private static final String ERROR_MESSAGE = "Error instantiating database manager";

	private static DatabaseManager instance;

	private RecoveryService recoveryService;

	private OrderTimestampStorage orderTimestampStorage;

	private DatabaseManager() throws SQLException {
		this.orderTimestampStorage = new OrderTimestampStorage();
	}

	public static DatabaseManager getInstance() {
		if (instance == null) {
			try {
				instance = new DatabaseManager();
			} catch (SQLException e) {
				LOGGER.error(ERROR_MESSAGE, e);
				throw new FatalErrorException(ERROR_MESSAGE, e);
			}
		}

		return instance;
	}

	@Override
	public void add(Order order) throws UnexpectedException {
		System.out.println("DB Manager repo add");
		try {
			this.recoveryService.save(order);
			this.orderTimestampStorage.addOrder(order);

		} catch (SQLException e) {
			throw new UnexpectedException(e.getMessage());
		}
	}

	@Override
	public void update(Order order) throws UnexpectedException {
		try {
			this.recoveryService.update(order);
			this.orderTimestampStorage.addOrder(order);

		} catch (SQLException e) {
			throw new UnexpectedException(e.getMessage());
		}
	}

	@Override
	public SynchronizedDoublyLinkedList readActiveOrders(OrderState orderState) throws UnexpectedException {

		SynchronizedDoublyLinkedList synchronizedDoublyLinkedList = new SynchronizedDoublyLinkedList();

		for (Order order : this.recoveryService.readActiveOrders(orderState)) {
			synchronizedDoublyLinkedList.addItem(order);
		}

		return synchronizedDoublyLinkedList;
	}

	public void setRecoveryService(RecoveryService recoveryService) {
		this.recoveryService = recoveryService;
	}
}
