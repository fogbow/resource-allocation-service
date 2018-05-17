package org.fogbowcloud.manager.core.datastore.repository;

import org.fogbowcloud.manager.core.models.orders.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order,String>{

    List<Order> findByType(String typeValue);

    List<Order> findByOrderState(String state);

}
