package dev.jahlopez.productcatalog.repository;

import dev.jahlopez.productcatalog.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {

    /**
     * Executes fuzzy full-text search across product name and description fields.
     */
    Page<ProductDocument> findByNameContainingOrDescriptionContaining(String name, String description, Pageable pageable);
}