package org.fogbowcloud.manager.core.datastore.repository;

import org.fogbowcloud.manager.core.models.orders.Order;
import java.util.List;

public interface OrderRepository {

    List<Order> findByType(String typeValue);

    List<Order> findByOrderState(String state);

}
