package org.fogbowcloud.manager.core.controllers;

import java.util.Collection;
import java.util.List;

import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.StorageOrder;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.services.AuthenticationService;
import org.fogbowcloud.manager.core.services.OrdersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class  ApplicationController {

	private static ApplicationController instance;
	private AuthenticationService authenticationService;
	private ManagerController managerController;
	private OrdersManagerController ordersManagerController;
	private OrdersService ordersService;

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationController.class);

	private ApplicationController() {
		this.ordersManagerController = new OrdersManagerController();
		this.ordersService = new OrdersService();
	}

	public static ApplicationController getInstance() {
		synchronized (ApplicationController.class) {
			if (instance == null) {
				instance = new ApplicationController();
			}
			return instance;
		}
	}

	public void allocateComputeOrder(ComputeOrder computeOrder) {
		
	}
	
	public Collection<ComputeOrder> findAllComputeOrder() {
		return null;		
	}
	
	public ComputeOrder findComputeOrderById(String id) {
		return null;		
	}
	
	public void removeComputeOrder(String id) {
		
	}
	
	public void allocateNetworkOrder(NetworkOrder networkOrder) {
		
	}
	
	public Collection<NetworkOrder> findAllNetworkOrder() {
		return null;		
	}
	
	public NetworkOrder findNetworkOrderById(String id) {
		return null;		
	}
	
	public void removeNetworkOrder(String id) {
		
	}
	
	public void allocateStorageOrder(StorageOrder storageOrder) {
		
	}
	
	public Collection<StorageOrder> findAllStorageOrder() {
		return null;		
	}
	
	public StorageOrder findStorageOrderById(String id) {
		return null;		
	}
	
	public void removeStorageOrder(String id) {
		
	}

	public AuthenticationService getAuthenticationController() {
		return authenticationService;
	}

	public void setAuthenticationController(AuthenticationService authenticationController) {
		this.authenticationService = authenticationController;
	}

	public ManagerController getManagerController() {
		return managerController;
	}

	public void setManagerController(ManagerController managerController) {
		this.managerController = managerController;
	}

	public List<Order> getAllComputes(String accessId, OrderType orderType) throws UnauthorizedException, UnauthenticatedException {
		this.authenticationService.authenticateAndAuthorize(accessId);
		Token federationToken = this.authenticationService.getFederationToken(accessId);
		return this.ordersService.getAllOrdersByType(federationToken, orderType);
	}

	public Order getOrderById(String id, String accessId, OrderType orderType) throws UnauthorizedException, UnauthenticatedException {
		this.authenticationService.authenticateAndAuthorize(accessId);
		Token federationToken = this.authenticationService.getFederationToken(accessId);
		return this.ordersService.getOrderByIdAndType(id, federationToken, orderType);
	}

	public void newOrderRequest(Order order, String accessId, String localTokenId) 
				throws OrderManagementException, UnauthorizedException, UnauthenticatedException, Exception {		
		this.authenticationService.authenticateAndAuthorize(accessId);
		Token federationToken = this.authenticationService.getFederationToken(accessId);
		String providingMember = order.getProvidingMember();
		Token localToken = this.authenticationService.getLocalToken(localTokenId, providingMember);
		this.ordersManagerController.newOrderRequest(order, federationToken, localToken);
	}

}
