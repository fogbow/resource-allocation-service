package org.fogbowcloud.manager.core.datastore.orderstorage;

import java.util.List;

import javax.transaction.Transactional;

import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
@Transactional
public interface OrderRepository extends JpaRepository<Order, String>{
	
	List<Order> findByOrderState(OrderState Orderstate);

}
