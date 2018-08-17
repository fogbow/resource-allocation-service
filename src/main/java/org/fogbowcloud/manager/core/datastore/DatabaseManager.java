package org.fogbowcloud.manager.core.datastore;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.datastore.orderstorage.*;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;

public class DatabaseManager implements StableStorage {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class);
    private static final String ERROR_MASSAGE = "Error instantiating database manager";

    private static DatabaseManager instance;
    
    @Autowired
    private OrderRepository orderRepository;
    
    
    private OrderTimestampStorage orderTimestampStorage;
    
    private DatabaseManager() throws SQLException {
        //this.orderTimestampStorage = new OrderTimestampStorage();
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
        	this.getOrderRepository().save(order);
            this.orderTimestampStorage.addOrder(order);

        } catch (SQLException e) {
            throw new UnexpectedException(e.getMessage());
        }
    }

    @Override
    public void update(Order order) throws UnexpectedException {
        try {
        	this.getOrderRepository().save(order);
            this.orderTimestampStorage.addOrder(order);

        } catch (SQLException e) {
            throw new UnexpectedException(e.getMessage());
        }
    }

    @Override
    public SynchronizedDoublyLinkedList readActiveOrders(OrderState orderState) throws UnexpectedException {
        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList = new SynchronizedDoublyLinkedList();
        
        for (Order order: this.getOrderRepository().findByOrderState(orderState)) {
        	synchronizedDoublyLinkedList.addItem(order);
        }
        
        return synchronizedDoublyLinkedList;
     }
    
    protected OrderRepository getOrderRepository() {
    	return this.orderRepository;
    }
    
    protected void setOrderRepository(OrderRepository orderRepository) {
    	this.orderRepository = orderRepository;
    }
}
