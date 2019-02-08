package cloud.fogbow.ras.core.datastore;

import cloud.fogbow.ras.core.datastore.orderstorage.AuditableOrderStateChange;
import cloud.fogbow.ras.core.datastore.orderstorage.AuditableRequestsRepository;
import cloud.fogbow.ras.core.datastore.orderstorage.OrderStateChangeRepository;
import cloud.fogbow.ras.core.models.auditing.AuditableRequest;
import cloud.fogbow.ras.core.models.orders.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class AuditService {

    @Autowired
    private OrderStateChangeRepository orderTimestampRepository;

    @Autowired
    private AuditableRequestsRepository auditableRequestsRepository;

    public void registerStateChange(Order order) {
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        AuditableOrderStateChange auditableOrderStateChange = new AuditableOrderStateChange(currentTimestamp, order, order.getOrderState());
        this.orderTimestampRepository.save(auditableOrderStateChange);
    }

    public void registerSyncRequest(AuditableRequest request) {
        this.auditableRequestsRepository.save(request);
    }
}
