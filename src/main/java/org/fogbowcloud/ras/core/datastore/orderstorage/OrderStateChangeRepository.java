package org.fogbowcloud.ras.core.datastore.orderstorage;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStateChangeRepository extends JpaRepository<AuditedOrderStateChange, String> {
}
