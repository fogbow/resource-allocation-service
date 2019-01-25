package org.fogbowcloud.ras.core.datastore;

import org.fogbowcloud.ras.core.datastore.orderstorage.AuditedOrderStateChange;
import org.fogbowcloud.ras.core.datastore.orderstorage.OrderStateChangeRepository;
import org.fogbowcloud.ras.core.datastore.orderstorage.SyncRequestRepository;
import org.fogbowcloud.ras.core.models.auditing.AuditableSyncRequest;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class AuditService {

    @Autowired
    private OrderStateChangeRepository orderTimestampRepository;

    @Autowired
    private SyncRequestRepository syncRequestRepository;

    public void registerStateChange(Order order) {
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        AuditedOrderStateChange auditedOrderStateChange = new AuditedOrderStateChange(currentTimestamp, order, order.getOrderState());
        this.orderTimestampRepository.save(auditedOrderStateChange);
    }

    public void registerSyncRequest(AuditableSyncRequest request) {
        this.syncRequestRepository.save(request);
    }
}
