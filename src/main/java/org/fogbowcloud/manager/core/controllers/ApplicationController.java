package org.fogbowcloud.manager.core.controllers;

import java.util.Collection;

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
	
	public ComputeOrder findComputeOrderById(Integer id) {
		return null;		
	}
	
	public void removeComputeOrder(Integer id) {
		
	}
	
	public void allocateNetworkOrder(NetworkOrder networkOrder) {
		
	}
	
	public Collection<NetworkOrder> findAllNetworkOrder() {
		return null;		
	}
	
	public NetworkOrder findNetworkOrderById(Integer id) {
		return null;		
	}
	
	public void removeNetworkOrder(Integer id) {
		
	}
	
	public void allocateStorageOrder(StorageOrder storageOrder) {
		
	}
	
	public Collection<StorageOrder> findAllStorageOrder() {
		return null;		
	}
	
	public StorageOrder findStorageOrderById(Integer id) {
		return null;		
	}
	
	public void removeStorageOrder(Integer id) {
		
	}	
}
