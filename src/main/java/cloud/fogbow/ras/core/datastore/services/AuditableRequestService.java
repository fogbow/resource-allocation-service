package cloud.fogbow.ras.core.datastore.services;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.core.datastore.orderstorage.AuditableRequestsRepository;
import cloud.fogbow.ras.core.models.auditing.AuditableRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditableRequestService extends FogbowDatabaseService<AuditableRequest> {
    @Autowired
    private AuditableRequestsRepository auditableRequestsRepository;

    public void registerSyncRequest(AuditableRequest request) throws UnexpectedException {
        safeSave(request, this.auditableRequestsRepository);
    }
}