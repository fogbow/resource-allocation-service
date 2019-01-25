package org.fogbowcloud.ras.core.datastore.orderstorage;

import org.fogbowcloud.ras.core.models.auditing.AuditableSyncRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncRequestRepository extends JpaRepository<AuditableSyncRequest, String> {
}
