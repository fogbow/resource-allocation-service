package org.fogbowcloud.manager.core.datastore.repository;

import org.fogbowcloud.manager.core.models.OrderTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderTransitionRepository extends JpaRepository<OrderTransition, Long>{

}
