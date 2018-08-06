package org.fogbowcloud.manager.core.datastore;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.datastore.orderstorage.*;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;

import java.sql.SQLException;

public class DatabaseManager implements StableStorage {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class);
    private static final String ERROR_MASSAGE = "Error instantiating database manager";

    private static DatabaseManager instance;

    private ComputeOrderStorage computeOrderStorage;
    private NetworkOrderStorage networkOrderStorage;
    private VolumeOrderStorage volumeOrderStorage;
    private AttachmentOrderStorage attachmentOrderStorage;
    private OrderTimestampStorage orderTimestampStorage;

    private DatabaseManager() throws SQLException {
        this.computeOrderStorage = new ComputeOrderStorage();
        this.networkOrderStorage = new NetworkOrderStorage();
        this.volumeOrderStorage = new VolumeOrderStorage();
        this.attachmentOrderStorage = new AttachmentOrderStorage();
        this.orderTimestampStorage = new OrderTimestampStorage();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            try {
                instance = new DatabaseManager();
            } catch (SQLException e) {
                LOGGER.error(ERROR_MASSAGE, e);
                throw new FatalErrorException(ERROR_MASSAGE, e);
            }
        }

        return instance;
    }

    @Override
    public void add(Order order) throws UnexpectedException {
        try {
            switch (order.getType()) {
                case COMPUTE:
                    this.computeOrderStorage.addOrder(order);
                    break;
                case NETWORK:
                    this.networkOrderStorage.addOrder(order);
                    break;
                case VOLUME:
                    this.volumeOrderStorage.addOrder(order);
                    break;
                case ATTACHMENT:
                    this.attachmentOrderStorage.addOrder(order);
                    break;
            }

            this.orderTimestampStorage.addOrder(order);

        } catch (SQLException e) {
            throw new UnexpectedException(e.getMessage());
        }
    }

    @Override
    public void update(Order order) throws UnexpectedException {
        try {
            switch (order.getType()) {
                case COMPUTE:
                    this.computeOrderStorage.updateOrder(order);
                    break;
                case NETWORK:
                    this.networkOrderStorage.updateOrder(order);
                    break;
                case VOLUME:
                    this.volumeOrderStorage.updateOrder(order);
                    break;
                case ATTACHMENT:
                    this.attachmentOrderStorage.updateOrder(order);
                    break;
            }

            this.orderTimestampStorage.addOrder(order);

        } catch (SQLException e) {
            throw new UnexpectedException(e.getMessage());
        }
    }

    @Override
    public SynchronizedDoublyLinkedList readActiveOrders(OrderState orderState) throws UnexpectedException {
        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList = new SynchronizedDoublyLinkedList();

        try {
            this.computeOrderStorage.readOrdersByState(orderState, synchronizedDoublyLinkedList);
            this.networkOrderStorage.readOrdersByState(orderState, synchronizedDoublyLinkedList);
            this.volumeOrderStorage.readOrdersByState(orderState, synchronizedDoublyLinkedList);
            this.attachmentOrderStorage.readOrdersByState(orderState, synchronizedDoublyLinkedList);
        } catch (SQLException e) {
            throw new UnexpectedException(e.getMessage());
        }

        return synchronizedDoublyLinkedList;
    }
}
