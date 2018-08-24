package org.fogbowcloud.manager.core.datastore.orderstorage;

import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
@Transactional
public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByOrderState(OrderState Orderstate);

}
