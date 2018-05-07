package org.fogbowcloud.manager.core.controllers;

import java.util.Collection;

import org.fogbowcloud.manager.core.ManagerController;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.StorageOrder;
import org.fogbowcloud.manager.core.services.AuthenticationService;

public class ApplicationController {
	
	private AuthenticationService authenticationController;
	private ManagerController managerController;
	
	public ApplicationController(AuthenticationService authenticationController, ManagerController managerController) {
		this.authenticationController = authenticationController;
		this.managerController = managerController;
	}

	public ApplicationController() {}

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
	
}
