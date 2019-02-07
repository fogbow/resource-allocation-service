package cloud.fogbow.ras.core.datastore.orderstorage;

import cloud.fogbow.ras.core.models.auditing.AuditableRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditableRequestsRepository extends JpaRepository<AuditableRequest, String> {
}
