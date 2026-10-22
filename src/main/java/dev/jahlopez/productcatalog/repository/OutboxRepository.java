package dev.jahlopez.productcatalog.repository;

import dev.jahlopez.productcatalog.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing persistent {@link OutboxEvent} records
 * within the transactional outbox pattern.
 */
@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Finds all outbox events that have not yet been processed or published.
     * Useful if implementing a scheduled outbox publisher poller.
     *
     * @param processed publication status flag
     * @return list of unprocessed outbox events
     */
    List<OutboxEvent> findByProcessed(boolean processed);
}