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
     * Finds outbox rows by their {@code processed} flag. The current CDC path publishes
     * from inserted rows and does not use this flag as a delivery acknowledgment; a query
     * result therefore does not establish whether an event reached Kafka.
     *
     * @param processed the value to match in the persisted flag
     * @return outbox rows whose flag matches {@code processed}
     */
    List<OutboxEvent> findByProcessed(boolean processed);
}