package org.fogbowcloud.manager.core.datastore.orderstorage;

import java.util.ArrayList;
import java.util.List;

import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

@Service
public class RecoveryService {
	
	public RecoveryService() {
		
	}	
	@Autowired
    private OrderRepository orderRepository;
	

	public List<Order> readActiveOrders(OrderState orderState) {

		return orderRepository.findByOrderState(orderState);
	}

	public Order save(Order order) throws UnexpectedException {
		if (orderRepository.exists(order.getId())) {
			throw new UnexpectedException("Order already exists");
		}
		
		return orderRepository.save(order);
	}
	
	public Order update(Order order) throws UnexpectedException {
		if (!orderRepository.exists(order.getId())) {
			throw new UnexpectedException("Order doesn't exist");
		}
		
		return orderRepository.save(order);
	}
	
	
	
	

}