package org.fogbowcloud.ras.core.datastore.orderstorage;

import org.fogbowcloud.ras.core.models.orders.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class AuditService {

    @Autowired
    private OrderStateChangeRepository orderTimestampRepository;

    public void updateStateTimestamp(Order order) {
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        OrderStateChange orderStateChange = new OrderStateChange(currentTimestamp, order, order.getOrderState());
        this.orderTimestampRepository.save(orderStateChange);
    }

}
