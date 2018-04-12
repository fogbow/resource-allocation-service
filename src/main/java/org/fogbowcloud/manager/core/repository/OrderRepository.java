package org.fogbowcloud.manager.core.repository;

import org.fogbowcloud.manager.core.models.orders.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OrderRepository extends CrudRepository<Order, Long> {

    @Query("select o from Order o where dtype = ?1")
    List<Order> findByType(String typeValue);
}
