package org.fogbowcloud.manager.core.repository;

import org.fogbowcloud.manager.core.models.orders.Order;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, Long> {

}
