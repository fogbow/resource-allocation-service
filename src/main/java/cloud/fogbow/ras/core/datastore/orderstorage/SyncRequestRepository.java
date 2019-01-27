package cloud.fogbow.ras.core.datastore.orderstorage;

import cloud.fogbow.ras.core.models.auditing.AuditableSyncRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncRequestRepository extends JpaRepository<AuditableSyncRequest, String> {
}
