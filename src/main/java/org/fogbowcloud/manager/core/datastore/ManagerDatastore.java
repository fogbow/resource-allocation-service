package org.fogbowcloud.manager.core.datastore;

import java.util.Collection;
import java.util.List;

import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.StorageOrder;
import org.fogbowcloud.manager.core.models.token.Token;

public interface ManagerDatastore {
	
	@Deprecated
	public void addOrder(Order order);
	
	public List<Order> getAllOrders();
	
	public Order getOrdersByUserId(Long id);
	
	public Order getOrdersByToken(Token token);
	
	public List<Order> getOrderByState(OrderState orderState);	
	
	@Deprecated
	public void updateOrder(Order order);
	
	@Deprecated
	public void deleteOrder(Order order);
	
	public void allocateComputeOrder(ComputeOrder computeOrder);
	
	public Collection<ComputeOrder> findAllComputeOrder();
	
	public ComputeOrder findComputeOrderById(Integer id);
	
	public void removeComputeOrder(Integer id);
	
	public void allocateNetworkOrder(NetworkOrder networkOrder);
	
	public Collection<NetworkOrder> findAllNetworkOrder();
	
	public NetworkOrder findNetworkOrderById(Integer id);
	
	public void removeNetworkOrder(Integer id);
	
	public void allocateStorageOrder(StorageOrder storageOrder);
	
	public Collection<StorageOrder> findAllStorageOrder();
	
	public StorageOrder findStorageOrderById(Integer id);
	
	public void removeStorageOrder(Integer id);
	
}