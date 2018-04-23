package org.fogbowcloud.manager.core.controllers;

import java.util.Collection;

import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.services.AuthenticationService;

public class ApplicationController {
	
	private AuthenticationService authenticationController;
	private ManagerController managerController;
	
	public ApplicationController(AuthenticationService authenticationController, ManagerController managerController) {
		this.authenticationController = authenticationController;
		this.managerController = managerController;
	}

	public ApplicationController() {}

	public void allocateOrder(Order order) {
		
	}
	
	public Collection<Order> findAllOrders() {
		return null;		
	}
	
	public Order findOrder(String id) {
		return null;		
	}
	
	public void removeOrder(String id) {
		
	}
		
}
