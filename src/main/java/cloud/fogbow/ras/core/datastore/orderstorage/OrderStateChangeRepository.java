package cloud.fogbow.ras.core.datastore.orderstorage;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStateChangeRepository extends JpaRepository<AuditableOrderStateChange, String> {
}
