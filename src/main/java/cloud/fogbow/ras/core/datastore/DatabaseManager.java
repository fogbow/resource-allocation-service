package cloud.fogbow.ras.core.datastore;

import cloud.fogbow.common.exceptions.UnexpectedException;
import org.apache.log4j.Logger;
import cloud.fogbow.ras.core.models.auditing.AuditableRequest;
import cloud.fogbow.ras.core.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;

public class DatabaseManager implements StableStorage {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class);

    private static DatabaseManager instance;

    private RecoveryService recoveryService;
    private AuditService auditService;

    private DatabaseManager() {
    }

    public synchronized static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    @Override
    public void add(Order order) throws UnexpectedException {
        this.recoveryService.save(order);
        this.auditService.registerStateChange(order);
    }

    @Override
    public void update(Order order, boolean orderStateChanged) throws UnexpectedException {
        this.recoveryService.update(order);
        if (orderStateChanged) {
            this.auditService.registerStateChange(order);
        }
    }

    @Override
    public SynchronizedDoublyLinkedList readActiveOrders(OrderState orderState) throws UnexpectedException {

        SynchronizedDoublyLinkedList synchronizedDoublyLinkedList = new SynchronizedDoublyLinkedList();

        for (Order order : this.recoveryService.readActiveOrders(orderState)) {
            synchronizedDoublyLinkedList.addItem(order);
        }
        return synchronizedDoublyLinkedList;
    }

    public void update(Order order) throws UnexpectedException {
        update(order, true);
    }

    public void setRecoveryService(RecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    public void setAuditService(AuditService auditService) {
        this.auditService = auditService;
    }

    public void auditRequest(AuditableRequest request) {
        this.auditService.registerSyncRequest(request);
    }
}
