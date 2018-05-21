package org.fogbowcloud.manager.core.datastore.repository;

import java.util.List;
import org.fogbowcloud.manager.core.models.orders.Order;

public interface OrderRepository {

    List<Order> findByType(String typeValue);

    List<Order> findByOrderState(String state);
}
