package cloud.fogbow.ras.core.datastore.services;

import cloud.fogbow.common.datastore.FogbowDatabaseService;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.core.datastore.orderstorage.AuditableOrderStateChange;
import cloud.fogbow.ras.core.datastore.orderstorage.OrderStateChangeRepository;
import cloud.fogbow.ras.core.models.orders.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class AuditableOrderStateChangeService extends FogbowDatabaseService<AuditableOrderStateChange> {
    @Autowired
    private OrderStateChangeRepository orderTimestampRepository;

    public void registerStateChange(Order order) throws UnexpectedException {
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        AuditableOrderStateChange auditableOrderStateChange = new AuditableOrderStateChange(currentTimestamp, order, order.getOrderState());
        this.orderTimestampRepository.save(auditableOrderStateChange);
        safeSave(auditableOrderStateChange, this.orderTimestampRepository);
    }
}
